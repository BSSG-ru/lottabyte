package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.system.SystemConnection;
import ru.bssg.lottabyte.core.model.tag.Tag;
import ru.bssg.lottabyte.core.model.tag.TagCategory;
import ru.bssg.lottabyte.core.model.tag.TagCategoryEntity;
import ru.bssg.lottabyte.core.model.tag.UpdatableTagCategoryEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class TagCategoryRepository extends GenericArtifactRepository<TagCategory> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = {};

    @Autowired
    public TagCategoryRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.tag_category.name(), extFields);
        super.setMapper(new TagCategoryRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    class TagCategoryRowMapper implements RowMapper<TagCategory> {
        @Override
        public TagCategory mapRow(ResultSet rs, int rowNum) throws SQLException {
            TagCategory tc = null;

            TagCategoryEntity tagCategoryEntity = new TagCategoryEntity();
            tagCategoryEntity.setName(rs.getString("name"));
            tagCategoryEntity.setDescription(rs.getString("description"));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));
            md.setName(rs.getString("name"));
            md.setArtifactType(tagCategoryEntity.getArtifactType().toString());
            md.setVersionId(rs.getInt("version_id"));
            md.setEffectiveStartDate(rs.getTimestamp("history_start").toLocalDateTime());
            md.setEffectiveEndDate(rs.getTimestamp("history_end").toLocalDateTime());

            try {
                tc = new TagCategory(tagCategoryEntity, md);
            } catch (LottabyteException e) {
                log.error(e.getMessage(), e);
            }
            return tc;
        }
    }

    public boolean categoryHasTags(String categoryId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".tag WHERE tag_category_id=?", Integer.class, UUID.fromString(categoryId)) > 0;
    }

    public boolean categoryNameExists(String name, UserDetails userDetails) {
        return categoryNameExists(name, null, userDetails);
    }
    public boolean categoryNameExists(String name, String thisCatId, UserDetails userDetails) {
        Integer c;
        if (thisCatId == null)
            c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".tag_category WHERE name=?", Integer.class, name);
        else
            c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".tag_category WHERE id<>? AND name=?", Integer.class, UUID.fromString(thisCatId), name);
        return c > 0;
    }

    private List<Tag> getTagsByCategoryId(String tagCategoryId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".tag WHERE tag_category_id=?",
                new TagRepository.TagRowMapper(), UUID.fromString(tagCategoryId));
    }

    public TagCategory getTagCategoryById(String tagCategoryId, UserDetails userDetails) {
        TagCategory tc = jdbcTemplate.queryForObject("SELECT * FROM da_" + userDetails.getTenant() + ".tag_category WHERE id=?",
                new TagCategoryRepository.TagCategoryRowMapper(), UUID.fromString(tagCategoryId));

        if (tc != null) {
            tc.getEntity().setTags(getTagsByCategoryId(tc.getId(), userDetails));
        }
        return tc;
    }

    @Override
    public PaginatedArtifactList<TagCategory> getAllPaginated(Integer offset, Integer limit, String url, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".tag_category", Integer.class);
        String query = "SELECT * FROM da_" + userDetails.getTenant() + ".tag_category offset ? limit ? ";
        List<TagCategory> tagCategoryList = jdbcTemplate.query(query, new TagCategoryRowMapper(), offset, limit);

        for (TagCategory tc : tagCategoryList) {
            tc.getEntity().setTags(getTagsByCategoryId(tc.getId(), userDetails));
        }

        PaginatedArtifactList<TagCategory> res = new PaginatedArtifactList<>(
                tagCategoryList, offset, limit, total, url);
        return res;
    }

    public TagCategory createTagCategory(TagCategoryEntity tagCategoryEntity, UserDetails userDetails) throws LottabyteException {
        UUID id = UUID.randomUUID();

        TagCategory tc = new TagCategory(tagCategoryEntity);
        tc.setId(id.toString());
        LocalDateTime now = LocalDateTime.now();
        tc.setCreatedAt(now);
        tc.setModifiedAt(now);
        tc.setCreatedBy(userDetails.getUid());
        tc.setModifiedBy(userDetails.getUid());

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".tag_category (id, name, description, created, creator, modified, modifier, history_start, history_end) VALUES (?,?,?,?,?,?,?,?,?)",
                id, tagCategoryEntity.getName(), tagCategoryEntity.getDescription(), tc.getCreatedAt(), tc.getCreatedBy(),
                tc.getModifiedAt(), tc.getModifiedBy(), now, now);

        return tc;
    }

    public TagCategory updateTagCategory(String tagCategoryId, UpdatableTagCategoryEntity tagCategoryEntity, UserDetails userDetails) throws LottabyteException {
        TagCategory tc = new TagCategory(tagCategoryEntity);
        tc.setId(tagCategoryId);

        LocalDateTime now = LocalDateTime.now();
        tc.setModifiedAt(now);
        tc.setModifiedBy(userDetails.getUid());

        jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".tag_category SET name=?,description=?,modified=?,modifier=? WHERE id=?",
                tc.getEntity().getName(), tc.getEntity().getDescription(), tc.getModifiedAt(), userDetails.getUid(), UUID.fromString(tagCategoryId));

        return tc;
    }
}
