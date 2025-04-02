package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.model.searchQuery.SearchQuery;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
@Slf4j
public class SearchQueryRepository {
    private final JdbcTemplate jdbcTemplate;

    public SearchQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public class SearchQueryRowMapper implements RowMapper<SearchQuery> {

        @Override
        public SearchQuery mapRow(ResultSet rs, int rowNum) throws SQLException {
            SearchQuery sq = new SearchQuery();
            sq.setId(UUID.fromString(rs.getString("id")));
            sq.setQuery(rs.getString("query"));
            sq.setCreated(rs.getTimestamp("created").toLocalDateTime());
            sq.setCreator(rs.getString("creator"));
            return sq;
        }
    }

    public void saveQuery(String query, UserDetails userDetails) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dt = now.minusMonths(1);

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".search_query(id, query, created, creator) VALUES (?,?,?,?)",
                UUID.randomUUID(), query, now, userDetails.getUid());

        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".search_query WHERE created < ?", dt);
    }

    public List<String> getPopularQueries(String search, Integer count, UserDetails userDetails) {

        if (search == null || search.isBlank())
            return jdbcTemplate.queryForList("SELECT query FROM (SELECT query, COUNT(id) AS cnt FROM da_"
                + userDetails.getTenant() + ".search_query GROUP BY query) sq ORDER BY cnt DESC LIMIT ?", String.class,
                count);
        else {
            List<String> words = Arrays.stream(search.split(" ")).filter(w -> !w.isBlank()).collect(Collectors.toList());
            log.info("words " + words.size());

            List<String> queries = jdbcTemplate.queryForList("SELECT query FROM (SELECT query, COUNT(id) AS cnt FROM da_"
                    + userDetails.getTenant() + ".search_query WHERE (" + StringUtils.join(words.stream().map(w -> "query LIKE '%" + ServiceUtils.escForLike(w.toLowerCase()) + "%'").toArray(), " OR ")
                    + ") GROUP BY query) sq ORDER BY cnt DESC LIMIT ?", String.class,
                    count);

            return queries.stream().map(q -> {
                final String[] r = {q};
                words.forEach(w -> { r[0] = r[0].replaceAll("(?i)" + w, "<em>$0</em>"); });
                return r[0];
            }).collect(Collectors.toList());
        }
    }

    public List<String> getUserQueries(Integer count, UserDetails userDetails) {
        return jdbcTemplate.queryForList("SELECT query FROM (SELECT sq.*, row_number() OVER (PARTITION BY query ORDER BY created DESC) AS rn FROM da_"
                        + userDetails.getTenant() + ".search_query sq WHERE creator=? ORDER BY created DESC) t WHERE rn=1 LIMIT ?",
            String.class, userDetails.getUid(), count);
    }
}
