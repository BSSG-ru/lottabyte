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
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.domain.DomainEntity;
import ru.bssg.lottabyte.core.model.domain.FlatDomain;
import ru.bssg.lottabyte.core.model.domain.UpdatableDomainEntity;
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
public class DomainRepository extends WorkflowableRepository<Domain> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = {};

    @Autowired
    public DomainRepository (JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.domain.name(), extFields);
        super.setMapper(new DomainRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    class DomainRowMapper implements RowMapper<Domain> {

        @Override
        public Domain mapRow(ResultSet rs, int rowNum) throws SQLException {
            DomainEntity domainEntity = new DomainEntity();
            domainEntity.setName(rs.getString("name"));
            domainEntity.setDescription(rs.getString("description"));

            return new Domain(domainEntity, new WorkflowableMetadata(rs, domainEntity.getArtifactType()));
        }
    }

    private static class FlatDomainRowMapper extends FlatItemRowMapper<FlatDomain> {

        public FlatDomainRowMapper() { super(FlatDomain::new); }

        @Override
        public FlatDomain mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatDomain fd = super.mapRow(rs, rowNum);
            fd.setState(ArtifactState.valueOf(rs.getString("state")));
            fd.setWorkflowTaskId(rs.getString("workflow_task_id"));
            return fd;
        }
    }

    public List<String> checkDomainsListExist(List<String> domainIds, UserDetails userDetails) {
        // Return empty list if all domains exist
        // Return inexistent domain ids in list
        List<String> res = new ArrayList<>();
        for (String s : domainIds) {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".domain WHERE id = ? AND State = ?) AS EXISTS",
                    Boolean.class, UUID.fromString(s), ArtifactState.PUBLISHED.name());
            if (!exists)
                res.add(s);
        }
        return res;
    }

    public boolean allDomainsExist(List<String> domainIds, UserDetails userDetails) {
        if (domainIds == null || domainIds.isEmpty())
            return true;

        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".domain WHERE id IN ('"
            + StringUtils.join(domainIds, "','") + "')", Integer.class);

        return c != null && c.equals(domainIds.size());
    }

    public Boolean existsSystemsInDomain(String domainId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT sd.ID FROM da_" + userDetails.getTenant() + ".system_to_domain sd " +
                "join da_" + userDetails.getTenant() + ".system s on sd.system_id = s.id " +
                "where s.state = ? and sd.domain_id = ? ) as exists",
                Boolean.class, ArtifactState.PUBLISHED.name(), UUID.fromString(domainId));
    }

    public boolean domainHasStewards(String domainId, UserDetails userDetails) {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".steward_to_domain WHERE domain_id=?",
                Integer.class, UUID.fromString(domainId));
        return c > 0;
    }

    public boolean domainNameExists(String name, String thisId, UserDetails userDetails) {
        Integer c;
        if (thisId == null)
            c = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".domain WHERE state='" + ArtifactState.PUBLISHED.name() + "' and name=?", Integer.class, name);
        else
            c = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".domain WHERE state='" + ArtifactState.PUBLISHED.name() + "' and name=? AND id<>?", Integer.class, name, UUID.fromString(thisId));
        return c > 0;
    }

    public boolean domainExists(String domainId, ArtifactState[] artifactStates, UserDetails userDetails) {
        String suffix = "";
        if (artifactStates != null)
            suffix = " and state in ("
                    + String.join(",", Arrays.asList(artifactStates).stream().map(x -> "'" + x + "'").collect(Collectors.toList()))
                    + ")";
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".domain WHERE id=? " + suffix,
                Integer.class, UUID.fromString(domainId));
        return c > 0;
    }

    @Override
    public Domain getById(String id, UserDetails userDetails) {
        Domain domain = super.getById(id, userDetails);
        if (domain != null) {
            domain.getEntity().setStewards(getStewardIdsByDomainId(id, userDetails));
            domain.getEntity().setSystemIds(getSystemIdsByDomainId(id, userDetails));
        }
        return domain;
    }

    public Domain getDomainByIdAndState(String domainId, UserDetails userDetails) {
        Domain domain = getByIdAndState(domainId, ArtifactState.PUBLISHED.name(), userDetails);

        if (domain != null) {
            domain.getEntity().setStewards(getStewardIdsByDomainId(domainId, userDetails));
            domain.getEntity().setSystemIds(getSystemIdsByDomainId(domainId, userDetails));
        }
        return domain;
    }

    @Override
    public Domain getVersionById(String domainId, Integer versionId, UserDetails userDetails) {
        Domain domain = super.getVersionById(domainId, versionId, userDetails);

        if (domain != null)
            domain.getEntity().setStewards(getStewardIdsByDomainId(domainId, userDetails));

        return domain;
    }

    public List<Domain> getDomainsBySystemId(String systemId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT d.* FROM da_" + userDetails.getTenant() + ".domain d "
                + " JOIN da_" + userDetails.getTenant() + ".system_to_domain s2d on s2d.domain_id = d.id "
                + " WHERE s2d.system_id = ? and d.state = ?", new DomainRowMapper(), UUID.fromString(systemId), ArtifactState.PUBLISHED.name());
    }

    public List<String> getStewardIdsByDomainId(String domainId, UserDetails userDetails) {
        return jdbcTemplate.queryForList("SELECT steward_id FROM da_" + userDetails.getTenant()
                + ".steward_to_domain sd WHERE sd.domain_id=?", String.class, UUID.fromString(domainId));
    }

    @Override
    public PaginatedArtifactList<Domain> getAllPaginated(Integer offset, Integer limit, String url, ArtifactState artifactState, UserDetails userDetails) {
        PaginatedArtifactList<Domain> res = super.getAllPaginated(offset, limit, url, artifactState, userDetails);

        for (Domain d : res.getResources()) {
            d.getEntity().setStewards(getStewardIdsByDomainId(d.getId(), userDetails));
            d.getEntity().setSystemIds(getSystemIdsByDomainId(d.getId(), userDetails));
        }

        return res;
    }

    @Override
    public PaginatedArtifactList<Domain> getVersionsById(String id, Integer offset, Integer limit, String url, UserDetails userDetails) {
        PaginatedArtifactList<Domain> res = super.getVersionsById(id, offset, limit, url, userDetails);

        for (Domain d : res.getResources()) {
            d.getEntity().setStewards(getStewardIdsByDomainId(d.getId(), userDetails));
        }

        return res;
    }

    public String createDomain(DomainEntity domain, String workflowTaskId, UserDetails userDetails) throws LottabyteException {
        UUID newId = domain.getId() != null ? UUID.fromString(domain.getId()) : UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".domain (id, name, description, state, workflow_task_id, created, creator, modified, modifier) VALUES (?,?,?,?,?,?,?,?,?)",
                newId, domain.getName(), domain.getDescription(),
                ArtifactState.DRAFT.toString(),
                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                ts, userDetails.getUid(), ts, userDetails.getUid());
        return newId.toString();
    }

    public String createDomainDraft(String publishedDomainId, String draftId, String workflowTaskId, UserDetails userDetails) {
        UUID newId = draftId != null ? UUID.fromString(draftId) : UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".domain (id, name, description, state, workflow_task_id, published_id, published_version_id, created, creator, modified, modifier) " +
                "SELECT ?, name, description, ?, ?, id, version_id, created, creator, modified, modifier FROM da_" + userDetails.getTenant() + ".domain where id = ?",
                newId, ArtifactState.DRAFT.toString(),
                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                UUID.fromString(publishedDomainId));
        return newId.toString();
    }

    public String publishDomainDraft(String draftDomainId, String publishedDomainId, UserDetails userDetails) {
        String res = null;
        if (publishedDomainId != null) {
            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".domain d SET name = draft.name, description = draft.description, "
                            + " ancestor_draft_id = draft.id, modified = draft.modified, modifier = draft.modifier "
                            + " from (select id, name, description, modified, modifier FROM da_" + userDetails.getTenant() + ".domain) as draft where d.id = ? and draft.id = ?",
                    UUID.fromString(publishedDomainId), UUID.fromString(draftDomainId));
            res = publishedDomainId;
        } else {
            UUID newId = UUID.randomUUID();
            jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".domain (id, name, description, state, workflow_task_id, "
                            + "published_id, published_version_id, ancestor_draft_id, created, creator, modified, modifier) "
                            + "SELECT ?, name, description, ?, ?, ?, ?, ?, created, creator, modified, modifier "
                            + "FROM da_" + userDetails.getTenant() + ".domain where id = ?",
                    newId, ArtifactState.PUBLISHED.toString(), null, null, null,
                    UUID.fromString(draftDomainId), UUID.fromString(draftDomainId));
            res = newId.toString();
        }
        jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".domain set state = ? where id = ?",
                ArtifactState.DRAFT_HISTORY.toString(), UUID.fromString(draftDomainId));
        //jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".domain where id = ?", UUID.fromString(draftDomainId));
        return res;
    }

    public Domain updateDomain(String domainId, UpdatableDomainEntity domainEntity, UserDetails userDetails) throws LottabyteException {
        Domain d = new Domain(domainEntity);
        d.setId(domainId);
        d.setModifiedBy(userDetails.getUid());
        d.setModifiedAt(LocalDateTime.now());

        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (domainEntity.getName() != null) {
            sets.add("name=?");
            args.add(domainEntity.getName());
        }
        if (domainEntity.getDescription() != null) {
            sets.add("description=?");
            args.add(domainEntity.getDescription());
        }
        if (sets.size() > 0) {
            sets.add("modified=?");
            sets.add("modifier=?");
            args.add(d.getModifiedAt());
            args.add(d.getModifiedBy());
            args.add(UUID.fromString(d.getId()));

            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".domain SET " + StringUtils.join(sets, ", ")
                    + " WHERE id=?", args.toArray());
        }
        return d;
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

    public Boolean hasAccessToDomain(String domainId, UserDetails userDetails) {
        return userDetails.getStewardId() == null ? true :
                jdbcTemplate.queryForObject("SELECT EXISTS(SELECT domain.ID FROM da_" + userDetails.getTenant() + ".domain " +
                QueryHelper.getJoinQuery(ArtifactType.domain, userDetails) + " where domain.id = ?) as exists", Boolean.class, UUID.fromString(domainId));
    }

    public SearchResponse<FlatDomain> searchDomains(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, "tbl1.id", true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "SELECT d.*, true as has_access FROM da_" + userDetails.getTenant() + ".domain d ";
        if (userDetails.getStewardId() != null) {
            String hasAccessJoinQuery =
                    " join da_" + userDetails.getTenant() + ".steward_to_domain s2d on d.id = s2d.domain_id "
                            + " join da_" + userDetails.getTenant() + ".steward s on s2d.steward_id = s.id and s.id = '"
                            + userDetails.getStewardId() + "'";
            subQuery = "SELECT d.*, case when acc.id is null then false else true end as has_access FROM da_" + userDetails.getTenant() + ".domain d "
                    + " left join (select d.id from da_" + userDetails.getTenant() + ".domain d " + hasAccessJoinQuery + ") acc on d.id = acc.id ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward())
                subQuery = "SELECT d.*, true as has_access FROM da_" + userDetails.getTenant() + ".domain d "
                        + hasAccessJoinQuery;
        }

        subQuery = "SELECT sq.*, wft.workflow_state FROM (" + subQuery + ") as sq left join da_" + userDetails.getTenant() + ".workflow_task wft "
                + " on sq.workflow_task_id = wft.id ";
        subQuery = "SELECT distinct sq.*, s.stewards, t.tags FROM (" + subQuery + ") as sq left join ("
                 + "select d.id as domain_id, string_agg(s.name, ',') as stewards from da_" + userDetails.getTenant() + ".domain d left join da_" + userDetails.getTenant() + ".steward_to_domain s2d on s2d.domain_id = d.id "
                 + "left join da_" + userDetails.getTenant() + ".steward s on s2d.steward_id = s.id group by d.id "
                 + ") s on s.domain_id = sq.id "
                + "left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_" + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant() + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id ";

        List<FlatDomain> flatItems =
                jdbcTemplate.query("SELECT * FROM (" + subQuery + ") as tbl1 " + where
                        + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                        + searchRequest.getLimit(), new FlatDomainRowMapper(), whereValues.toArray());

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(distinct id) FROM (" + subQuery + ") as tbl1 " + where, Integer.class, whereValues.toArray());

        SearchResponse<FlatDomain> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public List<String> getSystemIdsByDomainId(String domainId, UserDetails userDetails) {
        return jdbcTemplate.queryForList("SELECT system_id FROM da_" + userDetails.getTenant()
                + ".system_to_domain sd JOIN da_" + userDetails.getTenant() + ".system s ON sd.system_id = s.id WHERE sd.domain_id=? and state = ?",
                String.class, UUID.fromString(domainId), ArtifactState.PUBLISHED.name());
    }
}
