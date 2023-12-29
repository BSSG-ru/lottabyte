package ru.bssg.lottabyte.coreapi.repository;

import io.gsonfire.util.Mapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.Entity;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class WorkflowableRepository<T extends ModeledObject<? extends Entity>>
                extends GenericArtifactRepository<T> {

        private String[] workflowFields = { "state", "workflow_task_id", "published_id",
                        "ancestor_draft_id", "published_version_id", "version_id" };

        public WorkflowableRepository(JdbcTemplate jdbcTemplate,
                        String tableName, String[] extFields) {
                super(jdbcTemplate, tableName, extFields);
        }

        public String getIdByAncestorDraftId(String ancestorDraftId, UserDetails userDetails) {
                return jdbcTemplate.queryForList("select id from da_" + userDetails.getTenant() + "." + tableName
                                + " where ancestor_draft_id is not null and ancestor_draft_id = ? and state = ?",
                                String.class,
                                UUID.fromString(ancestorDraftId), ArtifactState.PUBLISHED.name())
                                .stream().findFirst().orElse(null);
        }

        public String getDraftId(String publishedId, UserDetails userDetails) {
                return jdbcTemplate.queryForList("select id from da_" + userDetails.getTenant() + "." + tableName
                                + " where published_id is not null and published_id = ? and state = ?", String.class,
                                UUID.fromString(publishedId), ArtifactState.DRAFT.name())
                                .stream().findFirst().orElse(null);
        }

        public boolean existsById(String id, ArtifactState[] artifactStates, UserDetails userDetails) {
                String suffix = "";
                if (artifactStates != null)
                        suffix = " and state in ("
                                        + String.join(",",
                                                        Arrays.asList(artifactStates).stream().map(x -> "'" + x + "'")
                                                                        .collect(Collectors.toList()))
                                        + ")";
                return jdbcTemplate
                                .queryForObject("SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + "."
                                                + tableName
                                                + " WHERE id = ? " + suffix + ") as exists", Boolean.class,
                                                UUID.fromString((id)));
        }

        public PaginatedArtifactList<T> getVersionsById(String id, Integer offset, Integer limit, String url,
                        UserDetails userDetails) {
                String subQuery = " select * from da_" + userDetails.getTenant() + "." + tableName + "_hist";
                int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM (" + subQuery + ") sq " +
                                "WHERE id = ?", Integer.class, UUID.fromString(id));
                List<T> resources = jdbcTemplate.query(
                                "SELECT * FROM (" + subQuery + ") sq WHERE id = ? order by version_id desc " +
                                                "offset ? limit ?",
                                mapper, UUID.fromString(id), offset, limit);

                return new PaginatedArtifactList<>(resources, offset, limit, total, url + "/" + id + "/versions");
        }

        public T getVersionById(String id, Integer versionId, UserDetails userDetails) {
                List<T> res = jdbcTemplate.query(
                                "SELECT * FROM da_" + userDetails.getTenant() + "." + tableName + " WHERE id=? " +
                                                "and version_id = ?",
                                mapper, UUID.fromString(id), versionId);
                if (res.isEmpty()) {
                        res = jdbcTemplate.query(
                                        "SELECT * FROM da_" + userDetails.getTenant() + "." + tableName
                                                        + "_hist WHERE id=? " +
                                                        "and version_id = ?",
                                        mapper, UUID.fromString(id), versionId);
                }
                return res.stream().findFirst().orElse(null);
        }

        public void setStateById(String id, ArtifactState artifactState, UserDetails userDetails) {
                jdbcTemplate.update(
                                "UPDATE da_" + userDetails.getTenant() + "." + tableName
                                                + " set state = ? where id = ?",
                                artifactState.name(), UUID.fromString(id));
        }

        public String createDraftFromPublished(String publishedId, String draftId, String workflowTaskId,
                        UserDetails userDetails) {
                // UUID newId = UUID.randomUUID();
                UUID newId = draftId != null ? UUID.fromString(draftId) : UUID.randomUUID();

                jdbcTemplate.update(
                                "INSERT INTO da_" + userDetails.getTenant() + "." + tableName
                                                + " (id, name, description, "
                                                + (extFields.length > 0
                                                                ? Arrays.asList(extFields).stream().collect(
                                                                                Collectors.joining(",", "", ","))
                                                                : "")
                                                + "state, workflow_task_id, published_id, published_version_id, "
                                                + "created, creator, modified, modifier) "
                                                + "SELECT ?, name, description, "
                                                + (extFields.length > 0
                                                                ? Arrays.asList(extFields).stream().collect(
                                                                                Collectors.joining(",", "", ","))
                                                                : "")
                                                + "?, ?, id, version_id, created, creator, modified, modifier FROM da_"
                                                + userDetails.getTenant() + "."
                                                + tableName + " where id = ?",
                                newId, ArtifactState.DRAFT.toString(),
                                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                                UUID.fromString(publishedId));
                return newId.toString();
        }

        public String publishDraft(String draftId, String publishedId, UserDetails userDetails) {
                String res = null;
                if (publishedId != null) {
                        jdbcTemplate.update(
                                        "UPDATE da_" + userDetails.getTenant() + "." + tableName
                                                        + " tbl SET name = draft.name, description = draft.description, "
                                                        + (extFields.length > 0
                                                                        ? Arrays.asList(extFields).stream()
                                                                                        .map(x -> x + "=draft." + x)
                                                                                        .collect(Collectors.joining(",",
                                                                                                        "", ","))
                                                                        : "")
                                                        + " ancestor_draft_id = draft.id, modified = draft.modified, modifier = draft.modifier "
                                                        + " from (select * FROM da_" + userDetails.getTenant() + "."
                                                        + tableName
                                                        + " where id = ?) as draft where tbl.id = ? and draft.id = ?",
                                        UUID.fromString(draftId), UUID.fromString(publishedId),
                                        UUID.fromString(draftId));
                        res = publishedId;
                } else {
                        UUID newId = UUID.randomUUID();
                        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + "." + tableName
                                        + " (id, name, description, "
                                        + (extFields.length > 0
                                                        ? Arrays.asList(extFields).stream()
                                                                        .collect(Collectors.joining(",", "", ","))
                                                        : "")
                                        + "state, workflow_task_id, "
                                        + "published_id, published_version_id, ancestor_draft_id, created, creator, modified, modifier) "
                                        + "SELECT ?, name, description, "
                                        + (extFields.length > 0
                                                        ? Arrays.asList(extFields).stream()
                                                                        .collect(Collectors.joining(",", "", ","))
                                                        : "")
                                        + "?, ?, ?, ?, ?, created, creator, modified, modifier "
                                        + "FROM da_" + userDetails.getTenant() + "." + tableName + " where id = ?",
                                        newId, ArtifactState.PUBLISHED.toString(), null, null, null,
                                        UUID.fromString(draftId), UUID.fromString(draftId));
                        res = newId.toString();
                }
                jdbcTemplate.update(
                                "UPDATE da_" + userDetails.getTenant() + "." + tableName
                                                + " set state = ? where id = ?",
                                ArtifactState.DRAFT_HISTORY.toString(), UUID.fromString(draftId));
                return res;
        }

}
