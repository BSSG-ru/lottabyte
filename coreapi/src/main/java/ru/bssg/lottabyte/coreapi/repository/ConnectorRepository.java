package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.AbstractPaginatedList;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.connector.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class ConnectorRepository extends GenericArtifactRepository<Connector> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = {  };

    @Autowired
    public ConnectorRepository (JdbcTemplate jdbcTemplate) {

        super(jdbcTemplate, ArtifactType.connector.name(), extFields);
        super.setMapper(new ConnectorRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    private class ConnectorRowMapper implements RowMapper<Connector> {
        @Override
        public Connector mapRow(ResultSet rs, int rowNum) throws SQLException {
            Connector s = null;

            ConnectorEntity connectorEntity = new ConnectorEntity();
            connectorEntity.setName(rs.getString("name"));
            connectorEntity.setDescription(rs.getString("description"));
            connectorEntity.setSystemType(rs.getString("system_type"));
            connectorEntity.setArtifactType(ArtifactType.connector);

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));

            try {
                s = new Connector(connectorEntity, md);
            } catch (LottabyteException e) {
                log.error(e.getMessage(), e);
            }
            return s;
        }
    }

    private class ConnectorParamRowMapper implements RowMapper<ConnectorParam> {
        @Override
        public ConnectorParam mapRow(ResultSet rs, int rowNum) throws SQLException {
            ConnectorParam s = null;

            ConnectorParamEntity connectorParamEntity = new ConnectorParamEntity();
            connectorParamEntity.setName(rs.getString("name"));
            connectorParamEntity.setDescription(rs.getString("description"));
            connectorParamEntity.setConnectorId(rs.getString("connector_id"));
            connectorParamEntity.setDisplayName(rs.getString("display_name"));
            connectorParamEntity.setExample(rs.getString("example"));
            connectorParamEntity.setParamType(ConnectorParamType.valueOf(rs.getString("param_type")));
            connectorParamEntity.setShowOrder(rs.getInt("show_order"));
            connectorParamEntity.setRequired(rs.getBoolean("required"));
            if (rs.getArray("enum_values") != null) {
                String[] array = (String[])rs.getArray("enum_values").getArray();
                connectorParamEntity.setEnumValues(new ArrayList<>(Arrays.asList(array)));
            }

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));

            try {
                s = new ConnectorParam(connectorParamEntity, md);
            } catch (LottabyteException e) {
                log.error(e.getMessage(), e);
            }
            return s;
        }
    }

    public ConnectorParam getConnectorParamById(String connectorParamId) {
        List<ConnectorParam> connectorParamList = jdbcTemplate.query("SELECT * FROM da.connector_param WHERE id=?",
                new ConnectorParamRowMapper(), UUID.fromString(connectorParamId));

        return connectorParamList.stream().findFirst().orElse(null);
    }

    public PaginatedArtifactList<ConnectorParam> getConnectorParamsPaginated(String connectorId, Integer offset, Integer limit) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da.connector_param where connector_id = ?", Integer.class,
                UUID.fromString(connectorId));
        String query = "SELECT * FROM da.connector_param where connector_id = ? offset ? limit ? ";
        List<ConnectorParam> connectorParamList = jdbcTemplate.query(query, new ConnectorParamRowMapper(),
                UUID.fromString(connectorId), offset, limit);

        return new PaginatedArtifactList<>(
                connectorParamList, offset, limit, total, "/v1/connectors/"+ connectorId + "/params");
    }

    public List<ConnectorParam> getConnectorParamsList(String connectorId) {
        return jdbcTemplate.query("SELECT * FROM da.connector_param where connector_id=?",
                new ConnectorParamRowMapper(), UUID.fromString(connectorId));
    }


}
