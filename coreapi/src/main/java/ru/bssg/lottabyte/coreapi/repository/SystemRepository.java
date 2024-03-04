package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.dal.FlatItemRowMapper;
import ru.bssg.lottabyte.core.dal.StringRowMapper;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.relation.ParentRelation;
import ru.bssg.lottabyte.core.model.system.*;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.service.DomainService;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class SystemRepository extends WorkflowableRepository<System> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = {"system_type","connector_id","system_folder_id"};

    public SystemRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.system.name(), extFields);
        super.setMapper(new SystemRepository.SystemRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    class SystemRowMapper implements RowMapper<System> {
        @Override
        public System mapRow(ResultSet rs, int rowNum) throws SQLException {
            SystemEntity systemEntity = new SystemEntity();
            systemEntity.setSystemFolderId(rs.getString("system_folder_id"));
            systemEntity.setConnectorId(rs.getString("connector_id"));
            systemEntity.setName(rs.getString("name"));
            systemEntity.setDescription(rs.getString("description"));
            systemEntity.setSystemType(rs.getString("system_type"));

            return new System(systemEntity, new WorkflowableMetadata(rs, systemEntity.getArtifactType()));
        }
    }

    class SystemFolderRowMapper implements RowMapper<SystemFolder> {
        @Override
        public SystemFolder mapRow(ResultSet rs, int rowNum) throws SQLException {
            Metadata metadata = new Metadata();
            metadata.setId(rs.getString("id"));
            metadata.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            metadata.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            metadata.setCreatedBy(rs.getString("creator"));
            metadata.setModifiedBy(rs.getString("modifier"));

            SystemFolderEntity systemFolderEntity = new SystemFolderEntity();
            systemFolderEntity.setParentId(rs.getString("parent_id"));
            systemFolderEntity.setName(rs.getString("name"));
            systemFolderEntity.setDescription(rs.getString("description"));
            systemFolderEntity.setArtifactType(ArtifactType.system_folder);
            return new SystemFolder(systemFolderEntity, metadata);
        }
    }
    class ParentRelationRowMapper implements RowMapper<ParentRelation> {
        @Override
        public ParentRelation mapRow(ResultSet rs, int rowNum) throws SQLException {
            ParentRelation parentRelation = new ParentRelation();
            parentRelation.setId(rs.getString("id"));
            parentRelation.setName(rs.getString("name"));
            parentRelation.setArtifactType(ArtifactType.system_folder.getText());
            parentRelation.setUrl(".../v1/systems/folders/" + parentRelation.getId() + "?include_children=true");
            parentRelation.setHasChildren(rs.getBoolean("has_children"));
            return parentRelation;
        }
    }

    private static class FlatSystemRowMapper extends FlatItemRowMapper<FlatSystem> {

        public FlatSystemRowMapper() { super(FlatSystem::new); }

        @Override
        public FlatSystem mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatSystem fs = super.mapRow(rs, rowNum);
            fs.setState(ArtifactState.valueOf(rs.getString("state")));
            fs.setWorkflowTaskId(rs.getString("workflow_task_id"));
            return fs;
        }
    }

    class SystemTypeRowMapper implements RowMapper<SystemType> {
        @Override
        public SystemType mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SystemType(rs.getString("id"), rs.getString("name"),
                    rs.getString("description"));
        }
    }

    public Boolean hasAccessToSystem(String systemId, UserDetails userDetails) {
        String query = "SELECT EXISTS(SELECT system.ID FROM da_" + userDetails.getTenant() + ".system " +
                QueryHelper.getJoinQuery(ArtifactType.system, userDetails) + " and system.id = ?) as exists";
        return userDetails.getStewardId() == null ? true :
            jdbcTemplate.queryForObject(query, Boolean.class, UUID.fromString(systemId));
    }

    public void removeSystemFromAllDomains(String systemId, UserDetails userDetails) {
        String query = "DELETE FROM da_" + userDetails.getTenant() + ".system_to_domain " +
                "WHERE system_id = ?";
        jdbcTemplate.update(query, UUID.fromString(systemId));
    }

    public Boolean existsSystemInFolder(String systemName, String folderId, String currentId, UserDetails userDetails) {
        Boolean res = null;
        List<Object> params = new ArrayList<>();
        String query = "SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant() + ".system WHERE name = ? AND state='PUBLISHED' AND ";
        params.add(systemName);
        if (folderId == null) {
            query += " system_folder_id is null ";
        } else {
            query += " system_folder_id = ? ";
            params.add(UUID.fromString(folderId));
        }
        if (currentId != null) {
            query += " and id <> ? ";
            params.add(UUID.fromString(currentId));
        }
        query += ") as exists";
        res = jdbcTemplate.queryForObject(query, Boolean.class, params.toArray());
        return res;
    }

    public boolean existsSubfolder(String name, String parentId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(select id from da_" + userDetails.getTenant() + ".system_folder where name = ? and parent_id = ?) as exists",
                Boolean.class, name, UUID.fromString(parentId));
    }

    public SystemFolder getSystemFolderById(String systemFolderId, Boolean includeChildren, UserDetails userDetails) {
        String query = "SELECT *, EXISTS(SELECT id FROM da_" + userDetails.getTenant() + ".system_folder sf2 where sf2.parent_id = sf1.id) as has_children  " +
                "FROM da_" + userDetails.getTenant() + ".system_folder sf1 " +
                "where id = ? ";
        if(includeChildren)
            query = query + "AND EXISTS(SELECT id FROM da_" + userDetails.getTenant() + ".system_folder sf2 where sf2.parent_id = sf1.id) = true;";

        List<SystemFolder> systemFolderList = jdbcTemplate.query(query, new SystemFolderRowMapper(), UUID.fromString(systemFolderId));

        if(includeChildren) {
            for(SystemFolder systemFolder : systemFolderList){
                List<ParentRelation> children = getChildrenFolderListByParentId(systemFolder.getMetadata().getId(), userDetails);
                if(!children.isEmpty()){
                    systemFolder.getEntity().setChildren(children);
                }
            }
        }

        return systemFolderList.stream().findFirst().orElse(null);
    }

    public List<SystemFolder> getSystemFolderWithAllChildrenById(String systemFolderId, UserDetails userDetails) {
        String query = "with recursive cte as ( \n" +
                    "select id, \"name\", description, parent_id, created, creator, modified, modifier, 1 as level from da_" + userDetails.getTenant() + ".system_folder sf where id = ?\n" +
                    "union all select sf_result.id, sf_result.\"name\", sf_result.description, sf_result.parent_id, sf_result.created, sf_result.creator, sf_result.modified, sf_result.modifier, level + 1 from cte c \n" +
                    "join da_" + userDetails.getTenant() + ".system_folder sf_result on sf_result.parent_id = c.id \n" +
                ") \n" +
                "select * from cte order by level desc";

        return jdbcTemplate.query(query, new SystemFolderRowMapper(), UUID.fromString(systemFolderId));
    }

    public SystemFolder getSystemFolderByName(String systemFolderName, Boolean includeChildren, UserDetails userDetails) {
        String query = "SELECT id, \"name\", description, parent_id, created, creator, modified, modifier, EXISTS(SELECT id FROM da_" + userDetails.getTenant() + ".system_folder sf2 where sf2.parent_id = sf1.id) as has_children  " +
                "FROM da_" + userDetails.getTenant() + ".system_folder sf1 " +
                "where name = ? ";
        List<SystemFolder> systemFolderList = jdbcTemplate.query(query, new SystemFolderRowMapper(), systemFolderName);

        if(includeChildren) {
            for(SystemFolder systemFolder : systemFolderList){
                List<ParentRelation> children = getChildrenFolderListByParentId(systemFolder.getMetadata().getId(), userDetails);
                if(!children.isEmpty()){
                    systemFolder.getEntity().setChildren(children);
                }
            }
        }

        return systemFolderList.stream().findFirst().orElse(null);
    }

    public List<ParentRelation> getChildrenFolderListByParentId(String systemFolderId, UserDetails userDetails) {
        String query = "SELECT id, \"name\", description, parent_id, created, creator, modified, modifier, EXISTS(SELECT id FROM da_" + userDetails.getTenant() + ".system_folder sf2 where sf2.parent_id = sf1.id) as has_children  " +
                "FROM da_" + userDetails.getTenant() + ".system_folder sf1 " +
                "where parent_id = ?;";
        return jdbcTemplate.query(query, new ParentRelationRowMapper(), UUID.fromString(systemFolderId));
    }

    public List<SystemFolder> getRootFolders(Boolean includeChildren, UserDetails userDetails) {
        List<SystemFolder> systemFolderList = new ArrayList<>();

        String query = "SELECT id, \"name\", description, parent_id, created, creator, modified, modifier, EXISTS(SELECT id FROM da_" + userDetails.getTenant() + ".system_folder sf2 where sf2.parent_id = sf1.id) as has_children " +
                "FROM da_" + userDetails.getTenant() + ".system_folder sf1 \n" +
                "where parent_id isnull ";
        jdbcTemplate.query(
                query,
                rs -> {
                    SystemFolder systemFolder = getSystemFolderById(rs.getString("id"), includeChildren, userDetails);
                    if(systemFolder != null && systemFolder.getId() != null)
                        systemFolderList.add(systemFolder);
                }
        );
        if(systemFolderList.size() > 1)
            systemFolderList.sort(Comparator.comparing(object -> object.getEntity().getName()));

        return systemFolderList;
    }

    public Boolean existSystemTypeByName(String systemTypeName, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant() + ".system_type WHERE name = ?) as exists",
                Boolean.class, systemTypeName);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public String createSystem(UpdatableSystemEntity newSystemEntity, String workflowTaskId, UserDetails userDetails) {
        UUID newId = newSystemEntity.getId() != null ? UUID.fromString(newSystemEntity.getId()) : UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        String query = "INSERT INTO da_" + userDetails.getTenant() + ".\"system\" " +
                "(id, \"name\", description, system_type, connector_id, system_folder_id, state, workflow_task_id, created, creator, modified, modifier) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(query, newId, newSystemEntity.getName(), newSystemEntity.getDescription(), newSystemEntity.getSystemType(),
                newSystemEntity.getConnectorId() != null ? UUID.fromString(newSystemEntity.getConnectorId()) : null,
                newSystemEntity.getSystemFolderId() != null ? UUID.fromString(newSystemEntity.getSystemFolderId()) : null,
                ArtifactState.DRAFT.toString(),
                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                ts, userDetails.getUid(), ts, userDetails.getUid());
        if (newSystemEntity.getDomainIds() != null && !newSystemEntity.getDomainIds().isEmpty()) {
            for (String s : newSystemEntity.getDomainIds()) {
                addSystemToDomain(newId.toString(), s, userDetails);
            }
        }
        return newId.toString();
    }

    public void removeSystemFromDomain(String systemId, String domainId, UserDetails userDetails) {
        String query = "DELETE FROM da_" + userDetails.getTenant() + ".system_to_domain " +
                "WHERE domain_id = ? and system_id = ?";
        jdbcTemplate.update(query, UUID.fromString(domainId), UUID.fromString(systemId));
    }

    public String addSystemToDomain(String systemId, String domainId, UserDetails userDetails) {
        UUID newId = java.util.UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        String query = "INSERT INTO da_" + userDetails.getTenant() + ".system_to_domain " +
                "(id, domain_id, system_id, description, created, creator, modified, modifier) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(query, newId, UUID.fromString(domainId), UUID.fromString(systemId), null,
                ts, userDetails.getUid(), ts, userDetails.getUid());
        return newId.toString();
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void recursiveDeletionFolders(List<SystemFolder> systemFolderList, UserDetails userDetails) {
        String query = "DELETE FROM da_" + userDetails.getTenant() + ".system_folder\n" +
                "WHERE id=?;";
        for(SystemFolder systemFolder : systemFolderList){
            jdbcTemplate.update(query, UUID.fromString(systemFolder.getId()));
        }
    }

    public String createFolder(UpdatableSystemFolderEntity newSystemFolderEntity, UserDetails userDetails) {
        UUID uuidForFolder = java.util.UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".system_folder\n" +
                        "(id, \"name\", description, parent_id, created, creator, modified, modifier)\n" +
                        "VALUES(?, ?, ?, ?, ?, ?, ?, ?);",
                uuidForFolder, newSystemFolderEntity.getName(), newSystemFolderEntity.getDescription(), (newSystemFolderEntity.getParentId() == null || newSystemFolderEntity.getParentId().isEmpty())? null : UUID.fromString(newSystemFolderEntity.getParentId()),
                new Timestamp(new java.util.Date().getTime()), userDetails.getUid(), new Timestamp(new java.util.Date().getTime()), userDetails.getUid());

        return uuidForFolder.toString();
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void patchSystem(String systemId, UpdatableSystemEntity systemEntity, UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".\"system\" SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));
        if (systemEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(systemEntity.getName());
        }
        if (systemEntity.getDescription() != null) {
            sets.add("description = ?");
            params.add(systemEntity.getDescription());
        }
        if (systemEntity.getSystemType() != null) {
            sets.add("system_type = ?");
            params.add(systemEntity.getSystemType());
        }
        if (systemEntity.getConnectorId() != null) {
            sets.add("connector_id = ?");
            params.add(UUID.fromString(systemEntity.getConnectorId()));
        }
        if (systemEntity.getSystemFolderId() != null) {
            sets.add("system_folder_id = ?");
            params.add(UUID.fromString(systemEntity.getSystemFolderId()));
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(systemId));
            jdbcTemplate.update(query, params.toArray());
        }

        if (systemEntity.getDomainIds() != null && !systemEntity.getDomainIds().isEmpty()) {
            List<String> currentDomains = getDomainIdsBySystemId(systemId, userDetails);

            List<String> newDomains = systemEntity.getDomainIds().stream().filter(x -> !currentDomains.contains(x)).collect(Collectors.toList());
            List<String> removeDomains = currentDomains.stream().filter(x -> !systemEntity.getDomainIds().contains(x)).collect(Collectors.toList());
            if (!newDomains.isEmpty())
                newDomains.stream().forEach(x -> addSystemToDomain(systemId, x, userDetails));
            if (!removeDomains.isEmpty())
                removeDomains.stream().forEach(x -> removeSystemFromDomain(systemId, x, userDetails));
        }
    }

    public SearchResponse<FlatSystem> searchSystems(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {
        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "select system.*, true as has_access from da_" + userDetails.getTenant() + ".system ";
        if (userDetails.getStewardId() != null) {
            String hasAccessJoinQuery = QueryHelper.getJoinQuery(ArtifactType.system, userDetails);
            subQuery = "select s.*, case when acc.id is null then false else true end as has_access from da_" + userDetails.getTenant() + ".system s "
                    + " left join (select system.id from da_" + userDetails.getTenant() + ".system " + hasAccessJoinQuery + ") acc on s.id = acc.id ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward()) {
                subQuery = "select system.*, true as has_access from da_" + userDetails.getTenant() + ".system "
                        + hasAccessJoinQuery;
            }
        }

        subQuery = "SELECT sq.*, wft.workflow_state FROM (" + subQuery + ") as sq left join da_" + userDetails.getTenant() + ".workflow_task wft "
                + " on sq.workflow_task_id = wft.id ";
        subQuery = "SELECT distinct sq.*, s.domains, t.tags FROM (" + subQuery + ") sq "
                + ((userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) ? (" join ("
                + "select s2d.system_id as system_id, string_agg(d.name, ',') as domains from da_" + userDetails.getTenant() + ".domain d "
                + "join da_" + userDetails.getTenant() + ".system_to_domain s2d on s2d.domain_id = d.id and d.id in ('" + StringUtils.join(userDetails.getUserDomains(), "','") + "') group by s2d.system_id"
                + ") s on s.system_id = sq.id ") : ("left join ("
                + "select s2d.system_id as system_id, string_agg(d.name, ',') as domains from da_" + userDetails.getTenant() + ".domain d "
                + "join da_" + userDetails.getTenant() + ".system_to_domain s2d on s2d.domain_id = d.id group by s2d.system_id"
                + ") s on s.system_id = sq.id "))
                + "left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_" + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id ";

        String queryForItems = "SELECT * FROM (" + subQuery + ") as tbl1 " + join
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatSystem> flatItems = jdbcTemplate.query(queryForItems,
                new FlatSystemRowMapper(), whereValues.toArray());

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1 " + join + where, Integer.class, whereValues.toArray());

        SearchResponse<FlatSystem> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public void patchFolder(String folderId, UpdatableSystemFolderEntity systemFolderEntity, UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".system_folder SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));
        if(systemFolderEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(systemFolderEntity.getName());
        }
        if(systemFolderEntity.getDescription() != null) {
            sets.add("description = ?");
            params.add(systemFolderEntity.getDescription());
        }
        if(systemFolderEntity.getParentId() != null) {
            sets.add("parent_id = ?");
            params.add(UUID.fromString(systemFolderEntity.getParentId()));
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(folderId));
            jdbcTemplate.update(query, params.toArray());
        }
    }

    public List<String> getDomainIdsBySystemId(String systemId, UserDetails userDetails) {
        return jdbcTemplate.queryForList("SELECT domain_id FROM da_" + userDetails.getTenant()
                + ".system_to_domain sd JOIN da_" + userDetails.getTenant() + ".domain d ON sd.domain_id = d.id WHERE sd.system_id=? and d.state = ?",
                String.class, UUID.fromString(systemId), ArtifactState.PUBLISHED.name());
    }

    @Override
    public System getById(String systemId, UserDetails userDetails) {
        String query = "SELECT * FROM da_" + userDetails.getTenant() + ".\"system\" where id = ?";
        List<System> res = jdbcTemplate.query(query, new SystemRowMapper(), UUID.fromString(systemId));
        if (res.isEmpty())
            return null;
        res.get(0).getEntity().setDomainIds(getDomainIdsBySystemId(systemId, userDetails));
        return res.get(0);
    }

    @Override
    public System getByIdAndState(String id, String state, UserDetails userDetails) {
        String query = "SELECT * FROM da_" + userDetails.getTenant() + ".\"system\" where id = ? and state = ? ";
        List<System> res = jdbcTemplate.query(query, new SystemRowMapper(), UUID.fromString(id), state);
        if (res.isEmpty())
            return null;
        res.get(0).getEntity().setDomainIds(getDomainIdsBySystemId(id, userDetails));
        return res.get(0);
    }

    public System getSystemBySystemFolderId(String systemFolderId, UserDetails userDetails) {
        String query = "SELECT * FROM da_" + userDetails.getTenant() + ".system where system_folder_id = ?";
        List<System> res = jdbcTemplate.query(query, new SystemRowMapper(), UUID.fromString(systemFolderId));
        if (res.isEmpty())
            return null;
        return res.get(0);
    }

    @Override
    public PaginatedArtifactList<System> getAllPaginated(Integer offset, Integer limit, String url, ArtifactState artifactState, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".system "
                + " WHERE state = ?", Integer.class, artifactState.toString());
        String query = "SELECT * FROM da_" + userDetails.getTenant() + ".system where state = ? offset ? limit ? ";
        List<System> systemList = jdbcTemplate.query(query, new SystemRowMapper(), artifactState.toString(), offset, limit);
        if (!systemList.isEmpty()) {
            systemList.forEach(x -> x.getEntity().setDomainIds(getDomainIdsBySystemId(x.getId(), userDetails)));
        }
        return new PaginatedArtifactList<>(systemList, offset, limit, total, url);
    }

    public PaginatedArtifactList<System> getPaginatedSystemsWithoutDomain(String domainId, Integer offset, Integer limit, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(distinct s.id) FROM da_" + userDetails.getTenant() + ".system s " +
                "join da_" + userDetails.getTenant() + ".system_to_domain std on std.system_id = s.id  " +
                "where s.state = ? " +
                "and std.domain_id != ?", Integer.class, ArtifactState.PUBLISHED.toString(), UUID.fromString(domainId));
        String query = "SELECT * FROM da_" + userDetails.getTenant() + ".system s " +
                "where s.id IN (SELECT distinct s.id FROM da_" + userDetails.getTenant() + ".system s " +
                "join da_" + userDetails.getTenant() + ".system_to_domain std on std.system_id = s.id  " +
                "where state = ? " +
                "and std.domain_id != ?) offset ? limit ? ";
        List<System> systemList = jdbcTemplate.query(query, new SystemRowMapper(), ArtifactState.PUBLISHED.toString(), UUID.fromString(domainId), offset, limit);
        if (!systemList.isEmpty()) {
            systemList.forEach(x -> x.getEntity().setDomainIds(getDomainIdsBySystemId(x.getId(), userDetails)));
        }
        return new PaginatedArtifactList<>(systemList, offset, limit, total, "/v1/systems/unliked_to_domain/" + domainId);
    }

    public List<System> getSystemsByEntityId(String entityId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT s.* FROM da_" + userDetails.getTenant() + ".system s "
            + " join da_" + userDetails.getTenant() + ".entity_to_system e2s on e2s.system_id = s.id "
            + " where e2s.entity_id = ?", new SystemRowMapper(), UUID.fromString(entityId));
    }

    public Boolean allSystemsExist(List<String> systemIds, UserDetails userDetails) {
        if (systemIds == null || systemIds.isEmpty())
            return true;
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".system WHERE state = ? and id IN ('"
            + StringUtils.join(systemIds, "','") + "')", Integer.class, ArtifactState.PUBLISHED.toString());
        return c != null && systemIds.size() == c;
    }

    public List<SystemType> getSystemTypes(UserDetails userDetails) {
        return jdbcTemplate.query("SELECT id, name, description FROM da_" + userDetails.getTenant() +
                ".system_type", new SystemTypeRowMapper());
    }
    public List<SystemType> getSystemTypesFromDaSchema() {
        return jdbcTemplate.query("SELECT id, name, description FROM da.system_type", new SystemTypeRowMapper());
    }

    public String createSystemType(SystemType systemType, UserDetails userDetails) {
        UUID uuidForSystemType = java.util.UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".system_type\n" +
                        "(id, \"name\", description, created, creator, modified, modifier)\n" +
                        "VALUES(?, ?, ?, ?, ?, ?, ?);",
                systemType.getId(), systemType.getName(), systemType.getId(),
                new Timestamp(new java.util.Date().getTime()), userDetails.getUid(), new Timestamp(new java.util.Date().getTime()), userDetails.getUid());

        return uuidForSystemType.toString();
    }

    public String publishSystemDraft(String draftSystemId, String publishedSystemId, UserDetails userDetails) {
        String res = null;
        if (publishedSystemId != null) {
            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".system d SET name = draft.name, description = draft.description, "
                            + " system_type = draft.system_type, connector_id = draft.connector_id, system_folder_id = draft.system_folder_id, "
                            + " ancestor_draft_id = draft.id, modified = draft.modified, modifier = draft.modifier "
                            + " from (select id, name, description, system_type, connector_id, system_folder_id, modified, modifier FROM da_" + userDetails.getTenant() + ".system) as draft where d.id = ? and draft.id = ?",
                    UUID.fromString(publishedSystemId), UUID.fromString(draftSystemId));
            res = publishedSystemId;
        } else {
            UUID newId = UUID.randomUUID();
            jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".system (id, name, description, system_type, connector_id, system_folder_id, state, workflow_task_id, "
                            + "published_id, published_version_id, ancestor_draft_id, created, creator, modified, modifier) "
                            + "SELECT ?, name, description, system_type, connector_id, system_folder_id, ?, ?, ?, ?, ?, created, creator, modified, modifier "
                            + "FROM da_" + userDetails.getTenant() + ".system where id = ?",
                    newId, ArtifactState.PUBLISHED.toString(), null, null, null,
                    UUID.fromString(draftSystemId), UUID.fromString(draftSystemId));
            res = newId.toString();
        }
        jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".system set state = ? where id = ?",
                ArtifactState.DRAFT_HISTORY.toString(), UUID.fromString(draftSystemId));
        return res;
    }

    public String createSystemDraft(String publishedSystemId, String draftId, String workflowTaskId, UserDetails userDetails) {
        UUID newId = draftId != null ? UUID.fromString(draftId) : UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".system (id, name, description, system_type, connector_id, system_folder_id, state, workflow_task_id, published_id, published_version_id, created, creator, modified, modifier) " +
                        "SELECT ?, name, description, system_type, connector_id, system_folder_id, ?, ?, id, version_id, created, creator, modified, modifier FROM da_" + userDetails.getTenant() + ".system where id = ?",
                newId, ArtifactState.DRAFT.toString(),
                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                UUID.fromString(publishedSystemId));
        return newId.toString();
    }

}
