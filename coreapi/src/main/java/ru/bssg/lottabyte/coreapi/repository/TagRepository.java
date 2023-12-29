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
import ru.bssg.lottabyte.core.model.AbstractPaginatedList;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.tag.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class TagRepository extends GenericArtifactRepository<Tag> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = {"tag_category_id"};

    @Autowired
    public TagRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.tag_category.name(), extFields);
        super.setMapper(new TagRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class TagRowMapper implements RowMapper<Tag> {
        @Override
        public Tag mapRow(ResultSet rs, int rowNum) throws SQLException {
            TagEntity tagEntity = new TagEntity();
            tagEntity.setName(rs.getString("name"));
            tagEntity.setDescription(rs.getString("description"));
            tagEntity.setTagCategoryId(rs.getString("tag_category_id"));
            return new Tag(tagEntity, new Metadata(rs, tagEntity.getArtifactType()));
        }
    }

    public static class FlatTagRowMapper implements RowMapper<FlatTag>  {
        @Override
        public FlatTag mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatTag ft = new FlatTag();
            ft.setId(rs.getString("id"));
            ft.setName(rs.getString("name"));
            return ft;
        }
    }

    public void deleteAllTagsByArtifactId(String artifactId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".tag_to_artifact WHERE artifact_id = ?",
                UUID.fromString(artifactId));
    }

    public Tag getTagByName(String tagName, String tagCategoryId, UserDetails userDetails) {
        List<Tag> tagList = jdbcTemplate.query("SELECT id, name, description, tag_category_id, history_start, "
                + "history_end, version_id, created, creator, modified, modifier FROM da_" + userDetails.getTenant()
                + ".tag WHERE tag_category_id=? AND name=?", new TagRowMapper(), UUID.fromString(tagCategoryId), tagName);

        return tagList.stream().findFirst().orElse(null);
    }

    public List<Tag> getArtifactTags(String artifactId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT t.* FROM da_" + userDetails.getTenant() + ".tag t JOIN da_" + userDetails.getTenant() +
                        ".tag_to_artifact ta ON t.id=ta.tag_id AND ta.artifact_id=?",
                new TagRowMapper(), UUID.fromString(artifactId));
    }

    public List<Tag> getArtifactTags(String artifactId, UserDetails userDetails, LocalDateTime dateFrom, LocalDateTime dateTo) {
        log.info(dateFrom.toString());
        log.info(dateTo.toString());
        List<Tag> resources =
                jdbcTemplate.query("SELECT t.* FROM da_" + userDetails.getTenant() + ".tag t JOIN da_" + userDetails.getTenant() +
                                ".tag_to_artifact ta ON t.id=ta.tag_id AND ta.artifact_id=uuid(?) and " +
                                "ta.history_start <= ? " +
                                "UNION ALL SELECT t.* FROM da_" + userDetails.getTenant() + ".tag t JOIN da_" + userDetails.getTenant() +
                                ".tag_to_artifact_hist ta ON t.id=ta.tag_id AND ta.artifact_id=uuid(?) and " +
                                "ta.history_end >= ? or ta.history_end < ?",
                        new TagRowMapper(), artifactId, dateTo, artifactId, dateFrom, dateTo);
        return resources;
    }

    public boolean tagNameExists(String name, String categoryId, UserDetails userDetails) {
        return tagNameExists(name, categoryId, null, userDetails);
    }

    public boolean tagNameExists(String name, String categoryId, String thisTagId, UserDetails userDetails) {
        Integer c;
        if (thisTagId == null)
            c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".tag WHERE name=? AND tag_category_id=?",
                Integer.class, name, UUID.fromString(categoryId));
        else
            c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".tag WHERE id<>? AND name=? AND tag_category_id=?",
                    Integer.class, UUID.fromString(thisTagId), name, UUID.fromString(categoryId));
        return c > 0;
    }

    public PaginatedArtifactList<Tag> getArtifactTagsPaginated(String artifactId, Integer offset, Integer limit, UserDetails userDetails) {
        List<Tag> resources =
                jdbcTemplate.query("SELECT t.* FROM da_" + userDetails.getTenant() + ".tag t JOIN da_" + userDetails.getTenant() +
                        ".tag_to_artifact ta ON t.id=ta.tag_id AND ta.artifact_id=? OFFSET " + offset + " LIMIT " + limit,
                        new TagRowMapper(), UUID.fromString(artifactId));

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".tag_to_artifact WHERE artifact_id=?",
                Integer.class, UUID.fromString(artifactId));

        PaginatedArtifactList<Tag> res = new PaginatedArtifactList<>(
                resources, offset, limit, total, "/v1/tags/artifacts/" + artifactId);
        return res;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Override
    public void deleteById(String tagId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".tag_to_artifact WHERE tag_id=?", UUID.fromString(tagId));
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".tag WHERE id=?", UUID.fromString(tagId));
    }

    public Tag createTag(TagEntity tagEntity, UserDetails userDetails) throws LottabyteException {
        UUID id = UUID.randomUUID();

        Tag tag = new Tag(tagEntity);
        tag.setId(id.toString());
        LocalDateTime now = LocalDateTime.now();
        tag.setCreatedAt(now);
        tag.setModifiedAt(now);
        tag.setCreatedBy(userDetails.getUid());
        tag.setModifiedBy(userDetails.getUid());

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".tag (id, name, description, tag_category_id, created, creator, modified, modifier, history_start, history_end) VALUES (?,?,?,?,?,?,?,?,?,?)",
                id, tagEntity.getName(), tagEntity.getDescription(), UUID.fromString(tagEntity.getTagCategoryId()), tag.getCreatedAt(), tag.getCreatedBy(),
                tag.getModifiedAt(), tag.getModifiedBy(), now, now);

        return tag;
    }

    public Tag updateTag(String tagId, UpdatableTagEntity tagEntity, UserDetails userDetails) throws LottabyteException {
        Tag tag = new Tag(tagEntity);
        tag.setId(tagId);

        LocalDateTime now = LocalDateTime.now();
        tag.setModifiedAt(now);
        tag.setModifiedBy(userDetails.getUid());

        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (tagEntity.getName() != null) {
            sets.add("name=?");
            args.add(tagEntity.getName());
        }
        if (tagEntity.getDescription() != null) {
            sets.add("description=?");
            args.add(tagEntity.getDescription());
        }
        if (tagEntity.getTagCategoryId() != null) {
            sets.add("tag_category_id=?");
            args.add(UUID.fromString(tagEntity.getTagCategoryId()));
        }
        if (sets.size() > 0) {
            sets.add("modified=?");
            sets.add("modifier=?");
            args.add(tag.getModifiedAt());
            args.add(tag.getModifiedBy());
            args.add(UUID.fromString(tag.getId()));

            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".tag SET " + StringUtils.join(sets, ", ") + " WHERE id=?", args.toArray());
        }

        return tag;
    }

    public Boolean tagIsLinkedToArtifact(String tagId, String artifactId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT 1 FROM da_" + userDetails.getTenant() +
                ".tag_to_artifact WHERE artifact_id=? AND tag_id=?)", Boolean.class, UUID.fromString(artifactId),
                UUID.fromString(tagId));
    }

    public void linkTagToArtifact(String tagId, String artifactId, String artifactType, UserDetails userDetails) {
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".tag_to_artifact (id, artifact_id, "
                + "artifact_type, tag_id, history_start, history_end, version_id, created, creator, modified, modifier) "
                + " VALUES (?,?,?,?,?,?,0,?,?,?,?)", UUID.randomUUID(), UUID.fromString(artifactId), artifactType,
                UUID.fromString(tagId), now, now, now, userDetails.getUid(), now, userDetails.getUid());
    }

    public void unlinkTagFromArtifact(String tagId, String artifactId, String artifactType, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".tag_to_artifact WHERE artifact_id=? AND tag_id=?",
                UUID.fromString(artifactId), UUID.fromString(tagId));
    }

    public List<FlatTag> searchTags(String query, Integer offset, Integer limit, UserDetails userDetails) {
        log.info("searchTags query: " + query);
        log.info("searchTags offset: " + offset);
        log.info("searchTags limit: " + limit);

        String q = "%";
        if (query != null && !query.isEmpty())
            q = "%" + query.toLowerCase() + "%";
        String sql = "SELECT * FROM (SELECT * FROM da_" + userDetails.getTenant() + ".tag where lower(name) like ?) AS t limit ? offset ?";
        return jdbcTemplate.query(sql, new FlatTagRowMapper(), q, limit, offset);
    }

}
