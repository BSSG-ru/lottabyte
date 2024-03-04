package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.dal.FlatItemRowMapper;
import ru.bssg.lottabyte.core.dal.StringRowMapper;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.domain.DomainEntity;
import ru.bssg.lottabyte.core.model.domain.FlatDomain;
import ru.bssg.lottabyte.core.model.domain.UpdatableDomainEntity;
import ru.bssg.lottabyte.core.model.dqRule.DQRule;
import ru.bssg.lottabyte.core.model.dqRule.DQRuleEntity;
import ru.bssg.lottabyte.core.model.dqRule.FlatDQRule;
import ru.bssg.lottabyte.core.model.dqRule.UpdatableDQRuleEntity;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRuleEntity;
import ru.bssg.lottabyte.core.model.entitySample.UpdatableEntitySampleDQRule;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class DQRuleRepository extends WorkflowableRepository<DQRule> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = { "rule_ref", "settings", "rule_type_id" };

    @Autowired
    public DQRuleRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.dq_rule.name(), extFields);
        super.setMapper(new DQRuleRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    class DQRuleRowMapper implements RowMapper<DQRule> {

        @Override
        public DQRule mapRow(ResultSet rs, int rowNum) throws SQLException {
            DQRuleEntity dqruleEntity = new DQRuleEntity();
            dqruleEntity.setName(rs.getString("name"));
            dqruleEntity.setDescription(rs.getString("description"));
            dqruleEntity.setRuleRef(rs.getString("rule_ref"));
            dqruleEntity.setSettings(rs.getString("settings"));
            dqruleEntity.setRuleTypeId(rs.getString("rule_type_id"));

            return new DQRule(dqruleEntity, new WorkflowableMetadata(rs, dqruleEntity.getArtifactType()));
        }
    }

    private static class FlatDQRuleRowMapper extends FlatItemRowMapper<FlatDQRule> {

        public FlatDQRuleRowMapper() {
            super(FlatDQRule::new);
        }

        @Override
        public FlatDQRule mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatDQRule fd = super.mapRow(rs, rowNum);
            fd.setState(ArtifactState.valueOf(rs.getString("state")));
            fd.setWorkflowTaskId(rs.getString("workflow_task_id"));
            fd.setRuleRef(rs.getString("rule_ref"));
            fd.setSettings(rs.getString("settings"));
            fd.setRuleTypeId(rs.getString("rule_type_id"));
            fd.setRuleTypeName(rs.getString("rule_type_name"));
            return fd;
        }
    }

    class RuleTypeRowMapper implements RowMapper<DQRuleType> {
        @Override
        public DQRuleType mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new DQRuleType(rs.getString("id"), rs.getString("name"),
                    rs.getString("description"));
        }
    }

    public boolean dqRuleIdNameExists(String name, String thisId, UserDetails userDetails) {
        Integer c;
        if (thisId == null)
            c = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant()
                    + ".dq_rule WHERE state='" + ArtifactState.PUBLISHED.name() + "' and name=?", Integer.class, name);
        else
            c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".dq_rule WHERE state='"
                            + ArtifactState.PUBLISHED.name() + "' and name=? AND id<>?",
                    Integer.class, name, UUID.fromString(thisId));
        return c > 0;
    }

    public boolean dqRuleEntityExists(String dqRuleId, UserDetails userDetails) {
        String suffix = "";

        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".entity_sample_to_dq_rule WHERE dq_rule_id=? "
                        + suffix,
                Integer.class, UUID.fromString(dqRuleId));
        return c > 0;
    }

    public boolean existsInLog(String ruleId, UserDetails userDetails) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".dq_log WHERE rule_id=?",
                Integer.class, UUID.fromString(ruleId));
        return c > 0;
    }

    public String createDQRule(DQRuleEntity dqRule, String workflowTaskId, UserDetails userDetails)
            throws LottabyteException {
        UUID newId = dqRule.getId() != null ? UUID.fromString(dqRule.getId()) : UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                + ".dq_rule (id, name, description,rule_ref, state, workflow_task_id, created, creator, modified, modifier,settings,rule_type_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                newId, dqRule.getName(), dqRule.getDescription(), dqRule.getRuleRef(),
                ArtifactState.DRAFT.toString(),
                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                ts, userDetails.getUid(), ts, userDetails.getUid(), dqRule.getSettings(),
                dqRule.getRuleTypeId() == null ? null : UUID.fromString(dqRule.getRuleTypeId()));
        return newId.toString();
    }

    public String createDQRuleDraft(String publishedDQRuleId, String draftId, String workflowTaskId, UserDetails userDetails) {
        UUID newId = draftId != null ? UUID.fromString(draftId) : UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                + ".dq_rule (id, name, description,rule_ref, state, workflow_task_id, published_id, published_version_id, created, creator, modified, modifier,settings,rule_type_id) "
                +
                "SELECT ?, name, description,rule_ref, ?, ?, id, version_id, created, creator, modified, modifier,settings,rule_type_id FROM da_"
                + userDetails.getTenant() + ".dq_rule where id = ?",
                newId, ArtifactState.DRAFT.toString(),
                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                UUID.fromString(publishedDQRuleId));
        return newId.toString();
    }

    public String publishDQRuleDraft(String draftDQRuleId, String publishedDQRuleId, UserDetails userDetails) {
        String res = null;
        if (publishedDQRuleId != null) {
            jdbcTemplate.update(
                    "UPDATE da_" + userDetails.getTenant()
                            + ".dq_rule d SET name = draft.name, description = draft.description, rule_type_id = draft.rule_type_id,rule_ref = draft.rule_ref, settings = draft.settings, "
                            + " ancestor_draft_id = draft.id, modified = draft.modified, modifier = draft.modifier "
                            + " from (select id, name, description,rule_ref, modified, modifier,settings,rule_type_id FROM da_"
                            + userDetails.getTenant() + ".dq_rule) as draft where d.id = ? and draft.id = ?",
                    UUID.fromString(publishedDQRuleId), UUID.fromString(draftDQRuleId));
            res = publishedDQRuleId;
        } else {
            UUID newId = UUID.randomUUID();
            jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                    + ".dq_rule (id, name, description,rule_ref, state, workflow_task_id, "
                    + "published_id, published_version_id, ancestor_draft_id, created, creator, modified, modifier,settings,rule_type_id) "
                    + "SELECT ?, name, description,rule_ref, ?, ?, ?, ?, ?, created, creator, modified, modifier,settings,rule_type_id "
                    + "FROM da_" + userDetails.getTenant() + ".dq_rule where id = ?",
                    newId, ArtifactState.PUBLISHED.toString(), null, null, null,
                    UUID.fromString(draftDQRuleId), UUID.fromString(draftDQRuleId));
            res = newId.toString();
        }
        jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".dq_rule set state = ? where id = ?",
                ArtifactState.DRAFT_HISTORY.toString(), UUID.fromString(draftDQRuleId));
        // jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".domain
        // where id = ?", UUID.fromString(draftDomainId));
        return res;
    }

    public DQRule updateDQRule(String dqRuleId, UpdatableDQRuleEntity dqRuleIEntity, UserDetails userDetails)
            throws LottabyteException {
        DQRule d = new DQRule(dqRuleIEntity);
        d.setId(dqRuleId);
        d.setModifiedBy(userDetails.getUid());
        d.setModifiedAt(LocalDateTime.now());

        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (dqRuleIEntity.getName() != null) {
            sets.add("name=?");
            args.add(dqRuleIEntity.getName());
        }
        if (dqRuleIEntity.getDescription() != null) {
            sets.add("description=?");
            args.add(dqRuleIEntity.getDescription());
        }
        if (dqRuleIEntity.getRuleRef() != null) {
            sets.add("rule_ref=?");
            args.add(dqRuleIEntity.getRuleRef());
        }
        if (dqRuleIEntity.getSettings() != null) {
            sets.add("settings=?");
            args.add(dqRuleIEntity.getSettings());
        }
        if (dqRuleIEntity.getRuleTypeId() != null) {
            sets.add("rule_type_id=?");
            args.add(dqRuleIEntity.getRuleTypeId() == null ? null : UUID.fromString(dqRuleIEntity.getRuleTypeId()));
        }
        if (sets.size() > 0) {
            sets.add("modified=?");
            sets.add("modifier=?");
            args.add(d.getModifiedAt());
            args.add(d.getModifiedBy());
            args.add(UUID.fromString(d.getId()));

            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".dq_rule SET " + StringUtils.join(sets, ", ")
                    + " WHERE id=?", args.toArray());
        }
        return d;
    }

    public SearchResponse<FlatDQRule> searchDQRules(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns,
            UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        //String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "SELECT d.*, true as has_access FROM da_" + userDetails.getTenant() + ".dq_rule d ";

        subQuery = "SELECT sq.*, wft.workflow_state FROM (" + subQuery + ") as sq left join da_"
                + userDetails.getTenant() + ".workflow_task wft "
                + " on sq.workflow_task_id = wft.id ";
        subQuery = "SELECT distinct sq.*, t.tags FROM (" + subQuery + ") as sq left join ("
                + "select d.id as dq_rule_id from da_" + userDetails.getTenant()
                + ".dq_rule d  ) s on s.dq_rule_id = sq.id "
                + "left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id ";

        List<FlatDQRule> flatItems = jdbcTemplate
                .query("SELECT tbl1.*, rt.name AS rule_type_name FROM (" + subQuery + ") as tbl1 LEFT JOIN da_"
                        + userDetails.getTenant() + ".rule_type rt ON tbl1.rule_type_id=rt.id " + where
                        + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                        + searchRequest.getLimit(), new FlatDQRuleRowMapper(), whereValues.toArray());

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(distinct id) FROM (" + subQuery + ") as tbl1 " + where, Integer.class,
                whereValues.toArray());

        SearchResponse<FlatDQRule> res = new SearchResponse<>(total, searchRequest.getLimit(),
                searchRequest.getOffset(), flatItems);

        return res;
    }

    public List<DQRuleType> getRuleTypes(UserDetails userDetails) {
        return jdbcTemplate.query("SELECT id, name, description FROM da_" + userDetails.getTenant() +
                ".rule_type", new RuleTypeRowMapper());
    }

    public DQRuleType getRuleTypeById(String id, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT id, name, description FROM da_" + userDetails.getTenant() +
                ".rule_type WHERE id=?", new RuleTypeRowMapper(), UUID.fromString(id));
    }

    public Integer getLastHistoryIdByPublishedId(String publishedId, UserDetails userDetails) {
        Integer historyId = 0;
        if(publishedId != null) {
            historyId = jdbcTemplate.queryForObject("SELECT max(history_id) FROM da_" + userDetails.getTenant() + ".entity_sample_to_dq_rule where published_id = ?",
                    Integer.class, UUID.fromString(publishedId));
            if(historyId == null)
                historyId = 0;
        }
        return historyId;
    }

    public void patchDQRuleByIndicatorIdAndRuleId(String indicatorId, String dqRuleId, UpdatableEntitySampleDQRule updatableEntitySampleDQRule, UserDetails userDetails) {
        jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".entity_sample_to_dq_rule SET published_id=?, indicator_id=? WHERE indicator_id=? AND dq_rule_id=?",
                UUID.fromString(updatableEntitySampleDQRule.getPublishedId()), UUID.fromString(updatableEntitySampleDQRule.getIndicatorId()), UUID.fromString(indicatorId),
                UUID.fromString(dqRuleId));
    }

    public void patchDQRuleLinkById(String id, UpdatableEntitySampleDQRule updatableEntitySampleDQRule, UserDetails userDetails) {
        jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".entity_sample_to_dq_rule SET indicator_id=?, product_id=?, asset_id=?, dq_rule_id=?, published_id=?, settings=?, send_mail=?, disabled=? WHERE id=?",
                updatableEntitySampleDQRule.getIndicatorId() == null ? null : UUID.fromString(updatableEntitySampleDQRule.getIndicatorId()),
                updatableEntitySampleDQRule.getProductId() == null ? null : UUID.fromString(updatableEntitySampleDQRule.getProductId()),
                updatableEntitySampleDQRule.getAssetId() == null ? null : UUID.fromString(updatableEntitySampleDQRule.getAssetId()),
                UUID.fromString(updatableEntitySampleDQRule.getDqRuleId()),
                UUID.fromString(updatableEntitySampleDQRule.getPublishedId()), updatableEntitySampleDQRule.getSettings(),
                updatableEntitySampleDQRule.getSendMail(), updatableEntitySampleDQRule.getDisabled(), UUID.fromString(id));
    }

    public void createDQRuleLink(EntitySampleDQRuleEntity entity, UserDetails userDetails) {
        Timestamp ts = new Timestamp(new java.util.Date().getTime());

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
            + ".entity_sample_to_dq_rule (id, dq_rule_id, settings, created, creator, modified, modifier, disabled, indicator_id, product_id, send_mail, asset_id, published_id)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)", UUID.randomUUID(), UUID.fromString(entity.getDqRuleId()), entity.getSettings(), ts, userDetails.getUid(), ts, userDetails.getUid(),
                entity.getDisabled(), entity.getIndicatorId() == null ? null : UUID.fromString(entity.getIndicatorId()),
                entity.getProductId() == null ? null : UUID.fromString(entity.getProductId()), entity.getSendMail(), entity.getAssetId() == null ? null :
                UUID.fromString(entity.getAssetId()), entity.getPublishedId() == null ? null : UUID.fromString(entity.getPublishedId()));
    }

    public void deleteDQRuleLinkById(String id, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".entity_sample_to_dq_rule WHERE id=?", UUID.fromString(id));
    }
}
