package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.dal.FlatItemRowMapper;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.dataasset.DataAsset;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQueryEntity;
import ru.bssg.lottabyte.core.model.entityQuery.FlatEntityQuery;
import ru.bssg.lottabyte.core.model.entityQuery.UpdatableEntityQueryEntity;
import ru.bssg.lottabyte.core.model.entitySample.EntitySample;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleType;
import ru.bssg.lottabyte.core.model.indicator.Indicator;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.util.Constants;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class EntityQueryRepository extends WorkflowableRepository<EntityQuery> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = {"query_text","entity_id","system_id"};

    public EntityQueryRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.entity_query.name(), extFields);
        super.setMapper(new EntityQueryRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class EntityQueryRowMapper implements RowMapper<EntityQuery> {
        @Override
        public EntityQuery mapRow(ResultSet rs, int rowNum) throws SQLException {
            EntityQueryEntity entityQueryEntity = new EntityQueryEntity();
            entityQueryEntity.setEntityId(rs.getString("entity_id"));
            entityQueryEntity.setName(rs.getString("name"));
            entityQueryEntity.setDescription(rs.getString("description"));
            entityQueryEntity.setQueryText(rs.getString("query_text"));
            entityQueryEntity.setSystemId(rs.getString("system_id"));
            entityQueryEntity.setArtifactType(ArtifactType.entity_query);
            return new EntityQuery(entityQueryEntity, new WorkflowableMetadata(rs, entityQueryEntity.getArtifactType()));
        }
    }

    private static class FlatEntityQueryRowMapper extends FlatItemRowMapper<FlatEntityQuery> {

        public FlatEntityQueryRowMapper() { super(FlatEntityQuery::new); }

        @Override
        public FlatEntityQuery mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatEntityQuery feq = super.mapRow(rs, rowNum);
            feq.setEntityId(rs.getString("entity_id"));
            feq.setEntityName(rs.getString("entity_name"));
            feq.setSystemId(rs.getString("system_id"));
            feq.setSystemName(rs.getString("system_name"));
            feq.setState(ArtifactState.valueOf(rs.getString("state")));
            feq.setWorkflowTaskId(rs.getString("workflow_task_id"));
            return feq;
        }
    }

    public boolean existQueriesInSystem(String systemId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(select id from da_" + userDetails.getTenant() + ".entity_query where system_id = ? and state = ?) as exists",
                Boolean.class, UUID.fromString(systemId), ArtifactState.PUBLISHED.toString());
    }

    public EntityQuery getEntityQueryByEntityIdAndSystemId(String entityId, String systemId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".entity_query  " +
                        "where entity_id = ? AND system_id = ?;",
                new EntityQueryRowMapper(), UUID.fromString(entityId), UUID.fromString(systemId))
                .stream().findFirst().orElse(null);
    }

    public List<EntityQuery> getEntityQueryListByEntityId(String entityId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".entity_query where entity_id = ? and state = ?",
                new EntityQueryRowMapper(), UUID.fromString(entityId), ArtifactState.PUBLISHED.toString());
    }

    public Boolean hasAccessToQuery(String queryId, UserDetails userDetails) {
        return userDetails.getStewardId() == null ? true :
            jdbcTemplate.queryForObject("SELECT EXISTS(SELECT entity_query.ID FROM da_" + userDetails.getTenant() + ".entity_query " +
                QueryHelper.getWhereIdInQuery(ArtifactType.entity_query, userDetails) + " and entity_query.id = ?) as exists", Boolean.class,
                UUID.fromString(queryId));
    }

    public PaginatedArtifactList<EntityQuery> getAllQueryEntitiesPaginated(String entityId, Integer offset, Integer limit, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".entity_query", Integer.class);
        String query = "SELECT * FROM da_" + userDetails.getTenant() + ".entity_query WHERE entity_id = ? offset ? limit ?";
        List<EntityQuery> entityQueryList = jdbcTemplate.query(query, new EntityQueryRowMapper(), UUID.fromString(entityId), offset, limit);

        return new PaginatedArtifactList<>(
                entityQueryList, offset, limit, total, "/v1/queries/entity/" + entityId);
    }

    public String createQuery(UpdatableEntityQueryEntity newEntityQueryEntity, String workflowTaskId, UserDetails userDetails) {
        UUID uuidForEntityQuery = newEntityQueryEntity.getId() != null ? UUID.fromString(newEntityQueryEntity.getId()) : UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".\"entity_query\"\n" +
                        "(id, \"name\", description, query_text, entity_id, system_id, state, workflow_task_id, created, creator, modified, modifier)\n" +
                        "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                uuidForEntityQuery, newEntityQueryEntity.getName(), newEntityQueryEntity.getDescription(), newEntityQueryEntity.getQueryText(), UUID.fromString(newEntityQueryEntity.getEntityId()),
                UUID.fromString(newEntityQueryEntity.getSystemId()),
                ArtifactState.DRAFT.toString(),
                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                ts, userDetails.getUid(), ts, userDetails.getUid());
        return uuidForEntityQuery.toString();
    }

    public void patchQuery(String entityQueryId, UpdatableEntityQueryEntity newEntityQueryEntity, UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".\"entity_query\" SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));
        if(newEntityQueryEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(newEntityQueryEntity.getName());
        }
        if(newEntityQueryEntity.getDescription() != null) {
            sets.add("description = ?");
            params.add(newEntityQueryEntity.getDescription());
        }
        if(newEntityQueryEntity.getQueryText() != null) {
            sets.add("query_text = ?");
            params.add(newEntityQueryEntity.getQueryText());
        }
        if(newEntityQueryEntity.getEntityId() != null) {
            sets.add("entity_id = ?");
            params.add(UUID.fromString(newEntityQueryEntity.getEntityId()));
        }
        if(newEntityQueryEntity.getSystemId() != null) {
            sets.add("system_id = ?");
            params.add(UUID.fromString(newEntityQueryEntity.getSystemId()));
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(entityQueryId));
            jdbcTemplate.update(query, params.toArray());
        }
    }

    public SearchResponse<FlatEntityQuery> searchEntityQuery(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "SELECT entity_query.*, true as has_access FROM da_" + userDetails.getTenant() + ".entity_query ";
        if (userDetails.getStewardId() != null) {
            String hasAccessJoinQuery = QueryHelper.getWhereIdInQuery(ArtifactType.entity_query, userDetails);
            subQuery = "SELECT entity_query.*, case when acc.id is null then false else true end as has_access FROM da_" + userDetails.getTenant() + ".entity_query "
                    + " left join (select entity_query.id from da_" + userDetails.getTenant() + ".entity_query " + hasAccessJoinQuery + ") acc on entity_query.id = acc.id ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward())
                subQuery = "SELECT entity_query.*, true as has_access FROM da_" + userDetails.getTenant() + ".entity_query "
                        + hasAccessJoinQuery;
        }
        subQuery = "SELECT sq.*, wft.workflow_state, t.tags FROM (" + subQuery + ") as sq left join da_" + userDetails.getTenant() + ".workflow_task wft "
                + " on sq.workflow_task_id = wft.id "
                + "left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_" + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant() + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id ";

        String queryForItems = "SELECT distinct tbl1.*,system.name as system_name, entity.name AS entity_name FROM (" + subQuery
                + ") tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                + ((userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) ?
                    " JOIN da_" + userDetails.getTenant() + ".system sys2 ON tbl1.system_id=sys2.id AND sys2.id IN (SELECT system_id FROM da_" + userDetails.getTenant() + ".system_to_domain WHERE domain_id IN ('" + StringUtils.join(userDetails.getUserDomains(), "','") + "'))"
                : "")
                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatEntityQuery> flatItems = jdbcTemplate.query(queryForItems, new FlatEntityQueryRowMapper(), whereValues.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                + ((userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) ?
                " JOIN da_" + userDetails.getTenant() + ".system sys2 ON tbl1.system_id=sys2.id AND sys2.id IN (SELECT system_id FROM da_" + userDetails.getTenant() + ".system_to_domain WHERE domain_id IN ('" + StringUtils.join(userDetails.getUserDomains(), "','") + "'))"
                : "")
                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                + where;
        Integer total = jdbcTemplate.queryForObject(queryForTotal, Integer.class, whereValues.toArray());

        SearchResponse<FlatEntityQuery> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public SearchResponse<FlatEntityQuery> searchEntityQueryByDomain(SearchRequestWithJoin searchRequest, String domainId, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        if (where.isEmpty()) {
            where = " where tbl1.system_id in (select tbl3.system_id from da_" + userDetails.getTenant() + ".system_to_domain tbl3 where tbl3.domain_id='" + domainId + "' LIMIT " + Constants.sqlInLimit +")";
        } else {
            where = where + " AND tbl1.system_id in (select tbl3.system_id from da_" + userDetails.getTenant() + ".system_to_domain tbl3 where tbl3.domain_id='" + domainId + "' LIMIT " + Constants.sqlInLimit +")";
        }

        String subQuery = "SELECT entity_query.*, true as has_access FROM da_" + userDetails.getTenant() + ".entity_query ";
        if (userDetails.getStewardId() != null) {
            String hasAccessJoinQuery = QueryHelper.getWhereIdInQuery(ArtifactType.entity_query, userDetails);
            subQuery = "SELECT entity_query.*, case when acc.id is null then false else true end as has_access FROM da_" + userDetails.getTenant() + ".entity_query "
                    + " left join (select entity_query.id from da_" + userDetails.getTenant() + ".entity_query " + hasAccessJoinQuery + ") acc on entity_query.id = acc.id ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward())
                subQuery = "SELECT entity_query.*, true as has_access FROM da_" + userDetails.getTenant() + ".entity_query "
                        + hasAccessJoinQuery;
        }
        subQuery = "SELECT sq.*, wft.workflow_state FROM (" + subQuery + ") as sq left join da_" + userDetails.getTenant() + ".workflow_task wft "
                + " on sq.workflow_task_id = wft.id ";

        String queryForItems = "SELECT distinct tbl1.*,system.name as system_name, entity.name AS entity_name FROM (" + subQuery
                + ") as tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatEntityQuery> flatItems = jdbcTemplate.query(queryForItems, new FlatEntityQueryRowMapper(), whereValues.toArray());


        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                + where;
        Integer total = jdbcTemplate.queryForObject(queryForTotal, Integer.class, whereValues.toArray());

        SearchResponse<FlatEntityQuery> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public List<String> getDomainIdsBySystemId(String systemId, UserDetails userDetails) {
        return jdbcTemplate.queryForList("SELECT domain_id FROM da_" + userDetails.getTenant()
                + ".system_to_domain sd WHERE sd.system_id=?", String.class, UUID.fromString(systemId));
    }
}
