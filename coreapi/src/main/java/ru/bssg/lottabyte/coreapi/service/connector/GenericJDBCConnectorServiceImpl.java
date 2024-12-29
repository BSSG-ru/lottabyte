package ru.bssg.lottabyte.coreapi.service.connector;

import lombok.extern.slf4j.Slf4j;
import org.jooq.impl.DSL;
import org.jooq.tools.StringUtils;
import org.json.JSONObject;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.connector.IConnectorService;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.connector.Connector;
import ru.bssg.lottabyte.core.model.connector.ConnectorParam;
import ru.bssg.lottabyte.core.model.dataentity.DataEntity;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQueryResult;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleType;
import ru.bssg.lottabyte.core.model.metaColumn.FlatMetaColumn;
import ru.bssg.lottabyte.core.model.metaDatabase.FlatMetaDatabase;
import ru.bssg.lottabyte.core.model.metaObject.FlatMetaObject;
import ru.bssg.lottabyte.core.model.reference.Reference;
import ru.bssg.lottabyte.core.model.reference.ReferenceType;
import ru.bssg.lottabyte.core.model.reference.UpdatableReferenceEntity;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.model.system.SystemConnection;
import ru.bssg.lottabyte.core.model.system.SystemConnectionParam;
import ru.bssg.lottabyte.core.model.task.Task;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.service.ElasticsearchService;
import ru.bssg.lottabyte.coreapi.service.MetadataService;
import ru.bssg.lottabyte.coreapi.service.ReferenceService;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GenericJDBCConnectorServiceImpl implements IConnectorService {

    private final String storagePath = "share";
    private Connection getConnection(String jdbcUrl, String jdbcUsername, String jdbcPassword, String jdbcDriverClassName, String jdbcDriverJarName) throws LottabyteException {
        try {
            Class.forName(jdbcDriverClassName);
            return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
        } catch (Exception e) {
            try {
                if (jdbcDriverJarName != null) {
                    File f = new File(storagePath + "/" + jdbcDriverJarName);
                    URLClassLoader urlCl = new URLClassLoader(new URL[]{f.toURI().toURL()}, System.class.getClassLoader());
                    Class<?> mySqlDriver = urlCl.loadClass(jdbcDriverClassName);

                    Driver sqlDriverInstance = (Driver) mySqlDriver.newInstance();
                    log.info("sqlDriverInstance: " + sqlDriverInstance);

                    Properties userDbCredentials = new Properties();
                    userDbCredentials.put("user", jdbcUsername);
                    userDbCredentials.put("password", jdbcPassword);

                    Connection con = sqlDriverInstance.connect(jdbcUrl, userDbCredentials);
                    log.info("con: " + con);

                    return con;
                }
                throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            } catch (MalformedURLException | InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException error) {
                throw new LottabyteException(HttpStatus.NOT_FOUND, error.getMessage(), error);
            }

        }
    }

    public EntityQueryResult querySystem(Connector connector,
                                         List<ConnectorParam> connectorParams,
                                         System system,
                                         DataEntity entity,
                                         EntityQuery entityQuery,
                                         SystemConnection systemConnection,
                                         List<SystemConnectionParam> systemConnectionParams,
                                         UserDetails userDetails)
            throws LottabyteException {
        EntityQueryResult res = new EntityQueryResult();

        if (entityQuery.getEntity() == null || entityQuery.getEntity().getQueryText() == null || entityQuery.getEntity().getQueryText().isEmpty())
            throw new LottabyteException(Message.format(Message.LBE00005.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), entityQuery.getId()));

        Map<String, String> paramValues = new HashMap<>();

        for (SystemConnectionParam scp : systemConnectionParams) {
            if (scp.getEntity() == null)
                throw new LottabyteException(Message.format(Message.LBE00006.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), scp.getId()));

            Optional<ConnectorParam> cp = connectorParams.stream().filter(
                    x -> x.getId().equals(scp.getEntity().getConnectorParamId())
            ).findFirst();
            if (cp.isEmpty())
                throw new LottabyteException(Message.format(Message.LBE00007.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), scp.getEntity().getConnectorParamId()));
            if(scp.getEntity().getParamValue() != null)
                paramValues.put(cp.get().getEntity().getName(), scp.getEntity().getParamValue());
        }

        if (!paramValues.containsKey("jdbc_url"))
            throw new LottabyteException(Message.format(Message.LBE00008.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), "jdbc_url"));
        if (!paramValues.containsKey("jdbc_username"))
            throw new LottabyteException(Message.format(Message.LBE00008.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), "jdbc_username"));
        if (!paramValues.containsKey("jdbc_password"))
            throw new LottabyteException(Message.format(Message.LBE00008.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), "jdbc_password"));
        if (!paramValues.containsKey("jdbc_driver_class_name"))
            throw new LottabyteException(Message.format(Message.LBE00008.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), "jdbc_driver_class_name"));

        Connection conn = getConnection(paramValues.get("jdbc_url"), paramValues.get("jdbc_username"), paramValues.get("jdbc_password"),
                paramValues.get("jdbc_driver_class_name"), paramValues.get("jdbc_driver_jar_name"));

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String q = entityQuery.getEntity().getQueryText();
            if (!StringUtils.isEmpty(paramValues.getOrDefault("sql_limit_position", null))
                && !StringUtils.isEmpty(paramValues.getOrDefault("sql_limit_clause", null))
                && !StringUtils.isEmpty(paramValues.getOrDefault("sql_record_count", null))
            ) {
                switch (paramValues.get("sql_limit_position")) {
                    case "SUFFIX":
                        q = "SELECT * FROM (" + q + ") AS main " + paramValues.get("sql_limit_clause") + " " + paramValues.get("sql_record_count");
                        break;
                    case "PREFIX":
                        q = "SELECT " + paramValues.get("sql_limit_clause") + " " + paramValues.get("sql_record_count") + " * FROM (" + q + ") AS main";
                        break;
                }
            }

            stmt = conn.prepareStatement(q);

            if (paramValues.containsKey("jdbc_command_timeout") && !paramValues.get("jdbc_command_timeout").isEmpty())
                stmt.setQueryTimeout(Integer.parseInt(paramValues.get("jdbc_command_timeout")));

            rs = stmt.executeQuery();

            JSONObject json = new JSONObject(DSL.using(conn).fetch(rs).formatJSON());

            res.setTextSampleBody(json.toString());
        } catch (Exception e) {
            throw new LottabyteException(Message.LBE00009, e);
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (Exception e) { throw new LottabyteException(Message.LBE00010, e); }
            }
            if (stmt != null) {
                try { stmt.close(); } catch (Exception e) { throw new LottabyteException(Message.LBE00011, e); }
            }
            try { conn.close(); } catch (Exception e) { throw new LottabyteException(Message.LBE00012, e); }
        }

        res.setSampleType(EntitySampleType.table);
        return res;
    }

    private void queryMetaDataSchemas(FlatMetaDatabase metaDatabase, Connection connection, MetadataService metadataService, UserDetails userDetails) throws SQLException {

        PreparedStatement stmt = connection.prepareStatement("SELECT schema_name FROM information_schema.schemata");
        ResultSet rs = null;
        try {
            rs = stmt.executeQuery();

            while (rs.next()) {
                FlatMetaObject schema = new FlatMetaObject();
                schema.setId(UUID.randomUUID().toString());
                schema.setName(rs.getString(1));
                schema.setMetaDatabaseId(UUID.fromString(metaDatabase.getId()));
                schema.setParentId(null);
                schema.setVersionId(metaDatabase.getVersionId());
                schema.setMetaObjectType("SCHEMA");

                listMetaObjects.add(schema);

                queryMetaDataTables(schema, connection);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        }
    }

    private void queryMetaDataTables(FlatMetaObject schema, Connection connection) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT tablename FROM pg_catalog.pg_tables WHERE schemaname=?");
        ResultSet rs = null;
        try {
            stmt.setString(1, schema.getName());
            rs = stmt.executeQuery();

            while (rs.next()) {
                FlatMetaObject tbl = new FlatMetaObject();
                tbl.setId(UUID.randomUUID().toString());
                tbl.setName(rs.getString(1));
                tbl.setMetaDatabaseId(schema.getMetaDatabaseId());
                tbl.setParentId(UUID.fromString(schema.getId()));
                tbl.setParentName(schema.getName());
                tbl.setVersionId(schema.getVersionId());
                tbl.setMetaObjectType("TABLE");

                listMetaObjects.add(tbl);

                queryMetaDataColumns(tbl, connection);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        }

        stmt = connection.prepareStatement("SELECT viewname FROM pg_catalog.pg_views WHERE schemaname=?");
        rs = null;
        try {
            stmt.setString(1, schema.getName());
            rs = stmt.executeQuery();

            while (rs.next()) {
                FlatMetaObject tbl = new FlatMetaObject();
                tbl.setId(UUID.randomUUID().toString());
                tbl.setName(rs.getString(1));
                tbl.setMetaDatabaseId(schema.getMetaDatabaseId());
                tbl.setParentId(UUID.fromString(schema.getId()));
                tbl.setParentName(schema.getName());
                tbl.setVersionId(schema.getVersionId());
                tbl.setMetaObjectType("VIEW");

                listMetaObjects.add(tbl);

                queryMetaDataColumns(tbl, connection);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        }
    }

    public void queryMetaDataColumns(FlatMetaObject table, Connection connection) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM information_schema.columns WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position");
        ResultSet rs = null;
        try {
            stmt.setString(1, table.getParentName());
            stmt.setString(2, table.getName());
            rs = stmt.executeQuery();

            while (rs.next()) {
                FlatMetaColumn col = new FlatMetaColumn();
                col.setId(UUID.randomUUID().toString());
                col.setName(rs.getString("column_name"));
                col.setColumnType(rs.getString("data_type"));
                col.setIsKey(false);
                col.setMetaObjectId(UUID.fromString(table.getId()));
                col.setMetaDatabaseId(table.getMetaDatabaseId());
                col.setMetaObjectName(table.getName());
                col.setSchemaName(table.getParentName());

                listMetaColumns.add(col);

            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        }
    }

    private List<FlatMetaObject> listMetaObjects = new ArrayList<>();
    private List<FlatMetaColumn> listMetaColumns = new ArrayList<>();

    private boolean haveChanges(FlatMetaDatabase fmb, MetadataService metadataService, UserDetails userDetails) {
        boolean haveChanges = false;

        for (FlatMetaObject fmo : listMetaObjects) {
            if (metadataService.metaObjectChanged(fmo, userDetails))
                return true;
        }
        for (FlatMetaColumn fmc : listMetaColumns) {
            if (metadataService.metaColumnChanged(fmc, userDetails))
                return true;
        }

        for (FlatMetaObject dbFMO : metadataService.listMetaObjects(fmb, userDetails)) {
            if (listMetaObjects.stream().filter(fmo -> {
                if (fmo.getMetaObjectType().equals("SCHEMA"))
                    return fmo.getMetaObjectType().equals(dbFMO.getMetaObjectType()) && fmo.getName().equals(dbFMO.getName());
                else
                    return fmo.getMetaObjectType().equals(dbFMO.getMetaObjectType()) && fmo.getName().equals(dbFMO.getName()) && fmo.getParentName().equals(dbFMO.getParentName());
            }).count() == 0)
                return true;
        }

        for (FlatMetaColumn dbFMC : metadataService.listMetaColumns(fmb, userDetails)) {
            if (listMetaColumns.stream().filter(fmc -> {
                return fmc.getSchemaName().equals(dbFMC.getSchemaName()) && fmc.getMetaObjectName().equals(dbFMC.getMetaObjectName()) && fmc.getName().equals(dbFMC.getName());
            }).count() == 0)
                return true;
        }

        return haveChanges;
    }

    private void saveMetaDataVersion(FlatMetaDatabase fmb, Task task, MetadataService metadataService, ElasticsearchService elasticsearchService, ReferenceService referenceService, UserDetails userDetails) throws LottabyteException {

        /*List <String> ids = metadataService.listMetaObjects(fmb, userDetails).stream().map(FlatModeledObject::getId).collect(Collectors.toList());
        if (!ids.isEmpty())
            elasticsearchService.deleteElasticSearchEntityById(ids, userDetails);
        ids = metadataService.listMetaColumns(fmb, userDetails).stream().map(FlatModeledObject::getId).collect(Collectors.toList());
        if (!ids.isEmpty())
            elasticsearchService.deleteElasticSearchEntityById(ids, userDetails);*/


        FlatMetaDatabase f1 = new FlatMetaDatabase();
        f1.setId(fmb.getId());
        f1.setVersionId(fmb.getVersionId());
        f1.setState("DRAFT_HISTORY");
        metadataService.updateMetaDatabase(f1, userDetails);

        for (FlatMetaObject mo : metadataService.listMetaObjects(f1, userDetails))
            elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(mo.getId()), userDetails);
        for (FlatMetaColumn mc : metadataService.listMetaColumns(f1, userDetails))
            elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(mc.getId()), userDetails);

        int versionId = fmb.getVersionId() + 1;
        fmb.setVersionId(versionId);
        fmb.setState("PUBLISHED");

        fmb = metadataService.createMetaDatabase(fmb, userDetails);
        //metadataService.updateMetaDatabase(fmb, userDetails);
        Reference ref = null;
        try { ref = referenceService.getReferenceBySourceIdAndTargetId(fmb.getId(), task.getId(), userDetails); }
        catch (Exception e) { }

        if (ref == null) {
            UpdatableReferenceEntity newRef = new UpdatableReferenceEntity();
            newRef.setSourceId(fmb.getId());
            newRef.setSourceType(ArtifactType.meta_database);
            newRef.setTargetId(task.getId());
            newRef.setTargetType(ArtifactType.task);
            newRef.setReferenceType(ReferenceType.META_DATABASE_TO_TASK);
            referenceService.createReference(newRef, userDetails);
        }

        for (FlatMetaObject schema : listMetaObjects.stream().filter(mo -> mo.getMetaObjectType().equals("SCHEMA")).collect(Collectors.toUnmodifiableList())) {
            schema.setVersionId(versionId);
            schema.setMetaDatabaseId(UUID.fromString(fmb.getId()));
            metadataService.createMetaObject(schema, userDetails);

            for (FlatMetaObject tbl : listMetaObjects.stream().filter(mo -> !mo.getMetaObjectType().equals("SCHEMA") && mo.getParentName().equals(schema.getName())).collect(Collectors.toUnmodifiableList())) {
                tbl.setVersionId(versionId);
                tbl.setMetaDatabaseId(UUID.fromString(fmb.getId()));
                metadataService.createMetaObject(tbl, userDetails);

                for (FlatMetaColumn col : listMetaColumns.stream().filter(c -> c.getMetaObjectName().equals(tbl.getName()) && c.getSchemaName().equals(schema.getName())).collect(Collectors.toUnmodifiableList())) {
                    col.setVersionId(versionId);
                    col.setMetaDatabaseId(UUID.fromString(fmb.getId()));
                    metadataService.createMetaColumn(col, userDetails);
                }
            }
        }
    }

    public void queryMetaData(Task task, Connector connector,
                              List<ConnectorParam> connectorParams,
                              SystemConnection systemConnection,
                              List<SystemConnectionParam> systemConnectionParams,
                              UserDetails userDetails, MetadataService metadataService,
                              ElasticsearchService elasticsearchService,
                              ReferenceService referenceService)
            throws LottabyteException, SQLException, ClassNotFoundException {

        Map<String, String> paramValues = new HashMap<>();

        for (SystemConnectionParam scp : systemConnectionParams) {
            if (scp.getEntity() == null)
                throw new LottabyteException(Message.format(Message.LBE00006.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), scp.getId()));

            Optional<ConnectorParam> cp = connectorParams.stream().filter(
                    x -> x.getId().equals(scp.getEntity().getConnectorParamId())
            ).findFirst();
            if (cp.isEmpty())
                throw new LottabyteException(Message.format(Message.LBE00007.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), scp.getEntity().getConnectorParamId()));
            if(scp.getEntity().getParamValue() != null)
                paramValues.put(cp.get().getEntity().getName(), scp.getEntity().getParamValue());
        }

        if (!paramValues.containsKey("jdbc_url"))
            throw new LottabyteException(Message.format(Message.LBE00008.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), "jdbc_url"));
        if (!paramValues.containsKey("jdbc_username"))
            throw new LottabyteException(Message.format(Message.LBE00008.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), "jdbc_username"));
        if (!paramValues.containsKey("jdbc_password"))
            throw new LottabyteException(Message.format(Message.LBE00008.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), "jdbc_password"));
        if (!paramValues.containsKey("jdbc_driver_class_name"))
            throw new LottabyteException(Message.format(Message.LBE00008.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), "jdbc_driver_class_name"));

        Connection conn = getConnection(paramValues.get("jdbc_url"), paramValues.get("jdbc_username"), paramValues.get("jdbc_password"),
                paramValues.get("jdbc_driver_class_name"), paramValues.get("jdbc_driver_jar_name"));

        if (conn == null)
            throw new LottabyteException("Connection failed");

        try {
            listMetaObjects.clear();
            listMetaColumns.clear();

            FlatMetaDatabase fmb = metadataService.findMetaDatabaseByUrl(paramValues.get("jdbc_url"), userDetails);
            boolean isNew = (fmb == null);
            if (isNew) {
                fmb = new FlatMetaDatabase();
                fmb.setId(UUID.randomUUID().toString());
                fmb.setName(task.getName() == null ? "test" : task.getName());
                fmb.setJdbcUrl(paramValues.get("jdbc_url"));
                fmb.setDriverClassName(paramValues.get("jdbc_driver_class_name"));
                fmb.setVersionId(0);
                fmb.setState("PUBLISHED");

                fmb = metadataService.createMetaDatabase(fmb, userDetails);
            }

            queryMetaDataSchemas(fmb, conn, metadataService, userDetails);

            if (isNew || haveChanges(fmb, metadataService, userDetails)) {
                saveMetaDataVersion(fmb, task, metadataService, elasticsearchService, referenceService, userDetails);
            }


        } catch (Exception e) {
            throw new LottabyteException(Message.LBE00009, e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception e) { throw new LottabyteException(Message.LBE00012, e); }
            }
        }

    }
}
