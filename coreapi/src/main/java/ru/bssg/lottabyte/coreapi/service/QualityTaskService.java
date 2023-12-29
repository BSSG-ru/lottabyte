package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;
import ru.bssg.lottabyte.core.model.qualityTask.FlatQualityRuleTask;
import ru.bssg.lottabyte.core.model.qualityTask.FlatQualityTask;
import ru.bssg.lottabyte.core.model.qualityTask.QualityRuleTask;
import ru.bssg.lottabyte.core.model.qualityTask.QualityTask;
import ru.bssg.lottabyte.core.model.qualityTask.QualityTaskAssertion;
import ru.bssg.lottabyte.core.model.qualityTask.QualityTaskRun;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchColumnForJoin;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.QualityTaskRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class QualityTaskService {
        private final QualityTaskRepository taskRepository;
        private final ElasticsearchService elasticsearchService;
        private final EntityQueryService entityQueryService;
        private final SystemConnectionService systemConnectionService;

        private final SearchColumn[] searchableColumns = {
                        new SearchColumn("name", SearchColumn.ColumnType.Text),
                        new SearchColumn("run_id", SearchColumn.ColumnType.Text),
                        new SearchColumn("parent_run_id", SearchColumn.ColumnType.Text),
                        new SearchColumn("event_type", SearchColumn.ColumnType.Text),
                        new SearchColumn("event_time", SearchColumn.ColumnType.Timestamp),
                        new SearchColumn("full_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("system_producer", SearchColumn.ColumnType.Text),
                        new SearchColumn("input_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("input_asset_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("input_id", SearchColumn.ColumnType.Text),
                        new SearchColumn("input_system_id", SearchColumn.ColumnType.Text),
                        new SearchColumn("input_system_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("output_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("output_asset_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("output_id", SearchColumn.ColumnType.Text),
                        new SearchColumn("output_system_id", SearchColumn.ColumnType.Text),
                        new SearchColumn("output_system_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("state_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("assertion_msg", SearchColumn.ColumnType.Text),
                        new SearchColumn("state", SearchColumn.ColumnType.Text)
        };
        private final SearchColumnForJoin[] joinColumns = {
        };

        private final SearchColumn[] ruleTaskSearchableColumns = {
                        new SearchColumn("system_id", SearchColumn.ColumnType.Text),
                        new SearchColumn("system_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("rule_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("rule_id", SearchColumn.ColumnType.Text),
                        new SearchColumn("rule_ref", SearchColumn.ColumnType.Text),
                        new SearchColumn("rule_settings", SearchColumn.ColumnType.Text),
                        new SearchColumn("query_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("entity_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("data_asset_id", SearchColumn.ColumnType.Text),
                        new SearchColumn("entity_sample_to_dq_rule_id", SearchColumn.ColumnType.Text),
                        new SearchColumn("indicator_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("product_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("data_asset_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("product_id", SearchColumn.ColumnType.Text),
                        new SearchColumn("indicator_id", SearchColumn.ColumnType.Text),
                        new SearchColumn("entity_sample_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("entity_sample_id", SearchColumn.ColumnType.Text),
                        new SearchColumn("is_crontab", SearchColumn.ColumnType.Text),

        };

        public SearchResponse<FlatQualityTask> searchQualityTask(SearchRequestWithJoin request, UserDetails userDetails)
                        throws LottabyteException {
                ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
                return taskRepository.searchTasks(request, searchableColumns,
                                joinColumns, userDetails);

        }

        public PaginatedArtifactList<QualityTask> getTasksWithPaging(Integer limit, Integer offset,
                        UserDetails userDetails) {
                return taskRepository.getAllPaginated(limit, offset, "/v1/quality_tasks", userDetails);
        }

        public List<QualityTask> getQualityTasksByRunId(String runId,
                        UserDetails userDetails) throws LottabyteException {

                return taskRepository.getQualityTasksByRunId(runId,
                                userDetails);
        }

        public List<QualityTaskAssertion> getQualityTasksAssertionByRunId(String runId,
                        UserDetails userDetails) throws LottabyteException {

                return taskRepository.getQualityTasksAssertionByRunId(runId,
                                userDetails);
        }

        public List<QualityRuleTask> getQualityRulesForSchedule(
                        UserDetails userDetails) throws LottabyteException {

                return taskRepository.getQualityRulesForSchedule(
                                userDetails);
        }

        public List<QualityRuleTask> getQualityRules(
                        UserDetails userDetails) throws LottabyteException {

                return taskRepository.getQualityRules(
                                userDetails);
        }

        public SearchResponse<FlatQualityRuleTask> searchQualityRuleTask(SearchRequestWithJoin request,
                        UserDetails userDetails)
                        throws LottabyteException {

                return taskRepository.searchQualityRules(request, ruleTaskSearchableColumns,
                                joinColumns, userDetails);

        }

        public void addQualityRuleTask(String ruleId,
                        UserDetails userDetails) throws LottabyteException {

                taskRepository.addQualityRuleTask(ruleId,
                                userDetails);
        }

        public List<QualityTaskRun> getQualityRuleRuns(String ruleId, UserDetails userDetails)
                        throws LottabyteException {

                return taskRepository.getQualityRuleRuns(ruleId, userDetails);
        }
}
