package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.backupRun.BackupRun;
import ru.bssg.lottabyte.core.model.backupRun.UpdatableBackupRunEntity;
import ru.bssg.lottabyte.core.model.externalGroup.ExternalGroup;
import ru.bssg.lottabyte.core.model.ldapProperty.LdapProperty;
import ru.bssg.lottabyte.core.model.task.TaskState;
import ru.bssg.lottabyte.core.model.tenant.Tenant;
import ru.bssg.lottabyte.core.model.tenant.TenantValue;
import ru.bssg.lottabyte.core.ui.model.SystemType;
import ru.bssg.lottabyte.core.usermanagement.model.UpdatableUserDetails;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.model.UserRole;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.repository.AdminRepository;
import ru.bssg.lottabyte.coreapi.repository.UserRepository;

import java.io.*;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {
    private final AdminRepository adminRepository;
    private final BackupRunService backupRunService;
    private final ElasticsearchService elasticsearchService;
    private final UserService userService;
    private final SystemService systemService;
    private final UserRepository userRepository;
    private final VersionService versionService;
    private final TenantService tenantService;
    private BufferedWriter writer;
    @Value("${system.data.storage}")
    private String DATA_STORAGE;
    @Value("${spring.datasource.host}")
    private String HOST;
    @Value("${spring.datasource.port}")
    private String PORT;
    @Value("${spring.datasource.db}")
    private String DB;
    @Value("${spring.datasource.username}")
    private String USERNAME;
    @Value("${spring.datasource.password}")
    private String PASSWORD;
    @Value("${kubernetes.image}")
    private String IMAGE;
    @Value("${kubernetes.api-version}")
    private String API_VERSION;
    @Value("${kubernetes.metadata.name}")
    private String METADATA_NAME;
    @Value("${kubernetes.namespace}")
    private String NAMESPACE;
    @Value("${kubernetes.container.name}")
    private String CONTAINER_NAME;
    @Value("${kubernetes.image-pull-policy}")
    private String IMAGE_PULL_POLICY;
    @Value("${kubernetes.volume-mount.name}")
    private String VOLUME_MOUNT_NAME;
    @Value("${kubernetes.volume.name}")
    private String VOLUME_NAME;
    @Value("${kubernetes.persistent-volume-claim-volume-source.claim-name}")
    private String CLAIM_NAME;
    @Value("${kubernetes.restart-policy}")
    private String RESTART_POLICY;
    @Value("${kubernetes.job.kind}")
    private String JOB_KIND;

    public Tenant createTenant(TenantValue tenantValue, UserDetails userDetails) throws LottabyteException {
        try {
            if (tenantValue.getTenantName() != null && tenantService.getTenantByName(tenantValue.getTenantName()) != null)
                throw new LottabyteException(Message.LBE02900, userDetails.getLanguage(), tenantValue.getTenantName());
            if (tenantValue.getDomainName() != null && tenantService.getTenantByDomain(tenantValue.getDomainName()) != null)
                throw new LottabyteException(Message.LBE02901, userDetails.getLanguage(), tenantValue.getDomainName());

            Integer tenantId = tenantService.createTenant(tenantValue.getTenantName(), tenantValue.getDomainName(), userDetails);
            Tenant tenant = tenantService.getTenantById(tenantId);
            Integer pgVersion = versionService.getLastVersionByType("pg");
            Integer elasticVersion = versionService.getLastVersionByType("elastic");
            String pgDDL = versionService.getDDLByVersion(pgVersion);
            String indexSetting = versionService.getSchemaByVersion(elasticVersion);

            pgDDL = pgDDL.replaceAll("\\{tenant_id}", tenant.getId());
            adminRepository.request(pgDDL);

            String newTenantName = "da_" + tenant.getId();
            elasticsearchService.createIndex(newTenantName, indexSetting);
            UpdatableUserDetails updatableUserDetails = new UpdatableUserDetails();
            updatableUserDetails.setUsername("admin");
            updatableUserDetails.setDisplayName("admin");
            List<String> roleList = new ArrayList<>();
            roleList.add("464f7580-b3f2-4bd5-824b-10a2d05e5e34");//переделать на получение из бд какой-нибудь дефолтной роли
            updatableUserDetails.setUserRolesIds(roleList);
            updatableUserDetails.setPassword(tenantValue.getPassword());

            UserDetails newUserDetails = userService.createUser(updatableUserDetails, tenant.getId());
            List<SystemType> systemTypeList = systemService.getSystemTypesFromDaSchema();
            for(SystemType systemType : systemTypeList)
                systemService.createSystemType(systemType, newUserDetails);
            return tenant;
        } catch (LottabyteException e) {
            Tenant tenant = tenantService.getTenantByName(tenantValue.getTenantName());
            if (tenant != null)
                deleteTenant(Integer.valueOf(tenant.getId()), userDetails);
            throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    public void deleteTenant(Integer tenantId, UserDetails userDetails) throws LottabyteException {
        try {
            Tenant tenant = tenantService.getTenantById(tenantId);
            if (tenant != null){
                if(tenant.getEntity().getDefaultTenant())
                    throw new LottabyteException(Message.LBE02902, userDetails.getLanguage(), tenant);
            }else{
                throw new LottabyteException(Message.LBE02903, userDetails.getLanguage(), tenantId);
            }

            try{
                adminRepository.request("DROP SCHEMA da_" + tenantId + " CASCADE;");
            }catch(Exception e){
                log.error(e.getMessage());
            }
            try{
                elasticsearchService.deleteIndex("da_" + tenant.getId());
            }catch(Exception e){
                log.error(e.getMessage());
            }

            tenantService.deleteTenant(tenantId);
            userService.deleteUserByTenant(String.valueOf(tenantId));
            userService.deleteUserRolesByTenantId(String.valueOf(tenantId));
            userService.deleteExternalGroupsByTenantId(String.valueOf(tenantId));
            userService.deleteLdapPropertiesByTenantId(tenantId);
        } catch (LottabyteException e) {
            throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    public void deleteTenantWithCreatingBackup(Integer tenantId, BackupRun backupRun, UserDetails userDetails) throws LottabyteException {
        try {
            File zipBackup = createBackup(tenantId, backupRun);

            Tenant tenant = tenantService.getTenantById(tenantId);
            if (tenant != null){
                if(tenant.getEntity().getDefaultTenant())
                    throw new LottabyteException(Message.LBE02902, userDetails.getLanguage(), tenant);
            }else{
                throw new LottabyteException(Message.LBE02903, userDetails.getLanguage(), tenantId);
            }

            try{
                adminRepository.request("DROP SCHEMA da_" + tenantId + " CASCADE;");
            }catch(Exception e){
                log.error(e.getMessage());
            }
            try{
                elasticsearchService.deleteIndex("da_" + tenant.getId());
            }catch(Exception e){
                log.error(e.getMessage());
            }

            tenantService.deleteTenant(tenantId);
            userService.deleteUserByTenant(String.valueOf(tenantId));
            userService.deleteUserRolesByTenantId(String.valueOf(tenantId));
            userService.deleteExternalGroupsByTenantId(String.valueOf(tenantId));
            userService.deleteLdapPropertiesByTenantId(tenantId);
        } catch (LottabyteException e) {
            throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    public String createTenantRelatedRecordsBackup(Integer tenantId) throws LottabyteException {
        Tenant tenant = tenantService.getTenantById(tenantId);
        List<UserDetails> userDetailsList = userService.getUserByTenantId(String.valueOf(tenantId));
        List<UserRole> userRoleList = userService.getUserRolesByTenantId(String.valueOf(tenantId));
        List<ExternalGroup> externalGroupList = userService.getExternalGroupsByTenantId(String.valueOf(tenantId));
        List<LdapProperty> ldapPropertyList = userService.getLdapPropertiesByTenantId(tenantId);
        try {
            if(
                    (userDetailsList != null && !userDetailsList.isEmpty()) ||
                    (userRoleList != null && !userRoleList.isEmpty()) ||
                    (externalGroupList != null && !externalGroupList.isEmpty()) ||
                    (ldapPropertyList != null && !ldapPropertyList.isEmpty())
            )
                return createTenantRelatedRecordsBackup(userDetailsList, userRoleList, externalGroupList, ldapPropertyList, tenant);
        } catch (IOException e) {
            throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
        return null;
    }

    public String createTenantRelatedRecordsBackup(List<UserDetails> userDetailsList, List<UserRole> userRoleList, List<ExternalGroup> externalGroupList, List<LdapProperty> ldapPropertyList, Tenant tenant) throws IOException {
        String fileName = "TenantEvolution.sql";
        String path = DATA_STORAGE + "/" + fileName;
        try {
            writer = new BufferedWriter(new FileWriter(path));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        Timestamp ts = new Timestamp(new Date().getTime());
        writer.write("--UP");
        writer.newLine();
        for(UserDetails userDetails : userDetailsList){
            writer.write("INSERT INTO usermgmt.platform_users \n" +
                    "(uid, username, display_name, description, email, password_hash, permissions, user_roles, approval_status, current_account_status, internal_user, deletable, authenticator, created, modified, tenant) \n" +
                    "VALUES((SELECT MAX(uid) FROM usermgmt.platform_users) + 1, '" + userDetails.getUsername() + "', '" + userDetails.getDisplayName() + "', '', " +
                    "'" + userDetails.getEmail() + "', '" + userDetails.getPassword() + "', '" + userRepository.createStringSqlArray(userDetails.getPermissions()) + "', '" + userRepository.createStringSqlArray(userDetails.getUserRoles()) + "', " +
                    "'" + userDetails.getApprovalStatus() + "', '" + userDetails.getCurrentAccountStatus() + "', '" + userDetails.getInternalUser() + "', " +
                    "'" + userDetails.getDeletable() + "', '" + userDetails.getAuthenticator() + "', '" + ts + "', '" + ts + "', '" + userDetails.getTenant() + "') ON CONFLICT DO NOTHING;"
            );
            writer.newLine();
            writer.newLine();
        }
        writer.newLine();

        for(UserRole userRole : userRoleList){
            writer.write("INSERT INTO usermgmt.user_roles \n" +
                    "(id, \"name\", description, permissions, created, modified, tenant) \n" +
                    "VALUES('" + userRole.getId() + "', '" + userRole.getName() + "', '" + userRole.getDescription() + "', " +
                    "'" + userRepository.createStringSqlArray(userRole.getPermissions()) + "', '" + ts + "', '" + ts + "', " +
                    "'" + tenant.getId() + "') ON CONFLICT DO NOTHING;"
            );
            writer.newLine();
            writer.newLine();
        }
        writer.newLine();

        for(ExternalGroup externalGroup : externalGroupList){
            writer.write("INSERT INTO usermgmt.external_groups \n" +
                    "(id, \"name\", description, created, modified, permissions, user_roles, \"attributes\", tenant) \n" +
                    "VALUES((SELECT MAX(uid) FROM usermgmt.platform_users) + 1, '" + externalGroup.getEntity().getName() + "', " +
                    "'" + externalGroup.getEntity().getDescription() + "', '" + ts + "', '" + ts + "', " +
                    "'" + userRepository.createStringSqlArray(externalGroup.getEntity().getPermissions()) + "', '" + userRepository.createStringSqlArray(externalGroup.getEntity().getUserRoles()) + "', '" + externalGroup.getEntity().getAttributes() + "', " +
                    "'" + tenant.getId() + "') ON CONFLICT DO NOTHING;"
            );
            writer.newLine();
            writer.newLine();
        }
        writer.newLine();

        for(LdapProperty ldapProperty : ldapPropertyList){
            writer.write("INSERT INTO da.ldap_properties \n" +
                    "(id, tenant_id, provider_url, principal, credentials, base_dn, user_query) \n" +
                    "VALUES('" + ldapProperty.getId() + "', '" + ldapProperty.getEntity().getTenantId() + "', " +
                    "'" + ldapProperty.getEntity().getProviderUrl() + "', '" + ldapProperty.getEntity().getPrincipal() + "', '" + ldapProperty.getEntity().getCredentials() + "', " +
                    "'" + ldapProperty.getEntity().getBase_dn() + "', '" + ldapProperty.getEntity().getUser_query() + "') ON CONFLICT DO NOTHING;"
            );
            writer.newLine();
            writer.newLine();
        }
        writer.flush();
        writer.close();

        return fileName;
    }

    public String exportDb(Integer tenantId) throws LottabyteException {
        String SCHEMA="da_" + tenantId;
        String FILENAME = "da_" + tenantId + ".backup";
        String FILE="/" + DATA_STORAGE + "/" + FILENAME;

        ApiClient client = null;
        try {
            client = Config.defaultClient();
        } catch (IOException e) {
            throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
        Configuration.setDefaultApiClient(client);
        CoreV1Api coreApi = new CoreV1Api(client);
        BatchV1Api batchApi = new BatchV1Api(client);

        try {

            V1Job job = new V1Job();
            job.setApiVersion(API_VERSION);
            job.setKind(JOB_KIND);
            V1ObjectMeta meta = new V1ObjectMeta();
            meta.setName(METADATA_NAME);
            meta.setNamespace(NAMESPACE);
            job.setMetadata(meta);
            V1JobSpec spec = new V1JobSpec();
            spec.setBackoffLimit(2);
            spec.setActiveDeadlineSeconds(60L);
            V1PodTemplateSpec tpl = new V1PodTemplateSpec();
            V1PodSpec podSpec = new V1PodSpec();
            podSpec.setRestartPolicy(RESTART_POLICY);
            V1Container container = new V1Container();
            container.setName(CONTAINER_NAME);
            container.setImage(IMAGE);
            container.setImagePullPolicy(IMAGE_PULL_POLICY);
            container.setArgs(Arrays.asList("/bin/sh", "-c", "PGPASSWORD=" + PASSWORD + " pg_dump --host " + HOST + " --port " + PORT + " --username " + USERNAME + " --format custom --blobs --schema-only --schema " + SCHEMA + " --file " + FILE + " " + DB));

            List<V1VolumeMount> v1VolumeMountList = new ArrayList<>();
            V1VolumeMount v1VolumeMount = new V1VolumeMount();
            v1VolumeMount.setName(VOLUME_MOUNT_NAME);
            v1VolumeMount.setMountPath("/" + DATA_STORAGE);
            v1VolumeMountList.add(v1VolumeMount);
            container.setVolumeMounts(v1VolumeMountList);

            List<V1Volume> v1VolumeList = new ArrayList<>();
            V1Volume v1Volume = new V1Volume();
            v1Volume.setName(VOLUME_NAME);

            V1PersistentVolumeClaimVolumeSource v1PersistentVolumeClaimVolumeSource = new V1PersistentVolumeClaimVolumeSource();
            v1PersistentVolumeClaimVolumeSource.setClaimName(CLAIM_NAME);

            v1Volume.setPersistentVolumeClaim(v1PersistentVolumeClaimVolumeSource);
            v1VolumeList.add(v1Volume);

            podSpec.setVolumes(v1VolumeList);
            podSpec.setContainers(Collections.singletonList(container));

            tpl.setSpec(podSpec);
            spec.setTemplate(tpl);
            job.setSpec(spec);

            log.info("batchApi: " + batchApi);
            log.info("job: " + job);
            log.info("V1JOB execution format: {}",Yaml.dump(job));
            ApiResponse<V1Job> response = batchApi.createNamespacedJobWithHttpInfo(NAMESPACE, job, "true", null, null, null);

            log.info("response: " + response);
            log.info("StatusCode: " + response.getStatusCode());
            String controllerUid = response.getData().getMetadata().getLabels().get("controller-uid");

            V1PodList pods;

            boolean repeat = true;
            while (repeat) {
                repeat = false;
                pods = coreApi.listNamespacedPod(NAMESPACE, "true", null, null, null, "controller-uid=" + controllerUid, null, null, null, null, null);

                System.out.println("Pods:");
                for (V1Pod pod : pods.getItems()) {
                    System.out.println("Pod: " + pod.getMetadata().getName());

                    System.out.println("Pod status: " + pod.getStatus().getPhase());

                    if ("Pending".equals(pod.getStatus().getPhase()) || "Running".equals(pod.getStatus().getPhase())){
                        repeat = true;
                    }
                }
                if (repeat)
                    Thread.sleep(5000);
            }

            batchApi.deleteNamespacedJob(job.getMetadata().getName(), NAMESPACE, "true", null, null, null, null, null);
            return FILENAME;
        } catch (ApiException | InterruptedException e) {
            log.error("Error Message: ", e);
            try {
                batchApi.deleteNamespacedJob(METADATA_NAME, NAMESPACE, "true", null, null, null, null, null);
            } catch (ApiException ex) {
                throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            }
            throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    public BackupRun createBackupRunBeforeRequest(Integer tenantId, String path) throws LottabyteException {
        UpdatableBackupRunEntity updatableBackupRunEntity = new UpdatableBackupRunEntity();
        updatableBackupRunEntity.setPath(path);
        updatableBackupRunEntity.setTenantId(tenantId);
        updatableBackupRunEntity.setBackupStart(new Timestamp(new Date().getTime()).toLocalDateTime());
        updatableBackupRunEntity.setBackupState(TaskState.STARTED.getText());

        BackupRun backupRun = backupRunService.createBackupRun(updatableBackupRunEntity);

        createBackup(tenantId, backupRun);

        return backupRun;
    }
    public File createBackup(Integer tenantId, BackupRun backupRun) throws LottabyteException {
        String tenantName = "da_" + tenantId;
        String path = DATA_STORAGE + "/" + tenantName + ".zip";
        List<String> fileList = new ArrayList<>();
        try {
            fileList.add(DATA_STORAGE + "/" + exportDb(tenantId));
            fileList.add(DATA_STORAGE + "/" + createTenantRelatedRecordsBackup(tenantId));

            File archive = createZip(path, fileList);
            UpdatableBackupRunEntity updatableBackupRunEntity = new UpdatableBackupRunEntity();
            updatableBackupRunEntity.setBackupState(TaskState.FINISHED.getText());
            updatableBackupRunEntity.setBackupEnd(new Timestamp(new Date().getTime()).toLocalDateTime());
            backupRunService.updateBackupRunById(backupRun.getId(), updatableBackupRunEntity);
            return archive;
        } catch (LottabyteException e) {
            UpdatableBackupRunEntity updatableBackupRunEntity = new UpdatableBackupRunEntity();
            updatableBackupRunEntity.setResultMsg(e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
            updatableBackupRunEntity.setBackupState(TaskState.FAILED.getText());
            updatableBackupRunEntity.setBackupEnd(new Timestamp(new Date().getTime()).toLocalDateTime());
            backupRunService.updateBackupRunById(backupRun.getId(), updatableBackupRunEntity);
            throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    public File createZip(String path, List<String> fileList) throws LottabyteException {
        try {
            File archive = new File(path);

            try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(path))) {
                for (String filePath : fileList) {
                    File fileToZip = new File(filePath);
                    zipOut.putNextEntry(new ZipEntry(fileToZip.getName()));
                    Files.copy(fileToZip.toPath(), zipOut);
                }
            }

            return archive;
        } catch (IOException e) {
            throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
}
