package ru.bssg.lottabyte.coreapi.service.connector;

import lombok.extern.slf4j.Slf4j;
import org.jooq.impl.DSL;
import org.jooq.tools.StringUtils;
import org.json.JSONObject;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.connector.IConnectorService;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.connector.Connector;
import ru.bssg.lottabyte.core.model.connector.ConnectorParam;
import ru.bssg.lottabyte.core.model.dataentity.DataEntity;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQueryResult;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleType;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.model.system.SystemConnection;
import ru.bssg.lottabyte.core.model.system.SystemConnectionParam;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.*;

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

}
