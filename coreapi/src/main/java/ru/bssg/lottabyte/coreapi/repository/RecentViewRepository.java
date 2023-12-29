package ru.bssg.lottabyte.coreapi.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.recentView.RecentView;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@Slf4j
@RequiredArgsConstructor
public class RecentViewRepository {
    private final JdbcTemplate jdbcTemplate;

    class RecentViewRowMapper implements RowMapper<RecentView> {
        @Override
        public RecentView mapRow(ResultSet rs, int rowNum) throws SQLException {
            RecentView recentView = new RecentView();
            recentView.setArtifactId(rs.getString("artifact_id"));
            recentView.setId(rs.getString("id"));
            recentView.setArtifactType(rs.getString("artifact_type"));
            recentView.setUserId(rs.getString("user_id"));
            recentView.setViewedTime(rs.getTimestamp("viewed_time"));
            return recentView;
        }
    }

    public Integer getViewsCount(UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".entity", Integer.class);
    }

    public Integer getRecentCountByType(String artifactType, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".recent_view where user_id = ? and artifact_type = ?",
                Integer.class, userDetails.getUid(), artifactType);
    }

    public RecentView getRecentViewByArtifactIdAndType(String artifactId, String artifactType, UserDetails userDetails) {
        List<RecentView> recentViewList = jdbcTemplate.query("SELECT id, user_id, artifact_id, artifact_type, viewed_time \n" +
                        "FROM da_" + userDetails.getTenant() + ".\"recent_view\"  " +
                        "where artifact_id = ? AND artifact_type = ?;",
                new RecentViewRowMapper(), UUID.fromString(artifactId), artifactType);

        return recentViewList.stream().findFirst().orElse(null);
    }

    public String createRecentView(String artifactId, String artifactType, UserDetails userDetails) {
        UUID uuidForRecentView = java.util.UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".recent_view " +
                        "(id, user_id, artifact_id, artifact_type, viewed_time) " +
                        "VALUES(?, ?, ?, ?, ?);",
                uuidForRecentView, userDetails.getUid(), UUID.fromString(artifactId), artifactType, new Timestamp(new java.util.Date().getTime()));
        return uuidForRecentView.toString();
    }

    public void patchRecentView(String artifactId, String artifactType, UserDetails userDetails) {
        List<Object> params = new ArrayList<>();
        String query = "UPDATE da_" + userDetails.getTenant() + ".recent_view SET viewed_time = ? WHERE user_id = ? AND artifact_id = ? AND artifact_type = ?";
        params.add(new Timestamp(new java.util.Date().getTime()));
        params.add(userDetails.getUid());
        params.add(UUID.fromString(artifactId));
        params.add(artifactType);
        jdbcTemplate.update(query, params.toArray());
    }

    public Boolean existsObjectByIdAndArtifactType(String artifactId, String artifactType, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant() + "." + artifactType + " WHERE id = ?) as exists",
                Boolean.class, UUID.fromString(artifactId));
    }

    public void deleteOldestRecentViewByType(String artifactType, Integer offset, UserDetails userDetails) {
        String query = "delete from da_" + userDetails.getTenant() + ".recent_view where id in " +
                "(SELECT id FROM da_" + userDetails.getTenant() + ".recent_view WHERE artifact_type = ? and user_id = ? ORDER BY viewed_time DESC OFFSET ?)";
        jdbcTemplate.update(query, artifactType, userDetails.getUid(), offset);
    }

    public List<RecentView> getRecentViews(String artifactType, Integer limit, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT id, user_id, artifact_id, artifact_type, viewed_time \n" +
                        "FROM da_" + userDetails.getTenant() + ".\"recent_view\" WHERE user_id=? " +
                        (artifactType != null ? " AND artifact_type = ? " : "") +
                        "ORDER BY viewed_time DESC LIMIT ?",
                new RecentViewRowMapper(),
                (artifactType != null ? new Object[]{userDetails.getUid(), artifactType, limit} : new Object[]{ userDetails.getUid(), limit}));
    }

}
