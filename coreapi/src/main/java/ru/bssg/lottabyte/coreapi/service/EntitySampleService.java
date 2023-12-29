package ru.bssg.lottabyte.coreapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.connector.ConnectorType;
import ru.bssg.lottabyte.core.connector.IConnectorService;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.connector.Connector;
import ru.bssg.lottabyte.core.model.connector.ConnectorParam;
import ru.bssg.lottabyte.core.model.dataentity.DataEntity;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQueryResult;
import ru.bssg.lottabyte.core.model.entityQuery.FlatEntityQuery;
import ru.bssg.lottabyte.core.model.entitySample.*;
import ru.bssg.lottabyte.core.model.task.TaskState;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.model.system.SystemConnection;
import ru.bssg.lottabyte.core.model.system.SystemConnectionParam;
import ru.bssg.lottabyte.core.model.task.Task;
import ru.bssg.lottabyte.core.model.taskrun.TaskRun;
import ru.bssg.lottabyte.core.model.taskrun.UpdatableTaskRunEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.CoreUtils;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.config.ApplicationConfig;
import ru.bssg.lottabyte.coreapi.repository.*;
import ru.bssg.lottabyte.coreapi.repository.impl.EntitySampleBodyS3RepositoryImpl;
import ru.bssg.lottabyte.coreapi.service.connector.ConnectorFactory;
import ru.bssg.lottabyte.coreapi.util.AllValidator;
import ru.bssg.lottabyte.coreapi.util.Helper;
import ru.bssg.lottabyte.coreapi.util.IValidator;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EntitySampleService {
        private final TagService tagService;
        private final CommentService commentService;
        private final EntitySampleRepository entitySampleRepository;
        private final EntityRepository entityRepository;
        private final ElasticsearchService elasticsearchService;
        private final EntityService entityService;
        private final EntityQueryService entityQueryService;
        private final SystemService systemService;
        private final SystemRepository systemRepository;
        private final IEntitySampleBodyRepository iEntitySampleBodyRepository;
        private final IValidator validator;
        private final ApplicationConfig applicationConfig;
        private final ConnectorService connectorService;
        private final TaskService taskService;
        private final TaskRunService taskRunService;
        private final SystemConnectionService systemConnectionService;
        private final EntitySampleBodyService entitySampleBodyService;
        private final DomainRepository domainRepository;
        private final RatingService ratingService;

        private final SearchColumn[] searchableColumns = {
                        new SearchColumn("name", SearchColumn.ColumnType.Text),
                        new SearchColumn("description", SearchColumn.ColumnType.Text),
                        new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
                        new SearchColumn("system_id", SearchColumn.ColumnType.UUID),
                        new SearchColumn("entity_id", SearchColumn.ColumnType.UUID),
                        new SearchColumn("entity_query_id", SearchColumn.ColumnType.UUID),
                        new SearchColumn("system.name", SearchColumn.ColumnType.Text),
                        new SearchColumn("entity.name", SearchColumn.ColumnType.Text),
                        new SearchColumn("tags", SearchColumn.ColumnType.Text),
                        new SearchColumn("entity_query.name", SearchColumn.ColumnType.Text),
        };

        private final SearchColumn[] searchablePropertyColumns = {
                        new SearchColumn("name", SearchColumn.ColumnType.Text),
                        new SearchColumn("description", SearchColumn.ColumnType.Text),
                        new SearchColumn("path_type", SearchColumn.ColumnType.Text),
                        new SearchColumn("path", SearchColumn.ColumnType.Text),
                        new SearchColumn("entity_sample_id", SearchColumn.ColumnType.UUID),
                        new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
                        new SearchColumn("entity_attribute.name", SearchColumn.ColumnType.Text),
        };

        private final SearchColumn[] searchableDQRuleColumns = {
                        new SearchColumn("entity_sample_id", SearchColumn.ColumnType.UUID),
                        new SearchColumn("dq_rule_id", SearchColumn.ColumnType.UUID),
                        new SearchColumn("settings", SearchColumn.ColumnType.Text),
                        new SearchColumn("id", SearchColumn.ColumnType.UUID),

        };
        private final SearchColumnForJoin[] joinPropertyColumns = {};

        private final SearchColumnForJoin[] joinColumns = {
                        new SearchColumnForJoin("domain_id", "system_to_domain", SearchColumn.ColumnType.UUID,
                                        "domain_id", "id")
        };

        @Autowired
        public EntitySampleService(
                        EntitySampleRepository entitySampleRepository,
                        ElasticsearchService elasticsearchService,
                        EntityService entityService,
                        EntityQueryService entityQueryService,
                        SystemService systemService,
                        IValidator validator,
                        EntityRepository entityRepository,
                        ApplicationConfig applicationConfig,
                        ConnectorService connectorService,
                        TaskService taskService,
                        TaskRunService taskRunService,
                        SystemConnectionService systemConnectionService,
                        EntitySampleBodyS3RepositoryImpl entitySampleBodyS3RepositoryImpl,
                        EntitySampleBodyService entitySampleBodyService,
                        TagService tagService,
                        CommentService commentService,
                        DomainRepository domainRepository,
                        SystemRepository systemRepository,
                        RatingService ratingService) {
                this.entitySampleRepository = entitySampleRepository;
                this.elasticsearchService = elasticsearchService;
                this.entityService = entityService;
                this.entityQueryService = entityQueryService;
                this.systemService = systemService;
                this.entityRepository = entityRepository;
                this.iEntitySampleBodyRepository = entitySampleBodyS3RepositoryImpl;
                this.validator = validator;
                this.applicationConfig = applicationConfig;
                this.connectorService = connectorService;
                this.taskService = taskService;
                this.taskRunService = taskRunService;
                this.systemConnectionService = systemConnectionService;
                this.entitySampleBodyService = entitySampleBodyService;
                this.tagService = tagService;
                this.commentService = commentService;
                this.domainRepository = domainRepository;
                this.systemRepository = systemRepository;
                this.ratingService = ratingService;
        }

        public boolean existSamplesInSystem(String systemId, UserDetails userDetails) {
                return entitySampleRepository.existSamplesInSystem(systemId, userDetails);
        }

        public TaskRun createTaskRunBeforeRequest(String taskId, UserDetails userDetails) throws LottabyteException {
                List<TaskRun> taskRunListToCheck = taskRunService.getTaskRunListByTaskId(taskId, userDetails);
                if (!taskRunListToCheck.isEmpty())
                        throw new LottabyteException(
                                        Message.LBE01502,
                                                        userDetails.getLanguage(), taskId);

                UpdatableTaskRunEntity updatableTaskRunEntity = new UpdatableTaskRunEntity();
                updatableTaskRunEntity.setTaskId(taskId);
                updatableTaskRunEntity.setStaredBy(userDetails.getUid());
                updatableTaskRunEntity.setTaskStart(new Timestamp(new Date().getTime()).toLocalDateTime());
                updatableTaskRunEntity.setTaskState(TaskState.STARTED.getText());
                updatableTaskRunEntity.setStartMode("manual");

                return taskRunService.createTaskRun(updatableTaskRunEntity, userDetails);
        }

        @Async("asyncExecutor")
        public void workWithConnectors(String taskId, TaskRun taskRun, UserDetails userDetails)
                        throws LottabyteException {
                UpdatableTaskRunEntity updatableTaskRunEntity = new UpdatableTaskRunEntity();
                String entitySampleId = null;

                try {
                        ConnectorFactory factory = new ConnectorFactory();

                        EntityQueryResult entityQueryResult = null;

                        Task task = taskService.getTaskById(taskId, userDetails);
                        if (task == null)
                                throw new LottabyteException(
                                                Message.LBE01401, userDetails.getLanguage(), taskId);

                        EntityQuery entityQuery = entityQueryService.getEntityQueryById(task.getEntity().getQueryId(),
                                        userDetails);
                        if (entityQuery == null)
                                throw new LottabyteException(Message.LBE00005,
                                                                userDetails.getLanguage(),
                                                                task.getEntity().getQueryId());

                        DataEntity dataEntity = entityService.getDataEntityById(entityQuery.getEntity().getEntityId(),
                                        userDetails);
                        if (dataEntity == null)
                                throw new LottabyteException(Message.LBE00301,
                                                                userDetails.getLanguage(),
                                                                entityQuery.getEntity().getEntityId());

                        SystemConnection systemConnection = systemConnectionService
                                        .getSystemConnectionById(task.getEntity().getSystemConnectionId(), userDetails);
                        if (systemConnection == null)
                                throw new LottabyteException(Message.LBE01201,
                                                                userDetails.getLanguage(),
                                                                task.getEntity().getSystemConnectionId());

                        System system = systemService.getSystemById(systemConnection.getEntity().getSystemId(),
                                        userDetails);
                        if (system == null)
                                throw new LottabyteException(Message.LBE00904,
                                                                userDetails.getLanguage(),
                                                                systemConnection.getEntity().getSystemId());

                        Connector connector = connectorService.getConnectorById(
                                        systemConnection.getEntity().getConnectorId(),
                                        userDetails);
                        if (connector == null)
                                throw new LottabyteException(Message.LBE01101,
                                                                userDetails.getLanguage(),
                                                                systemConnection.getEntity().getConnectorId());

                        IConnectorService iConnectorService = factory.getConnector(
                                        Objects.requireNonNull(
                                                        ConnectorType.fromString(connector.getEntity().getName())),
                                        userDetails);

                        updatableTaskRunEntity = new UpdatableTaskRunEntity();
                        updatableTaskRunEntity.setTaskState(TaskState.RUNNING.getText());
                        taskRunService.updateTaskRunById(taskRun.getId(), updatableTaskRunEntity, userDetails);

                        List<ConnectorParam> connectorParamList = connectorService
                                        .getConnectorParamsList(systemConnection.getEntity().getConnectorId(),
                                                        userDetails);
                        if (connectorParamList == null || connectorParamList.isEmpty())
                                throw new LottabyteException(Message.LBE00007,
                                                                userDetails.getLanguage(),
                                                                systemConnection.getEntity().getConnectorId());

                        List<SystemConnectionParam> systemConnectionParamList = systemConnectionService
                                        .getSystemConnectionParamsList(systemConnection.getId(), userDetails);
                        if (systemConnectionParamList == null || systemConnectionParamList.isEmpty())
                                throw new LottabyteException(Message.LBE01202,
                                                                userDetails.getLanguage(),
                                                                systemConnection.getId());

                        entityQueryResult = iConnectorService.querySystem(connector, connectorParamList, system,
                                        dataEntity,
                                        entityQuery, systemConnection, systemConnectionParamList, userDetails);

                        if (entityQueryResult.getTextSampleBody() == null
                                        || entityQueryResult.getTextSampleBody().isEmpty())
                                throw new LottabyteException(Message.LBE01602,
                                                userDetails.getLanguage());

                        List<EntitySampleProperty> entitySamplePropertyFromTableList = getEntitySamplePropertiesFromTable(
                                        entityQueryResult.getTextSampleBody());

                        EntitySample currentEntitySample = getEntitySampleByQueryId(entityQuery.getId(), true,
                                        userDetails);
                        if (currentEntitySample != null) {
                                if (!currentEntitySample.getEntity().getSampleBody()
                                                .equals(entityQueryResult.getTextSampleBody())) {
                                        List<EntitySampleProperty> entitySamplePropertyList = getSamplesPropertiesList(
                                                        currentEntitySample.getId(), userDetails);
                                        boolean updateProperties = false;
                                        for (EntitySampleProperty entitySamplePropertyFromJdbc : entitySamplePropertyFromTableList) {
                                                if (entitySamplePropertyList.stream()
                                                                .filter(entitySampleProperty -> entitySamplePropertyFromJdbc
                                                                                .getEntity().getPath()
                                                                                .equals(entitySampleProperty.getEntity()
                                                                                                .getPath()))
                                                                .findFirst().orElse(null) == null) {
                                                        updateProperties = true;
                                                        break;
                                                }
                                        }
                                        if (updateProperties) {
                                                for (EntitySampleProperty entitySampleProperty : entitySamplePropertyList) {
                                                        deleteSampleProperty(entitySampleProperty.getId(), true,
                                                                        userDetails);
                                                }
                                                for (EntitySampleProperty entitySamplePropertyFromJdbc : entitySamplePropertyFromTableList) {
                                                        createSampleProperty(currentEntitySample.getId(),
                                                                        new UpdatableEntitySampleProperty(
                                                                                        entitySamplePropertyFromJdbc
                                                                                                        .getEntity()),
                                                                        userDetails);
                                                }
                                        }
                                }
                                EntitySample entitySample = updateSampleBody(currentEntitySample.getId(),
                                                entityQueryResult.getTextSampleBody(), userDetails);
                                entitySampleId = entitySample.getId();
                        } else {
                                UpdatableEntitySampleEntity newEntitySampleEntity = new UpdatableEntitySampleEntity();
                                newEntitySampleEntity.setSampleBody(entityQueryResult.getTextSampleBody());
                                newEntitySampleEntity.setSampleType(entityQueryResult.getSampleType());
                                newEntitySampleEntity.setSystemId(system.getId());
                                newEntitySampleEntity.setEntityQueryId(entityQuery.getId());
                                newEntitySampleEntity.setEntityId(dataEntity.getId());
                                newEntitySampleEntity.setName(String.format(
                                                "Sample name for query %s for entity %s for system %s",
                                                entityQuery.getName(), dataEntity.getName(), system.getName()));

                                EntitySample entitySample = createSample(newEntitySampleEntity, userDetails);
                                entitySampleId = entitySample.getId();

                                for (EntitySampleProperty entitySampleProperty : entitySamplePropertyFromTableList) {
                                        UpdatableEntitySampleProperty updatableEntitySampleProperty = new UpdatableEntitySampleProperty(
                                                        entitySampleProperty.getEntity());
                                        createSampleProperty(entitySample.getId(), updatableEntitySampleProperty,
                                                        userDetails);
                                }
                        }

                        updatableTaskRunEntity.setTaskState(TaskState.FINISHED.getText());
                        updatableTaskRunEntity.setTaskEnd(new Timestamp(new Date().getTime()).toLocalDateTime());
                        updatableTaskRunEntity.setResultSampleId(entitySampleId);
                        taskRunService.updateTaskRunById(taskRun.getId(), updatableTaskRunEntity, userDetails);
                } catch (Exception e) {
                        updatableTaskRunEntity.setResultMsg(e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
                        updatableTaskRunEntity.setTaskState(TaskState.FAILED.getText());
                        updatableTaskRunEntity.setTaskEnd(new Timestamp(new Date().getTime()).toLocalDateTime());
                        taskRunService.updateTaskRunById(taskRun.getId(), updatableTaskRunEntity, userDetails);
                        throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage(), e);
                }
        }

        public String getSamplesPropertiesForTest(String taskId, Integer rowsNumber, UserDetails userDetails)
                        throws LottabyteException {
                List<TaskRun> taskRunListToCheck = taskRunService.getTaskRunListByTaskId(taskId, userDetails);
                if (!taskRunListToCheck.isEmpty())
                        throw new LottabyteException(Message.LBE01502,
                                                        userDetails.getLanguage(), taskId);

                try {
                        ConnectorFactory factory = new ConnectorFactory();

                        EntityQueryResult entityQueryResult = null;

                        Task task = taskService.getTaskById(taskId, userDetails);
                        if (task == null)
                                throw new LottabyteException(Message.LBE01401,
                                                                userDetails.getLanguage(), taskId);

                        EntityQuery entityQuery = entityQueryService.getEntityQueryById(task.getEntity().getQueryId(),
                                        userDetails);
                        if (entityQuery == null)
                                throw new LottabyteException(Message.LBE00005,
                                                                userDetails.getLanguage(),
                                                                task.getEntity().getQueryId());

                        DataEntity dataEntity = entityService.getDataEntityById(entityQuery.getEntity().getEntityId(),
                                        userDetails);
                        if (dataEntity == null)
                                throw new LottabyteException(Message.LBE00301,
                                                                userDetails.getLanguage(),
                                                                entityQuery.getEntity().getEntityId());

                        SystemConnection systemConnection = systemConnectionService
                                        .getSystemConnectionById(task.getEntity().getSystemConnectionId(), userDetails);
                        if (systemConnection == null)
                                throw new LottabyteException(Message.LBE01201,
                                                                userDetails.getLanguage(),
                                                                task.getEntity().getSystemConnectionId());

                        System system = systemService.getSystemById(systemConnection.getEntity().getSystemId(),
                                        userDetails);
                        if (system == null)
                                throw new LottabyteException(Message.LBE00904,
                                                                userDetails.getLanguage(),
                                                                systemConnection.getEntity().getSystemId());

                        Connector connector = connectorService.getConnectorById(
                                        systemConnection.getEntity().getConnectorId(),
                                        userDetails);
                        if (connector == null)
                                throw new LottabyteException(Message.LBE01101,
                                                                userDetails.getLanguage(),
                                                                systemConnection.getEntity().getConnectorId());

                        IConnectorService iConnectorService = factory.getConnector(
                                        Objects.requireNonNull(
                                                        ConnectorType.fromString(connector.getEntity().getName())),
                                        userDetails);

                        List<ConnectorParam> connectorParamList = connectorService
                                        .getConnectorParamsList(systemConnection.getEntity().getConnectorId(),
                                                        userDetails);
                        if (connectorParamList == null || connectorParamList.isEmpty())
                                throw new LottabyteException(Message.LBE00007,
                                                                userDetails.getLanguage(),
                                                                systemConnection.getEntity().getConnectorId());

                        List<SystemConnectionParam> systemConnectionParamList = systemConnectionService
                                        .getSystemConnectionParamsList(systemConnection.getId(), userDetails);
                        if (systemConnectionParamList == null || systemConnectionParamList.isEmpty())
                                throw new LottabyteException(Message.LBE01202,
                                                                userDetails.getLanguage(),
                                                                systemConnection.getId());

                        entityQueryResult = iConnectorService.querySystem(connector, connectorParamList, system,
                                        dataEntity,
                                        entityQuery, systemConnection, systemConnectionParamList, userDetails);

                        if (entityQueryResult.getTextSampleBody() == null
                                        || entityQueryResult.getTextSampleBody().isEmpty())
                                throw new LottabyteException(
                                                Message.LBE01602,
                                                userDetails.getLanguage());

                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                        JsonNode tree = null;
                        try {
                                tree = objectMapper.readTree(entityQueryResult.getTextSampleBody());
                                entityQueryResult.setTextSampleBody(objectMapper.writeValueAsString(tree));
                        } catch (JsonProcessingException e) {
                                throw new LottabyteException(HttpStatus.BAD_REQUEST, e.getMessage());
                        }
                        return Helper.stringClipping(entityQueryResult.getTextSampleBody(), rowsNumber - 1);
                } catch (Exception e) {
                        throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage());
                }
        }

        public EntitySampleProperty getSamplePropertyById(String propertyId, UserDetails userDetails) {
                return entitySampleRepository.getSamplePropertyById(propertyId, userDetails);
        }

        public List<EntitySampleProperty> getAllSamplePropertyBySampleId(String propertyId, UserDetails userDetails) {
                return entitySampleRepository.getAllSamplePropertyBySampleId(propertyId, userDetails);
        }

        public PaginatedArtifactList<EntitySampleProperty> getSamplesPropertiesPaginated(String sampleId, Integer limit,
                        Integer offset, UserDetails userDetails) {
                return entitySampleRepository.getSamplesPropertiesPaginated(sampleId, limit, offset, userDetails);
        }

        public List<EntitySampleProperty> getSamplesPropertiesList(String sampleId, UserDetails userDetails) {
                return entitySampleRepository.getSamplesPropertiesList(sampleId, userDetails);
        }

        private boolean validateSamplePath(String path, EntitySamplePropertyPathType type) {
                if (path == null || type == null)
                        return true;

                switch (type) {
                        case xpath:
                                return CoreUtils.validateXPath(path);
                        case json_path:
                                /* TODO */ return true;
                }

                return true;
        }

        public EntitySampleProperty createSampleProperty(String sampleId,
                        UpdatableEntitySampleProperty entitySampleProperty, UserDetails userDetails)
                        throws LottabyteException {
                if (!entitySampleRepository.existsById(sampleId, userDetails))
                        throw new LottabyteException(Message.LBE02001,
                                                        userDetails.getLanguage(), sampleId);
                if (userDetails.getStewardId() != null
                                && !entitySampleRepository.hasAccessToSample(sampleId, userDetails))
                        throw new LottabyteException(Message.LBE02006, userDetails.getLanguage(), sampleId);
                if (!validateSamplePath(entitySampleProperty.getPath(), entitySampleProperty.getPathType()))
                        throw new LottabyteException(Message.LBE00033,
                                                        userDetails.getLanguage(),
                                                        entitySampleProperty.getPath());

                if (entitySampleProperty.getMappedAttributeIds() != null
                                && !entitySampleProperty.getMappedAttributeIds().isEmpty()) {
                        EntitySample es = getEntitySampleById(sampleId, true, userDetails);
                        if (!entityRepository.entityAttributesExistAndBelongToEntity(
                                        entitySampleProperty.getMappedAttributeIds(),
                                        es.getEntity().getEntityId(), userDetails))
                                throw new LottabyteException(Message.LBE00034,
                                                                userDetails.getLanguage(),
                                                                es.getEntity().getEntityId(),
                                                                StringUtils.join(entitySampleProperty
                                                                                .getMappedAttributeIds(), ", "));
                }

                EntitySampleProperty sampleProperty = entitySampleRepository.createSampleProperty(sampleId,
                                entitySampleProperty, userDetails);
                // elasticsearchService.insertElasticSearchEntity(Collections.singletonList(sampleProperty.getSearchableArtifact()),
                // userDetails);
                return entitySampleRepository.getSamplePropertyById(sampleProperty.getId(), userDetails);
        }

        public EntitySampleProperty updateSampleProperty(String propertyId,
                        UpdatableEntitySampleProperty entitySampleProperty, UserDetails userDetails)
                        throws LottabyteException {
                if (!entitySampleRepository.samplePropertyExists(propertyId, userDetails))
                        throw new LottabyteException(
                                        Message.LBE02002,
                                                        userDetails.getLanguage(),
                                        propertyId);
                EntitySampleProperty current = entitySampleRepository.getSamplePropertyById(propertyId, userDetails);
                if (entitySampleProperty.getEntitySampleId() != null
                                && !current.getEntity().getEntitySampleId()
                                                .equals(entitySampleProperty.getEntitySampleId()))
                        throw new LottabyteException(
                                        Message.LBE02007,
                                                        userDetails.getLanguage(),
                                        propertyId);
                if (userDetails.getStewardId() != null) {
                        if (!entitySampleRepository.hasAccessToSample(current.getEntity().getEntitySampleId(),
                                        userDetails))
                                throw new LottabyteException(
                                                                Message.LBE02006,
                                                                userDetails.getLanguage(),
                                                current.getEntity().getEntitySampleId());
                }

                if (!validateSamplePath(entitySampleProperty.getPath(), entitySampleProperty.getPathType()))
                        throw new LottabyteException(
                                        Message.LBE00033, userDetails.getLanguage(),
                                        entitySampleProperty.getPath());

                if (entitySampleProperty.getMappedAttributeIds() != null
                                && !entitySampleProperty.getMappedAttributeIds().isEmpty()) {
                        EntitySample es = getEntitySampleById(current.getEntity().getEntitySampleId(), true,
                                        userDetails);
                        if (!entityRepository.entityAttributesExistAndBelongToEntity(
                                        entitySampleProperty.getMappedAttributeIds(),
                                        es.getEntity().getEntityId(), userDetails))
                                throw new LottabyteException(
                                                                Message.LBE00034,
                                                                userDetails.getLanguage(),
                                                es.getEntity().getEntityId(),
                                                StringUtils.join(entitySampleProperty.getMappedAttributeIds(), ", "));
                }
                EntitySampleProperty sampleProperty = entitySampleRepository.updateSampleProperty(propertyId,
                                entitySampleProperty, userDetails);
                // elasticsearchService.updateElasticSearchEntity(Collections.singletonList(sampleProperty.getSearchableArtifact()),
                // userDetails);
                return entitySampleRepository.getSamplePropertyById(propertyId, userDetails);
        }

        public void deleteSampleProperty(String propertyId, Boolean force, UserDetails userDetails)
                        throws LottabyteException {
                if (!entitySampleRepository.samplePropertyExists(propertyId, userDetails))
                        throw new LottabyteException(Message.LBE02002,
                                                        userDetails.getLanguage(), propertyId);
                if (userDetails.getStewardId() != null) {
                        EntitySampleProperty current = entitySampleRepository.getSamplePropertyById(propertyId,
                                        userDetails);
                        if (!entitySampleRepository.hasAccessToSample(current.getEntity().getEntitySampleId(),
                                        userDetails))
                                throw new LottabyteException(Message.LBE02006,
                                                                userDetails.getLanguage(),
                                                                current.getEntity().getEntitySampleId());
                }
                if (entitySampleRepository.existsSamplePropertyBySamplePropertyId(propertyId, userDetails))
                        throw new LottabyteException(Message.LBE00319,
                                                        userDetails.getLanguage(), propertyId);

                elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(propertyId), userDetails);
                entitySampleRepository.deleteSampleProperty(propertyId, force, userDetails);
        }

        public EntitySample updateSampleBody(String sampleId, String sampleBody, UserDetails userDetails)
                        throws LottabyteException {
                EntitySample entitySample = getEntitySampleById(sampleId, true, userDetails);
                if (entitySample == null)
                        throw new LottabyteException(Message.LBE02001,
                                                        userDetails.getLanguage(), sampleId);
                if (userDetails.getStewardId() != null
                                && !entitySampleRepository.hasAccessToSample(sampleId, userDetails))
                        throw new LottabyteException(Message.LBE02006,
                                                        userDetails.getLanguage(), sampleId);
                // if(entitySample != null &&
                // entitySample.getEntity().getSampleType().equals(EntitySampleType.json) &&
                // sampleBody != null && !this.validator.validate(sampleBody)){
                // throw new LottabyteException(HttpStatus.BAD_REQUEST,
                // Message.format(Message.LBE02101,
                // entitySample.getEntity().getSampleType().toString(), sampleBody));
                // }
                // if(entitySample != null &&
                // entitySample.getEntity().getSampleType().equals(EntitySampleType.xml) &&
                // sampleBody != null && !AllValidator.validate(sampleBody)){
                // throw new LottabyteException(HttpStatus.BAD_REQUEST,
                // Message.format(Message.LBE02101,
                // entitySample.getEntity().getSampleType().toString(), sampleBody));
                // }

                if (sampleBody.length() > 1024 * 1024)
                        throw new LottabyteException(
                                        Message.LBE02103,
                                        userDetails.getLanguage(), "1");

                // Прикрутить потом валидацию для всех типов файлов, которые могут,
                // потенциально, быть залиты
                iEntitySampleBodyRepository.createEntitySampleBody(sampleId, sampleBody, userDetails);

                EntitySample result = getEntitySampleById(sampleId, true, userDetails);
                // elasticsearchService.updateElasticSearchEntity(Collections.singletonList(result.getSearchableArtifact()),
                // userDetails);
                return result;
        }

        public EntitySample getEntitySampleById(String sampleId, Boolean includeBody, UserDetails userDetails)
                        throws LottabyteException {
                EntitySample entitySample = entitySampleRepository.getEntitySampleById(sampleId, includeBody,
                                userDetails);
                List<EntitySampleDQRule> dqRules = entitySampleRepository.getSampleDQRulesByEntitySample(sampleId,
                                userDetails);
                entitySample.getEntity().setDqRules(dqRules);

                if (entitySample == null)
                        throw new LottabyteException(Message.LBE02001,
                                                        userDetails.getLanguage(), sampleId);
                else
                        entitySample.getMetadata().setTags(tagService.getArtifactTags(sampleId, userDetails));
                return entitySample;
        }

        public EntitySample getMainEntitySampleByEntityIdAndSystemId(String entityId, String sampleId,
                        UserDetails userDetails) {
                return entitySampleRepository.getMainEntitySampleByEntityIdAndSystemId(entityId, sampleId, userDetails);
        }

        public EntitySample getEntitySampleByName(String entityName, UserDetails userDetails) {
                return entitySampleRepository.getEntitySampleByName(entityName, userDetails);
        }

        public String getEntitySampleBodyById(String entityId, UserDetails userDetails) {
                return entitySampleRepository.getEntitySampleBodyById(entityId, userDetails);
        }

        public PaginatedArtifactList<EntitySample> getEntitySampleWithPaging(Integer offset, Integer limit,
                        Boolean includeBody, UserDetails userDetails) {
                return entitySampleRepository.getEntitySampleWithPaging(offset, limit, includeBody, userDetails);
        }

        public EntitySample getEntitySampleByQueryId(String queryId, Boolean includeBody, UserDetails userDetails)
                        throws LottabyteException {
                EntitySample entitySample = entitySampleRepository.getEntitySampleByQueryId(queryId, userDetails);
                if (includeBody && entitySample != null) {
                        entitySample.getEntity().setSampleBody(
                                        entitySampleBodyService.getEntitySampleBodyFromS3ById(entitySample.getId(),
                                                        userDetails));
                }

                return entitySample;
        }

        @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
        public ArchiveResponse deleteSample(String sampleId, Boolean force, UserDetails userDetails)
                        throws LottabyteException {
                EntitySample entitySample = getEntitySampleById(sampleId, true, userDetails);
                if (entitySample == null)
                        throw new LottabyteException(Message.LBE02001,
                                                        userDetails.getLanguage(), sampleId);
                if (userDetails.getStewardId() != null
                                && !entitySampleRepository.hasAccessToSample(sampleId, userDetails))
                        throw new LottabyteException(Message.LBE02006,
                                                        userDetails.getLanguage(), sampleId);
                List<EntitySampleProperty> entitySamplePropertyList = getAllSamplePropertyBySampleId(sampleId,
                                userDetails);
                for (EntitySampleProperty entitySampleProperty : entitySamplePropertyList) {
                        if (entitySampleProperty != null)
                                deleteSampleProperty(entitySampleProperty.getId(), force, userDetails);
                }

                entitySampleRepository.deleteSample(sampleId, entitySample.getEntity().getSampleBody() != null,
                                userDetails);
                tagService.deleteAllTagsByArtifactId(sampleId, userDetails);
                commentService.deleteAllCommentsByArtifactId(sampleId, userDetails);
                ratingService.removeArtifactRate(sampleId, userDetails);
                ArchiveResponse archiveResponse = new ArchiveResponse();
                archiveResponse.setArchivedGuids(Collections.singletonList(sampleId));
                elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(sampleId), userDetails);
                entitySampleBodyService.deleteEntitySampleBodyFromS3ById(sampleId, userDetails);
                // entitySampleRepository.deleteSampleBody(sampleId, userDetails);

                return archiveResponse;
        }

        public SearchResponse<FlatEntitySample> searchEntitySamples(SearchRequestWithJoin request,
                        UserDetails userDetails)
                        throws LottabyteException {
                ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
                SearchResponse<FlatEntitySample> res = entitySampleRepository.searchEntitySamples(request,
                                searchableColumns,
                                joinColumns, userDetails);
                res.getItems().stream().forEach(
                                x -> x.setTags(tagService.getArtifactTags(x.getId(), userDetails)
                                                .stream().map(y -> y.getName()).collect(Collectors.toList())));

                return res;
        }

        public SearchResponse<FlatEntitySample> searchEntitySamplesByDomain(SearchRequestWithJoin request,
                        String domainId,
                        UserDetails userDetails) throws LottabyteException {
                ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
                if (domainId == null || domainId.isEmpty() || !domainId
                                .matches("[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}")) {
                        throw new LottabyteException(Message.LBE00101,
                                                        userDetails.getLanguage(), domainId);
                }
                if (request.getLimitSteward() != null && request.getLimitSteward() && userDetails.getStewardId() != null
                                &&
                                !domainRepository.hasAccessToDomain(domainId, userDetails))
                        return new SearchResponse<>(0, request.getLimit(), request.getOffset(), new ArrayList<>());

                return entitySampleRepository.searchEntitySamplesByDomain(request, domainId, searchableColumns,
                                joinColumns,
                                userDetails);
        }

        private void validateParams(UpdatableEntitySampleEntity entitySampleEntity, UserDetails userDetails)
                        throws LottabyteException {
                if (entitySampleEntity.getEntityId() != null) {
                        if (entityService.getDataEntityByIdAndState(entitySampleEntity.getEntityId(),
                                        ArtifactState.PUBLISHED,
                                        userDetails) == null)
                                throw new LottabyteException(Message.LBE00301,
                                                                userDetails.getLanguage(),
                                                                entitySampleEntity.getEntityId());
                        if (userDetails.getStewardId() != null
                                        && !entityRepository.hasAccessToEntity(entitySampleEntity.getEntityId(),
                                                        userDetails))
                                throw new LottabyteException(
                                                                Message.LBE00324,
                                                                userDetails.getLanguage(),
                                                entitySampleEntity.getEntityId());
                }
                if (entitySampleEntity.getEntityQueryId() != null) {
                        if (entityQueryService.getEntityQueryByIdAndStation(entitySampleEntity.getEntityQueryId(),
                                        ArtifactState.PUBLISHED, userDetails) == null)
                                throw new LottabyteException(Message.LBE00401,
                                                                userDetails.getLanguage(),
                                                                entitySampleEntity.getEntityQueryId());
                        if (userDetails.getStewardId() != null
                                        && !entityQueryService.hasAccessToQuery(entitySampleEntity.getEntityQueryId(),
                                                        userDetails))
                                throw new LottabyteException(Message.LBE00403,
                                                                userDetails.getLanguage(),
                                                                entitySampleEntity.getEntityQueryId());
                }
                if (entitySampleEntity.getSystemId() != null) {
                        if (systemService.getSystemByIdAndState(entitySampleEntity.getSystemId(),
                                        ArtifactState.PUBLISHED,
                                        userDetails) == null)
                                throw new LottabyteException(Message.LBE00904,
                                                                userDetails.getLanguage(),
                                                                entitySampleEntity.getSystemId());
                        if (userDetails.getStewardId() != null
                                        && !systemRepository.hasAccessToSystem(entitySampleEntity.getSystemId(),
                                                        userDetails))
                                throw new LottabyteException(
                                                                Message.LBE00921,
                                                                userDetails.getLanguage(),
                                                entitySampleEntity.getEntityId());
                }
        }

        public EntitySample createSample(UpdatableEntitySampleEntity newEntitySampleEntity, UserDetails userDetails)
                        throws LottabyteException {
                if (newEntitySampleEntity.getName() != null
                                && getEntitySampleByName(newEntitySampleEntity.getName(), userDetails) != null) {
                        throw new LottabyteException(Message.LBE02004,
                                                        userDetails.getLanguage(),
                                                        newEntitySampleEntity.getName());
                }
                validateParams(newEntitySampleEntity, userDetails);
                // if(newEntitySampleEntity.getSampleType().equals(EntitySampleType.json) &&
                // newEntitySampleEntity.getSampleBody() != null &&
                // !this.validator.validate(newEntitySampleEntity.getSampleBody())){
                // throw new LottabyteException(HttpStatus.BAD_REQUEST,
                // Message.format(Message.LBE02101,
                // newEntitySampleEntity.getSampleType().toString(),
                // newEntitySampleEntity.getSampleBody()));
                // }
                // if(newEntitySampleEntity.getSampleType().equals(EntitySampleType.xml) &&
                // newEntitySampleEntity.getSampleBody() != null &&
                // !AllValidator.validate(newEntitySampleEntity.getSampleBody())){
                // throw new LottabyteException(HttpStatus.BAD_REQUEST,
                // Message.format(Message.LBE02101,
                // newEntitySampleEntity.getSampleType().toString(),
                // newEntitySampleEntity.getSampleBody()));
                // }
                if (newEntitySampleEntity.getIsMain() != null && newEntitySampleEntity.getIsMain()) {
                        entitySampleRepository.updateAllEntitySampleMainStatus(newEntitySampleEntity.getEntityId(),
                                        newEntitySampleEntity.getSystemId(), userDetails);
                }
                String customAttributeDefElementId = entitySampleRepository.createEntitySampleEntity(
                                newEntitySampleEntity,
                                userDetails);
                if (newEntitySampleEntity.getSampleBody() != null && !newEntitySampleEntity.getSampleBody().isEmpty())
                        iEntitySampleBodyRepository.createEntitySampleBody(customAttributeDefElementId,
                                        newEntitySampleEntity.getSampleBody(), userDetails);

                EntitySample entitySample = getEntitySampleById(customAttributeDefElementId, true, userDetails);
                elasticsearchService.insertElasticSearchEntity(
                                Collections.singletonList(getSearchableArtifact(entitySample, userDetails)),
                                userDetails);
                return entitySample;
        }

        public EntitySample patchSample(String sampleId, UpdatableEntitySampleEntity entitySampleEntity,
                        UserDetails userDetails) throws LottabyteException {
                EntitySample entitySample = getEntitySampleById(sampleId, true, userDetails);
                if (entitySample == null)
                        throw new LottabyteException(Message.LBE02001,
                                                        userDetails.getLanguage(), sampleId);
                if (userDetails.getStewardId() != null
                                && !entitySampleRepository.hasAccessToSample(sampleId, userDetails))
                        throw new LottabyteException(Message.LBE02006,
                                                        userDetails.getLanguage(), sampleId);
                if (entitySampleEntity.getName() != null
                                && getEntitySampleByName(entitySampleEntity.getName(), userDetails) != null)
                        throw new LottabyteException(Message.LBE02004,
                                                        userDetails.getLanguage(),
                                                        entitySampleEntity.getName());
                if (entitySampleEntity.getName() != null) {
                        EntitySample entitySampleByName = getEntitySampleByName(entitySampleEntity.getName(),
                                        userDetails);
                        if (entitySampleByName != null && !entitySampleByName.getId().equals(sampleId))
                                throw new LottabyteException(
                                                Message.LBE01904,
                                                userDetails.getLanguage(), entitySampleByName.getName());
                }
                validateParams(entitySampleEntity, userDetails);
                if (entitySampleEntity.getSampleType() != null
                                && entitySampleEntity.getSampleType().equals(EntitySampleType.json)
                                && entitySampleEntity.getSampleBody() != null
                                && !this.validator.validate(entitySampleEntity.getSampleBody())) {
                        throw new LottabyteException(Message.LBE02101,
                                                        userDetails.getLanguage(),
                                                        entitySampleEntity.getSampleType().toString(),
                                                        entitySampleEntity.getSampleBody());
                }
                if (entitySampleEntity.getSampleType() == null && entitySampleEntity.getSampleBody() != null
                                && entitySample.getEntity().getSampleType().equals(EntitySampleType.json)
                                && !this.validator.validate(entitySampleEntity.getSampleBody())) {
                        throw new LottabyteException(Message.LBE02101,
                                                        userDetails.getLanguage(),
                                                        entitySample.getEntity().getSampleType().toString(),
                                                        entitySampleEntity.getSampleBody());
                }
                if (entitySampleEntity.getSampleType() != null
                                && entitySampleEntity.getSampleType().equals(EntitySampleType.xml)
                                && entitySampleEntity.getSampleBody() != null
                                && !AllValidator.isXml(entitySampleEntity.getSampleBody())) {
                        throw new LottabyteException(Message.LBE02101,
                                                        userDetails.getLanguage(),
                                                        entitySampleEntity.getSampleType().toString(),
                                                        entitySampleEntity.getSampleBody());
                }
                if (entitySampleEntity.getSampleType() != null && entitySampleEntity.getSampleBody() == null
                                && !entitySample.getEntity().getSampleType()
                                                .equals(entitySampleEntity.getSampleType())) {
                        throw new LottabyteException(Message.LBE02102,
                                                        userDetails.getLanguage(),
                                                        entitySample.getEntity().getSampleType().toString());
                }
                if (entitySampleEntity.getIsMain() != null && entitySampleEntity.getIsMain()) {
                        if (entitySampleEntity.getEntityId() != null && entitySampleEntity.getSystemId() != null) {
                                entitySampleRepository.updateAllEntitySampleMainStatus(entitySampleEntity.getEntityId(),
                                                entitySampleEntity.getSystemId(), userDetails);
                        } else {
                                entitySampleRepository.updateAllEntitySampleMainStatus(
                                                entitySample.getEntity().getEntityId(),
                                                entitySample.getEntity().getSystemId(), userDetails);
                        }
                }
                entitySampleRepository.patchSample(sampleId, entitySampleEntity, userDetails);
                EntitySample sample = getEntitySampleById(sampleId, true, userDetails);
                elasticsearchService.updateElasticSearchEntity(
                                Collections.singletonList(getSearchableArtifact(sample, userDetails)), userDetails);
                return sample;
        }

        public List<EntitySampleProperty> getEntitySamplePropertiesFromTable(String textSampleBody)
                        throws LottabyteException {
                try {
                        List<EntitySampleProperty> entitySamplePropertyList = new ArrayList<>();
                        Map<String, Object> result = new ObjectMapper().readValue(textSampleBody, HashMap.class);
                        ObjectMapper objectMapper = new ObjectMapper();
                        String respData = objectMapper.writeValueAsString(result.get("fields"));
                        List<HashMap<String, Object>> list = objectMapper.readValue(respData, List.class);
                        if (list != null && !list.isEmpty()) {
                                for (HashMap<String, Object> map : list) {
                                        EntitySampleProperty entitySampleProperty = new EntitySampleProperty();

                                        EntitySamplePropertyEntity espEntity = new EntitySamplePropertyEntity();
                                        espEntity.setName((String) map.get("name"));
                                        espEntity.setPath((String) map.get("name"));
                                        espEntity.setType((String) map.get("type"));
                                        espEntity.setPathType(EntitySamplePropertyPathType.column_name);// переделать на
                                                                                                        // тип, через
                                                                                                        // условку,
                                                                                                        // сейчас
                                                                                                        // статический

                                        Metadata md = new Metadata();
                                        md.setArtifactType(espEntity.getArtifactType().toString());

                                        entitySampleProperty.setEntity(espEntity);
                                        entitySampleProperty.setMetadata(md);

                                        entitySamplePropertyList.add(entitySampleProperty);
                                }
                        }
                        return entitySamplePropertyList;
                } catch (JsonProcessingException e) {
                        throw new LottabyteException(e.getMessage());
                }
        }

        public PaginatedArtifactList<EntitySample> getEntitySampleVersions(String sampleId, Integer offset,
                        Integer limit,
                        UserDetails userDetails) throws LottabyteException {
                if (entitySampleRepository.getById(sampleId, userDetails) == null) {
                        throw new LottabyteException(
                                        Message.LBE02001,
                                                        userDetails.getLanguage(), sampleId);
                }

                return entitySampleRepository.getEntitySampleVersions(sampleId, offset, limit, userDetails);
        }

        public SearchResponse<FlatEntitySampleProperty> searchSampleProperties(SearchRequestWithJoin request,
                        UserDetails userDetails) throws LottabyteException {
                ServiceUtils.validateSearchRequestWithJoin(request, searchablePropertyColumns, joinPropertyColumns,
                                userDetails);

                return entitySampleRepository.searchSampleProperties(request, searchablePropertyColumns,
                                joinPropertyColumns,
                                userDetails);
        }

        public SearchResponse<FlatEntitySampleDQRule> searchSampleDQRules(SearchRequestWithJoin request,
                        UserDetails userDetails) throws LottabyteException {
                ServiceUtils.validateSearchRequestWithJoin(request, searchableDQRuleColumns, joinPropertyColumns,
                                userDetails);

                return entitySampleRepository.searchSampleDQRules(request, searchableDQRuleColumns, joinPropertyColumns,
                                userDetails);
        }

        public List<EntitySampleDQRule> getSampleDQRules(String sampleId,
                        UserDetails userDetails) throws LottabyteException {

                return entitySampleRepository.getSampleDQRulesByEntitySample(sampleId,
                                userDetails);
        }

        public EntitySampleDQRule getSampleDQRuleById(String dqRuleId, UserDetails userDetails) {
                return entitySampleRepository.getSampleDQRule(dqRuleId, userDetails);
        }

        public void deleteSampleDQRuleBy(String dqRuleId, Boolean force, UserDetails userDetails)
                        throws LottabyteException {

                entitySampleRepository.deleteSampleDQRule(dqRuleId, force, userDetails);
        }

        public EntitySampleDQRule updateSampleDQRule(String dqRuleId,
                        UpdatableEntitySampleDQRule entitySampleDQRule, UserDetails userDetails)
                        throws LottabyteException {
                if (!entitySampleRepository.existsSampleDQRuleByDQRuleId(dqRuleId, userDetails))
                        throw new LottabyteException(
                                        Message.LBE02002,
                                                        userDetails.getLanguage(),
                                        dqRuleId);

                EntitySampleDQRule result = entitySampleRepository.updateSampleDQRule(dqRuleId,
                                entitySampleDQRule, userDetails);

                return result;
        }

        public EntitySampleDQRule createSampleDQRule(String sampleId,
                        UpdatableEntitySampleDQRule entitySampleDQRule, UserDetails userDetails)
                        throws LottabyteException {
                if (!entitySampleRepository.existsById(sampleId, userDetails))
                        throw new LottabyteException(Message.LBE02001,
                                                        userDetails.getLanguage(), sampleId);

                EntitySampleDQRule sampleDQRule = entitySampleRepository.createSampleDQRule(sampleId,
                                entitySampleDQRule, userDetails);
                // elasticsearchService.insertElasticSearchEntity(Collections.singletonList(sampleProperty.getSearchableArtifact()),
                // userDetails);
                return entitySampleRepository.getSampleDQRule(sampleDQRule.getId(), userDetails);
        }

        public SearchableEntitySample getSearchableArtifact(EntitySample entitySample, UserDetails userDetails) {
                SearchableEntitySample sa = SearchableEntitySample.builder()
                        .id(entitySample.getMetadata().getId())
                        .versionId(entitySample.getMetadata().getVersionId())
                        .name(entitySample.getMetadata().getName())
                        .description(entitySample.getEntity().getDescription())
                        .modifiedBy(entitySample.getMetadata().getModifiedBy())
                        .modifiedAt(entitySample.getMetadata().getModifiedAt())
                        .artifactType(entitySample.getMetadata().getArtifactType())
                        .effectiveStartDate(entitySample.getMetadata().getEffectiveStartDate())
                        .effectiveEndDate(entitySample.getMetadata().getEffectiveEndDate())
                        .tags(Helper.getEmptyListIfNull(entitySample.getMetadata().getTags()).stream()
                                        .map(x -> x.getName()).collect(Collectors.toList()))

                        .entityId(entitySample.getEntity().getEntityId())
                        .systemId(entitySample.getEntity().getSystemId())
                        .entityQueryId(entitySample.getEntity().getEntityQueryId())
                        .sampleType(entitySample.getEntity().getSampleType())
                        .sampleBody(entitySample.getEntity().getSampleBody())
                        .isMain(entitySample.getEntity().getIsMain()).build();

                sa.setDomains(entitySampleRepository.getDomainIdsBySystemId(entitySample.getEntity().getSystemId(),
                                userDetails));

                sa.setPropertyNames(Helper.getEmptyListIfNull(entitySampleRepository.getAllSamplePropertyBySampleId(entitySample.getMetadata().getId(), userDetails)
                        .stream().map(esp -> esp.getName()).collect(Collectors.toList())));

                return sa;
        }

        public SearchableEntitySampleProperty getPropertySearchableArtifact(EntitySampleProperty entitySampleProperty,
                        UserDetails userDetails) {
                SearchableEntitySampleProperty sa = SearchableEntitySampleProperty.builder()
                        .id(entitySampleProperty.getMetadata().getId())
                        .versionId(entitySampleProperty.getMetadata().getVersionId())
                        .name(entitySampleProperty.getMetadata().getName())
                        .description(entitySampleProperty.getEntity().getDescription())
                        .modifiedBy(entitySampleProperty.getMetadata().getModifiedBy())
                        .modifiedAt(entitySampleProperty.getMetadata().getModifiedAt())
                        .artifactType(entitySampleProperty.getMetadata().getArtifactType())
                        .effectiveStartDate(entitySampleProperty.getMetadata().getEffectiveStartDate())
                        .effectiveEndDate(entitySampleProperty.getMetadata().getEffectiveEndDate())
                        .tags(Helper.getEmptyListIfNull(entitySampleProperty.getMetadata().getTags()).stream()
                                        .map(x -> x.getName()).collect(Collectors.toList()))

                        .pathType(entitySampleProperty.getEntity().getPathType())
                        .path(entitySampleProperty.getEntity().getPath())
                        .entitySampleId(entitySampleProperty.getEntity().getEntitySampleId())
                        .mappedAttributeIds(entitySampleProperty.getEntity().getMappedAttributeIds()).build();

                EntitySample es = entitySampleRepository
                                .getById(entitySampleProperty.getEntity().getEntitySampleId(), userDetails);
                if (es != null)
                        sa.setEntitySampleName(es.getName());

                return sa;
        }

        public static boolean containsDQRule(List<EntitySampleDQRule> ids, EntitySampleDQRule pattern) {
                boolean result = false;
                for (EntitySampleDQRule val : ids) {
                        if (val.getEntity().getDqRuleId().equals(pattern.getEntity().getDqRuleId()))
                                result = true;
                }
                return result;
        }
}
