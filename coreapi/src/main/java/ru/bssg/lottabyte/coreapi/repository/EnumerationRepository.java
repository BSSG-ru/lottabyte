package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.enumeration.Enumeration;
import ru.bssg.lottabyte.core.model.enumeration.EnumerationEntity;
import ru.bssg.lottabyte.core.model.enumeration.FlatEnumeration;
import ru.bssg.lottabyte.core.model.enumeration.UpdatableEnumerationEntity;
import ru.bssg.lottabyte.core.model.tag.Tag;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class EnumerationRepository extends GenericArtifactRepository<Enumeration> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = {"variants"};

    @Autowired
    public EnumerationRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.enumeration.name(), extFields);
        super.setMapper(new EnumerationRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class EnumerationRowMapper implements RowMapper<Enumeration> {
        @Override
        public Enumeration mapRow(ResultSet rs, int rowNum) throws SQLException {
            EnumerationEntity enumerationEntity = new EnumerationEntity();
            enumerationEntity.setName(rs.getString("name"));
            enumerationEntity.setDescription(rs.getString("description"));
            if (rs.getArray("variants") != null) {
                String[] array = (String[]) rs.getArray("variants").getArray();
                enumerationEntity.setVariants(new ArrayList<>(Arrays.asList(array)));
            }

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));
            md.setName(rs.getString("name"));
            md.setVersionId(rs.getInt("version_id"));
            md.setEffectiveStartDate(rs.getTimestamp("history_start").toLocalDateTime());
            md.setEffectiveEndDate(rs.getTimestamp("history_end").toLocalDateTime());
            return new Enumeration(enumerationEntity, md);
        }
    }

    public static class FlatEnumerationRowMapper implements RowMapper<FlatEnumeration> {
        @Override
        public FlatEnumeration mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatEnumeration enumerationEntity = new FlatEnumeration();
            enumerationEntity.setName(rs.getString("name"));
            enumerationEntity.setDescription(rs.getString("description"));
            if (rs.getArray("variants") != null) {
                String[] array = (String[]) rs.getArray("variants").getArray();
                enumerationEntity.setVariants(new ArrayList<>(Arrays.asList(array)));
            }
            enumerationEntity.setId(rs.getString("id"));

            return enumerationEntity;
        }
    }

    public String createEnumeration(UpdatableEnumerationEntity newEnumerationEntity, UserDetails userDetails) {
        UUID newId = UUID.randomUUID();
        Timestamp ts = new Timestamp(new Date().getTime());
        String variantsString = newEnumerationEntity.getVariants() != null ?
                String.join(",", newEnumerationEntity.getVariants())
                : null;

        String query = "INSERT INTO da_" + userDetails.getTenant() + ".enumeration " +
                "(id, \"name\", description, variants, history_start, history_end, version_id, created, creator, modified, modifier) " +
                "VALUES(?, ?, ?, string_to_array(?,','), ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(query, newId, newEnumerationEntity.getName(), newEnumerationEntity.getDescription(),
                variantsString,
                ts, ts, 0, ts, userDetails.getUid(), ts, userDetails.getUid());
        return newId.toString();
    }

    public void patchEnumeration(String enumerationId, UpdatableEnumerationEntity enumerationEntity, UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".enumeration SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new Date().getTime()));
        if (enumerationEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(enumerationEntity.getName());
        }
        if (enumerationEntity.getDescription() != null) {
            sets.add("description = ?");
            params.add(enumerationEntity.getDescription());
        }
        if (enumerationEntity.getVariants() != null) {
            sets.add("variants = string_to_array(?,',')");
            String altNamesString = String.join(",", enumerationEntity.getVariants());
            params.add(altNamesString);
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(enumerationId));
            jdbcTemplate.update(query, params.toArray());
        }
    }

    public PaginatedArtifactList<Enumeration> getEnumerationVersions(String businessEntityId, Integer offset, Integer limit, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".enumeration_hist " +
                "WHERE id = ?", Integer.class, UUID.fromString(businessEntityId));
        List<Enumeration> resources = jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".enumeration_hist WHERE id = ? " +
                        "offset ? limit ?",
                new EnumerationRowMapper(), UUID.fromString(businessEntityId), offset, limit);

        return new PaginatedArtifactList<>(
                resources, offset, limit, total, "/v1/enumeration/" + businessEntityId + "/versions");
    }

    public SearchResponse<FlatEnumeration> searchEnumeration(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String queryForItems = "SELECT * FROM da_" + userDetails.getTenant() + ".enumeration tbl1 " + join
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatEnumeration> items = jdbcTemplate.query(queryForItems, new FlatEnumerationRowMapper(), whereValues.toArray());

        String queryForTotal = "SELECT COUNT(tbl1.id) FROM da_" + userDetails.getTenant() + ".enumeration tbl1 " + join + where;
        final int[] count = {0};
        jdbcTemplate.query(
                queryForTotal,
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        count[0] = rs.getInt("count");
                    }
                },
                whereValues.toArray()
        );

        int total = count[0];

        SearchResponse<FlatEnumeration> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), items);

        return res;
    }
}
