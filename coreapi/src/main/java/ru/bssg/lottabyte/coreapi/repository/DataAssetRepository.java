package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.dataasset.DataAsset;
import ru.bssg.lottabyte.core.model.dataasset.DataAssetEntity;
import ru.bssg.lottabyte.core.model.dataasset.FlatDataAsset;
import ru.bssg.lottabyte.core.model.dataasset.UpdatableDataAssetEntity;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.util.JDBCUtil;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@Slf4j
public class DataAssetRepository extends WorkflowableRepository<DataAsset> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = { "system_id", "domain_id", "entity_id", "rows_count", "data_size", "roles" };

    @Autowired
    public DataAssetRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.data_asset.name(), extFields);
        super.setMapper(new DataAssetRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    private static class DataAssetRowMapper implements RowMapper<DataAsset> {
        public static DataAsset mapDataAssetRow(ResultSet rs) throws SQLException {
            DataAssetEntity dataAssetEntity = new DataAssetEntity();
            dataAssetEntity.setName(rs.getString("name"));
            dataAssetEntity.setDescription(rs.getString("description"));
            dataAssetEntity.setSystemId(rs.getString("system_id"));
            dataAssetEntity.setDomainId(rs.getString("domain_id"));
            dataAssetEntity.setEntityId(rs.getString("entity_id"));
            dataAssetEntity.setDataSize(JDBCUtil.getInt(rs, "data_size"));
            dataAssetEntity.setRowsCount(JDBCUtil.getInt(rs, "rows_count"));
            dataAssetEntity.setRoles(rs.getString("roles"));

            return new DataAsset(dataAssetEntity, new WorkflowableMetadata(rs, dataAssetEntity.getArtifactType()));
        }

        @Override
        public DataAsset mapRow(ResultSet rs, int rowNum) throws SQLException {
            return mapDataAssetRow(rs);
        }
    }

    private static class FlatDataAssetRowMapper implements RowMapper<FlatDataAsset> {
        @Override
        public FlatDataAsset mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatDataAsset flatDataAsset = new FlatDataAsset(DataAssetRowMapper.mapDataAssetRow(rs));

            flatDataAsset.setDomainName(rs.getString("domain_name"));
            flatDataAsset.setSystemName(rs.getString("system_name"));
            flatDataAsset.setEntityName(rs.getString("entity_name"));
            flatDataAsset.setState(ArtifactState.valueOf(rs.getString("state")));
            flatDataAsset.setWorkflowTaskId(rs.getString("workflow_task_id"));
            return flatDataAsset;
        }
    }

    public Boolean allDataAssetsExist(List<String> dataAssetIds, UserDetails userDetails) {
        if (dataAssetIds == null || dataAssetIds.isEmpty())
            return true;
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".data_asset WHERE state = ? and id IN ('"
                        + StringUtils.join(dataAssetIds, "','") + "')",
                Integer.class, ArtifactState.PUBLISHED.toString());
        return c != null && dataAssetIds.size() == c;
    }

    public boolean existsDataAssetWithSystemAndDomain(String systemId, String domainId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".data_asset "
                +
                "WHERE system_id is not null and system_id = ? and domain_id is not null and domain_id = ?) AS EXISTS",
                Boolean.class, UUID.fromString(systemId), UUID.fromString(domainId));
    }

    public boolean existsDataAssetWithSystemAndEntity(String systemId, String entityId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".data_asset "
                +
                "WHERE system_id is not null and system_id = ? and entity_id is not null and entity_id = ?) AS EXISTS",
                Boolean.class, UUID.fromString(systemId), UUID.fromString(entityId));
    }

    public boolean existsDataAssetWithDomain(String domainId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".data_asset " +
                        "WHERE domain_id is not null and domain_id = ? and state = ?) AS EXISTS",
                Boolean.class, UUID.fromString(domainId), ArtifactState.PUBLISHED.toString());
    }

    public boolean existsDataAssetWithSystem(String systemId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".data_asset " +
                        "WHERE system_id is not null and system_id = ? and state = ?) AS EXISTS",
                Boolean.class, UUID.fromString(systemId), ArtifactState.PUBLISHED.toString());
    }

    public boolean existsDataAssetWithEntity(String entityId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".data_asset " +
                        "WHERE entity_id is not null and entity_id = ? and state = ?) AS EXISTS",
                Boolean.class, UUID.fromString(entityId), ArtifactState.PUBLISHED.toString());
    }

    public Boolean hasAccessToDataAsset(String dataAssetId, UserDetails userDetails) {
        log.info(
                "access q " + "SELECT EXISTS(SELECT data_asset.ID FROM da_" + userDetails.getTenant() + ".data_asset " +
                        QueryHelper.getWhereIdInQuery(ArtifactType.data_asset, userDetails)
                        + " and data_asset.ID = ?) as exists");
        return userDetails.getStewardId() == null ? true
                : jdbcTemplate.queryForObject(
                        "SELECT EXISTS(SELECT data_asset.ID FROM da_" + userDetails.getTenant() + ".data_asset " +
                                QueryHelper.getWhereIdInQuery(ArtifactType.data_asset, userDetails)
                                + " and data_asset.ID = ?) as exists",
                        Boolean.class,
                        UUID.fromString(dataAssetId));
    }

    public List<DataAsset> getDataAssetsBySystemAndDomain(String systemId, String domainId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".data_asset " +
                "WHERE system_id is not null and system_id = ? and domain_id is not null and domain_id = ?",
                new DataAssetRowMapper(), UUID.fromString(systemId), UUID.fromString(domainId));
    }

    public List<DataAsset> getDataAssetsBySystemAndEntity(String systemId, String entityId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".data_asset " +
                "WHERE system_id is not null and system_id = ? and entity_id is not null and entity_id = ?",
                new DataAssetRowMapper(), UUID.fromString(systemId), UUID.fromString(entityId));
    }

    public String createDataAsset(UpdatableDataAssetEntity nde, String workflowTaskId, UserDetails userDetails)
            throws LottabyteException {
        UUID newId = nde.getId() != null ? UUID.fromString(nde.getId()) : UUID.randomUUID();

        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        String query = "INSERT INTO da_" + userDetails.getTenant() + ".data_asset " +
                "(id, \"name\", description, system_id, domain_id, entity_id, rows_count, data_size, state, workflow_task_id, created, creator, modified, modifier, roles) "
                +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(query, newId, nde.getName(), nde.getDescription(),
                nde.getSystemId() != null ? UUID.fromString(nde.getSystemId()) : null,
                nde.getDomainId() != null ? UUID.fromString(nde.getDomainId()) : null,
                nde.getEntityId() != null ? UUID.fromString(nde.getEntityId()) : null,
                nde.getRowsCount(), nde.getDataSize(),
                ArtifactState.DRAFT.toString(),
                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                ts, userDetails.getUid(), ts, userDetails.getUid(), nde.getRoles());
        return newId.toString();
    }

    public void patchDataAsset(String dataAssetId, UpdatableDataAssetEntity dataAssetEntity, UserDetails userDetails)
            throws LottabyteException {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".data_asset SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));
        if (dataAssetEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(dataAssetEntity.getName());
        }
        if (dataAssetEntity.getDescription() != null) {
            sets.add("description = ?");
            params.add(dataAssetEntity.getDescription());
        }
        if (dataAssetEntity.getDomainId() != null) {
            sets.add("domain_id = ?");
            params.add(
                    !dataAssetEntity.getDomainId().isEmpty() ? UUID.fromString(dataAssetEntity.getDomainId()) : null);
        }
        if (dataAssetEntity.getSystemId() != null) {
            sets.add("system_id = ?");
            params.add(
                    !dataAssetEntity.getSystemId().isEmpty() ? UUID.fromString(dataAssetEntity.getSystemId()) : null);
        }
        if (dataAssetEntity.getEntityId() != null) {
            sets.add("entity_id = ?");
            params.add(
                    !dataAssetEntity.getEntityId().isEmpty() ? UUID.fromString(dataAssetEntity.getEntityId()) : null);
        }
        if (dataAssetEntity.getRoles() != null) {
            sets.add("roles = ?");
            params.add(dataAssetEntity.getRoles());
        }
        if (dataAssetEntity.getDataSize() != null) {
            sets.add("data_size = ?");
            params.add(dataAssetEntity.getDataSize());
        }
        if (dataAssetEntity.getRowsCount() != null) {
            sets.add("rows_count = ?");
            params.add(dataAssetEntity.getRowsCount());
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(dataAssetId));
            jdbcTemplate.update(query, params.toArray());
        }
    }

    public SearchResponse<FlatDataAsset> searchDataAssets(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, "tbl1.domain_id", true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> vals = searchSQLParts.getWhereValues();

        String subQuery = "SELECT data_asset.* FROM da_" + userDetails.getTenant() + ".data_asset ";
        if (userDetails.getStewardId() != null && searchRequest.getLimitSteward() != null
                && searchRequest.getLimitSteward()) {
            subQuery = subQuery + QueryHelper.getWhereIdInQuery(ArtifactType.data_asset, userDetails);
        }

        subQuery = "SELECT sq.*, wft.workflow_state, t.tags FROM (" + subQuery + ") as sq left join da_"
                + userDetails.getTenant() + ".workflow_task wft "
                + " on sq.workflow_task_id = wft.id "
                + "left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id ";
        String queryForItems = "SELECT distinct tbl1.*, domain.name AS domain_name, system.name as system_name, "
                + "entity.name AS entity_name FROM (" + subQuery + ") as tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatDataAsset> items = jdbcTemplate.query(queryForItems, new FlatDataAssetRowMapper(), vals.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                + where;
        Long total = jdbcTemplate.queryForObject(queryForTotal, Long.class, vals.toArray());

        SearchResponse<FlatDataAsset> res = new SearchResponse<>(total.intValue(), searchRequest.getLimit(), searchRequest.getOffset(), items);

        return res;
    }

    public SearchResponse<FlatDataAsset> searchDataAssetsByBE(SearchRequestWithJoin searchRequest, String beId,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, "tbl1.domain_id", true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> vals = searchSQLParts.getWhereValues();

        if (where.isEmpty())
            where = " WHERE tbl1.entity_id IN (SELECT e.id FROM da_" + userDetails.getTenant() + ".entity e JOIN da_"
                    + userDetails.getTenant() + ".reference r ON e.id=r.source_id AND r.target_id='" + beId + "' )";
        else
            where += " AND tbl1.entity_id IN (SELECT e.id FROM da_" + userDetails.getTenant() + ".entity e JOIN da_"
                    + userDetails.getTenant() + ".reference r ON e.id=r.source_id AND r.target_id='" + beId + "' )";

        String subQuery = "SELECT data_asset.* FROM da_" + userDetails.getTenant() + ".data_asset ";
        if (userDetails.getStewardId() != null && searchRequest.getLimitSteward() != null
                && searchRequest.getLimitSteward()) {
            subQuery = subQuery + QueryHelper.getWhereIdInQuery(ArtifactType.data_asset, userDetails);
        }

        subQuery = "SELECT sq.*, wft.workflow_state, t.tags FROM (" + subQuery + ") as sq left join da_"
                + userDetails.getTenant() + ".workflow_task wft "
                + " on sq.workflow_task_id = wft.id "
                + "left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id ";
        String queryForItems = "SELECT distinct tbl1.*, domain.name AS domain_name, system.name as system_name, "
                + "entity.name AS entity_name FROM (" + subQuery + ") as tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatDataAsset> items = jdbcTemplate.query(queryForItems, new FlatDataAssetRowMapper(), vals.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                + where;
        Long total = jdbcTemplate.queryForObject(queryForTotal, Long.class, vals.toArray());

        SearchResponse<FlatDataAsset> res = new SearchResponse<>(total.intValue(), searchRequest.getLimit(), searchRequest.getOffset(), items);

        return res;
    }

    public void removeDQRule(String id, UserDetails userDetails) {
        String query = "DELETE FROM da_" + userDetails.getTenant() + ".entity_sample_to_dq_rule WHERE id = ?";
        jdbcTemplate.update(query, UUID.fromString(id));
    }

    public void addDQRule(String assetId,
                          EntitySampleDQRule entitySampleDQRule, UserDetails userDetails) {
        UUID id = UUID.randomUUID();

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                        + ".entity_sample_to_dq_rule (id,  dq_rule_id, settings, created, creator, modified, modifier, disabled,  asset_id, send_mail) VALUES (?,?,?,?,?,?,?,?,?,?)",
                id, UUID.fromString(entitySampleDQRule.getEntity().getDqRuleId()),
                entitySampleDQRule.getEntity().getSettings(), now, userDetails.getUid(), now, userDetails.getUid(),
                entitySampleDQRule.getEntity().getDisabled(),
                UUID.fromString(assetId),
                entitySampleDQRule.getEntity().getSendMail());

    }

}
