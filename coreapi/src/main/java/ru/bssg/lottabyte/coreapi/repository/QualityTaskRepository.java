package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import liquibase.pro.packaged.v;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.indicator.Indicator;
import ru.bssg.lottabyte.core.model.qualityTask.FlatQualityRuleTask;
import ru.bssg.lottabyte.core.model.qualityTask.FlatQualityTask;
import ru.bssg.lottabyte.core.model.qualityTask.QualityRuleTask;
import ru.bssg.lottabyte.core.model.qualityTask.QualityRuleTaskEntity;
import ru.bssg.lottabyte.core.model.qualityTask.QualityTask;
import ru.bssg.lottabyte.core.model.qualityTask.QualityTaskAssertion;
import ru.bssg.lottabyte.core.model.qualityTask.QualityTaskAssertionEntity;
import ru.bssg.lottabyte.core.model.qualityTask.QualityTaskEntity;
import ru.bssg.lottabyte.core.model.qualityTask.QualityTaskRun;
import ru.bssg.lottabyte.core.model.qualityTask.QualityTaskRunEntity;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
@Slf4j
public class QualityTaskRepository extends GenericArtifactRepository<QualityTask> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = { "run_id", "parent_run_id", "event_type", "event_time",
            "full_name", "system_producer",
            "input_name", "input_asset_name", "input_id", "input_system_id", "input_system_name",
            "output_name", "output_asset_name", "output_id", "output_system_id", "output_system_name",
            "state_name", "assertion_msg", "state", "state_local", "state_name", "state_name_local", "producer",
            "output_asset_domain_name", "input_asset_domain_name",
            "output_asset_domain_id", "input_asset_domain_id",
            "input_asset_id", "output_asset_id" };

    @Autowired
    public QualityTaskRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.qualityTask.name(), extFields);
        super.setMapper(new QualityTaskRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    class QualityTaskRowMapper implements RowMapper<QualityTask> {
        @Override
        public QualityTask mapRow(ResultSet rs, int rowNum) throws SQLException {
            QualityTask task = null;

            QualityTaskEntity taskEntity = new QualityTaskEntity();
            taskEntity.setRunId(rs.getString("run_id"));
            taskEntity.setParentRunId(rs.getString("parent_run_id"));
            taskEntity.setEventType(rs.getString("event_type"));
            taskEntity.setEventTime(rs.getTimestamp("event_time"));
            taskEntity.setFullName(rs.getString("full_name"));
            taskEntity.setSystemProducer(rs.getString("system_producer"));
            taskEntity.setInputName(rs.getString("input_name"));
            taskEntity.setInputAssetName(rs.getString("input_asset_name"));
            taskEntity.setInputId(rs.getString("input_id"));
            taskEntity.setInputSystemId(rs.getString("input_system_id"));
            taskEntity.setInputSystemName(rs.getString("input_system_name"));
            taskEntity.setOutputName(rs.getString("output_name"));
            taskEntity.setOutputAssetName(rs.getString("output_asset_name"));
            taskEntity.setOutputId(rs.getString("output_id"));
            taskEntity.setOutputSystemId(rs.getString("output_system_id"));
            taskEntity.setOutputSystemName(rs.getString("output_system_name"));
            taskEntity.setStateName(rs.getString("state_name"));
            taskEntity.setStateNameLocal(rs.getString("state_name_local"));
            taskEntity.setAssertionMsg(rs.getString("assertion_msg"));
            taskEntity.setState(rs.getString("state"));
            taskEntity.setStateLocal(rs.getString("state_local"));
            taskEntity.setProducer(rs.getString("producer"));
            taskEntity.setOutputAssetDomainName(rs.getString("output_asset_domain_name"));
            taskEntity.setInputAssetDomainName(rs.getString("input_asset_domain_name"));
            taskEntity.setOutputAssetDomainId(rs.getString("output_asset_domain_id"));
            taskEntity.setInputAssetDomainId(rs.getString("input_asset_domain_id"));
            taskEntity.setInputAssetId(rs.getString("input_asset_id"));
            taskEntity.setOutputAssetId(rs.getString("output_asset_id"));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            task = new QualityTask(taskEntity, md);

            return task;
        }
    }

    class FlatQualityTaskRowMapper implements RowMapper<FlatQualityTask> {
        @Override
        public FlatQualityTask mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatQualityTask task = new FlatQualityTask();

            task.setRunId(rs.getString("run_id"));
            task.setParentRunId(rs.getString("parent_run_id"));
            task.setEventType(rs.getString("event_type"));
            task.setEventTime(rs.getTimestamp("event_time"));
            task.setFullName(rs.getString("full_name"));
            task.setSystemProducer(rs.getString("system_producer"));
            task.setInputName(rs.getString("input_name"));
            task.setInputAssetName(rs.getString("input_asset_name"));
            task.setInputId(rs.getString("input_id"));
            task.setInputSystemId(rs.getString("input_system_id"));
            task.setInputSystemName(rs.getString("input_system_name"));
            task.setOutputName(rs.getString("output_name"));
            task.setOutputAssetName(rs.getString("output_asset_name"));
            task.setOutputId(rs.getString("output_id"));
            task.setOutputSystemId(rs.getString("output_system_id"));
            task.setOutputSystemName(rs.getString("output_system_name"));
            task.setStateName(rs.getString("state_name"));
            task.setStateNameLocal(rs.getString("state_name_local"));
            task.setAssertionMsg(rs.getString("assertion_msg"));
            task.setState(rs.getString("state"));
            task.setStateLocal(rs.getString("state_local"));
            task.setProducer(rs.getString("producer"));
            task.setOutputAssetDomainName(rs.getString("output_asset_domain_name"));
            task.setInputAssetDomainName(rs.getString("input_asset_domain_name"));
            task.setOutputAssetDomainId(rs.getString("output_asset_domain_id"));
            task.setInputAssetDomainId(rs.getString("input_asset_domain_id"));
            task.setInputAssetId(rs.getString("input_asset_id"));
            task.setOutputAssetId(rs.getString("output_asset_id"));

            return task;
        }
    }

    class QualityRuleTaskRowMapper implements RowMapper<QualityRuleTask> {
        @Override
        public QualityRuleTask mapRow(ResultSet rs, int rowNum) throws SQLException {
            QualityRuleTask task = null;

            QualityRuleTaskEntity taskEntity = new QualityRuleTaskEntity();
            taskEntity.setSystemId(rs.getString("system_id"));
            taskEntity.setSystemName(rs.getString("system_name"));
            taskEntity.setRuleName(rs.getString("rule_name"));
            taskEntity.setRuleId(rs.getString("rule_id"));
            taskEntity.setRuleRef(rs.getString("rule_ref"));
            taskEntity.setRuleSettings(rs.getString("rule_settings"));
            taskEntity.setQueryName(rs.getString("query_name"));
            taskEntity.setEntityName(rs.getString("entity_name"));
            taskEntity.setDataAssetId(rs.getString("data_asset_id"));
            taskEntity.setEntitySampleToDqRuleId(rs.getString("entity_sample_to_dq_rule_id"));
            taskEntity.setIndicatorName(rs.getString("indicator_name"));
            taskEntity.setProductName(rs.getString("product_name"));
            taskEntity.setDataAssetName(rs.getString("data_asset_name"));
            taskEntity.setProductId(rs.getString("product_id"));
            taskEntity.setIndicatorId(rs.getString("indicator_id"));
            taskEntity.setEntitySampleId(rs.getString("entity_sample_id"));
            taskEntity.setEntitySampleName(rs.getString("entity_sample_name"));
            taskEntity.setIsCrontab(rs.getString("is_crontab"));

            Metadata md = new Metadata();
            task = new QualityRuleTask(taskEntity, md);

            return task;
        }
    }

    class FlatQualityRuleTaskRowMapper implements RowMapper<FlatQualityRuleTask> {
        @Override
        public FlatQualityRuleTask mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatQualityRuleTask task = new FlatQualityRuleTask();

            task.setSystemId(rs.getString("system_id"));
            task.setSystemName(rs.getString("system_name"));
            task.setRuleName(rs.getString("rule_name"));
            task.setRuleId(rs.getString("rule_id"));
            task.setRuleRef(rs.getString("rule_ref"));
            task.setRuleSettings(rs.getString("rule_settings"));
            task.setQueryName(rs.getString("query_name"));
            task.setEntityName(rs.getString("entity_name"));
            task.setDataAssetId(rs.getString("data_asset_id"));
            task.setEntitySampleToDqRuleId(rs.getString("entity_sample_to_dq_rule_id"));
            task.setIndicatorName(rs.getString("indicator_name"));
            task.setProductName(rs.getString("product_name"));
            task.setDataAssetName(rs.getString("data_asset_name"));
            task.setProductId(rs.getString("product_id"));
            task.setIndicatorId(rs.getString("indicator_id"));
            task.setEntitySampleId(rs.getString("entity_sample_id"));
            task.setEntitySampleName(rs.getString("entity_sample_name"));
            task.setIsCrontab(rs.getString("is_crontab"));

            return task;
        }
    }

    private FlatQualityRuleTask flatQualityRuleTaskMap(ResultSet rs) throws LottabyteException, SQLException {
        FlatQualityRuleTask flatTask = new FlatQualityRuleTask(ruleTaskMap(rs));
        flatTask.setSystemId(rs.getString("system_id"));
        flatTask.setSystemName(rs.getString("system_name"));
        flatTask.setRuleName(rs.getString("rule_name"));
        flatTask.setRuleId(rs.getString("rule_id"));
        flatTask.setRuleRef(rs.getString("rule_ref"));
        flatTask.setRuleSettings(rs.getString("rule_settings"));
        flatTask.setQueryName(rs.getString("query_name"));
        flatTask.setEntityName(rs.getString("entity_name"));
        flatTask.setDataAssetId(rs.getString("data_asset_id"));
        flatTask.setEntitySampleToDqRuleId(rs.getString("entity_sample_to_dq_rule_id"));
        flatTask.setIndicatorName(rs.getString("indicator_name"));
        flatTask.setProductName(rs.getString("product_name"));
        flatTask.setDataAssetName(rs.getString("data_asset_name"));
        flatTask.setProductId(rs.getString("product_id"));
        flatTask.setIndicatorId(rs.getString("indicator_id"));
        flatTask.setEntitySampleId(rs.getString("entity_sample_id"));
        flatTask.setEntitySampleName(rs.getString("entity_sample_name"));
        flatTask.setIsCrontab(rs.getString("is_crontab"));
        return flatTask;
    }

    private QualityRuleTask ruleTaskMap(ResultSet rs) throws LottabyteException, SQLException {
        QualityRuleTask task = new QualityRuleTask();

        QualityRuleTaskEntity taskEntity = new QualityRuleTaskEntity();
        taskEntity.setSystemId(rs.getString("system_id"));
        taskEntity.setSystemName(rs.getString("system_name"));
        taskEntity.setRuleName(rs.getString("rule_name"));
        taskEntity.setRuleId(rs.getString("rule_id"));
        taskEntity.setRuleRef(rs.getString("rule_ref"));
        taskEntity.setRuleSettings(rs.getString("rule_settings"));
        taskEntity.setQueryName(rs.getString("query_name"));
        taskEntity.setEntityName(rs.getString("entity_name"));
        taskEntity.setDataAssetId(rs.getString("data_asset_id"));
        taskEntity.setEntitySampleToDqRuleId(rs.getString("entity_sample_to_dq_rule_id"));
        taskEntity.setIndicatorName(rs.getString("indicator_name"));
        taskEntity.setProductName(rs.getString("product_name"));
        taskEntity.setDataAssetName(rs.getString("data_asset_name"));
        taskEntity.setProductId(rs.getString("product_id"));
        taskEntity.setIndicatorId(rs.getString("indicator_id"));
        taskEntity.setEntitySampleId(rs.getString("entity_sample_id"));
        taskEntity.setEntitySampleName(rs.getString("entity_sample_name"));
        taskEntity.setIsCrontab(rs.getString("is_crontab"));
        Metadata md = new Metadata();
        task = new QualityRuleTask(taskEntity, md);

        return task;

    }

    class QualityTaskAssertionRowMapper implements RowMapper<QualityTaskAssertion> {
        @Override
        public QualityTaskAssertion mapRow(ResultSet rs, int rowNum) throws SQLException {
            QualityTaskAssertion task = null;

            QualityTaskAssertionEntity taskEntity = new QualityTaskAssertionEntity();
            taskEntity.setStateName(rs.getString("state_name"));
            taskEntity.setState(rs.getString("state"));
            taskEntity.setRuleName(rs.getString("rule_name"));
            taskEntity.setColumn(rs.getString("column"));
            taskEntity.setMsg(rs.getString("msg"));
            taskEntity.setRuleId(rs.getString("rule_id"));
            taskEntity.setOlId(rs.getString("ol_id"));
            taskEntity.setAssertion(rs.getString("assertion"));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            task = new QualityTaskAssertion(taskEntity, md);

            return task;
        }
    }

    class QualityTaskRunRowMapper implements RowMapper<QualityTaskRun> {
        @Override
        public QualityTaskRun mapRow(ResultSet rs, int rowNum) throws SQLException {
            QualityTaskRun task = null;

            QualityTaskRunEntity taskEntity = new QualityTaskRunEntity();
            taskEntity.setRuleId(rs.getString("rule_id"));
            taskEntity.setName(rs.getString("name"));
            taskEntity.setTime(rs.getTimestamp("time"));
            taskEntity.setState(rs.getString("state"));

            Metadata md = new Metadata();

            task = new QualityTaskRun(taskEntity, md);

            return task;
        }
    }

    private QualityTask taskMap(ResultSet rs) throws LottabyteException, SQLException {
        QualityTask task = new QualityTask();

        QualityTaskEntity taskEntity = new QualityTaskEntity();
        taskEntity.setRunId(rs.getString("run_id"));
        taskEntity.setParentRunId(rs.getString("parent_run_id"));
        taskEntity.setEventType(rs.getString("event_type"));
        taskEntity.setEventTime(rs.getTimestamp("event_time"));
        taskEntity.setFullName(rs.getString("full_name"));
        taskEntity.setSystemProducer(rs.getString("system_producer"));
        taskEntity.setInputName(rs.getString("input_name"));
        taskEntity.setInputAssetName(rs.getString("input_asset_name"));
        taskEntity.setInputId(rs.getString("input_id"));
        taskEntity.setInputSystemId(rs.getString("input_system_id"));
        taskEntity.setInputSystemName(rs.getString("input_system_name"));
        taskEntity.setOutputName(rs.getString("output_name"));
        taskEntity.setOutputAssetName(rs.getString("output_asset_name"));
        taskEntity.setOutputId(rs.getString("output_id"));
        taskEntity.setOutputSystemId(rs.getString("output_system_id"));
        taskEntity.setOutputSystemName(rs.getString("output_system_name"));
        taskEntity.setStateName(rs.getString("state_name"));
        taskEntity.setAssertionMsg(rs.getString("assertion_msg"));
        taskEntity.setState(rs.getString("state"));
        taskEntity.setProducer(rs.getString("producer"));
        taskEntity.setOutputAssetDomainName(rs.getString("output_asset_domain_name"));
        taskEntity.setInputAssetDomainName(rs.getString("input_asset_domain_name"));
        taskEntity.setOutputAssetDomainId(rs.getString("output_asset_domain_id"));
        taskEntity.setInputAssetDomainId(rs.getString("input_asset_domain_id"));
        taskEntity.setInputAssetId(rs.getString("input_asset_id"));
        taskEntity.setOutputAssetId(rs.getString("output_asset_id"));
        taskEntity.setStateNameLocal(rs.getString("state_name_local"));
        taskEntity.setStateLocal(rs.getString("state_local"));
        Metadata md = new Metadata();
        md.setId(rs.getString("id"));
        task = new QualityTask(taskEntity, md);

        return task;

    }

    private FlatQualityTask flatQualityTaskMap(ResultSet rs) throws LottabyteException, SQLException {
        FlatQualityTask flatTask = new FlatQualityTask(taskMap(rs));
        flatTask.setRunId(rs.getString("run_id"));
        flatTask.setParentRunId(rs.getString("parent_run_id"));
        flatTask.setEventType(rs.getString("event_type"));
        flatTask.setEventTime(rs.getTimestamp("event_time"));
        flatTask.setFullName(rs.getString("full_name"));
        flatTask.setSystemProducer(rs.getString("system_producer"));
        flatTask.setInputName(rs.getString("input_name"));
        flatTask.setInputAssetName(rs.getString("input_asset_name"));
        flatTask.setInputId(rs.getString("input_id"));
        flatTask.setInputSystemId(rs.getString("input_system_id"));
        flatTask.setInputSystemName(rs.getString("input_system_name"));
        flatTask.setOutputName(rs.getString("output_name"));
        flatTask.setOutputAssetName(rs.getString("output_asset_name"));
        flatTask.setOutputId(rs.getString("output_id"));
        flatTask.setOutputSystemId(rs.getString("output_system_id"));
        flatTask.setOutputSystemName(rs.getString("output_system_name"));
        flatTask.setStateName(rs.getString("state_name"));
        flatTask.setAssertionMsg(rs.getString("assertion_msg"));
        flatTask.setState(rs.getString("state"));
        flatTask.setProducer(rs.getString("producer"));
        flatTask.setOutputAssetDomainName(rs.getString("output_asset_domain_name"));
        flatTask.setInputAssetDomainName(rs.getString("input_asset_domain_name"));
        flatTask.setOutputAssetDomainId(rs.getString("output_asset_domain_id"));
        flatTask.setInputAssetDomainId(rs.getString("input_asset_domain_id"));
        flatTask.setInputAssetId(rs.getString("input_asset_id"));
        flatTask.setOutputAssetId(rs.getString("output_asset_id"));
        flatTask.setStateNameLocal(rs.getString("state_name_local"));
        flatTask.setStateLocal(rs.getString("state_local"));
        return flatTask;
    }

    public SearchResponse<FlatQualityTask> searchTasks(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails ud) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, ud);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "SELECT openlinage_log_monitor_draft.*, true as has_access from da_" + ud.getTenant()
                + ".openlinage_log_monitor_draft ";
        if (ud.getStewardId() != null) {
            String hasAccessJoinQuery = QueryHelper.getWhereIdInQuery(ArtifactType.task, ud);
            subQuery = "SELECT openlinage_log_monitor_draft.*, case when acc.id is null then false else true end as has_access FROM da_"
                    + ud.getTenant() + ".openlinage_log_monitor_draft "
                    + " left join (select openlinage_log_monitor_draft.id from da_" + ud.getTenant()
                    + ".openlinage_log_monitor_draft " + hasAccessJoinQuery
                    + ") acc on openlinage_log_monitor_draft.id = acc.id ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward())
                subQuery = "SELECT openlinage_log_monitor_draft.*, true as has_access FROM da_" + ud.getTenant()
                        + ".openlinage_log_monitor_draft "
                        + hasAccessJoinQuery;
        }
        String where_sec = (ud.getUserDomains() != null && !ud.getUserDomains().isEmpty())
                ? "(input_asset_domain_id in (select domain_id from da_" + ud.getTenant()
                        + ".user_to_domain utd where utd.user_id = " + ud.getUid() + ") and "
                        +
                        "output_asset_domain_id in (select domain_id from da_" + ud.getTenant()
                        + ".user_to_domain utd where utd.user_id = " + ud.getUid() + ")) "
                : "1=1";

        where = where.trim().length() > 0 ? where + " and " + where_sec : " where " + where_sec;
        String queryForItems = "SELECT * FROM (" + subQuery + ") as tbl1 "
                + join + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatQualityTask> items = jdbcTemplate.query(queryForItems, new FlatQualityTaskRowMapper(), whereValues.toArray());

        String queryForTotal = "SELECT COUNT(tbl1.id) FROM (" + subQuery + ") tbl1 " + join + where;
        Integer total = jdbcTemplate.queryForObject(queryForTotal, Integer.class, whereValues.toArray());

        SearchResponse<FlatQualityTask> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), items);

        return res;
    }

    public List<QualityTask> getQualityTasksByRunId(String runId, UserDetails userDetails) {
        System.out.println("runId = " + runId);
        Integer uid = Integer.parseInt(userDetails.getUid());
        List<QualityTask> res = jdbcTemplate.query(
                "SELECT * FROM da_" + userDetails.getTenant()
                        + ".openlinage_log_monitor where run_id = ? and event_type = 'START' ",
                new QualityTaskRepository.QualityTaskRowMapper(), UUID.fromString(runId));

        List<QualityTask> children = jdbcTemplate.query(
                "SELECT * FROM da_" + userDetails.getTenant()
                        + ".openlinage_log_monitor where parent_run_id = ? and event_type = 'START' ",
                new QualityTaskRepository.QualityTaskRowMapper(), UUID.fromString(runId));

        for (QualityTask q : children) {
            res = Stream
                    .concat(res.stream(), this.getQualityTasksByRunId(q.getEntity().getRunId(), userDetails).stream())
                    .collect(Collectors.toList());
        }

        return res;
    }

    public List<QualityTaskAssertion> getQualityTasksAssertionByRunId(String runId, UserDetails userDetails) {

        List<QualityTaskAssertion> res = jdbcTemplate.query(
                "SELECT * FROM da_" + userDetails.getTenant() + ".openlinage_log_assertions_monitor where run_id = ?",
                new QualityTaskRepository.QualityTaskAssertionRowMapper(), UUID.fromString(runId));

        return res;
    }

    public List<QualityRuleTask> getQualityRulesForSchedule(UserDetails userDetails) {

        List<QualityRuleTask> res = jdbcTemplate.query(
                "SELECT * FROM da_" + userDetails.getTenant() + ".dq_rule_tasks where rule_settings like '%crontab%'",
                new QualityTaskRepository.QualityRuleTaskRowMapper());

        return res;
    }

    public List<QualityRuleTask> getQualityRules(UserDetails userDetails) {

        List<QualityRuleTask> res = jdbcTemplate.query(
                "SELECT * FROM da_" + userDetails.getTenant() + ".dq_rule_tasks",
                new QualityTaskRepository.QualityRuleTaskRowMapper());

        return res;
    }

    public SearchResponse<FlatQualityRuleTask> searchQualityRules(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails ud) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, ud);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        if (orderby.equals("name"))
            orderby = "rule_name";

        String subQuery = "SELECT dq_rule_tasks.*, true as has_access from da_" + ud.getTenant()
                + ".dq_rule_tasks ";
        if (ud.getStewardId() != null) {
            String hasAccessJoinQuery = QueryHelper.getWhereIdInQuery(ArtifactType.task, ud);
            subQuery = "SELECT dq_rule_tasks.*, case when acc.id is null then false else true end as has_access FROM da_"
                    + ud.getTenant() + ".dq_rule_tasks ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward())
                subQuery = "SELECT dq_rule_tasks.*, true as has_access FROM da_" + ud.getTenant()
                        + ".dq_rule_tasks "
                        + hasAccessJoinQuery;
        }

        String queryForItems = "SELECT * FROM (" + subQuery + ") as tbl1 "
                + join + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatQualityRuleTask> items = jdbcTemplate.query(queryForItems, new FlatQualityRuleTaskRowMapper(), whereValues.toArray());

        String queryForTotal = "SELECT COUNT(tbl1.id) FROM (" + subQuery + ") tbl1 " + join + where;
        Integer total = jdbcTemplate.queryForObject(queryForTotal, Integer.class, whereValues.toArray());

        SearchResponse<FlatQualityRuleTask> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), items);

        return res;
    }

    public void addQualityRuleTask(String ruleId,
            UserDetails userDetails) {

        UUID newId = UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        jdbcTemplate.update("insert into da_" + userDetails.getTenant() + ".dq_tasks (id, \"time\", rule_id, state) values (?,?,?,?)",
                newId, ts, UUID.fromString(ruleId), 0);
    }

    public List<QualityTaskRun> getQualityRuleRuns(String ruleId, UserDetails userDetails) {

        List<QualityTaskRun> res = jdbcTemplate.query(
                "select dt.rule_id, dr.\"name\" ,dt.\"time\" ,dt.state  from da_" + userDetails.getTenant() + ".dq_tasks dt join da_"
                        + userDetails.getTenant() + ".entity_sample_to_dq_rule r on r.id = dt.rule_id "
                        + "LEFT JOIN da_" + userDetails.getTenant()
                        + ".dq_rule dr ON r.dq_rule_id = dr.id where dt.rule_id = ? order by dt.\"time\" desc",
                new QualityTaskRepository.QualityTaskRunRowMapper(), UUID.fromString(ruleId));

        return res;
    }
}
