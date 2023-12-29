package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.FlatRelation;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.steward.FlatSteward;
import ru.bssg.lottabyte.core.model.steward.Steward;
import ru.bssg.lottabyte.core.model.steward.StewardEntity;
import ru.bssg.lottabyte.core.model.steward.UpdatableStewardEntity;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.util.JDBCUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class StewardRepository extends GenericArtifactRepository<Steward> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = {};

    @Autowired
    public StewardRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.steward.name(), extFields);
        super.setMapper(new StewardRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    private class StewardRowMapper implements RowMapper<Steward> {
        @Override
        public Steward mapRow(ResultSet rs, int rowNum) throws SQLException {
            Steward s = null;

            StewardEntity stewardEntity = new StewardEntity();
            stewardEntity.setName(rs.getString("name"));
            stewardEntity.setDescription(rs.getString("description"));
            stewardEntity.setUserId(rs.getInt("user_id"));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));
            md.setName(rs.getString("name"));
            md.setArtifactType(stewardEntity.getArtifactType().toString());
            md.setVersionId(rs.getInt("version_id"));
            md.setEffectiveStartDate(rs.getTimestamp("history_start").toLocalDateTime());
            md.setEffectiveEndDate(rs.getTimestamp("history_end").toLocalDateTime());

            try {
                s = new Steward(stewardEntity, md);
            } catch (LottabyteException e) {
                log.error(e.getMessage(), e);
            }
            return s;
        }
    }

    private class FlatStewardRowMapper implements RowMapper<FlatSteward> {
        @Override
        public FlatSteward mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatSteward fs = new FlatSteward();
            fs.setId(rs.getString("id"));
            fs.setName(rs.getString("name"));
            fs.setDescription(rs.getString("description"));
            fs.setModified(rs.getTimestamp("modified").toLocalDateTime());
            fs.setUserId(JDBCUtil.getInt(rs, "user_id"));

            if (rs.getArray("domain_names") != null && rs.getArray("domain_ids") != null) {
                UUID[] ids = (UUID[])rs.getArray("domain_ids").getArray();
                String[] names = (String[])rs.getArray("domain_names").getArray();
                List<FlatRelation> items = new ArrayList<>();
                for (int i = 0; i < ids.length; i++) {
                    if (ids[i] != null && names[i] != null)
                        items.add(new FlatRelation(ids[i].toString(), names[i], "/v1/domain/" + ids[i].toString()));
                }
                fs.setDomains(items);
            }
            return fs;
        }
    }

    public List<String> getDomainIdsByStewardId(String stewardId, UserDetails userDetails) {
        List<String> res = jdbcTemplate.queryForList("SELECT domain_id FROM da_" + userDetails.getTenant()
                + ".steward_to_domain sd WHERE sd.steward_id=?", String.class, UUID.fromString(stewardId));
        return res;
    }


    @Override
    public Steward getById(String stewardId, UserDetails userDetails) {
        Steward steward = jdbcTemplate.queryForObject("SELECT * FROM da_" + userDetails.getTenant() + ".steward WHERE id=?",
                new StewardRowMapper(), UUID.fromString(stewardId));

        if (steward != null)
            steward.getEntity().setDomains(getDomainIdsByStewardId(stewardId, userDetails));

        return steward;
    }

    public List<Steward> getStewardsByDomainId(String domainId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".steward join da_" + userDetails.getTenant() + ".steward_to_domain "
                + " on steward.id = steward_to_domain.steward_id WHERE steward_to_domain.domain_id = ?", new StewardRowMapper(),
                UUID.fromString(domainId));
    }

    public PaginatedArtifactList<Steward> getAllPaginated(Integer offset, Integer limit, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".steward", Integer.class);
        String query = "SELECT * FROM da_" + userDetails.getTenant() + ".steward offset ? limit ? ";
        List<Steward> stewardList = jdbcTemplate.query(query, new StewardRowMapper(), offset, limit);

        for (Steward s : stewardList) {
            s.getEntity().setDomains(getDomainIdsByStewardId(s.getId(), userDetails));
        }

        PaginatedArtifactList<Steward> res = new PaginatedArtifactList<>(
                stewardList, offset, limit, total, "/v1/stewards/");
        return res;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Override
    public void deleteById(String stewardId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".steward WHERE id=?", UUID.fromString(stewardId));
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".steward_to_domain WHERE steward_id=?", UUID.fromString(stewardId));
    }

    public boolean userIdExists(Integer userId, UserDetails userDetails) {
        return userIdExists(userId, null, userDetails);
    }

    public boolean userIdExists(Integer userId, String thisStewardId, UserDetails userDetails) {
        Integer c;
        if (thisStewardId == null)
            c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".steward WHERE user_id=?",
                    Integer.class, userId);
        else
            c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".steward WHERE user_id=? AND id<>?",
                    Integer.class, userId, UUID.fromString(thisStewardId));
        return c > 0;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Steward createSteward(UpdatableStewardEntity stewardEntity, UserDetails userDetails) throws LottabyteException {
        UUID id = UUID.randomUUID();

        Steward s = new Steward(stewardEntity);
        s.setId(id.toString());
        LocalDateTime now = LocalDateTime.now();
        s.setCreatedAt(now);
        s.setModifiedAt(now);
        s.setCreatedBy(userDetails.getUid());
        s.setModifiedBy(userDetails.getUid());

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".steward (id, name, description, user_id, created, creator, modified, modifier, history_start, history_end) VALUES (?,?,?,?,?,?,?,?,?,?)",
                id, stewardEntity.getName(), stewardEntity.getDescription(), stewardEntity.getUserId(), s.getCreatedAt(), s.getCreatedBy(),
                s.getModifiedAt(), s.getModifiedBy(), now, now);

        for (String domainId : stewardEntity.getDomains()) {
            jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".steward_to_domain (id, domain_id, steward_id, created, creator, modified, modifier) VALUES (?,?,?,?,?,?,?)",
                    UUID.randomUUID(), UUID.fromString(domainId), id, now, userDetails.getUid(), now, userDetails.getUid());
        }

        return s;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Steward updateSteward(String stewardId, UpdatableStewardEntity stewardEntity, UserDetails userDetails) throws LottabyteException {
        Steward s = new Steward(stewardEntity);
        s.setId(stewardId);

        LocalDateTime now = LocalDateTime.now();
        s.setModifiedAt(now);
        s.setModifiedBy(userDetails.getUid());

        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (stewardEntity.getName() != null) {
            sets.add("name=?");
            args.add(stewardEntity.getName());
        }
        if (stewardEntity.getDescription() != null) {
            sets.add("description=?");
            args.add(stewardEntity.getDescription());
        }
        if (stewardEntity.getUserId() != null) {
            sets.add("user_id=?");
            args.add(stewardEntity.getUserId());
        }
        if (!sets.isEmpty()) {
            sets.add("modified=?");
            sets.add("modifier=?");
            args.add(now);
            args.add(userDetails.getUid());
        }
        args.add(UUID.fromString(stewardId));

        if (!sets.isEmpty()) {
            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".steward SET " + StringUtils.join(sets, ", ") + " WHERE id=?",
                    args.toArray());
        }

        List<String> domainIds = jdbcTemplate.queryForList("SELECT domain_id FROM da_" + userDetails.getTenant() + ".steward_to_domain WHERE steward_id=?",
            String.class, UUID.fromString(stewardId));

        for (String did : domainIds) {
            if (stewardEntity.getDomains() != null && !stewardEntity.getDomains().contains(did))
                jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".steward_to_domain WHERE steward_id=? AND domain_id=?", UUID.fromString(stewardId), UUID.fromString(did));
        }

        if (stewardEntity.getDomains() != null) {
            for (String did : stewardEntity.getDomains()) {
                if (!domainIds.contains(did))
                    jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".steward_to_domain (id, steward_id, domain_id, created, creator, modified, modifier) VALUES (?,?,?,?,?,?,?)",
                            UUID.randomUUID(), UUID.fromString(stewardId), UUID.fromString(did), now, userDetails.getUid(), now, userDetails.getUid());
            }
        }

        return s;
    }

    public SearchResponse<FlatSteward> searchStewards(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "SELECT steward.id, steward.name, steward.description, steward.user_id, steward.modified, " +
                "array_agg(domain.name order by domain.id) as domain_names, " +
                "string_agg(domain.name, ',') as domains, " +
                "array_agg(domain.id order by domain.id) as domain_ids " +
                "from da_" + userDetails.getTenant() + ".steward " +
                "left join da_" + userDetails.getTenant() + ".steward_to_domain on steward_to_domain.steward_id = steward.id " +
                "left join da_" + userDetails.getTenant() + ".domain on steward_to_domain.domain_id = domain.id and domain.state = 'PUBLISHED' group by " +
                "steward.id, steward.name, steward.description, steward.user_id, steward.modified";
        List<FlatSteward> flatItems =
                jdbcTemplate.query("SELECT * FROM (" + subQuery + ") as tbl1 " + where
                        + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                        + searchRequest.getLimit(), new FlatStewardRowMapper());

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM (" + subQuery + ") as tbl1 " + where, Integer.class);

        SearchResponse<FlatSteward> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public String addStewardToDomain(String stewardId, String domainId, UserDetails userDetails) {
        UUID newId = java.util.UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        String query = "INSERT INTO da_" + userDetails.getTenant() + ".steward_to_domain " +
                "(id, steward_id, domain_id, created, creator, modified, modifier) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(query, newId, UUID.fromString(stewardId), UUID.fromString(domainId),
                ts, userDetails.getUid(), ts, userDetails.getUid());
        return newId.toString();
    }

    public String removeStewardFromDomain(String stewardId, String domainId, UserDetails userDetails) {
        UUID newId = java.util.UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        String query = "DELETE FROM da_" + userDetails.getTenant() + ".steward_to_domain " +
                "where steward_id = ? and domain_id = ?";
        jdbcTemplate.update(query, UUID.fromString(stewardId), UUID.fromString(domainId));
        return newId.toString();
    }
}
