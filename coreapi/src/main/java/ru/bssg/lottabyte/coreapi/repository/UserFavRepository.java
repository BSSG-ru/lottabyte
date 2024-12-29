package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.model.userfav.UserFav;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class UserFavRepository {
    private JdbcTemplate jdbcTemplate;

    public UserFavRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public class UserFavRowMapper implements RowMapper<UserFav> {

        @Override
        public UserFav mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserFav uf = new UserFav();
            uf.setId(rs.getString("id"));
            uf.setUserId(rs.getInt("user_id"));
            uf.setArtifactId(rs.getString("artifact_id"));
            uf.setArtifactType(rs.getString("artifact_type"));

            return uf;
        }
    }

    public List<UserFav> getUserFavs(Integer userId, String artifactType, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".user_fav WHERE user_id=?"
            + ((artifactType != null && !artifactType.isEmpty()) ? (" AND artifact_type='" + artifactType + "'") : ""),
                new UserFavRowMapper(), userId);
    }

    public void addUserFav(String artifactId, String artifactType, Integer userId, UserDetails userDetails) {
        Timestamp ts = new Timestamp(new java.util.Date().getTime());

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".user_fav (id, user_id, artifact_id, " +
                "artifact_type, created, creator, modified, modifier) VALUES (?,?,?,?,?,?,?,?)", UUID.randomUUID(),
                userId, UUID.fromString(artifactId), artifactType, ts, userDetails.getUid(), ts, userDetails.getUid());
    }

    public void delUserFav(Integer userId, String artifactId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".user_fav WHERE user_id=? AND artifact_id=?",
                userId, UUID.fromString(artifactId));
    }

    public Boolean isInFav(String artifactId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant() +
                ".user_fav WHERE user_id=? AND artifact_id=?) as exists", Boolean.class, Integer.parseInt(userDetails.getUid()),
                UUID.fromString(artifactId));
    }
}
