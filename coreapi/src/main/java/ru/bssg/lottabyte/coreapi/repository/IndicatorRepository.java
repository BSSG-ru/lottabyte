package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.businessEntity.BusinessEntity;
import ru.bssg.lottabyte.core.model.dataentity.DataEntityAttributeEntity;
import ru.bssg.lottabyte.core.model.dataentity.DataEntityEntity;
import ru.bssg.lottabyte.core.model.dataentity.FlatDataEntity;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;
import ru.bssg.lottabyte.core.model.entitySample.UpdatableEntitySampleDQRule;
import ru.bssg.lottabyte.core.model.indicator.FlatIndicator;
import ru.bssg.lottabyte.core.model.indicator.Indicator;
import ru.bssg.lottabyte.core.model.indicator.IndicatorEntity;
import ru.bssg.lottabyte.core.model.indicator.UpdatableIndicatorEntity;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.util.Constants;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@Slf4j
public class IndicatorRepository extends WorkflowableRepository<Indicator> {

    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = { "calc_code", "dq_checks", "formula", "domain_id", "indicator_type_id", "examples", "link", "datatype_id", "limits", "limits_internal", "roles" };

    public IndicatorRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.indicator.name(), extFields);
        super.setMapper(new IndicatorRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class IndicatorRowMapper implements RowMapper<Indicator> {
        @Override
        public Indicator mapRow(ResultSet rs, int rowNum) throws SQLException {
            IndicatorEntity indicatorEntity = new IndicatorEntity();
            indicatorEntity.setName(rs.getString("name"));
            indicatorEntity.setDescription(rs.getString("description"));
            indicatorEntity.setCalcCode(rs.getString("calc_code"));
            indicatorEntity.setFormula(rs.getString("formula"));
            indicatorEntity.setDomainId(rs.getString("domain_id"));
            indicatorEntity.setIndicatorTypeId(rs.getString("indicator_type_id"));
            if (rs.getArray("dq_checks") != null) {
                String[] array = (String[]) rs.getArray("dq_checks").getArray();
                indicatorEntity.setDqChecks(new ArrayList<>(Arrays.asList(array)));
            }
            indicatorEntity.setExamples(rs.getString("examples"));
            indicatorEntity.setLink(rs.getString("link"));
            indicatorEntity.setDatatypeId(rs.getString("datatype_id"));
            indicatorEntity.setLimits(rs.getString("limits"));
            indicatorEntity.setLimits_internal(rs.getString("limits_internal"));
            indicatorEntity.setRoles(rs.getString("roles"));
            return new Indicator(indicatorEntity, new WorkflowableMetadata(rs, indicatorEntity.getArtifactType()));
        }
    }

    public static class FormulaDataRowMapper implements RowMapper<Map<DataEntityEntity, DataEntityAttributeEntity>> {
        @Override
        public Map<DataEntityEntity, DataEntityAttributeEntity> mapRow(ResultSet rs, int rowNum) throws SQLException {
            Map<DataEntityEntity, DataEntityAttributeEntity> formulaData = new HashMap<>();
            DataEntityEntity dataEntityEntity = new DataEntityEntity();
            dataEntityEntity.setId(rs.getString("e_id"));
            dataEntityEntity.setName(rs.getString("e_name"));
            dataEntityEntity.setArtifactType(ArtifactType.entity);
            DataEntityAttributeEntity dataEntityAttributeEntity = new DataEntityAttributeEntity();
            dataEntityAttributeEntity.setId(rs.getString("ea_id"));
            dataEntityAttributeEntity.setName(rs.getString("ea_name"));
            dataEntityAttributeEntity.setArtifactType(ArtifactType.entity_attribute);
            formulaData.put(dataEntityEntity, dataEntityAttributeEntity);

            return formulaData;
        }
    }

    public static class FlatIndicatorRowMapper implements RowMapper<FlatIndicator> {
        @Override
        public FlatIndicator mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatIndicator fi = new FlatIndicator();
            fi.setId(rs.getString("id"));
            fi.setName(rs.getString("name"));
            fi.setDescription(rs.getString("description"));
            fi.setVersionId(rs.getInt("version_id"));
            fi.setModified(rs.getTimestamp("modified").toLocalDateTime());
            fi.setCalcCode(rs.getString("calc_code"));
            fi.setFormula(rs.getString("formula"));
            fi.setDomainId(rs.getString("domain_id"));
            fi.setDomainName(rs.getString("domain_name"));
            fi.setIndicatorTypeName(rs.getString("indicator_type_name"));
            fi.setIndicatorTypeId(rs.getString("indicator_type_id"));
            if (rs.getArray("dq_checks") != null) {
                String[] array = (String[]) rs.getArray("dq_checks").getArray();
                fi.setDqChecks(new ArrayList<>(Arrays.asList(array)));
            }
            fi.setState(ArtifactState.valueOf(rs.getString("state")));
            fi.setWorkflowTaskId(rs.getString("workflow_task_id"));
            return fi;
        }
    }

    class IndicatorTypeRowMapper implements RowMapper<IndicatorType> {
        @Override
        public IndicatorType mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new IndicatorType(rs.getString("id"), rs.getString("name"),
                    rs.getString("description"));
        }
    }

    public Map<String, List<DataEntityAttributeEntity>> getEntityAttributesByIndicatorId(String indicatorId,
            UserDetails userDetails) {
        return jdbcTemplate.query("SELECT ea.id as ea_id, ea.name as ea_name, e.id as e_id, e.name as e_name \n" +
                "FROM da_" + userDetails.getTenant() + ".\"indicator\" i \n" +
                "join da_" + userDetails.getTenant()
                + ".reference r1 on r1.source_id=i.id and r1.target_artifact_type='data_asset' \n" +
                "join da_" + userDetails.getTenant() + ".data_asset da on r1.target_id=da.id \n" +
                "join da_" + userDetails.getTenant() + ".entity e on e.id=da.entity_id \n" +
                "join da_" + userDetails.getTenant() + ".entity_attribute ea on ea.entity_id=da.entity_id \n" +
                "where i.id = ?",
                new ResultSetExtractor<Map>() {
                    @Override
                    public Map<String, List<DataEntityAttributeEntity>> extractData(ResultSet rs) throws SQLException {
                        Map<String, List<DataEntityAttributeEntity>> result = new HashMap<>();
                        while (rs.next()) {
                            DataEntityAttributeEntity newDataEntityAttributeEntity = new DataEntityAttributeEntity();
                            newDataEntityAttributeEntity.setId(rs.getString("ea_id"));
                            newDataEntityAttributeEntity.setName(rs.getString("ea_name"));
                            newDataEntityAttributeEntity.setArtifactType(ArtifactType.entity_attribute);
                            String entityId = rs.getString("e_id");

                            List<DataEntityAttributeEntity> resultEntityAttributeIdList = result.getOrDefault(entityId,
                                    new ArrayList<>());
                            resultEntityAttributeIdList.add(newDataEntityAttributeEntity);

                            result.put(entityId, resultEntityAttributeIdList);
                        }

                        return result;
                    }
                }, UUID.fromString(indicatorId));
    }

    public List<String> entityAttributeExistInAllFormulas(String entityAttributeId, UserDetails userDetails) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM da_" + userDetails.getTenant()
                        + ".indicator WHERE state='PUBLISHED' AND formula like '%" + entityAttributeId + "%'",
                String.class);
    }

    public List<String> indicatorExistInAllFormulas(String indicatorId, UserDetails userDetails) {
        return jdbcTemplate.queryForList("SELECT id FROM da_" + userDetails.getTenant()
                + ".indicator WHERE state='PUBLISHED' AND formula like '%" + indicatorId + "%'", String.class);
    }

    public Boolean allIndicatorsExist(List<String> indicatorIds, UserDetails userDetails) {
        if (indicatorIds == null || indicatorIds.isEmpty())
            return true;
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".\"indicator\" WHERE state = ? and id IN ('"
                        + StringUtils.join(indicatorIds, "','") + "')",
                Integer.class, ArtifactState.PUBLISHED.toString());
        return c != null && indicatorIds.size() == c;
    }

    public String createIndicator(UpdatableIndicatorEntity newIndicatorEntity, String workflowTaskId,
            UserDetails userDetails) {
        UUID newId = newIndicatorEntity.getId() != null ? UUID.fromString(newIndicatorEntity.getId())
                : UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        String dqChecksString = null;
        if (newIndicatorEntity.getDqChecks() != null && !newIndicatorEntity.getDqChecks().isEmpty())
            dqChecksString = String.join(",", newIndicatorEntity.getDqChecks());

        String query = "INSERT INTO da_" + userDetails.getTenant() + ".\"indicator\" " +
                "(id, \"name\", description, calc_code, dq_checks, state, workflow_task_id, created, creator, modified, modifier, formula, domain_id, indicator_type_id, examples, link, datatype_id, limits, limits_internal, roles) "
                +
                "VALUES(?, ?, ?, ?, string_to_array(?,','), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(query, newId, newIndicatorEntity.getName(), newIndicatorEntity.getDescription(),
                newIndicatorEntity.getCalcCode(),
                dqChecksString,
                ArtifactState.DRAFT.toString(),
                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                ts, userDetails.getUid(), ts, userDetails.getUid(), newIndicatorEntity.getFormula(),
                (newIndicatorEntity.getDomainId() == null ? null : UUID.fromString(newIndicatorEntity.getDomainId())),
                UUID.fromString(newIndicatorEntity.getIndicatorTypeId()),
                newIndicatorEntity.getExamples(), newIndicatorEntity.getLink(), newIndicatorEntity.getDatatypeId() == null ?
                null : UUID.fromString(newIndicatorEntity.getDatatypeId()), newIndicatorEntity.getLimits(), newIndicatorEntity.getLimits_internal(),
                newIndicatorEntity.getRoles());
        return newId.toString();
    }

    public void patchIndicator(String indicatorId, UpdatableIndicatorEntity indicatorEntity, UserDetails userDetails)
            throws LottabyteException {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".\"indicator\" SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));
        if (indicatorEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(indicatorEntity.getName());
        }
        if (indicatorEntity.getDescription() != null) {
            sets.add("description = ?");
            params.add(indicatorEntity.getDescription());
        }
        if (indicatorEntity.getCalcCode() != null) {
            sets.add("calc_code = ?");
            params.add(indicatorEntity.getCalcCode());
        }
        if (indicatorEntity.getDqChecks() != null) {
            sets.add("dq_checks = string_to_array(?,',')");
            String dqChecksString = String.join(",", indicatorEntity.getDqChecks());
            params.add(dqChecksString);
        }
        if (indicatorEntity.getFormula() != null) {
            sets.add("formula = ?");
            params.add(indicatorEntity.getFormula());
        }
        if (indicatorEntity.getDomainId() != null) {

            sets.add("domain_id = ?");
            params.add(indicatorEntity.getDomainId().isEmpty() ? null : UUID.fromString(indicatorEntity.getDomainId()));
        }
        if (indicatorEntity.getIndicatorTypeId() != null) {
            sets.add("indicator_type_id = ?");
            params.add(UUID.fromString(indicatorEntity.getIndicatorTypeId()));
        }
        if (indicatorEntity.getExamples() != null) {
            sets.add("examples = ?");
            params.add(indicatorEntity.getExamples());
        }
        if (indicatorEntity.getLink() != null) {
            sets.add("link = ?");
            params.add(indicatorEntity.getLink());
        }
        if (indicatorEntity.getDatatypeId() != null) {
            sets.add("datatype_id = ?");
            params.add(indicatorEntity.getDatatypeId() == null ? null : UUID.fromString(indicatorEntity.getDatatypeId()));
        }
        if (indicatorEntity.getLimits() != null) {
            sets.add("limits = ?");
            params.add(indicatorEntity.getLimits());
        }
        if (indicatorEntity.getLimits_internal() != null) {
            sets.add("limits_internal = ?");
            params.add(indicatorEntity.getLimits_internal());
        }
        if (indicatorEntity.getRoles() != null) {
            sets.add("roles = ?");
            params.add(indicatorEntity.getRoles());
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(indicatorId));
            jdbcTemplate.update(query, params.toArray());
        }
    }

    public SearchResponse<FlatIndicator> searchIndicators(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, "tbl1.domain_id", true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "select * from da_" + userDetails.getTenant() + ".indicator ";
        if (userDetails.getStewardId() != null && searchRequest.getLimitSteward() != null
                && searchRequest.getLimitSteward()) {
            subQuery = subQuery + QueryHelper.getWhereIdInQuery(ArtifactType.indicator, userDetails);
        }

        subQuery = "SELECT sq.*, wft.workflow_state, t.tags FROM (" + subQuery + ") as sq left join da_"
                + userDetails.getTenant() + ".workflow_task wft "
                + " on sq.workflow_task_id = wft.id "
                + "left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id ";

        String queryForItems = "SELECT distinct tbl1.*, domain.name AS domain_name, indicator_type.name AS indicator_type_name FROM ("
                + subQuery + ") tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id"
                + " LEFT JOIN da_" + userDetails.getTenant()
                + ".indicator_type indicator_type on tbl1.indicator_type_id=indicator_type.id "
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatIndicator> flatItems = jdbcTemplate.query(queryForItems, new FlatIndicatorRowMapper(), whereValues.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") tbl1 "
                + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id"
                + " LEFT JOIN da_" + userDetails.getTenant()
                + ".indicator_type indicator_type on tbl1.indicator_type_id=indicator_type.id "
                + where;
        Integer total = jdbcTemplate.queryForObject(queryForTotal, Integer.class, whereValues.toArray());

        SearchResponse<FlatIndicator> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public Boolean entityAttributeExistsInDataAssets(String attributeId, List<String> dataAssetIds,
            UserDetails userDetails) {
        if (dataAssetIds == null || dataAssetIds.isEmpty())
            return false;

        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant()
                + ".entity_attribute WHERE entity_id IN (SELECT entity_id FROM da_" + userDetails.getTenant()
                + ".data_asset WHERE id IN ('" + StringUtils.join(dataAssetIds, "','") + "')) AND id=?) as exists",
                Boolean.class, UUID.fromString(attributeId));
    }

    public Boolean existArtifactById(String id, ArtifactType artifactType, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant() + "." + artifactType.getText()
                        + " WHERE id = ?) as exists",
                Boolean.class, UUID.fromString(id));
    }

    public List<IndicatorType> getIndicatorTypes(UserDetails userDetails) {
        return jdbcTemplate.query("SELECT id, name, description FROM da_" + userDetails.getTenant() +
                ".indicator_type", new IndicatorRepository.IndicatorTypeRowMapper());
    }

    public IndicatorType getIndicatorTypeById(String id, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT id, name, description FROM da_" + userDetails.getTenant() +
                ".indicator_type WHERE id=?", new IndicatorRepository.IndicatorTypeRowMapper(), UUID.fromString(id));
    }

    public SearchResponse<FlatIndicator> searchIndicatorsByDomain(SearchRequestWithJoin searchRequest, String domainId,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, "tbl1.domain_id", true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        if (where.isEmpty()) {
            /*where = " where tbl1.id in (select tbl3.source_id from da_" + userDetails.getTenant()
                    + ".reference tbl3 where tbl3.target_id in (select tbl4.id from da_" + userDetails.getTenant()
                    + ".data_asset tbl4 where tbl4.domain_id='" + domainId + "' LIMIT " + Constants.sqlInLimit + "))";*/
            where = " where tbl1.domain_id = '" + domainId + "'";
        } else {
            where = where + " AND tbl1.domain_id = '" + domainId + "'";
            /*where = where + " AND tbl1.id in (select tbl3.source_id from da_" + userDetails.getTenant()
                    + ".reference tbl3 where tbl3.target_id in (select tbl4.id from da_" + userDetails.getTenant()
                    + ".data_asset tbl4 where tbl4.domain_id='" + domainId + "' LIMIT " + Constants.sqlInLimit + "))";*/
        }

        String subQuery = "select indicator.*, true as has_access, t.tags from da_" + userDetails.getTenant()
                + ".indicator ";
        if (userDetails.getStewardId() != null) {
            String hasAccessJoinQuery = QueryHelper.getJoinQuery(ArtifactType.indicator, userDetails);
            subQuery = "SELECT indicator.*, case when acc.id is null then false else true end as has_access, t.tags FROM da_"
                    + userDetails.getTenant() + ".indicator " +
                    " left join (select indicator.id from da_" + userDetails.getTenant() + ".indicator "
                    + hasAccessJoinQuery + ") acc on indicator.id = acc.id ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward())
                subQuery = "select indicator.*, true as has_access, t.tags from da_" + userDetails.getTenant()
                        + ".indicator "
                        + hasAccessJoinQuery;
        }
        subQuery += " left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=indicator.id ";

        String queryForItems = "SELECT distinct tbl1.*, domain.name AS domain_name, indicator_type.name AS indicator_type_name FROM ("
                + subQuery + ") as tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id"
                + " LEFT JOIN da_" + userDetails.getTenant()
                + ".indicator_type indicator_type on tbl1.indicator_type_id=indicator_type.id "
                + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatIndicator> flatItems = jdbcTemplate.query(queryForItems, new FlatIndicatorRowMapper(), whereValues.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id"
                + " LEFT JOIN da_" + userDetails.getTenant()
                + ".indicator_type indicator_type on tbl1.indicator_type_id=indicator_type.id "
                + where;
        final int[] count = { 0 };
        jdbcTemplate.query(
                queryForTotal,
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        count[0] = rs.getInt("count");
                    }
                },
                whereValues.toArray());

        int total = count[0];

        SearchResponse<FlatIndicator> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public EntitySampleDQRule createDQRuleLink(String indicatorId,
            UpdatableEntitySampleDQRule entitySampleDQRule, UserDetails userDetails) throws LottabyteException {
        UUID id = entitySampleDQRule.getId() == null ? UUID.randomUUID() : UUID.fromString(entitySampleDQRule.getId());

        EntitySampleDQRule esp = new EntitySampleDQRule(entitySampleDQRule);
        esp.setId(id.toString());
        LocalDateTime now = LocalDateTime.now();
        esp.setCreatedAt(now);
        esp.setModifiedAt(now);
        esp.setCreatedBy(userDetails.getUid());
        esp.setModifiedBy(userDetails.getUid());

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                + ".entity_sample_to_dq_rule (id, dq_rule_id, settings, created, creator, modified, modifier, disabled, indicator_id, send_mail, history_id, published_id, ancestor_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id, UUID.fromString(entitySampleDQRule.getDqRuleId()),
                entitySampleDQRule.getSettings(), now, userDetails.getUid(), now, userDetails.getUid(),
                entitySampleDQRule.getDisabled(),
                UUID.fromString(indicatorId),
                entitySampleDQRule.getSendMail(),
                entitySampleDQRule.getHistoryId(),
                UUID.fromString(entitySampleDQRule.getPublishedId()),
                entitySampleDQRule.getAncestorId() == null ? null : UUID.fromString(entitySampleDQRule.getAncestorId()));

        return esp;
    }

    public void addDQRuleLink(String indicatorId, String publishedId,
            EntitySampleDQRule entitySampleDQRule, UserDetails userDetails) {

        UUID id = UUID.randomUUID();

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                + ".entity_sample_to_dq_rule (id,  dq_rule_id, settings, created, creator, modified, modifier, disabled,  indicator_id, send_mail, history_id, published_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                id, UUID.fromString(entitySampleDQRule.getEntity().getDqRuleId()),
                entitySampleDQRule.getEntity().getSettings(), now, userDetails.getUid(), now, userDetails.getUid(),
                entitySampleDQRule.getEntity().getDisabled(),
                UUID.fromString(indicatorId),
                entitySampleDQRule.getEntity().getSendMail(), entitySampleDQRule.getEntity().getHistoryId(),
                publishedId == null ? null : UUID.fromString(publishedId));

    }

    public boolean existsIndicatorWithDomain(String domainId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".indicator " +
                        "WHERE domain_id is not null and domain_id = ? and state = ?) AS EXISTS",
                Boolean.class, UUID.fromString(domainId), ArtifactState.PUBLISHED.toString());
    }

    public List<BusinessEntity> getTermLinksById(String indicatorId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT be.* FROM da_" + userDetails.getTenant() + ".business_entity be "
                + " join da_" + userDetails.getTenant() + ".reference r on r.target_id = be.id "
                + " where r.source_id = ? AND r.reference_type='INDICATOR_TO_BUSINESS_ENTITY_LINK'", new BusinessEntityRepository.BusinessEntityRowMapper(), UUID.fromString(indicatorId));
    }
}
