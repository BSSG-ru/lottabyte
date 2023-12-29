package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.dal.FlatItemRowMapper;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.dataasset.UpdatableDataAssetEntity;
import ru.bssg.lottabyte.core.model.reference.Reference;
import ru.bssg.lottabyte.core.model.steward.Steward;
import ru.bssg.lottabyte.core.model.steward.UpdatableStewardEntity;
import ru.bssg.lottabyte.core.model.system.*;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class SystemConnectionRepository extends GenericArtifactRepository<SystemConnection> {
    private final JdbcTemplate jdbcTemplate;

    private static String[] extFields = {"connector_id", "system_id", "enabled"};

    @Autowired
    public SystemConnectionRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.system_connection.name(), extFields);
        super.setMapper(new SystemConnectionRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    private class SystemConnectionRowMapper implements RowMapper<SystemConnection> {
        @Override
        public SystemConnection mapRow(ResultSet rs, int rowNum) throws SQLException {
            SystemConnection s = null;

            SystemConnectionEntity systemConnectionEntity = new SystemConnectionEntity();
            systemConnectionEntity.setName(rs.getString("name"));
            systemConnectionEntity.setDescription(rs.getString("description"));
            systemConnectionEntity.setSystemId(rs.getString("system_id"));
            systemConnectionEntity.setArtifactType(ArtifactType.system_connection);
            systemConnectionEntity.setEnabled(rs.getBoolean("enabled"));
            systemConnectionEntity.setConnectorId(rs.getString("connector_id"));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setName(rs.getString("name"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));

            try {
                s = new SystemConnection(systemConnectionEntity, md);
            } catch (LottabyteException e) {
                log.error(e.getMessage(), e);
            }
            return s;
        }
    }

    private class SystemConnectionParamRowMapper implements RowMapper<SystemConnectionParam> {
        @Override
        public SystemConnectionParam mapRow(ResultSet rs, int rowNum) throws SQLException {
            SystemConnectionParam s = null;

            SystemConnectionParamEntity systemConnectionParamEntity = new SystemConnectionParamEntity();
            systemConnectionParamEntity.setConnectorParamId(rs.getString("connector_param_id"));
            systemConnectionParamEntity.setArtifactType(ArtifactType.system_connection_param);
            systemConnectionParamEntity.setSystemConnectionId(rs.getString("system_connection_id"));
            systemConnectionParamEntity.setParamValue(rs.getString("param_value"));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));

            try {
                s = new SystemConnectionParam(systemConnectionParamEntity, md);
            } catch (LottabyteException e) {
                log.error(e.getMessage(), e);
            }
            return s;
        }
    }

    private class FlatSystemConnectorRowMapper extends FlatItemRowMapper<FlatSystemConnection> {

        public FlatSystemConnectorRowMapper() { super(FlatSystemConnection::new); }

        @Override
        public FlatSystemConnection mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatSystemConnection c = super.mapRow(rs, rowNum);
            c.setConnectorName(rs.getString("connector_name"));
            c.setSystemName(rs.getString("system_name"));
            return c;
        }
    }

    public boolean existSystemConnectionsInSystem(String systemId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(select id from da_" + userDetails.getTenant() + ".system_connection where system_id = ?) as exists",
                Boolean.class, UUID.fromString(systemId));
    }

    public Boolean hasAccessToSystemConnection(String systemConnectionId, UserDetails userDetails) {
        return userDetails.getStewardId() == null ? true :
            jdbcTemplate.queryForObject("SELECT EXISTS(SELECT system_connection.ID FROM da_" + userDetails.getTenant() + ".system_connection " +
                QueryHelper.getJoinQuery(ArtifactType.system_connection, userDetails) + " and system_connection.id = ?) as exists", Boolean.class,
                UUID.fromString(systemConnectionId));
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public SystemConnection createSystemConnection(UpdatableSystemConnectionEntity updatableSystemConnectionEntity, UserDetails userDetails) throws LottabyteException {
        UUID systemConnectionId = UUID.randomUUID();

        SystemConnection systemConnection = new SystemConnection(updatableSystemConnectionEntity);
        systemConnection.setId(systemConnectionId.toString());
        LocalDateTime now = LocalDateTime.now();
        systemConnection.setCreatedAt(now);
        systemConnection.setModifiedAt(now);
        systemConnection.setCreatedBy(userDetails.getUid());
        systemConnection.setModifiedBy(userDetails.getUid());

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".system_connection " +
                "(id, \"name\", description, connector_id, system_id, enabled, created, creator, modified, modifier) VALUES (?,?,?,?,?,?,?,?,?,?)",
                systemConnectionId, updatableSystemConnectionEntity.getName(), updatableSystemConnectionEntity.getDescription(), UUID.fromString(updatableSystemConnectionEntity.getConnectorId()), UUID.fromString(updatableSystemConnectionEntity.getSystemId()),
                updatableSystemConnectionEntity.getEnabled(), systemConnection.getCreatedAt(), systemConnection.getCreatedBy(),
                systemConnection.getModifiedAt(), systemConnection.getModifiedBy());

        for (SystemConnectionParamEntity systemConnectionParamEntity : updatableSystemConnectionEntity.getConnectorParam()) {
            createSystemConnectionParam(systemConnectionId.toString(), systemConnectionParamEntity, userDetails);
        }

        return systemConnection;
    }

    public String createSystemConnectionParam(String systemConnectionId, SystemConnectionParamEntity systemConnectionParamEntity, UserDetails userDetails) {
        UUID systemConnectionParamId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".system_connection_param " +
                        "(id, system_connection_id, connector_param_id, param_value, created, creator, modified, modifier) VALUES (?,?,?,?,?,?,?,?)",
                systemConnectionParamId, UUID.fromString(systemConnectionId), UUID.fromString(systemConnectionParamEntity.getConnectorParamId()), systemConnectionParamEntity.getParamValue(), now, userDetails.getUid(), now, userDetails.getUid());

        return systemConnectionParamId.toString();
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void patchSystemConnection(String systemConnectionId, UpdatableSystemConnectionEntity updatableSystemConnectionEntity, UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        SystemConnection currentSystemConnection = getById(systemConnectionId, userDetails);
        String connectorId = currentSystemConnection.getEntity().getConnectorId();
        if (updatableSystemConnectionEntity.getConnectorId() != null && !updatableSystemConnectionEntity.getConnectorId().isEmpty())
            connectorId = updatableSystemConnectionEntity.getConnectorId();

        if (currentSystemConnection.getEntity().getConnectorId().equals(connectorId)) {
            if (updatableSystemConnectionEntity.getConnectorParam() != null) {
                for (SystemConnectionParamEntity systemConnectionParamEntity : updatableSystemConnectionEntity.getConnectorParam()) {
                    SystemConnectionParam systemConnectionParam = getSystemConnectionParamBySystemConnectionIdAndConnectorParamId(systemConnectionId, systemConnectionParamEntity.getConnectorParamId(), userDetails);
                    if (systemConnectionParam != null) {
                        patchSystemConnectionParam(systemConnectionParam.getId(), systemConnectionParamEntity, userDetails);
                    } else {
                        createSystemConnectionParam(systemConnectionId, systemConnectionParamEntity, userDetails);
                    }
                }
            }
        } else {
            deleteSystemConnectionParamBySystemConnectionId(systemConnectionId, userDetails);
            if (updatableSystemConnectionEntity.getConnectorParam() != null) {
                for (SystemConnectionParamEntity systemConnectionParamEntity : updatableSystemConnectionEntity.getConnectorParam()) {
                    createSystemConnectionParam(systemConnectionId, systemConnectionParamEntity, userDetails);
                }
            }
        }
        String query = "UPDATE da_" + userDetails.getTenant() + ".system_connection SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new Date().getTime()));
        if (updatableSystemConnectionEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(updatableSystemConnectionEntity.getName());
        }
        if (updatableSystemConnectionEntity.getDescription() != null) {
            sets.add("description = ?");
            params.add(updatableSystemConnectionEntity.getDescription());
        }
        if (updatableSystemConnectionEntity.getConnectorId() != null) {
            sets.add("connector_id = ?");
            params.add(!updatableSystemConnectionEntity.getConnectorId().isEmpty() ? UUID.fromString(updatableSystemConnectionEntity.getConnectorId()) : null);
        }
        if (updatableSystemConnectionEntity.getSystemId() != null) {
            sets.add("system_id = ?");
            params.add(!updatableSystemConnectionEntity.getSystemId().isEmpty() ? UUID.fromString(updatableSystemConnectionEntity.getSystemId()) : null);
        }
        if (updatableSystemConnectionEntity.getEnabled() != null) {
            sets.add("enabled = ?");
            params.add(updatableSystemConnectionEntity.getEnabled());
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(systemConnectionId));
            jdbcTemplate.update(query, params.toArray());
        }
    }

    public void deleteSystemConnectionParamBySystemConnectionId(String systemConnectionId, UserDetails userDetails) {
        String query = "DELETE FROM da_" + userDetails.getTenant() + ".system_connection_param " +
                "WHERE system_connection_id=?;";
        jdbcTemplate.update(query, UUID.fromString(systemConnectionId));
    }

    public SystemConnectionParam getSystemConnectionParamBySystemConnectionIdAndConnectorParamId(String systemConnectionId, String connectorParamId, UserDetails userDetails) {
        List<SystemConnectionParam> systemConnectionParamList = jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() +
                        ".system_connection_param WHERE system_connection_id = ? AND connector_param_id = ?",
                new SystemConnectionParamRowMapper(), UUID.fromString(systemConnectionId), UUID.fromString(connectorParamId));

        return systemConnectionParamList.stream().findFirst().orElse(null);
    }

    public void patchSystemConnectionParam(String systemConnectionParamId, SystemConnectionParamEntity systemConnectionParamEntity, UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".system_connection_param SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new Date().getTime()));
        if (systemConnectionParamEntity.getSystemConnectionId() != null) {
            sets.add("system_connection_id = ?");
            params.add(!systemConnectionParamEntity.getSystemConnectionId().isEmpty() ? UUID.fromString(systemConnectionParamEntity.getSystemConnectionId()) : null);
        }
        if (systemConnectionParamEntity.getConnectorParamId() != null) {
            sets.add("connector_param_id = ?");
            params.add(!systemConnectionParamEntity.getConnectorParamId().isEmpty() ? UUID.fromString(systemConnectionParamEntity.getConnectorParamId()) : null);
        }
        if (systemConnectionParamEntity.getParamValue() != null) {
            sets.add("param_value = ?");
            params.add(systemConnectionParamEntity.getParamValue());
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(systemConnectionParamId));
            jdbcTemplate.update(query, params.toArray());
        }
    }


    public List<SystemConnection> getSystemConnectionsBySystemId(String systemId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".system_connection WHERE system_id=?",
                new SystemConnectionRowMapper(), UUID.fromString(systemId));
    }

    public PaginatedArtifactList<SystemConnection> getAllPaginated(String systemId, Integer offset, Integer limit, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".system_connection", Integer.class);
        String query = "SELECT * FROM da_" + userDetails.getTenant() + ".system_connection offset ? limit ? ";
        List<SystemConnection> systemConnectionList = jdbcTemplate.query(query, new SystemConnectionRowMapper(), offset, limit);

        PaginatedArtifactList<SystemConnection> res = new PaginatedArtifactList<>(
                systemConnectionList, offset, limit, total, "/v1/system/" + systemId);
        return res;
    }

    public SystemConnectionParam getSystemConnectionParamById(String systemConnectionParamId, UserDetails userDetails) {
        List<SystemConnectionParam> systemConnectionParamList = jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() +
                        ".system_connection_param WHERE id=?",
                new SystemConnectionParamRowMapper(), UUID.fromString(systemConnectionParamId));

        return systemConnectionParamList.stream().findFirst().orElse(null);
    }
    public SystemConnectionParam getSystemConnectionParamBySystemConnectionId(String systemConnectionId, UserDetails userDetails) {
        List<SystemConnectionParam> systemConnectionParamList = jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() +
                        ".system_connection_param WHERE system_connection_id=?",
                new SystemConnectionParamRowMapper(), UUID.fromString(systemConnectionId));

        return systemConnectionParamList.stream().findFirst().orElse(null);
    }

    public PaginatedArtifactList<SystemConnectionParam> getSystemConnectionParamsPaginated(String systemConnectionId, Integer offset, Integer limit, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".system_connection_param where system_connection_id = ?", Integer.class, UUID.fromString(systemConnectionId));
        String query = "SELECT * FROM da_" + userDetails.getTenant() + ".system_connection_param where system_connection_id = ? offset ? limit ? ";
        List<SystemConnectionParam> systemConnectionParamList = jdbcTemplate.query(query, new SystemConnectionParamRowMapper(), UUID.fromString(systemConnectionId), offset, limit);

        PaginatedArtifactList<SystemConnectionParam> res = new PaginatedArtifactList<>(
                systemConnectionParamList, offset, limit, total, "/v1/connectors/"+ systemConnectionId + "/params");
        return res;
    }

    public SearchResponse<FlatSystemConnection> searchSystemConnection(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "SELECT system_connection.*, true as has_access FROM da_" + userDetails.getTenant() + ".system_connection ";
        if (userDetails.getStewardId() != null) {
            String hasAccessJoinQuery = QueryHelper.getJoinQuery(ArtifactType.system_connection, userDetails);
             subQuery = "SELECT system_connection.*, case when acc.id is null then false else true end as has_access FROM da_" + userDetails.getTenant() + ".system_connection "
                    + " left join (select system_connection.id from da_" + userDetails.getTenant() + ".system_connection " + hasAccessJoinQuery + ") acc on system_connection.id = acc.id ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward())
                subQuery = "SELECT system_connection.*, true as has_access FROM da_" + userDetails.getTenant() + ".system_connection "
                        + hasAccessJoinQuery;
        }
        String subQuery2 = "SELECT distinct sc.*,null as version_id, s.name as system_name, c.name as connector_name from (" + subQuery + ") as sc "
                + " left join da_" + userDetails.getTenant() + ".system s on sc.system_id = s.id "
                + " left join da.connector c on sc.connector_id = c.id";
        String queryForItems = "SELECT * FROM (" + subQuery2 + ") as tbl1 "
                + join + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatSystemConnection> flatItems = jdbcTemplate.query(queryForItems, new FlatSystemConnectorRowMapper(), whereValues.toArray());

        int total = ServiceUtils.getTotalForSearchRequestWithJoinAndSubQuery(jdbcTemplate, userDetails,
                subQuery2, join, where, whereValues);

        SearchResponse<FlatSystemConnection> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public List<SystemConnectionParam> getSystemConnectionParamsList(String systemConnectionId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".system_connection_param where system_connection_id = ?",
                new SystemConnectionParamRowMapper(), UUID.fromString(systemConnectionId));
    }
}
