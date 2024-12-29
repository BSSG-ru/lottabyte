package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.WorkflowableMetadata;
import ru.bssg.lottabyte.core.model.product.FlatProduct;
import ru.bssg.lottabyte.core.model.product.Product;
import ru.bssg.lottabyte.core.model.product.ProductEntity;
import ru.bssg.lottabyte.core.model.relation.Relation;
import ru.bssg.lottabyte.core.model.tag.Tag;
import ru.bssg.lottabyte.core.model.task.*;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchColumnForJoin;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@Slf4j
public class TaskRepository extends GenericArtifactRepository<Task> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = {"system_connection_id", "query_id"};


    @Autowired
    public TaskRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.task.name(), extFields);
        super.setMapper(new TaskRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    class TaskRowMapper implements RowMapper<Task> {
        @Override
        public Task mapRow(ResultSet rs, int rowNum) throws SQLException {
            Task task = null;

            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setName(rs.getString("name"));
            taskEntity.setDescription(rs.getString("description"));
            taskEntity.setShortDescription(rs.getString("short_description"));
            taskEntity.setQueryId(rs.getString("query_id"));
            taskEntity.setSystemConnectionId(rs.getString("system_connection_id"));
            taskEntity.setIsMetadataTask(rs.getBoolean("is_metadata_task"));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));
            md.setName(rs.getString("name"));
            md.setArtifactType(taskEntity.getArtifactType().toString());
            task = new Task(taskEntity, md);

            return task;
        }
    }

    public static class FlatTaskRowMapper implements RowMapper<FlatTask> {
        @Override
        public FlatTask mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatTask flatTask = new FlatTask();
            try {
                TaskEntity taskEntity = new TaskEntity();
                taskEntity.setId(rs.getString("id"));
                taskEntity.setName(rs.getString("name"));
                taskEntity.setDescription(rs.getString("description"));
                taskEntity.setShortDescription(rs.getString("short_description"));
                taskEntity.setIsMetadataTask(rs.getBoolean("is_metadata_task"));

                flatTask = new FlatTask(
                        new Task(taskEntity, new Metadata(rs, taskEntity.getArtifactType())));

                return flatTask;
            } catch (Exception e) {
                return flatTask;
            }
        }
    }

    public Boolean hasAccessToTask(String taskId, UserDetails userDetails) {
        return userDetails.getStewardId() == null ? true :
                jdbcTemplate.queryForObject("SELECT EXISTS(SELECT task.ID FROM da_" + userDetails.getTenant() + ".task " +
                                QueryHelper.getWhereIdInQuery(ArtifactType.task, userDetails) + " and task.id = ?) as exists", Boolean.class,
                        UUID.fromString(taskId));
    }

    public List<Task> getTasksBySystemConnectionId(String systemConnectionId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() +
                ".task WHERE system_connection_id = ?", new RowMapper<Task>() {
            @Override
            public Task mapRow(ResultSet rs, int rowNum) throws SQLException {
                TaskEntity e = new TaskEntity();
                e.setId(rs.getString("id"));
                e.setName(rs.getString("name"));
                return new Task(e, new Metadata(rs, e.getArtifactType()));
            }
        }, UUID.fromString(systemConnectionId));
    }

    public Boolean existsTaskWithSystemConnection(String systemConnectionId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() +
                ".task WHERE system_connection_id = ?) AS EXISTS", Boolean.class, UUID.fromString(systemConnectionId));
    }

    public PaginatedArtifactList<Task> getTasksByQueryId(String queryId, Integer limit, Integer offset, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".task WHERE query_id=?", Integer.class, UUID.fromString(queryId));
        String query = "SELECT * FROM da_" + userDetails.getTenant() + ".task WHERE query_id=? offset ? limit ? ";
        List<Task> taskList = jdbcTemplate.query(query, new TaskRowMapper(), UUID.fromString(queryId), offset, limit);

        PaginatedArtifactList<Task> paginatedArtifactList = new PaginatedArtifactList<>(
                taskList, offset, limit, total, "/v1/task");
        return paginatedArtifactList;
    }

    public Task getTaskByName(String name, UserDetails userDetails) {
        List<Task> taskList = jdbcTemplate.query("SELECT id, \"name\", description, short_description, system_connection_id, query_id, created, creator, modified, modifier FROM da_" + userDetails.getTenant() + ".task WHERE name=?",
                new TaskRowMapper(), name);

        Task t = taskList.stream().findFirst().orElse(null);

        return t;
    }

    public String createTask(UpdatableTaskEntity newTaskEntity, UserDetails userDetails) {
        UUID id = UUID.randomUUID();

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".task (id, \"name\", description, short_description, system_connection_id, query_id, created, creator, modified, modifier, is_metadata_task) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                id, newTaskEntity.getName(), newTaskEntity.getDescription(), newTaskEntity.getShortDescription(), UUID.fromString(newTaskEntity.getSystemConnectionId()), newTaskEntity.getQueryId() == null ? null : UUID.fromString(newTaskEntity.getQueryId()),
                now, userDetails.getUid(), now, userDetails.getUid(), newTaskEntity.getIsMetadataTask());

        return id.toString();
    }

    public void updateTask(String taskId, UpdatableTaskEntity taskEntity, UserDetails userDetails) throws LottabyteException {
        Task task = new Task(taskEntity);
        task.setId(taskId);

        LocalDateTime now = LocalDateTime.now();
        task.setModifiedAt(now);
        task.setModifiedBy(userDetails.getUid());

        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (taskEntity.getName() != null) {
            sets.add("name=?");
            args.add(taskEntity.getName());
        }
        if (taskEntity.getDescription() != null) {
            sets.add("description=?");
            args.add(taskEntity.getDescription());
        }
        if (taskEntity.getShortDescription() != null) {
            sets.add("short_description=?");
            args.add(taskEntity.getShortDescription());
        }
        if (taskEntity.getSystemConnectionId() != null) {
            sets.add("system_connection_id=?");
            args.add(UUID.fromString(taskEntity.getSystemConnectionId()));
        }
        if (taskEntity.getQueryId() != null) {
            sets.add("query_id=?");
            args.add(UUID.fromString(taskEntity.getQueryId()));
        }
        if (taskEntity.getIsMetadataTask() != null) {
            sets.add("is_metadata_task=?");
            args.add(taskEntity.getIsMetadataTask());
        }
        if (sets.size() > 0) {
            sets.add("modified=?");
            sets.add("modifier=?");
            args.add(task.getModifiedAt());
            args.add(task.getModifiedBy());
            args.add(UUID.fromString(task.getId()));

            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".task SET " + StringUtils.join(sets, ", ") + " WHERE id=?", args.toArray());
        }
    }
    public void deleteTasksByQueryId(String queryId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".task WHERE query_id=?", UUID.fromString(queryId));
    }

    private Task taskMap(ResultSet rs) throws LottabyteException, SQLException {
        Task task = new Task();

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setName(rs.getString("name"));
        taskEntity.setDescription(rs.getString("description"));
        taskEntity.setShortDescription(rs.getString("short_description"));
        taskEntity.setQueryId(rs.getString("query_id"));
        taskEntity.setSystemConnectionId(rs.getString("system_connection_id"));
        taskEntity.setIsMetadataTask(rs.getBoolean("is_metadata_task"));

        Metadata md = new Metadata();
        md.setId(rs.getString("id"));
        md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
        md.setModifiedBy(rs.getString("modifier"));
        md.setName(rs.getString("name"));
        md.setArtifactType(taskEntity.getArtifactType().toString());

        try {
            task = new Task(taskEntity, md);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return task;
    }

    private FlatTask flatTaskMap(ResultSet rs) throws LottabyteException, SQLException {
        FlatTask flatTask = new FlatTask(taskMap(rs));

        flatTask.setQueryName(rs.getString("query_name"));
        flatTask.setSystemConnectionName(rs.getString("system_connection_name"));
        flatTask.setTaskState(rs.getString("task_state"));
        flatTask.setLastUpdated(rs.getTimestamp("last_updated") == null? null : rs.getTimestamp("last_updated").toLocalDateTime());

        return flatTask;
    }

    public SearchResponse<FlatTask> searchTaskRuns(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails ud) {
        String orderby = "name";
        if (!StringUtils.isEmpty(searchRequest.getSort()))
            orderby = searchRequest.getSort().replaceAll("[\\-\\+]", "") + ((searchRequest.getSort().contains("-")) ? " DESC" : " ASC");
        Map<String, List<Object>> wheresMap = ServiceUtils.buildWhereForSearchRequestWithJoin(searchRequest, searchableColumns);
        String where = "";
        List<Object> vals = new ArrayList<>();
        for (String key : wheresMap.keySet()) {
            where = key;
            vals = wheresMap.get(key);
        }
        String join = "";
        if (!searchRequest.getFiltersForJoin().isEmpty()) {
            join = "left join da_" + ud.getTenant() + "." + searchRequest.getFiltersForJoin().get(0).getTable() + " "
                    + "tbl2 on tbl1." + searchRequest.getFiltersForJoin().get(0).getOnColumn() + "=tbl2." + searchRequest.getFiltersForJoin().get(0).getEqualColumn() +" ";
            if (where.isEmpty()) {
                where = " WHERE tbl2." + searchRequest.getFiltersForJoin().get(0).getColumn() + " = " + searchRequest.getFiltersForJoin().get(0).getValue();
            } else {
                where = where + " AND tbl2." + searchRequest.getFiltersForJoin().get(0).getColumn() + " = " + searchRequest.getFiltersForJoin().get(0).getValue();
            }
        }
        List<FlatTask> items = new ArrayList<>();

        String subQuery = "SELECT task.*, true as has_access from da_" + ud.getTenant() + ".task ";
        if (ud.getStewardId() != null) {
            String hasAccessJoinQuery = QueryHelper.getWhereIdInQuery(ArtifactType.task, ud);
            subQuery = "SELECT task.*, case when acc.id is null then false else true end as has_access FROM da_" + ud.getTenant() + ".task "
                    + " left join (select task.id from da_" + ud.getTenant() + ".task " + hasAccessJoinQuery + ") acc on task.id = acc.id ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward())
                subQuery = "SELECT task.*, true as has_access FROM da_" + ud.getTenant() + ".task "
                        + hasAccessJoinQuery;
        }
        String subQuery0 = "SELECT t.*, sc.name as system_connection_name, t.name as query_name, tr1.last_updated, tr2.task_state FROM (" + subQuery + ") as t "
                + " LEFT JOIN da_" + ud.getTenant() + ".system_connection sc ON t.system_connection_id=sc.id "
                + ((ud.getUserDomains() != null && !ud.getUserDomains().isEmpty()) ?
                " JOIN da_" + ud.getTenant() + ".entity_query q ON t.query_id = q.id AND q.system_id IN (SELECT system_id FROM da_" + ud.getTenant() + ".system_to_domain WHERE domain_id IN ('" + StringUtils.join(ud.getUserDomains(), "','") + "')) "
                : " LEFT JOIN da_" + ud.getTenant() + ".entity_query q ON t.query_id=q.id ")

                + " LEFT JOIN ( SELECT task_id, MAX(last_updated) AS last_updated FROM da_" + ud.getTenant() + ".task_run GROUP BY task_id) tr1 ON t.id=tr1.task_id "
                + " LEFT JOIN da_" + ud.getTenant() + ".task_run tr2 on tr1.task_id = tr2.task_id AND tr1.last_updated = tr2.last_updated ";

        String queryForItems = "SELECT * FROM (" + subQuery0 + ") as tbl1 "
                + join + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        log.info("query: " + queryForItems);

        jdbcTemplate.query(
                queryForItems,
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        FlatTask flatTask = null;
                        try {
                            flatTask = flatTaskMap(rs);
                            if (flatTask.getTaskState() != null) {
                                Message m = null;
                                try { m = Message.valueOf("TASK_STATUS_" + flatTask.getTaskState().toUpperCase()); } catch (Exception e) {}
                                if (m != null)
                                    flatTask.setTaskState(m.getText(ud.getLanguage().name()));
                            }
                        } catch (LottabyteException e) {
                            log.error(e.getMessage());
                        }
                        items.add(flatTask);
                    }
                },
                vals.toArray()
        );

        String queryForTotal = "SELECT COUNT(tbl1.id) FROM (" + subQuery0 + ") tbl1 " + join + where;
        Long total = jdbcTemplate.queryForObject(queryForTotal, Long.class, vals.toArray());

        SearchResponse<FlatTask> res = new SearchResponse<>();
        res.setCount(total.intValue());
        res.setLimit(searchRequest.getLimit());
        res.setOffset(searchRequest.getOffset());

        int num = searchRequest.getOffset() + 1;
        for (FlatTask fs : items)
            fs.setNum(num++);

        res.setItems(items);

        return res;
    }

    public List<String> getDomainIdsByTaskId(String taskId, UserDetails userDetails) {
        return jdbcTemplate.queryForList("SELECT domain_id FROM da_" + userDetails.getTenant() + ".system_to_domain s2d JOIN da_" + userDetails.getTenant()
                + ".entity_query eq ON s2d.system_id=eq.system_id JOIN da_" + userDetails.getTenant() + ".task t ON eq.id=t.query_id WHERE t.id=?",
                String.class, UUID.fromString(taskId));
    }

    public List<Relation> getTaskMetaDatabases(String id, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT md.id, md.name FROM da_" + userDetails.getTenant() + ".reference r JOIN da_" + userDetails.getTenant() +
                        ".meta_database md ON r.source_id=md.id AND md.state='PUBLISHED' WHERE r.target_id=? AND r.reference_type='META_DATABASE_TO_TASK'",
                new RowMapper<Relation>() {
                    @Override
                    public Relation mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new Relation(rs.getString("id"), rs.getString("name"));
                    }
                },
                UUID.fromString(id)
        );
    }
}
