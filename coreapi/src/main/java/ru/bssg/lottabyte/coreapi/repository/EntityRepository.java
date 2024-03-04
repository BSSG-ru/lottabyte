package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;


import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.dal.FlatItemRowMapper;
import ru.bssg.lottabyte.core.model.*;

import java.time.LocalDateTime;

import ru.bssg.lottabyte.core.model.reference.ReferenceType;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.ui.model.gojs.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.model.dataentity.*;
import ru.bssg.lottabyte.core.model.relation.ParentRelation;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.util.Constants;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class EntityRepository extends WorkflowableRepository<DataEntity> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = { "entity_folder_id" };

    public EntityRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.entity.name(), extFields);
        super.setMapper(new EntityRepository.DataEntityRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    private static class FlatDataEntityRowMapper extends FlatItemRowMapper<FlatDataEntity> {

        public FlatDataEntityRowMapper() {
            super(FlatDataEntity::new);
        }

        @Override
        public FlatDataEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatDataEntity fde = super.mapRow(rs, rowNum);
            fde.setEntityFolderId(rs.getString("entity_folder_id"));
            return fde;
        }
    }

    public static class ParentRelationRowMapper implements RowMapper<ParentRelation> {
        @Override
        public ParentRelation mapRow(ResultSet rs, int rowNum) throws SQLException {
            ParentRelation parentRelation = new ParentRelation();
            parentRelation.setId(rs.getString("id"));
            parentRelation.setName(rs.getString("name"));
            parentRelation.setArtifactType(ArtifactType.entity_folder.toString());
            parentRelation.setUrl(".../v1/entities/folders/" + parentRelation.getId() + "?include_children=true");
            parentRelation.setHasChildren(rs.getBoolean("has_children"));
            return parentRelation;
        }
    }

    public static class DataEntityAttributeRowMapper implements RowMapper<DataEntityAttribute> {
        @Override
        public DataEntityAttribute mapRow(ResultSet rs, int rowNum) throws SQLException {
            DataEntityAttributeEntity dataEntityAttributeEntity = new DataEntityAttributeEntity();
            dataEntityAttributeEntity.setName(rs.getString("name"));
            dataEntityAttributeEntity.setDescription(rs.getString("description"));
            dataEntityAttributeEntity.setEnumerationId(rs.getString("enumeration_id"));
            dataEntityAttributeEntity.setAttributeType(dataEntityAttributeTypeMap(rs.getString("attribute_type")));
            dataEntityAttributeEntity.setEntityId(rs.getString("entity_id"));
            dataEntityAttributeEntity.setAttributeId(rs.getString("attribute_id"));
            dataEntityAttributeEntity.setIsPk(rs.getBoolean("is_pk"));

            Metadata metadata = new Metadata();
            metadata.setId(rs.getString("id"));
            metadata.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            metadata.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            metadata.setCreatedBy(rs.getString("creator"));
            metadata.setName(rs.getString("name"));
            metadata.setVersionId(rs.getInt("version_id"));
            metadata.setModifiedBy(rs.getString("modifier"));
            metadata.setArtifactType(dataEntityAttributeEntity.getArtifactType().toString());

            return new DataEntityAttribute(dataEntityAttributeEntity, metadata);
        }
    }

    public static class FlatDataEntityAttributeRowMapper implements RowMapper<FlatDataEntityAttribute> {
        @Override
        public FlatDataEntityAttribute mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatDataEntityAttribute dataEntityAttributeEntity = new FlatDataEntityAttribute();
            dataEntityAttributeEntity.setName(rs.getString("name"));
            dataEntityAttributeEntity.setDescription(rs.getString("description"));
            dataEntityAttributeEntity.setAttributeType(dataEntityAttributeTypeMap(rs.getString("attribute_type")));
            dataEntityAttributeEntity.setEntityId(rs.getString("entity_id"));
            dataEntityAttributeEntity.setAttributeId(rs.getString("attribute_id"));
            dataEntityAttributeEntity.setId(rs.getString("id"));
            dataEntityAttributeEntity.setIsPk(rs.getBoolean("is_pk"));

            return dataEntityAttributeEntity;
        }
    }

    static class DataEntityFolderRowMapper implements RowMapper<DataEntityFolder> {
        @Override
        public DataEntityFolder mapRow(ResultSet rs, int rowNum) throws SQLException {
            DataEntityFolderEntity dataEntityFolderEntity = new DataEntityFolderEntity();
            dataEntityFolderEntity.setParentId(rs.getString("parent_id"));
            dataEntityFolderEntity.setName(rs.getString("name"));
            dataEntityFolderEntity.setDescription(rs.getString("description"));

            Metadata metadata = new Metadata();
            metadata.setId(rs.getString("id"));
            metadata.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            metadata.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            metadata.setCreatedBy(rs.getString("creator"));
            metadata.setModifiedBy(rs.getString("modifier"));
            metadata.setArtifactType(dataEntityFolderEntity.getArtifactType().toString());

            return new DataEntityFolder(dataEntityFolderEntity, metadata);
        }
    }

    static class DataEntityRowMapper implements RowMapper<DataEntity> {
        @Override
        public DataEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            DataEntityEntity dataEntityEntity = new DataEntityEntity();
            dataEntityEntity.setEntityFolderId(rs.getString("entity_folder_id"));
            dataEntityEntity.setName(rs.getString("name"));
            dataEntityEntity.setDescription(rs.getString("description"));
            dataEntityEntity.setRoles(rs.getString("roles"));

            return new DataEntity(dataEntityEntity, new WorkflowableMetadata(rs, dataEntityEntity.getArtifactType()));
        }
    }

    public boolean existEntitiesInSystem(String systemId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant()
                        + ".entity_to_system WHERE system_id = ?) as exists",
                Boolean.class, UUID.fromString(systemId));
    }

    public DataEntityAttribute getEntityAttributeById(String entityAttributeId, UserDetails userDetails) {
        List<DataEntityAttribute> dataEntityAttributeList = jdbcTemplate.query("SELECT * \n" +
                "FROM da_" + userDetails.getTenant() + ".\"entity_attribute\"  " +
                "where id = ?;",
                new DataEntityAttributeRowMapper(), UUID.fromString(entityAttributeId));

        return dataEntityAttributeList.stream().findFirst().orElse(null);
    }

    public List<DataEntityAttribute> getEntityAttributeListByEntityId(String entityId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * \n" +
                "FROM da_" + userDetails.getTenant() + ".entity_attribute  " +
                "where entity_id = ?;",
                new DataEntityAttributeRowMapper(), UUID.fromString(entityId));
    }

    public PaginatedArtifactList<DataEntityAttribute> getEntityAttributesWithPaging(String entityId, Integer offset,
            Integer limit, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject(
                "SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".\"entity_attribute\" where entity_id = ?",
                Integer.class, UUID.fromString(entityId));
        String query = "SELECT * FROM da_" + userDetails.getTenant()
                + ".\"entity_attribute\" where entity_id = ? offset ? limit ? ";
        List<DataEntityAttribute> dataEntityAttributeList = jdbcTemplate.query(query,
                new DataEntityAttributeRowMapper(), UUID.fromString(entityId), offset, limit);

        PaginatedArtifactList<DataEntityAttribute> paginatedArtifactList = new PaginatedArtifactList<>(
                dataEntityAttributeList, offset, limit, total, "/v1/entities/" + entityId + "/attributes");
        return paginatedArtifactList;
    }

    public static DataEntityAttributeType dataEntityAttributeTypeMap(String attributeType) {
        DataEntityAttributeType dataEntityAttributeType = null;
        try {
            dataEntityAttributeType = DataEntityAttributeType.valueOf(attributeType);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return dataEntityAttributeType;
    }

    public String createEntityAttribute(String entityId,
            UpdatableDataEntityAttributeEntity newDataEntityAttributeEntity, UserDetails userDetails) {
        UUID uuidForEntityAttribute = java.util.UUID.randomUUID();
        String attributeId = newDataEntityAttributeEntity.getAttributeId();
        if (attributeId == null || attributeId.isEmpty())
            attributeId = uuidForEntityAttribute.toString();

        Timestamp ts = new Timestamp(new java.util.Date().getTime());

        if (newDataEntityAttributeEntity.getIsPk() != null && newDataEntityAttributeEntity.getIsPk()) {
            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".entity_attribute SET is_pk=false, modified=?, modifier=? WHERE entity_id=? AND is_pk",
                    ts, userDetails.getUid(), entityId == null ? null : UUID.fromString(entityId));
        }

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".\"entity_attribute\"\n" +
                "(id, attribute_id, \"name\", description, enumeration_id, entity_id, attribute_type, created, creator, modified, modifier, is_pk)\n"
                +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                uuidForEntityAttribute, UUID.fromString(attributeId),
                newDataEntityAttributeEntity.getName(),
                newDataEntityAttributeEntity.getDescription(),
                (newDataEntityAttributeEntity.getEnumerationId() != null
                        ? UUID.fromString(newDataEntityAttributeEntity.getEnumerationId())
                        : null),
                (entityId != null ? UUID.fromString(entityId) : null),
                newDataEntityAttributeEntity.getAttributeType().name(),
                ts, userDetails.getUid(), ts, userDetails.getUid(), newDataEntityAttributeEntity.getIsPk());
        return uuidForEntityAttribute.toString();
    }

    public Boolean existEntityAttributeByName(String entityId, String entityAttributeName, String currentAttributeId,
            UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant()
                        + ".entity_attribute WHERE entity_id = ? and name = ? " +
                        (currentAttributeId != null ? " and id <> ? " : "") +
                        ") as exists",
                Boolean.class,
                (currentAttributeId != null
                        ? new Object[] { UUID.fromString(entityId), entityAttributeName,
                                UUID.fromString(currentAttributeId) }
                        : new Object[] { UUID.fromString(entityId), entityAttributeName }));
    }

    public Boolean existEntityAttributeById(String id, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant()
                        + ".entity_attribute WHERE id = ?) as exists",
                Boolean.class, UUID.fromString(id));
    }

    public void patchEntityAttribute(String entityAttributeId,
            UpdatableDataEntityAttributeEntity newDataEntityAttributeEntity, UserDetails userDetails)
            throws LottabyteException {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".\"entity_attribute\" SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));
        if (newDataEntityAttributeEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(newDataEntityAttributeEntity.getName());
        }
        if (newDataEntityAttributeEntity.getDescription() != null) {
            sets.add("description = ?");
            params.add(newDataEntityAttributeEntity.getDescription());
        }
        if (newDataEntityAttributeEntity.getAttributeType() != null) {
            sets.add("attribute_type = ?");
            params.add(newDataEntityAttributeEntity.getAttributeType().name());
        }
        if (newDataEntityAttributeEntity.getEntityId() != null) {
            sets.add("entity_id = ?");
            params.add(UUID.fromString(newDataEntityAttributeEntity.getEntityId()));
        }
        if (newDataEntityAttributeEntity.getEnumerationId() != null) {
            sets.add("enumeration_id = ?");
            params.add(UUID.fromString(newDataEntityAttributeEntity.getEnumerationId()));
        }
        if (newDataEntityAttributeEntity.getIsPk() != null) {
            sets.add("is_pk = ?");
            params.add(newDataEntityAttributeEntity.getIsPk());
        }
        if (!sets.isEmpty()) {
            Timestamp ts = new Timestamp(new java.util.Date().getTime());
            if (newDataEntityAttributeEntity.getIsPk() != null && newDataEntityAttributeEntity.getIsPk()) {
                jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".entity_attribute SET is_pk=false, modified=?, modifier=? WHERE entity_id=(SELECT entity_id FROM da_"
                        + userDetails.getTenant() + ".entity_attribute WHERE id=?) AND is_pk",
                        ts, userDetails.getUid(), UUID.fromString(entityAttributeId));
            }

            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(entityAttributeId));
            jdbcTemplate.update(query, params.toArray());
        }
    }

    public void deleteEntityAttributeToSampleProperty(String entityAttributeId, UserDetails userDetails) {
        String query = "DELETE FROM da_" + userDetails.getTenant() + ".entity_attribute_to_sample_property " +
                "WHERE entity_attribute_id = ?";
        jdbcTemplate.update(query, UUID.fromString(entityAttributeId));
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void deleteEntityAttribute(String entityAttributeId, Boolean force, UserDetails userDetails) {

        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".tag_to_artifact WHERE artifact_id=?",
                UUID.fromString(entityAttributeId));

        String query = "DELETE FROM da_" + userDetails.getTenant() + ".entity_attribute " +
                "WHERE id = ?";
        jdbcTemplate.update(query, UUID.fromString(entityAttributeId));

        if (force)
            deleteEntityAttributeToSampleProperty(entityAttributeId, userDetails);
    }

    public Boolean existEntityFolderById(String entityId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant() + ".entity_folder WHERE id = ?) as exists",
                Boolean.class, UUID.fromString(entityId));
    }

    public DataEntityFolder getDataEntityFolderById(String entityFolderId, Boolean includeChildren,
            UserDetails userDetails) {
        String query = "SELECT id, \"name\", description, parent_id, created, creator, modified, modifier, EXISTS(SELECT id FROM da_"
                + userDetails.getTenant() + ".entity_folder ef2 where ef2.parent_id = ef1.id) as has_children  " +
                "FROM da_" + userDetails.getTenant() + ".entity_folder ef1 " +
                "where id = ? ";
        if (includeChildren)
            query = query + " AND parent_id is null";

        DataEntityFolder dataEntityFolder = jdbcTemplate.query(query,
                new DataEntityFolderRowMapper(), UUID.fromString(entityFolderId)).stream().findFirst().orElse(null);

        if (includeChildren && dataEntityFolder != null) {
            List<ParentRelation> children = getChildrenFolderListByParentId(dataEntityFolder.getMetadata().getId(),
                    userDetails);
            if (!children.isEmpty()) {
                dataEntityFolder.getEntity().setChildren(children);
            }
        }
        return dataEntityFolder;
    }

    public List<DataEntityFolder> getDataEntityFolderWithAllChildrenById(String dataEntityFolderId,
            UserDetails userDetails) {
        String query = "with recursive cte as ( \n" +
                "select id, \"name\", description, parent_id, created, creator, modified, modifier, 1 as level from da_"
                + userDetails.getTenant() + ".entity_folder ef where id = ?\n" +
                "union all select ef_result.id, ef_result.\"name\", ef_result.description, ef_result.parent_id, ef_result.created, ef_result.creator, ef_result.modified, ef_result.modifier, level + 1 from cte c \n"
                +
                "join da_" + userDetails.getTenant() + ".entity_folder ef_result on ef_result.parent_id = c.id \n" +
                ") \n" +
                "select * from cte order by level desc";

        return jdbcTemplate.query(query,
                new DataEntityFolderRowMapper(), UUID.fromString(dataEntityFolderId));
    }

    public DataEntityFolder getDataEntityFolderByNameAndParent(String parentId, String dataEntityFolderName,
            Boolean includeChildren, UserDetails userDetails) throws LottabyteException {
        String query = "SELECT id, \"name\", description, parent_id, created, creator, modified, modifier, EXISTS(SELECT id FROM da_"
                + userDetails.getTenant() + ".entity_folder ef2 where ef2.parent_id = ef1.id) as has_children  " +
                "FROM da_" + userDetails.getTenant() + ".entity_folder ef1 " +
                "where name = ? and parent_id = ?";

        DataEntityFolder dataEntityFolder = jdbcTemplate.query(query,
                new DataEntityFolderRowMapper(), dataEntityFolderName, UUID.fromString(parentId)).stream().findFirst()
                .orElse(null);

        if (includeChildren && dataEntityFolder != null) {
            List<ParentRelation> children = getChildrenFolderListByParentId(dataEntityFolder.getMetadata().getId(),
                    userDetails);
            if (!children.isEmpty()) {
                dataEntityFolder.getEntity().setChildren(children);
            }
        }
        return dataEntityFolder;
    }

    public List<String> getSystemIdsForDataEntity(String dataEntityId, UserDetails userDetails) {
        return jdbcTemplate.queryForList(
                "SELECT system_id FROM da_" + userDetails.getTenant() + ".entity_to_system WHERE entity_id=?",
                String.class, UUID.fromString(dataEntityId));
    }

    public Boolean dataEntityNameExists(String name, String entityFolderId, String thisEntityId,
            UserDetails userDetails) {
        if (thisEntityId == null)
            return jdbcTemplate.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM da_" + userDetails.getTenant()
                            + ".entity WHERE state = ? AND name=? AND entity_folder_id=?)",
                    Boolean.class, ArtifactState.PUBLISHED.name(), name,
                    entityFolderId == null ? null : UUID.fromString(entityFolderId));
        else
            return jdbcTemplate.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM da_" + userDetails.getTenant()
                            + ".entity WHERE state = ? AND name=? AND entity_folder_id=? AND id<>?)",
                    Boolean.class, ArtifactState.PUBLISHED.name(), name,
                    entityFolderId == null ? null : UUID.fromString(entityFolderId),
                    UUID.fromString(thisEntityId));
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public String createDataEntity(DataEntityEntity dataEntity, String workflowTaskId, UserDetails userDetails) {
        UUID newId = dataEntity.getId() != null ? UUID.fromString(dataEntity.getId()) : UUID.randomUUID();

        Timestamp ts = new Timestamp(new java.util.Date().getTime());

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                + ".entity (id, name, description, entity_folder_id, state, workflow_task_id, created, creator, modified, modifier, roles) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                newId, dataEntity.getName(), dataEntity.getDescription(),
                dataEntity.getEntityFolderId() != null ? UUID.fromString(dataEntity.getEntityFolderId()) : null,
                ArtifactState.DRAFT.toString(),
                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                ts, userDetails.getUid(), ts, userDetails.getUid(), dataEntity.getRoles());

        if (dataEntity.getSystemIds() != null && !dataEntity.getSystemIds().isEmpty()) {
            dataEntity.getSystemIds().forEach(x -> addEntityToSystem(newId.toString(), x, userDetails));
        }

        return newId.toString();
    }

    public String addEntityToSystem(String entityId, String systemId, UserDetails userDetails) {
        UUID newId = java.util.UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        String query = "INSERT INTO da_" + userDetails.getTenant() + ".entity_to_system " +
                "(id, entity_id, system_id, description, created, creator, modified, modifier) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(query, newId, UUID.fromString(entityId), UUID.fromString(systemId), null,
                ts, userDetails.getUid(), ts, userDetails.getUid());
        return newId.toString();
    }

    public void removeEntityFromSystem(String entityId, String systemId, UserDetails userDetails) {
        String query = "DELETE FROM da_" + userDetails.getTenant() + ".entity_to_system " +
                "WHERE entity_id = ? and system_id = ?";
        jdbcTemplate.update(query, UUID.fromString(entityId), UUID.fromString(systemId));
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public DataEntity updateDataEntity(String dataEntityId, UpdatableDataEntityEntity dataEntityEntity,
            UserDetails userDetails) {
        DataEntity de = new DataEntity(dataEntityEntity);
        de.setId(dataEntityId);

        LocalDateTime now = LocalDateTime.now();
        de.setModifiedAt(now);
        de.setModifiedBy(userDetails.getUid());

        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (dataEntityEntity.getName() != null) {
            sets.add("name=?");
            args.add(dataEntityEntity.getName());
        }
        if (dataEntityEntity.getDescription() != null) {
            sets.add("description=?");
            args.add(dataEntityEntity.getDescription());
        }
        if (dataEntityEntity.getEntityFolderId() != null) {
            sets.add("entity_folder_id=?");
            args.add(UUID.fromString(dataEntityEntity.getEntityFolderId()));
        }
        if (dataEntityEntity.getRoles() != null) {
            sets.add("roles = ?");
            args.add(dataEntityEntity.getRoles());
        }
        if (sets.size() > 0) {
            sets.add("modified=?");
            sets.add("modifier=?");
            args.add(de.getModifiedAt());
            args.add(de.getModifiedBy());
            args.add(UUID.fromString(de.getId()));

            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".entity SET " + StringUtils.join(sets, ", ")
                    + " WHERE id=?", args.toArray());
        }

        if (dataEntityEntity.getSystemIds() != null) {
            List<String> currSystemIds = getSystemIdsForDataEntity(de.getId(), userDetails);

            for (String sid : currSystemIds) {
                if (!dataEntityEntity.getSystemIds().contains(sid))
                    jdbcTemplate.update(
                            "DELETE FROM da_" + userDetails.getTenant()
                                    + ".entity_to_system WHERE entity_id=? AND system_id=?",
                            UUID.fromString(de.getId()),
                            UUID.fromString(sid));
            }
            for (String sid : dataEntityEntity.getSystemIds()) {
                if (!currSystemIds.contains(sid))
                    jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                            + ".entity_to_system (id, entity_id, system_id, created, creator, modified, modifier) VALUES (?,?,?,?,?,?,?)",
                            UUID.randomUUID(), UUID.fromString(de.getId()), UUID.fromString(sid), now,
                            userDetails.getUid(), now, userDetails.getUid());
            }
        }

        return de;
    }

    public void deleteDataEntityById(String dataEntityId, UserDetails userDetails) {
        /*
         * jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() +
         * ".entity_attribute_to_sample_property WHERE entity_attribute_id IN ("
         * + "SELECT id FROM da_" + userDetails.getTenant() +
         * ".entity_attribute WHERE entity_id=?"
         * + ") OR entity_sample_property_id IN ("
         * + "SELECT esp.id FROM da_" + userDetails.getTenant() +
         * ".entity_sample_property esp JOIN da_" + userDetails.getTenant() +
         * ".entity_sample es ON esp.entity_sample_id=es.id WHERE es.entity_id=?"
         * + ")", UUID.fromString(dataEntityId), UUID.fromString(dataEntityId));
         * 
         * jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() +
         * ".entity_sample_body WHERE entity_sample_id IN (SELECT id FROM da_" +
         * userDetails.getTenant() + ".entity_sample WHERE entity_id=?)",
         * UUID.fromString(dataEntityId));
         * jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() +
         * ".entity_sample_property WHERE entity_sample_id IN (SELECT id FROM da_" +
         * userDetails.getTenant() + ".entity_sample WHERE entity_id=?)",
         * UUID.fromString(dataEntityId));
         * jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() +
         * ".entity_sample WHERE entity_id=?", UUID.fromString(dataEntityId));
         * jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() +
         * ".entity_query WHERE entity_id=?", UUID.fromString(dataEntityId));
         */
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".entity_attribute WHERE entity_id=?",
                UUID.fromString(dataEntityId));
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".entity_to_system WHERE entity_id=?",
                UUID.fromString(dataEntityId));
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".entity WHERE id=?",
                UUID.fromString(dataEntityId));
    }

    public List<ParentRelation> getChildrenFolderListByParentId(String dataEntityFolderId, UserDetails userDetails) {
        return jdbcTemplate.query(
                "SELECT id, \"name\", description, parent_id, created, creator, modified, modifier, EXISTS(SELECT id FROM da_"
                        + userDetails.getTenant() + ".entity_folder ef2 where ef2.parent_id = ef1.id) as has_children  "
                        +
                        "FROM da_" + userDetails.getTenant() + ".entity_folder ef1 " +
                        "where parent_id = ?;",
                new ParentRelationRowMapper(), UUID.fromString(dataEntityFolderId));
    }

    public List<DataEntityFolder> getRootFolders(Boolean includeChildren, UserDetails userDetails) {
        List<DataEntityFolder> dataEntityFolderList = new ArrayList<>();

        String query = "SELECT id, \"name\", description, parent_id, created, creator, modified, modifier, EXISTS(SELECT id FROM da_"
                + userDetails.getTenant() + ".entity_folder ef2 where ef2.parent_id = ef1.id) as has_children " +
                "FROM da_" + userDetails.getTenant() + ".entity_folder ef1 \n" +
                "where parent_id is null";
        jdbcTemplate.query(
                query,
                rs -> {
                    DataEntityFolder dataEntityFolder = getDataEntityFolderById(rs.getString("id"), includeChildren,
                            userDetails);
                    if (dataEntityFolder != null && dataEntityFolder.getId() != null)
                        dataEntityFolderList.add(dataEntityFolder);
                });
        if (dataEntityFolderList.size() > 1)
            dataEntityFolderList.sort(Comparator.comparing(object -> object.getEntity().getName()));

        return dataEntityFolderList;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void deletionFolders(List<DataEntityFolder> idList, UserDetails userDetails) {
        for (DataEntityFolder entityFolder : idList) {
            String query = "DELETE FROM da_" + userDetails.getTenant() + ".entity_folder " +
                    "WHERE id = ?";
            jdbcTemplate.update(query, UUID.fromString(entityFolder.getId()));
        }
    }

    public String createFolder(UpdatableDataEntityFolderEntity newDataEntityFolderEntity, UserDetails userDetails) {
        UUID uuidForFolder = java.util.UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".entity_folder\n" +
                "(id, \"name\", description, parent_id, created, creator, modified, modifier)\n" +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?);",
                uuidForFolder, newDataEntityFolderEntity.getName(), newDataEntityFolderEntity.getDescription(),
                UUID.fromString(newDataEntityFolderEntity.getParentId()),
                new Timestamp(new java.util.Date().getTime()), userDetails.getUid(),
                new Timestamp(new java.util.Date().getTime()), userDetails.getUid());

        return uuidForFolder.toString();
    }

    public void patchFolder(String folderId, UpdatableDataEntityFolderEntity dataEntityFolderEntity,
            UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".\"entity_folder\" SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));
        if (dataEntityFolderEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(dataEntityFolderEntity.getName());
        }
        if (dataEntityFolderEntity.getDescription() != null) {
            sets.add("description = ?");
            params.add(dataEntityFolderEntity.getDescription());
        }
        if (dataEntityFolderEntity.getParentId() != null) {
            sets.add("parent_id = ?");
            params.add(UUID.fromString(dataEntityFolderEntity.getParentId()));
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(folderId));
            jdbcTemplate.update(query, params.toArray());
        }
    }

    public Boolean existsDataEntityByEntityFolderId(String dataEntityFolderId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant()
                        + ".entity WHERE entity_folder_id = ?) as exists",
                Boolean.class, UUID.fromString(dataEntityFolderId));
    }

    public List<String> getChildrenIds(List<String> childrenList, String dataEntityFolderId, UserDetails userDetails) {
        String query = "SELECT id, parent_id FROM da_" + userDetails.getTenant() + ".entity_folder " +
                "where parent_id = ?;";
        jdbcTemplate.query(
                query,
                rs -> {
                    childrenList.add(rs.getString("id"));
                    getChildrenIds(childrenList, rs.getString("id"), userDetails);
                },
                UUID.fromString(dataEntityFolderId));
        return childrenList;
    }

    public Boolean entityAttributesExistAndBelongToEntity(List<String> attributeIds, String entityId,
            UserDetails userDetails) {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM da_" + userDetails.getTenant()
                + ".entity_attribute WHERE entity_id=? AND id IN ('" + StringUtils.join(attributeIds, "','") + "')",
                Integer.class, UUID.fromString(entityId));

        return c == attributeIds.size();
    }

    public SearchResponse<FlatDataEntity> searchDataEntities(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchColumn[] whereSearchableColumns = Arrays.stream(searchableColumns)
                .filter(x -> !x.getColumn().equals("attribute_type")).toArray(SearchColumn[]::new);

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, whereSearchableColumns, null, true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "select entity.*, true as has_access from da_" + userDetails.getTenant() + ".entity ";
        if (userDetails.getStewardId() != null) {
            String hasAccessJoinQuery = QueryHelper.getJoinQuery(ArtifactType.entity, userDetails);
            subQuery = "SELECT entity.*, case when acc.id is null then false else true end as has_access FROM da_"
                    + userDetails.getTenant() + ".entity " +
                    " left join (select entity.id from da_" + userDetails.getTenant() + ".entity " + hasAccessJoinQuery
                    + ") acc on entity.id = acc.id ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward())
                subQuery = "select entity.*, true as has_access from da_" + userDetails.getTenant() + ".entity "
                        + hasAccessJoinQuery;
        }
        subQuery = "SELECT sq.*, wft.workflow_state FROM (" + subQuery + ") as sq left join da_"
                + userDetails.getTenant() + ".workflow_task wft "
                + " on sq.workflow_task_id = wft.id ";
        subQuery = "SELECT distinct sq.*, s.systems, d.domains, t.tags from (" + subQuery + ") sq "
                + ((userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) ? " join ("
                        + "select e2s.entity_id, string_agg(s.name, ',') as systems from da_" + userDetails.getTenant()
                        + ".system s "
                        + "join da_" + userDetails.getTenant()
                        + ".entity_to_system e2s on e2s.system_id = s.id and e2s.system_id IN (SELECT system_id FROM da_"
                        + userDetails.getTenant() + ".system_to_domain WHERE domain_id IN ('"
                        + StringUtils.join(userDetails.getUserDomains(), "','") + "')) group by e2s.entity_id"
                        + ") s on s.entity_id = sq.id "
                        : " left join ("
                                + "select e2s.entity_id, string_agg(s.name, ',') as systems from da_"
                                + userDetails.getTenant() + ".system s "
                                + "join da_" + userDetails.getTenant()
                                + ".entity_to_system e2s on e2s.system_id = s.id group by e2s.entity_id"
                                + ") s on s.entity_id = sq.id ")

                + " left join ("
                + "select e2s.entity_id, string_agg(d.name, ',') as domains from da_" + userDetails.getTenant()
                + ".domain d "
                + "join da_" + userDetails.getTenant() + ".system_to_domain s2d on s2d.domain_id = d.id "
                + "join da_" + userDetails.getTenant() + ".system s on s2d.system_id = s.id "
                + "join da_" + userDetails.getTenant()
                + ".entity_to_system e2s on e2s.system_id = s.id group by e2s.entity_id "
                + ") d on d.entity_id = sq.id "

                + "left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id ";

        String queryForItems = "SELECT * FROM (" + subQuery + ") as tbl1 " + join + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatDataEntity> flatItems = jdbcTemplate.query(queryForItems, new FlatDataEntityRowMapper(),
                whereValues.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") tbl1 " + join + where;
        Integer total = jdbcTemplate.queryForObject(queryForTotal, Integer.class, whereValues.toArray());

        SearchResponse<FlatDataEntity> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public Boolean hasAccessToEntity(String entityId, UserDetails userDetails) {
        return userDetails.getStewardId() == null ? true
                : jdbcTemplate.queryForObject(
                        "SELECT EXISTS(SELECT entity.ID FROM da_" + userDetails.getTenant() + ".entity " +
                                QueryHelper.getJoinQuery(ArtifactType.entity, userDetails)
                                + " and entity.id = ?) as exists",
                        Boolean.class,
                        UUID.fromString(entityId));
    }

    public SearchResponse<FlatDataEntityAttribute> searchAttributesByEntityId(SearchRequestWithJoin searchRequest,
            String entityId, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin,
            UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        if (where.isEmpty()) {
            where = " where tbl1.entity_id = '" + entityId + "'";
        } else {
            where = where + " AND tbl1.entity_id = '" + entityId + "'";
        }

        Boolean hasAccessToEntity = hasAccessToEntity(entityId, userDetails);
        String subQuery = "SELECT ea.*, eat.name AS attribute_type_name, t.tags FROM da_" + userDetails.getTenant()
                + ".entity_attribute ea LEFT JOIN da_" + userDetails.getTenant()
                + ".entity_attribute_type eat ON ea.attribute_type=eat.id "
                + "left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=ea.id ";
        String queryForItems = "SELECT * FROM (" + subQuery + ") as tbl1 " + join + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatDataEntityAttribute> items = jdbcTemplate.query(queryForItems, new FlatDataEntityAttributeRowMapper(),
                whereValues.toArray());

        String queryForTotal = "SELECT COUNT(tbl1.id) FROM (" + subQuery + ") as tbl1 " + join + where;
        final int[] count = { 0 };
        jdbcTemplate.query(
                queryForTotal,
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        count[0] = rs.getInt("count");
                    }
                },
                whereValues.toArray());

        int total = count[0];

        SearchResponse<FlatDataEntityAttribute> res = new SearchResponse<FlatDataEntityAttribute>(total, searchRequest.getLimit(), searchRequest.getOffset(), items);

        return res;
    }

    public SearchResponse<FlatDataEntityAttribute> searchAttributes(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();


        String subQuery = "SELECT * FROM da_" + userDetails.getTenant() + ".entity_attribute";

        if (userDetails.getStewardId() != null) {
            String hasAccessJoinQuery = QueryHelper.getJoinQuery(ArtifactType.entity_attribute, userDetails);
            subQuery = "SELECT entity_attribute.*, case when acc.id is null then false else true end as has_access FROM da_"
                    + userDetails.getTenant() + ".entity_attribute " +
                    " left join (select entity_attribute.id from da_" + userDetails.getTenant() + ".entity_attribute "
                    + hasAccessJoinQuery + ") acc on entity_attribute.id = acc.id ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward())
                subQuery = "select entity_attribute.*, true as has_access from da_" + userDetails.getTenant()
                        + ".entity_attribute "
                        + hasAccessJoinQuery;
        }
        String queryForItems = "SELECT * FROM (" + subQuery + ") as tbl1 " + join + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatDataEntityAttribute> items = jdbcTemplate.query(queryForItems, new FlatDataEntityAttributeRowMapper(),
                whereValues.toArray());

        String queryForTotal = "SELECT COUNT(tbl1.id) FROM (" + subQuery + ") as tbl1 " + join + where;
        final int[] count = { 0 };
        jdbcTemplate.query(
                queryForTotal,
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        count[0] = rs.getInt("count");
                    }
                },
                whereValues.toArray());

        int total = count[0];

        SearchResponse<FlatDataEntityAttribute> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), items);

        return res;
    }

    public List<String> getDomainIdsByEntityId(String entityId, UserDetails userDetails) {
        return jdbcTemplate.queryForList("SELECT domain_id FROM da_" + userDetails.getTenant()
                + ".system_to_domain sd JOIN da_" + userDetails.getTenant()
                + ".entity_to_system e2s ON sd.system_id=e2s.system_id WHERE e2s.entity_id=?", String.class,
                UUID.fromString(entityId));
    }

    public SearchResponse<FlatDataEntity> searchDataEntityByDomain(SearchRequestWithJoin searchRequest, String domainId,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        if (where.isEmpty()) {
            where = " where tbl1.id in (select tbl3.entity_id from da_" + userDetails.getTenant()
                    + ".entity_to_system tbl3 where tbl3.system_id in (select tbl4.system_id from da_"
                    + userDetails.getTenant() + ".system_to_domain tbl4 where tbl4.domain_id='" + domainId + "' LIMIT "
                    + Constants.sqlInLimit + "))";
        } else {
            where = where + " AND tbl1.id in (select tbl3.entity_id from da_" + userDetails.getTenant()
                    + ".entity_to_system tbl3 where tbl3.system_id in (select tbl4.system_id from da_"
                    + userDetails.getTenant() + ".system_to_domain tbl4 where tbl4.domain_id='" + domainId + "' LIMIT "
                    + Constants.sqlInLimit + "))";
        }

        String subQuery = "select entity.*, true as has_access from da_" + userDetails.getTenant() + ".entity ";
        if (userDetails.getStewardId() != null) {
            String hasAccessJoinQuery = QueryHelper.getJoinQuery(ArtifactType.entity, userDetails);
            subQuery = "SELECT entity.*, case when acc.id is null then false else true end as has_access FROM da_"
                    + userDetails.getTenant() + ".entity " +
                    " left join (select entity.id from da_" + userDetails.getTenant() + ".entity " + hasAccessJoinQuery
                    + ") acc on entity.id = acc.id ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward())
                subQuery = "select entity.*, true as has_access from da_" + userDetails.getTenant() + ".entity "
                        + hasAccessJoinQuery;
        }
        String queryForItems = "SELECT distinct * FROM (" + subQuery + ") as tbl1 " + join + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatDataEntity> flatItems = jdbcTemplate.query(queryForItems, new FlatDataEntityRowMapper(),
                whereValues.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1 " + join + where;
        final int[] count = { 0 };
        jdbcTemplate.query(
                queryForTotal,
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        count[0] = rs.getInt("count");
                    }
                },
                whereValues.toArray());

        int total = count[0];

        SearchResponse<FlatDataEntity> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public SearchResponse<FlatDataEntity> searchDataEntityByIndicator(SearchRequestWithJoin searchRequest,
            String indicatorId, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin,
            UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        if (where.isEmpty()) {
            where = " where tbl1.id in (select dasset.entity_id from da_" + userDetails.getTenant()
                    + ".data_asset dasset where dasset.id IN (SELECT target_id FROM da_" + userDetails.getTenant()
                    + ".reference where reference_type='"
                    + ReferenceType.INDICATOR_TO_DATA_ASSET.name() + "' and source_id='" + indicatorId + "' LIMIT "
                    + Constants.sqlInLimit + "))";
        } else {
            where = where + " AND tbl1.id in (select dasset.entity_id from da_" + userDetails.getTenant()
                    + ".data_asset dasset where dasset.id IN (SELECT target_id FROM da_" + userDetails.getTenant()
                    + ".reference where reference_type='"
                    + ReferenceType.INDICATOR_TO_DATA_ASSET.name() + "' and source_id='" + indicatorId + "' LIMIT "
                    + Constants.sqlInLimit + "))";
        }

        String subQuery = "select entity.*, true as has_access from da_" + userDetails.getTenant() + ".entity ";
        if (userDetails.getStewardId() != null) {
            String hasAccessJoinQuery = QueryHelper.getJoinQuery(ArtifactType.entity, userDetails);
            subQuery = "SELECT entity.*, case when acc.id is null then false else true end as has_access FROM da_"
                    + userDetails.getTenant() + ".entity " +
                    " left join (select entity.id from da_" + userDetails.getTenant() + ".entity " + hasAccessJoinQuery
                    + ") acc on entity.id = acc.id ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward())
                subQuery = "select entity.*, true as has_access from da_" + userDetails.getTenant() + ".entity "
                        + hasAccessJoinQuery;
        }
        String queryForItems = "SELECT distinct * FROM (" + subQuery + ") as tbl1 " + join + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatDataEntity> flatItems = jdbcTemplate.query(queryForItems, new FlatDataEntityRowMapper(),
                whereValues.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1 " + join + where;
        final int[] count = { 0 };
        jdbcTemplate.query(
                queryForTotal,
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        count[0] = rs.getInt("count");
                    }
                },
                whereValues.toArray());

        int total = count[0];

        SearchResponse<FlatDataEntity> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public SearchResponse<FlatDataEntity> searchDataEntityByBE(SearchRequestWithJoin searchRequest, String beId,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();


        if (where.isEmpty()) {
            where = " where tbl1.id in (select r1.source_id from da_" + userDetails.getTenant()
                    + ".reference r1 where r1.target_id = '" + beId + "' LIMIT " + Constants.sqlInLimit + ")";
        } else {
            where = where + " AND tbl1.id in (select r1.source_id from da_" + userDetails.getTenant()
                    + ".reference r1 where r1.target_id = '" + beId + "' LIMIT " + Constants.sqlInLimit + ")";
        }

        String subQuery = "select entity.*, true as has_access, t.tags, s.systems from da_" + userDetails.getTenant()
                + ".entity ";
        if (userDetails.getStewardId() != null) {
            String hasAccessJoinQuery = QueryHelper.getJoinQuery(ArtifactType.entity, userDetails);
            subQuery = "SELECT entity.*, case when acc.id is null then false else true end as has_access, t.tags, s.systems FROM da_"
                    + userDetails.getTenant() + ".entity " +
                    " left join (select entity.id from da_" + userDetails.getTenant() + ".entity " + hasAccessJoinQuery
                    + ") acc on entity.id = acc.id ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward())
                subQuery = "select entity.*, true as has_access, t.tags, s.systems from da_" + userDetails.getTenant()
                        + ".entity "
                        + hasAccessJoinQuery;
        }

        subQuery += " left join ("
                + "select e2s.entity_id, string_agg(s.name, ',') as systems from da_" + userDetails.getTenant()
                + ".system s "
                + "join da_" + userDetails.getTenant()
                + ".entity_to_system e2s on e2s.system_id = s.id group by e2s.entity_id"
                + ") s on s.entity_id = entity.id";
        subQuery += " left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=entity.id ";

        String queryForItems = "SELECT distinct * FROM (" + subQuery + ") as tbl1 " + join + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatDataEntity> flatItems = jdbcTemplate.query(queryForItems, new FlatDataEntityRowMapper(),
                whereValues.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1 " + join + where;
        final int[] count = { 0 };
        jdbcTemplate.query(
                queryForTotal,
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        count[0] = rs.getInt("count");
                    }
                },
                whereValues.toArray());

        int total = count[0];

        SearchResponse<FlatDataEntity> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public String publishEntityDraft(String draftEntityId, String publishedEntityId, UserDetails userDetails) {
        String res = null;
        if (publishedEntityId != null) {
            jdbcTemplate.update(
                    "UPDATE da_" + userDetails.getTenant()
                            + ".entity e SET name = draft.name, description = draft.description, "
                            + " entity_folder_id = draft.entity_folder_id, roles = draft.roles,"
                            + " ancestor_draft_id = draft.id, modified = draft.modified, modifier = draft.modifier "
                            + " from (select id, name, description, entity_folder_id, modified, modifier, roles FROM da_"
                            + userDetails.getTenant() + ".entity) as draft where e.id = ? and draft.id = ?",
                    UUID.fromString(publishedEntityId), UUID.fromString(draftEntityId));
            res = publishedEntityId;
        } else {
            UUID newId = UUID.randomUUID();
            jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                    + ".entity (id, name, description, entity_folder_id, state, workflow_task_id, "
                    + "published_id, published_version_id, ancestor_draft_id, created, creator, modified, modifier, roles) "
                    + "SELECT ?, name, description, entity_folder_id, ?, ?, ?, ?, ?, created, creator, modified, modifier, roles "
                    + "FROM da_" + userDetails.getTenant() + ".entity where id = ?",
                    newId, ArtifactState.PUBLISHED.toString(), null, null, null,
                    UUID.fromString(draftEntityId), UUID.fromString(draftEntityId));
            res = newId.toString();
        }
        jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".entity set state = ? where id = ?",
                ArtifactState.DRAFT_HISTORY.toString(), UUID.fromString(draftEntityId));
        return res;
    }

    public Boolean allAttributesExist(List<String> attributeIds, UserDetails userDetails) {
        if (attributeIds == null || attributeIds.isEmpty())
            return true;
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM da_" + userDetails.getTenant() + ".entity_attribute WHERE id IN ('"
                        + StringUtils.join(attributeIds, "','") + "')",
                Integer.class);
        return c != null && attributeIds.size() == c;
    }

    public String createEntityDraft(String publishedEntityId, String draftId, String workflowTaskId,
            UserDetails userDetails) {
        UUID newId = draftId != null ? UUID.fromString(draftId) : UUID.randomUUID();

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                + ".entity (id, name, description, entity_folder_id, state, workflow_task_id, published_id, published_version_id, created, creator, modified, modifier, roles) "
                +
                "SELECT ?, name, description, entity_folder_id, ?, ?, id, version_id, created, creator, modified, modifier, roles FROM da_"
                + userDetails.getTenant() + ".entity where id = ?",
                newId, ArtifactState.DRAFT.toString(),
                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                UUID.fromString(publishedEntityId));
        return newId.toString();
    }

    public void copyEntityAttributeToSamplePropertyLinks(String sourceAttributeId, String targetAttributeId,
            UserDetails userDetails) {
        List<String> targetLinksIds = jdbcTemplate.queryForList("select id from da_" + userDetails.getTenant() +
                ".entity_attribute_to_sample_property where entity_attribute_id = ?", String.class,
                UUID.fromString(sourceAttributeId));
        if (targetLinksIds != null && !targetLinksIds.isEmpty()) {
            for (String oldId : targetLinksIds) {
                UUID newId = UUID.randomUUID();
                Timestamp ts = new Timestamp(new java.util.Date().getTime());
                jdbcTemplate.update("insert into da_" + userDetails.getTenant() + ".entity_attribute_to_sample_property " +
                        "(id, entity_attribute_id, entity_sample_property_id, name, description, created, creator, modified, modifier) "
                        + "select ?, ?, entity_sample_property_id, name, description, created, creator, ?, ? from da_"
                        + userDetails.getTenant() + ".entity_attribute_to_sample_property where id = ?",
                        newId, UUID.fromString(targetAttributeId), ts, userDetails.getUid(), UUID.fromString(oldId));
            }
        }
    }

    public List<EntityAttributeType> getEntityAttributeTypes(UserDetails userDetails) {
        List<EntityAttributeType> res = new ArrayList<>();
        jdbcTemplate.query("SELECT id, name FROM da_" + userDetails.getTenant() + ".entity_attribute_type",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        res.add(new EntityAttributeType(DataEntityAttributeType.valueOf(rs.getString("id")),
                                rs.getString("name")));
                    }
                });
        return res;
    }

    public GojsModelData getModel(UserDetails userDetails) {
        GojsModelData res = new GojsModelData();
        List<GojsModelNodeData> nodes = new ArrayList<>();
        List<GojsModelLinkData> links = new ArrayList<>();

        jdbcTemplate.query("SELECT em.*, e.name, (SELECT string_agg(t.name, '%SP%') FROM da_" + userDetails.getTenant()
                + ".tag t JOIN da_" + userDetails.getTenant() + ".tag_to_artifact ta ON t.id=ta.tag_id AND ta.artifact_id=em.entity_id) AS tag_names, (SELECT string_agg(d.name, '%SP%') FROM da_"
                + userDetails.getTenant() + ".domain d JOIN da_" + userDetails.getTenant() + ".system_to_domain sd ON sd.domain_id=d.id JOIN da_"
                + userDetails.getTenant() + ".entity_to_system es ON es.system_id=sd.system_id AND es.entity_id=em.entity_id) AS domain_names FROM da_"
                + userDetails.getTenant() + ".entity_model em JOIN da_" + userDetails.getTenant() + ".entity e ON em.entity_id=e.id WHERE state='PUBLISHED'",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        GojsModelNodeData nd = new GojsModelNodeData();
                        nd.setId(rs.getString("entity_id"));
                        nd.setName(rs.getString("name"));
                        nd.setType("defaultNodeType");
                        nd.setZOrder(1);
                        nd.setText(rs.getString("name"));
                        nd.setIsGroup(true);
                        nd.setGroup("");
                        nd.setParentId("");
                        nd.setLoc(rs.getString("loc"));
                        nd.setArtifactType(ArtifactType.entity.getText());
                        if (rs.getString("tag_names") != null)
                            nd.setTagNames(rs.getString("tag_names").split("%SP%"));
                        if (rs.getString("domain_names") != null)
                            nd.setDomainNames(rs.getString("domain_names").split("%SP%"));

                        nodes.add(nd);
                    }
                });

        jdbcTemplate.query("SELECT ea.* FROM da_" + userDetails.getTenant() + ".entity_model em JOIN da_"
                + userDetails.getTenant() + ".entity_attribute ea "
                + "ON em.entity_id=ea.entity_id JOIN da_" + userDetails.getTenant()
                + ".entity e ON em.entity_id=e.id WHERE e.state='PUBLISHED'", new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {

                        GojsModelNodeData nd = new GojsModelNodeData();
                        nd.setId(rs.getString("id"));
                        nd.setName(rs.getString("name"));
                        nd.setType("defaultNodeType");
                        nd.setZOrder(1);
                        nd.setText(rs.getString("name"));
                        nd.setParentId(rs.getString("entity_id"));
                        nd.setGroup(rs.getString("entity_id"));
                        nd.setIsGroup(false);
                        nd.setOrder(1);
                        nd.setDatatype(rs.getString("attribute_type"));
                        nd.setArtifactType(ArtifactType.entity_attribute.getText());
                        nd.setIsPk(rs.getBoolean("is_pk"));
                        nd.setIsFk(false);
                        nodes.add(nd);
                    }
                });

        jdbcTemplate.query("SELECT r.* FROM da_" + userDetails.getTenant() + ".reference r JOIN da_"
                + userDetails.getTenant()
                + ".entity_attribute ea1 ON r.source_id=ea1.id JOIN da_" + userDetails.getTenant()
                + ".entity_model em1 ON ea1.entity_id=em1.entity_id JOIN da_" + userDetails.getTenant() + ".entity_attribute ea2"
                + " ON r.target_id=ea2.id JOIN da_" + userDetails.getTenant() + ".entity_model em2 ON ea2.entity_id=em2.entity_id "
                + " JOIN da_" + userDetails.getTenant() + ".entity e1 ON em1.entity_id=e1.id"
                + " JOIN da_" + userDetails.getTenant() + ".entity e2 ON em2.entity_id=e2.id"
                + " WHERE reference_type=? AND e1.state='PUBLISHED' AND e2.state='PUBLISHED'",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        GojsModelLinkData ld = new GojsModelLinkData();
                        ld.setId(rs.getString("id"));
                        ld.setFrom(rs.getString("source_id"));
                        ld.setTo(rs.getString("target_id"));
                        ld.setPoints(rs.getString("points"));
                        ld.setZOrder(1);

                        links.add(ld);

                        for (GojsModelNodeData n : nodes) {
                            if (n.getId().equals(ld.getFrom()))
                                n.setIsFk(true);
                        }
                    }
                }, ReferenceType.ENTITY_ATTRIBUTE_TO_ENTITY_ATTRIBUTE.name());

        res.setNodes(nodes);
        res.setLinks(links);
        return res;
    }

    public List<GojsModelNodeData> updateModel(UpdatableGojsModelData updatableGojsModelData, UserDetails userDetails) {

        if (updatableGojsModelData.getUpdateNodes() != null) {
            for (GojsModelNodeData nodeData : updatableGojsModelData.getUpdateNodes()) {
                if (nodeData.getIsGroup()) {
                    if (jdbcTemplate.queryForObject("SELECT EXISTS(SELECT id FROM da_" + userDetails.getTenant() + ".entity_model WHERE entity_id=?) as exists",
                            Boolean.class, UUID.fromString(nodeData.getId()))) {

                        jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".entity_model SET loc=? WHERE entity_id=?",
                                nodeData.getLoc(), UUID.fromString(nodeData.getId()));
                    } else {
                        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".entity_model(id, entity_id, loc) VALUES (?,?,?)",
                                UUID.randomUUID(), UUID.fromString(nodeData.getId()), nodeData.getLoc());
                    }
                }
            }
        }

        if (updatableGojsModelData.getUpdateLinks() != null) {
            for (GojsModelLinkData linkData : updatableGojsModelData.getUpdateLinks()) {
                Timestamp ts = new Timestamp(new java.util.Date().getTime());

                boolean linkExists = jdbcTemplate
                        .queryForObject("SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".reference"
                                + " WHERE id = ? ) as exists", Boolean.class, UUID.fromString((linkData.getId())));

                if (linkExists) {
                    jdbcTemplate.update("UPDATE da_" + userDetails.getTenant()
                            + ".reference SET source_id=?, target_id=?, points=?, modified=?, modifier=?, history_end=? WHERE id=?",
                            UUID.fromString(linkData.getFrom()), UUID.fromString(linkData.getTo()),
                            linkData.getPoints(), ts, userDetails.getUid(), ts, UUID.fromString(linkData.getId()));
                } else {//
                    jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                            + ".reference (id, source_id, source_artifact_type, target_id, "
                            + "target_artifact_type, reference_type, created, creator, modified, modifier, history_start, history_end, version_id, published_id, points)"
                            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", UUID.fromString(linkData.getId()),
                            UUID.fromString(linkData.getFrom()), ArtifactType.entity_attribute.getText(),
                            UUID.fromString(linkData.getTo()), ArtifactType.entity_attribute.getText(),
                            ReferenceType.ENTITY_ATTRIBUTE_TO_ENTITY_ATTRIBUTE.name(),
                            ts, userDetails.getUid(), ts, userDetails.getUid(), ts, ts, 0,
                            UUID.fromString(linkData.getFrom()), linkData.getPoints());

                }
            }
        }

        if (updatableGojsModelData.getDeleteLinks() != null) {
            for (String linkId : updatableGojsModelData.getDeleteLinks()) {
                jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".reference WHERE id=?",
                        UUID.fromString(linkId));
            }
        }

        if (updatableGojsModelData.getDeleteNodes() != null) {
            for (String nodeId : updatableGojsModelData.getDeleteNodes()) {
                jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".entity_model WHERE entity_id=?",
                        UUID.fromString(nodeId));
            }
        }

        return null;
    }
}
