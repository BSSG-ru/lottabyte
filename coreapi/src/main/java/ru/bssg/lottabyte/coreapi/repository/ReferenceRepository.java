package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.reference.Reference;
import ru.bssg.lottabyte.core.model.reference.ReferenceEntity;
import ru.bssg.lottabyte.core.model.reference.ReferenceType;
import ru.bssg.lottabyte.core.model.reference.UpdatableReferenceEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class ReferenceRepository extends GenericArtifactRepository<Reference> {

    private final JdbcTemplate jdbcTemplate;

    private static String[] extFields = {"source_id", "source_artifact_type", "target_id", "target_artifact_type", "reference_type"};

    @Autowired
    public ReferenceRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.reference.name(), extFields);
        super.setMapper(new ReferenceRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    class ReferenceRowMapper implements RowMapper<Reference> {
        @Override
        public Reference mapRow(ResultSet rs, int rowNum) throws SQLException {
            ReferenceEntity referenceEntity = new ReferenceEntity();
            referenceEntity.setSourceId(rs.getString("source_id"));
            referenceEntity.setSourceType(ArtifactType.valueOf(rs.getString("source_artifact_type")));
            referenceEntity.setTargetId(rs.getString("target_id"));
            referenceEntity.setPublishedId(rs.getString("published_id"));
            referenceEntity.setTargetType(ArtifactType.valueOf(rs.getString("target_artifact_type")));
            referenceEntity.setReferenceType(ReferenceType.valueOf(rs.getString("reference_type")));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));
            md.setVersionId(rs.getInt("version_id"));
            md.setEffectiveStartDate(rs.getTimestamp("history_start").toLocalDateTime());
            md.setEffectiveEndDate(rs.getTimestamp("history_end").toLocalDateTime());

            return new Reference(referenceEntity, md);
        }
    }

    public Reference getReferenceBySourceIdAndTargetId(String sourceId, String targetId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".reference " +
                        "where source_id = ? and target_id = ?", new ReferenceRowMapper(), UUID.fromString(sourceId), UUID.fromString(targetId))
                .stream().findFirst().orElse(null);
    }

    public Reference getReferenceBySourceId(String id, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".reference " +
                        "where source_id = ?", new ReferenceRowMapper(), UUID.fromString(id))
                .stream().findFirst().orElse(null);
    }

    public List<Reference> getAllReferenceBySourceIdAndTargetType(String id, String type, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".reference " +
                        "where source_id = ? AND target_artifact_type = ?", new ReferenceRowMapper(), UUID.fromString(id), type);
    }
    public List<Reference> getAllReferenceByPublishedIdAndTypeAndVersionId(String publishedId, Integer versionId, String type, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".reference " +
                "where published_id = ? AND version_id = ? AND target_artifact_type = ? AND source_id != published_id", new ReferenceRowMapper(), UUID.fromString(publishedId), versionId, type);
    }

    public List<Reference> getAllReferencesByObjectId(String objectId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".reference where source_id=? or target_id=?",
                new ReferenceRowMapper(), UUID.fromString(objectId), UUID.fromString(objectId));
    }

    public List<Reference> getAllReferencesByTargetIdAndRefType(String targetId, ReferenceType referenceType, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".reference where target_id=? AND reference_type=?",
                new ReferenceRowMapper(), UUID.fromString(targetId), String.valueOf(referenceType));
    }

    public List<Reference> getAllByArtifactId(String artifactId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".reference " +
                "where source_id = ? or target_id = ?",
                new ReferenceRowMapper(), UUID.fromString(artifactId), UUID.fromString(artifactId));
    }

    public Boolean existsReference(String sourceId, String targetId, ReferenceType referenceType, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(select id from da_" + userDetails.getTenant() + ".reference " +
                "where (source_id = ? and target_id = ?) or (source_id = ? and target_id = ?) and reference_type =?) as exists",
                Boolean.class, UUID.fromString(sourceId), UUID.fromString(targetId), UUID.fromString(sourceId), UUID.fromString(targetId),
                referenceType.name());
    }

    public void patchReferenceBySourceIdAndTargetId(UpdatableReferenceEntity newReferenceEntity, UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".reference SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));
        if(newReferenceEntity.getPublishedId() != null) {
            sets.add("published_id = ?");
            params.add(UUID.fromString(newReferenceEntity.getPublishedId()));
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE source_id = ? and target_id = ?";
            params.add(UUID.fromString(newReferenceEntity.getSourceId()));
            params.add(UUID.fromString(newReferenceEntity.getTargetId()));
            jdbcTemplate.update(query, params.toArray());
        }
    }
    public Integer getLastVersionByPublishedId(String publishedId, UserDetails userDetails) {
        Integer versionId = 0;
        if(publishedId != null) {
            versionId = jdbcTemplate.queryForObject("" +
                            "        SELECT max(version_id) " +
                            "        FROM da_" + userDetails.getTenant() + ".reference where published_id = ?",
                    Integer.class, UUID.fromString(publishedId));
            if(versionId == null)
                versionId = 0;
        }
        return versionId;
    }
    public String createReference(UpdatableReferenceEntity newReferenceEntity, UserDetails userDetails) {
        UUID newId = UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".reference " +
                "(id, source_id, source_artifact_type, target_id, target_artifact_type, reference_type, created, creator, modified, modifier, history_start, history_end, version_id, published_id) " +
                " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                newId,
                UUID.fromString(newReferenceEntity.getSourceId()),
                newReferenceEntity.getSourceType().getText(),
                UUID.fromString(newReferenceEntity.getTargetId()),
                newReferenceEntity.getTargetType().getText(),
                newReferenceEntity.getReferenceType().name(),
                ts, userDetails.getUid(), ts, userDetails.getUid(), ts, ts, newReferenceEntity.getVersionId() == null ? 0 : newReferenceEntity.getVersionId(), newReferenceEntity.getPublishedId() != null ? UUID.fromString(newReferenceEntity.getPublishedId()) : null);
        return newId.toString();
    }
    public void deleteReferenceBySourceId(String id, UserDetails userDetails) {
        jdbcTemplate.update("delete from da_" + userDetails.getTenant() + ".reference where source_id = ?",
                UUID.fromString(id));
    }

    public void deleteAllByArtifactId(String artifactId, UserDetails userDetails) {
        jdbcTemplate.update("delete from da_" + userDetails.getTenant() + ".reference where source_id = ? or target_id = ?",
                UUID.fromString(artifactId), UUID.fromString(artifactId));
    }

    public void deleteByReferenceSourceIdAndTargetId(String sourceId, String targetId, UserDetails userDetails) {
        jdbcTemplate.update("delete from da_" + userDetails.getTenant() + ".reference where source_id = ? AND target_id = ?",
                UUID.fromString(sourceId), UUID.fromString(targetId));
    }
}
