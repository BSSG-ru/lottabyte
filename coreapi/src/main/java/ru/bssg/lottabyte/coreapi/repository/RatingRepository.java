package ru.bssg.lottabyte.coreapi.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.rating.Rating;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.*;
import java.util.*;

@Repository
@Slf4j
@RequiredArgsConstructor
public class RatingRepository {
    private final JdbcTemplate jdbcTemplate;

    class RatingRowMapper implements RowMapper<Rating> {
        @Override
        public Rating mapRow(ResultSet rs, int rowNum) throws SQLException {
            Rating rating = new Rating();
            rating.setArtifactId(rs.getString("artifact_id"));
            rating.setArtifactName("art_name");
            rating.setArtifactType(rs.getString("artifact_type"));
            rating.setRating(rs.getDouble("avg"));
            rating.setTotalRates(rs.getInt("total_rating"));

            return rating;
        }
    }

    public Boolean existsObjectByIdAndArtifactType(String artifactId, String artifactType, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant() + "." + artifactType + " WHERE id = ?) as exists",
                Boolean.class, UUID.fromString(artifactId));
    }

    public Boolean existsRatingByArtifactId(String artifactId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant() + ".rating WHERE artifact_id = ?) as exists",
                Boolean.class, UUID.fromString(artifactId));
    }

    public Boolean existsRatingByArtifactIdAndUserId(String artifactId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant() + ".rating WHERE artifact_id = ? and user_id = ?) as exists",
                Boolean.class, UUID.fromString(artifactId), userDetails.getUid());
    }

    public Rating getRatingByArtifactId(String artifactId, UserDetails userDetails) {
        List<Rating> ratingList = jdbcTemplate.query("select id, artifact_id, (select avg(rating) from da_" + userDetails.getTenant() + ".rating where artifact_id = ?) as avg, " +
                        "(select count(*) from da_" + userDetails.getTenant() + ".rating where artifact_id = ?) as total_rating, " +
                        "artifact_type from da_" + userDetails.getTenant() + ".rating",
                new RatingRowMapper(), UUID.fromString(artifactId), UUID.fromString(artifactId));

        return ratingList.stream().findFirst().orElse(null);
    }

    public Integer getOwnRatingByArtifactId(String artifactId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("select rating from da_" + userDetails.getTenant() + ".rating where artifact_id = ? AND user_id = ?",
                Integer.class, UUID.fromString(artifactId), userDetails.getUid());
    }

    public ArtifactType getDataArtifactType (String artifactType) {
        ArtifactType dataArtifactType = null;
        try {
            dataArtifactType = ArtifactType.valueOf(artifactType);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return dataArtifactType;
    }

    public String createRating(String artifactType, String artifactId, Integer rating, UserDetails userDetails) {
        UUID uuidForRating = java.util.UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".\"rating\"\n" +
                        "(id, artifact_id, artifact_type, user_id, rating, created, modified)\n" +
                        "VALUES(?, ?, ?, ?, ?, ?, ?);",
                uuidForRating, UUID.fromString(artifactId), artifactType, userDetails.getUid(), rating, new Timestamp(new java.util.Date().getTime()), new Timestamp(new java.util.Date().getTime()));
        return uuidForRating.toString();
    }

    public void patchRating(String artifactId, Integer rating, UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".rating SET modified = ?";
        params.add(new Timestamp(new java.util.Date().getTime()));
        if (rating != null) {
            sets.add("rating = ?");
            params.add(rating);
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE artifact_id = ? AND user_id = ?";
            params.add(UUID.fromString(artifactId));
            params.add(userDetails.getUid());
            jdbcTemplate.update(query, params.toArray());
        }
    }

    public void deleteRating(String artifactId, UserDetails userDetails) {
        String query = "DELETE FROM da_" + userDetails.getTenant() + ".\"rating\" WHERE artifact_id=?";
        jdbcTemplate.update(query, UUID.fromString(artifactId));
    }
}
