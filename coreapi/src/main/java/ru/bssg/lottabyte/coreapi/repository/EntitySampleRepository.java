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

import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;
import ru.bssg.lottabyte.core.model.entitySample.*;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.model.entitySample.EntitySample;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleEntity;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleType;
import ru.bssg.lottabyte.core.model.entitySample.UpdatableEntitySampleEntity;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.service.EntityQueryService;
import ru.bssg.lottabyte.coreapi.util.Constants;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class EntitySampleRepository extends GenericArtifactRepository<EntitySample> {
    private final JdbcTemplate jdbcTemplate;
    private final EntityQueryRepository entityQueryRepository;
    private static String[] extFields = {  };

    public EntitySampleRepository(JdbcTemplate jdbcTemplate, EntityQueryRepository entityQueryRepository) {
        super(jdbcTemplate, ArtifactType.entity_sample.name(), extFields);
        super.setMapper(new EntitySampleRowMapper());
        this.entityQueryRepository = entityQueryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class EntitySamplePropertyRowMapper implements RowMapper<EntitySampleProperty> {

        public static EntitySampleProperty mapEntitySamplePropertyRow(ResultSet rs) throws SQLException {
            EntitySampleProperty esp = null;

            EntitySamplePropertyEntity espEntity = new EntitySamplePropertyEntity();
            espEntity.setName(rs.getString("name"));
            espEntity.setDescription(rs.getString("description"));

            EntitySamplePropertyPathType ptype = null;
            try {
                ptype = EntitySamplePropertyPathType.valueOf(rs.getString("path_type"));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            espEntity.setPathType(ptype);
            espEntity.setPath(rs.getString("path"));
            espEntity.setEntitySampleId(rs.getString("entity_sample_id"));
            espEntity.setType(rs.getString("property_type"));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));
            md.setName(rs.getString("name"));
            md.setArtifactType(espEntity.getArtifactType().toString());
            md.setVersionId(rs.getInt("version_id"));
            md.setEffectiveStartDate(rs.getTimestamp("history_start").toLocalDateTime());
            md.setEffectiveEndDate(rs.getTimestamp("history_end").toLocalDateTime());

            try {
                esp = new EntitySampleProperty(espEntity, md);
            } catch (LottabyteException e) {
                log.error(e.getMessage(), e);
            }
            return esp;
        }

        @Override
        public EntitySampleProperty mapRow(ResultSet rs, int rowNum) throws SQLException {
            return mapEntitySamplePropertyRow(rs);
        }
    }

    public static class FlatEntitySamplePropertyRowMapper implements RowMapper<FlatEntitySampleProperty> {

        @Override
        public FlatEntitySampleProperty mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatEntitySampleProperty flatEntitySampleProperty = new FlatEntitySampleProperty(
                    EntitySamplePropertyRowMapper.mapEntitySamplePropertyRow(rs));

            flatEntitySampleProperty.setEntityAttributeName(rs.getString("entity_attribute_name"));
            flatEntitySampleProperty.setEntityAttributeId(rs.getString("entity_attribute_id"));

            return flatEntitySampleProperty;
        }
    }

    public static class EntitySampleDQRuleRowMapper implements RowMapper<EntitySampleDQRule> {

        public static EntitySampleDQRule mapEntitySampleDQRuleRow(ResultSet rs) throws SQLException {
            EntitySampleDQRule esp = null;

            EntitySampleDQRuleEntity espEntity = new EntitySampleDQRuleEntity();
            espEntity.setSettings(rs.getString("settings"));
            espEntity.setEntitySampleId(rs.getString("entity_sample_id"));
            espEntity.setDqRuleId(rs.getString("dq_rule_id"));
            espEntity.setDisabled(rs.getBoolean("disabled"));
            espEntity.setSendMail(rs.getBoolean("send_mail"));
            espEntity.setHistoryId(rs.getInt("history_id"));
            espEntity.setPublishedId(rs.getString("published_id"));
            espEntity.setIndicatorId(rs.getString("indicator_id"));
            espEntity.setAncestorId(rs.getString("ancestor_id"));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));

            try {
                esp = new EntitySampleDQRule(espEntity, md);
            } catch (LottabyteException e) {
                log.error(e.getMessage(), e);
            }
            return esp;
        }

        @Override
        public EntitySampleDQRule mapRow(ResultSet rs, int rowNum) throws SQLException {
            return mapEntitySampleDQRuleRow(rs);
        }
    }

    public static class FlatEntitySampleDQRuleRowMapper implements RowMapper<FlatEntitySampleDQRule> {

        @Override
        public FlatEntitySampleDQRule mapRow(ResultSet rs, int rowNum) throws SQLException {

            FlatEntitySampleDQRule flatEntitySampleDQRule = new FlatEntitySampleDQRule(
                    EntitySampleDQRuleRowMapper.mapEntitySampleDQRuleRow(rs));

            flatEntitySampleDQRule.setDq_rule_id(rs.getString("dq_rule_id"));
            flatEntitySampleDQRule.setEntity_sample_id(rs.getString("entity_sample_id"));
            flatEntitySampleDQRule.setSettings(rs.getString("settings"));

            return flatEntitySampleDQRule;
        }
    }

    public boolean hasAccessToSample(String sampleId, UserDetails userDetails) {
        return userDetails.getStewardId() == null ? true
                : jdbcTemplate.queryForObject(
                        "SELECT EXISTS(SELECT entity_sample.ID FROM da_" + userDetails.getTenant() + ".entity_sample " +
                                QueryHelper.getWhereIdInQuery(ArtifactType.entity_sample, userDetails)
                                + " and entity_sample.id = ?) as exists",
                        Boolean.class,
                        UUID.fromString(sampleId));
    }

    public boolean existSamplesInSystem(String systemId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(select id from da_" + userDetails.getTenant()
                        + ".entity_sample where system_id = ?) as exists",
                Boolean.class, UUID.fromString(systemId));
    }

    private List<String> getMappedAttributesIdsForSampleProperty(String propertyId, UserDetails userDetails) {
        return jdbcTemplate.queryForList("SELECT entity_attribute_id FROM da_" + userDetails.getTenant()
                + ".entity_attribute_to_sample_property WHERE entity_sample_property_id=?", String.class,
                UUID.fromString(propertyId));
    }

    public boolean existsSamplePropertyByEntityAttributeId(String entityId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".entity_attribute_to_sample_property " +
                        "WHERE entity_attribute_id = ?) AS EXISTS",
                Boolean.class, UUID.fromString(entityId));
    }

    public boolean existsSamplePropertyBySamplePropertyId(String entityId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".entity_attribute_to_sample_property " +
                        "WHERE entity_sample_property_id = ?) AS EXISTS",
                Boolean.class, UUID.fromString(entityId));
    }

    public EntitySampleProperty getSamplePropertyById(String propertyId, UserDetails userDetails) {
        EntitySampleProperty entitySampleProperty = jdbcTemplate
                .query("SELECT * FROM da_" + userDetails.getTenant() + ".entity_sample_property WHERE id=?",
                        new EntitySamplePropertyRowMapper(), UUID.fromString(propertyId))
                .stream().findFirst().orElse(null);

        if (entitySampleProperty != null)
            entitySampleProperty.getEntity().setMappedAttributeIds(
                    getMappedAttributesIdsForSampleProperty(entitySampleProperty.getId(), userDetails));

        return entitySampleProperty;
    }

    public List<EntitySampleProperty> getAllSamplePropertyBySampleId(String sampleId, UserDetails userDetails) {
        List<EntitySampleProperty> entitySamplePropertyList = jdbcTemplate.query(
                "SELECT * FROM da_" + userDetails.getTenant() + ".entity_sample_property WHERE entity_sample_id=?",
                new EntitySamplePropertyRowMapper(), UUID.fromString(sampleId));

        for (EntitySampleProperty entitySampleProperty : entitySamplePropertyList) {
            if (entitySampleProperty != null)
                entitySampleProperty.getEntity().setMappedAttributeIds(
                        getMappedAttributesIdsForSampleProperty(entitySampleProperty.getId(), userDetails));
        }

        return entitySamplePropertyList;
    }

    public PaginatedArtifactList<EntitySampleProperty> getSamplesPropertiesPaginated(String sampleId, Integer limit,
            Integer offset, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject(
                "SELECT COUNT(id) FROM da_" + userDetails.getTenant()
                        + ".entity_sample_property WHERE entity_sample_id = ?",
                Integer.class, UUID.fromString(sampleId));
        String query = "SELECT * FROM da_" + userDetails.getTenant()
                + ".entity_sample_property WHERE entity_sample_id = ? offset ? limit ? ";
        List<EntitySampleProperty> entitySamplePropertyList = jdbcTemplate.query(query,
                new EntitySamplePropertyRowMapper(), UUID.fromString(sampleId), offset, limit);

        for (EntitySampleProperty esp : entitySamplePropertyList) {
            esp.getEntity().setMappedAttributeIds(getMappedAttributesIdsForSampleProperty(esp.getId(), userDetails));
        }

        return new PaginatedArtifactList<>(
                entitySamplePropertyList, offset, limit, total, "/v1/samples/" + sampleId + "/properties/");
    }

    public List<EntitySampleProperty> getSamplesPropertiesList(String sampleId, UserDetails userDetails) {
        return jdbcTemplate.query(
                "SELECT id, \"name\", description, path_type, \"path\", entity_sample_id, history_start, history_end, version_id, created, creator, modified, modifier, property_type FROM da_"
                        + userDetails.getTenant() + ".entity_sample_property WHERE entity_sample_id=?",
                new EntitySamplePropertyRowMapper(), UUID.fromString(sampleId));
    }

    public Boolean samplePropertyExists(String propertyId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM da_" + userDetails.getTenant() + ".entity_sample_property WHERE id=?)",
                Boolean.class, UUID.fromString(propertyId));
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public EntitySampleProperty createSampleProperty(String sampleId,
            UpdatableEntitySampleProperty entitySampleProperty, UserDetails userDetails) throws LottabyteException {
        UUID id = UUID.randomUUID();

        EntitySampleProperty esp = new EntitySampleProperty(entitySampleProperty);
        esp.setId(id.toString());
        LocalDateTime now = LocalDateTime.now();
        esp.setCreatedAt(now);
        esp.setModifiedAt(now);
        esp.setCreatedBy(userDetails.getUid());
        esp.setModifiedBy(userDetails.getUid());

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                + ".entity_sample_property (id, name, description, path_type, path, entity_sample_id, history_start, history_end, version_id, created, creator, modified, modifier, property_type) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id, entitySampleProperty.getName(), entitySampleProperty.getDescription(),
                entitySampleProperty.getPathType().toString(), entitySampleProperty.getPath(),
                UUID.fromString(sampleId), now, now, 1, now, userDetails.getUid(), now, userDetails.getUid(),
                entitySampleProperty.getType());

        if (entitySampleProperty.getMappedAttributeIds() != null) {
            for (String aid : entitySampleProperty.getMappedAttributeIds()) {
                jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                        + ".entity_attribute_to_sample_property (id, entity_attribute_id, entity_sample_property_id, created, creator, modified, modifier) VALUES (?,?,?,?,?,?,?)",
                        UUID.randomUUID(), UUID.fromString(aid), id, now, userDetails.getUid(), now,
                        userDetails.getUid());
            }
        }

        return esp;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public EntitySampleProperty updateSampleProperty(String propertyId,
            UpdatableEntitySampleProperty entitySampleProperty, UserDetails userDetails) throws LottabyteException {
        EntitySampleProperty esp = new EntitySampleProperty(entitySampleProperty);
        esp.setId(propertyId);

        LocalDateTime now = LocalDateTime.now();
        esp.setModifiedAt(now);
        esp.setModifiedBy(userDetails.getUid());

        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (entitySampleProperty.getName() != null) {
            sets.add("name=?");
            args.add(entitySampleProperty.getName());
        }
        if (entitySampleProperty.getDescription() != null) {
            sets.add("description=?");
            args.add(entitySampleProperty.getDescription());
        }
        if (entitySampleProperty.getPathType() != null) {
            sets.add("path_type=?");
            args.add(entitySampleProperty.getPathType());
        }
        if (entitySampleProperty.getPath() != null) {
            sets.add("path=?");
            args.add(entitySampleProperty.getPath());
        }
        if (entitySampleProperty.getEntitySampleId() != null) {
            sets.add("entity_sample_id=?");
            args.add(UUID.fromString(entitySampleProperty.getEntitySampleId()));
        }
        if (!sets.isEmpty() || entitySampleProperty.getMappedAttributeIds() != null) {
            sets.add("modified=?");
            sets.add("modifier=?");
            args.add(esp.getModifiedAt());
            args.add(esp.getModifiedBy());
            args.add(UUID.fromString(esp.getId()));

            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".entity_sample_property SET "
                    + StringUtils.join(sets, ", ") + " WHERE id=?", args.toArray());

            if (entitySampleProperty.getMappedAttributeIds() != null) {
                List<String> currAttributeIds = getMappedAttributesIdsForSampleProperty(esp.getId(), userDetails);
                for (String aid : currAttributeIds) {
                    if (!entitySampleProperty.getMappedAttributeIds().contains(aid)) {
                        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant()
                                + ".entity_attribute_to_sample_property WHERE entity_attribute_id=? AND entity_sample_property_id=?",
                                UUID.fromString(aid), UUID.fromString(propertyId));
                    }
                }
                for (String aid : entitySampleProperty.getMappedAttributeIds()) {
                    if (!currAttributeIds.contains(aid)) {
                        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                                + ".entity_attribute_to_sample_property (id, entity_attribute_id, entity_sample_property_id, created, creator, modified, modifier) VALUES (?,?,?,?,?,?,?)",
                                UUID.randomUUID(), UUID.fromString(aid), UUID.fromString(propertyId), now,
                                userDetails.getUid(), now, userDetails.getUid());
                    }
                }
            }
        }
        return esp;
    }

    public static class EntitySampleRowMapper implements RowMapper<EntitySample> {

        public static EntitySample mapEntitySampleRow(ResultSet rs) throws SQLException {
            EntitySampleEntity entitySampleEntity = new EntitySampleEntity();
            entitySampleEntity.setName(rs.getString("name"));
            entitySampleEntity.setDescription(rs.getString("description"));
            entitySampleEntity.setEntityId(rs.getString("entity_id"));
            entitySampleEntity.setEntityQueryId(rs.getString("entity_query_id"));
            entitySampleEntity.setSampleType(EntitySampleType.valueOf(rs.getString("sample_type")));
            entitySampleEntity.setSystemId(rs.getString("system_id"));
            entitySampleEntity.setLastUpdated(rs.getTimestamp("last_updated").toLocalDateTime());
            entitySampleEntity.setIsMain(rs.getBoolean("is_main"));
            entitySampleEntity.setRoles(rs.getString("roles"));
            try {
                rs.findColumn("entity_query_version_id");
                if (rs.getObject("entity_query_version_id") != null)
                    entitySampleEntity.setEntityQueryVersionId(rs.getInt("entity_sample_version_id"));
            } catch (SQLException ignored) {
            }
            return new EntitySample(entitySampleEntity, new Metadata(rs, entitySampleEntity.getArtifactType()));
        }

        @Override
        public EntitySample mapRow(ResultSet rs, int rowNum) throws SQLException {
            return mapEntitySampleRow(rs);
        }
    }

    public static class FlatEntitySampleRowMapper extends FlatItemRowMapper<FlatEntitySample> {

        public FlatEntitySampleRowMapper() {
            super(FlatEntitySample::new);
        }

        @Override
        public FlatEntitySample mapRow(ResultSet rs, int rowNum) throws SQLException {
            // FlatEntitySample flatEntitySample = new
            // FlatEntitySample(EntitySampleRowMapper.mapEntitySampleRow(rs));
            FlatEntitySample flatEntitySample = super.mapRow(rs, rowNum);
            flatEntitySample.setSystemName(rs.getString("system_name"));
            flatEntitySample.setEntityName(rs.getString("entity_name"));
            flatEntitySample.setEntityQueryName(rs.getString("entity_query_name"));
            return flatEntitySample;
        }
    }

    private EntitySample mapRow(ResultSet rs) throws SQLException {
        EntitySampleEntity entitySampleEntity = new EntitySampleEntity();
        entitySampleEntity.setName(rs.getString("name"));
        entitySampleEntity.setDescription(rs.getString("description"));
        entitySampleEntity.setEntityId(rs.getString("entity_id"));
        entitySampleEntity.setEntityQueryId(rs.getString("entity_query_id"));
        entitySampleEntity.setSampleType(EntitySampleType.valueOf(rs.getString("sample_type")));
        entitySampleEntity.setSystemId(rs.getString("system_id"));
        entitySampleEntity.setIsMain(rs.getBoolean("is_main"));
        entitySampleEntity.setLastUpdated(rs.getTimestamp("last_updated").toLocalDateTime());
        try {
            rs.findColumn("entity_sample_version_id");
            if (rs.getObject("entity_sample_version_id") != null)
                entitySampleEntity.setEntityQueryVersionId(rs.getInt("entity_sample_version_id"));
        } catch (SQLException ignored) {
        }

        return new EntitySample(entitySampleEntity, new Metadata(rs, entitySampleEntity.getArtifactType()));
    }

    public EntitySample getEntitySampleById(String entityId, Boolean includeBody, UserDetails userDetails) {
        List<EntitySample> entitySampleList = jdbcTemplate.query(
                "SELECT * FROM da_" + userDetails.getTenant() + ".entity_sample WHERE id=?",
                new EntitySampleRowMapper(), UUID.fromString(entityId));
        EntitySample entitySample = entitySampleList.stream().findFirst().orElse(null);

        if (entitySample != null && entitySample.getId() != null
                && entitySample.getEntity().getSampleType().equals(EntitySampleType.json) && includeBody) {
            entitySample.getEntity().setSampleBody(getEntitySampleBodyById(entitySample.getId(), userDetails));
        }

        return entitySample;
    }

    public EntitySample getMainEntitySampleByEntityIdAndSystemId(String entityId, String systemId,
            UserDetails userDetails) {
        List<EntitySample> entitySampleList = jdbcTemplate.query(
                "SELECT * FROM da_" + userDetails.getTenant()
                        + ".entity_sample WHERE entity_id = ? AND system_id = ? AND is_main = true",
                new EntitySampleRowMapper(), UUID.fromString(entityId), UUID.fromString(systemId));

        return entitySampleList.stream().findFirst().orElse(null);
    }

    public boolean existsEntitySampleWithEntity(String entityId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".entity_sample " +
                        "WHERE entity_id is not null and entity_id = ?) AS EXISTS",
                Boolean.class, UUID.fromString(entityId));
    }

    public String getEntitySampleBodyById(String entityId, UserDetails userDetails) {
        final String[] entitySampleBody = new String[1];

        String query = "SELECT entity_sample_id, sample_body  " +
                "FROM da_" + userDetails.getTenant() + ".entity_sample_body " +
                "where entity_sample_id = ? ";

        jdbcTemplate.query(
                query,
                rs -> {
                    entitySampleBody[0] = rs.getString("sample_body");
                },
                UUID.fromString(entityId));
        return entitySampleBody[0];
    }

    public EntitySample getEntitySampleByName(String entityName, UserDetails userDetails) {
        List<EntitySample> entitySampleList = jdbcTemplate.query(
                "SELECT * FROM da_" + userDetails.getTenant() + ".entity WHERE name=?",
                new EntitySampleRowMapper(), entityName);

        return entitySampleList.stream().findFirst().orElse(null);
    }

    public String createEntitySampleBody(UUID uuidSample, UpdatableEntitySampleEntity newEntitySampleEntity,
            UserDetails userDetails) {
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".entity_sample_body " +
                "(entity_sample_id, sample_body) " +
                "VALUES(?, ?);",
                uuidSample, newEntitySampleEntity.getSampleBody());
        return uuidSample.toString();
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public String createEntitySampleEntity(UpdatableEntitySampleEntity newEntitySampleEntity, UserDetails userDetails) {
        UUID uuidSample = java.util.UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        if (newEntitySampleEntity.getSampleBody() != null) {
            createEntitySampleBody(uuidSample, newEntitySampleEntity, userDetails);
        }

        Integer queryVersionId = null;
        if (newEntitySampleEntity.getEntityQueryId() != null && !newEntitySampleEntity.getEntityQueryId().isEmpty()) {
            EntityQuery q = entityQueryRepository.getById(newEntitySampleEntity.getEntityQueryId(), userDetails);
            WorkflowableMetadata md = (WorkflowableMetadata) q.getMetadata();
            queryVersionId = md.getVersionId();
        }

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".entity_sample " +
                "(id, name, description, sample_type, last_updated, created, creator, modified, modifier, entity_id, system_id, entity_query_id, entity_query_version_id, is_main, roles) "
                +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                uuidSample, newEntitySampleEntity.getName(), newEntitySampleEntity.getDescription(),
                newEntitySampleEntity.getSampleType().toString(),
                ts, ts, userDetails.getUid(), ts, userDetails.getUid(),
                UUID.fromString(newEntitySampleEntity.getEntityId()),
                UUID.fromString(newEntitySampleEntity.getSystemId()),
                UUID.fromString(newEntitySampleEntity.getEntityQueryId()),
                queryVersionId,
                newEntitySampleEntity.getIsMain(), newEntitySampleEntity.getRoles());

        return uuidSample.toString();
    }

    public void deleteSampleBody(String sampleId, UserDetails userDetails) {
        String query = "DELETE FROM da_" + userDetails.getTenant() + ".entity_sample_body " +
                "WHERE entity_sample_id = ?";
        jdbcTemplate.update(query, UUID.fromString(sampleId));
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void deleteSample(String sampleId, boolean deleteSampleBody, UserDetails userDetails) {
        if (deleteSampleBody)
            deleteSampleBody(sampleId, userDetails);

        String query = "DELETE FROM da_" + userDetails.getTenant() + ".entity_sample " +
                "WHERE id = ?";
        jdbcTemplate.update(query, UUID.fromString(sampleId));
    }

    public void patchSampleBody(String sampleId, UpdatableEntitySampleEntity entitySampleEntity,
            UserDetails userDetails) {
        String query = "UPDATE da_" + userDetails.getTenant() + ".entity_sample_body SET " +
                "sample_body = ? " +
                "WHERE entity_sample_id = ?;";
        jdbcTemplate.update(query, entitySampleEntity.getSampleBody(), UUID.fromString(sampleId));
    }

    public void updateAllEntitySampleMainStatus(String entityId, String systemId, UserDetails userDetails) {
        String query = "UPDATE da_" + userDetails.getTenant() + ".entity_sample SET " +
                "is_main = false " +
                "WHERE entity_id = ? and system_id = ?;";
        jdbcTemplate.update(query, UUID.fromString(entityId), UUID.fromString(systemId));
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void patchSample(String sampleId, UpdatableEntitySampleEntity entitySampleEntity, UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".entity_sample SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));
        if (entitySampleEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(entitySampleEntity.getName());
        }
        if (entitySampleEntity.getDescription() != null) {
            sets.add("description = ?");
            params.add(entitySampleEntity.getDescription());
        }
        if (entitySampleEntity.getEntityId() != null) {
            sets.add("entity_id = ?");
            params.add(UUID.fromString(entitySampleEntity.getEntityId()));
        }
        if (entitySampleEntity.getSystemId() != null) {
            sets.add("system_id = ?");
            params.add(UUID.fromString(entitySampleEntity.getSystemId()));
        }
        if (entitySampleEntity.getRoles() != null) {
            sets.add("roles = ?");
            params.add(entitySampleEntity.getRoles());
        }
        if (entitySampleEntity.getEntityQueryId() != null) {
            sets.add("entity_query_id = ?");
            params.add(UUID.fromString(entitySampleEntity.getEntityQueryId()));
            sets.add("entity_query_version_id = ?");
            Integer queryVersionId = null;
            if (!entitySampleEntity.getEntityQueryId().isEmpty()) {
                EntityQuery q = entityQueryRepository.getById(entitySampleEntity.getEntityQueryId(), userDetails);
                WorkflowableMetadata md = (WorkflowableMetadata) q.getMetadata();
                queryVersionId = md.getVersionId();
            }
            params.add(queryVersionId);
        }
        if (entitySampleEntity.getSampleType() != null) {
            sets.add("sample_type = ?");
            params.add(entitySampleEntity.getSampleType().toString());
        }
        if (entitySampleEntity.getIsMain() != null) {
            sets.add("is_main = ?");
            params.add(entitySampleEntity.getIsMain());
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(sampleId));
            jdbcTemplate.update(query, params.toArray());
        }

        if (entitySampleEntity.getSampleBody() != null) {
            patchSampleBody(sampleId, entitySampleEntity, userDetails);
        }
    }

    public PaginatedArtifactList<EntitySample> getEntitySampleWithPaging(Integer offset, Integer limit,
            Boolean includeBody, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject(
                "SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".entity_sample", Integer.class);
        String query = "SELECT * FROM da_" + userDetails.getTenant() + ".entity_sample offset ? limit ? ";
        List<EntitySample> entitySampleList = jdbcTemplate.query(query, new EntitySampleRowMapper(), offset, limit);

        if (includeBody) {
            for (EntitySample entitySample : entitySampleList) {
                if (entitySample != null && entitySample.getId() != null
                        && entitySample.getEntity().getSampleType().equals(EntitySampleType.json)) {
                    entitySample.getEntity().setSampleBody(getEntitySampleBodyById(entitySample.getId(), userDetails));
                }
            }
        }
        PaginatedArtifactList<EntitySample> paginatedArtifactList = new PaginatedArtifactList<>(
                entitySampleList, offset, limit, total, "/v1/samples/entity/", "&include_body=" + includeBody);
        return paginatedArtifactList;
    }

    public EntitySample getEntitySampleByQueryId(String queryId, UserDetails userDetails) {
        List<EntitySample> entitySampleList = jdbcTemplate.query(
                "SELECT id, \"name\", description, entity_id, system_id, entity_query_id, sample_type, last_updated, history_start, history_end, version_id, created, creator, modified, modifier, is_main "
                        +
                        "FROM da_" + userDetails.getTenant() + ".entity_sample " +
                        "WHERE entity_query_id = ? " +
                        "order by last_updated desc\n" +
                        "limit 1",
                new EntitySampleRowMapper(), UUID.fromString(queryId));

        return entitySampleList.stream().findFirst().orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void deleteSampleProperty(String propertyId, Boolean force, UserDetails userDetails) {
        if (force)
            jdbcTemplate.update(
                    "DELETE FROM da_" + userDetails.getTenant()
                            + ".entity_attribute_to_sample_property WHERE entity_sample_property_id=?",
                    UUID.fromString(propertyId));
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".entity_sample_property WHERE id=?",
                UUID.fromString(propertyId));
    }

    public SearchResponse<FlatEntitySample> searchEntitySamples(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "SELECT entity_sample.*, t.tags, true as has_access FROM da_" + userDetails.getTenant()
                + ".entity_sample ";
        if (userDetails.getStewardId() != null) {
            String hasAccessJoinQuery = QueryHelper.getWhereIdInQuery(ArtifactType.entity_sample, userDetails);
            subQuery = "SELECT entity_sample.*, t.tags, case when acc.id is null then false else true end as has_access FROM da_"
                    + userDetails.getTenant() + ".entity_sample "
                    + " left join (select entity_sample.id from da_" + userDetails.getTenant() + ".entity_sample "
                    + hasAccessJoinQuery + ") acc on entity_sample.id = acc.id ";
            if (searchRequest.getLimitSteward() != null && searchRequest.getLimitSteward()) {
                subQuery = "SELECT entity_sample.*, t.tags, true as has_access FROM da_" + userDetails.getTenant()
                        + ".entity_sample ";
                subQuery += " left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                        + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                        + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=entity_sample.id ";
                subQuery += hasAccessJoinQuery;
            } else
                subQuery += " left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                        + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                        + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=entity_sample.id ";
        } else
            subQuery += " left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                    + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                    + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=entity_sample.id ";

        if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
            if (where.isEmpty())
                where = " WHERE ";
            else
                where += " AND ";
            where += " tbl1.system_id IN (SELECT system_id FROM da_" + userDetails.getTenant() + ".system_to_domain WHERE domain_id IN ('" + StringUtils.join(userDetails.getUserDomains(), "','") + "'))";
        }

        String queryForItems = "SELECT distinct tbl1.has_access, tbl1.id,tbl1.name,tbl1.description,tbl1.entity_id,tbl1.system_id,tbl1.entity_query_id,"
                + "tbl1.sample_type,tbl1.last_updated,tbl1.history_start,tbl1.history_end,tbl1.version_id,tbl1.created,"
                + "tbl1.creator,tbl1.modified,tbl1.modifier,tbl1.is_main, system.name as system_name, entity.name as entity_name, entity_query.name as entity_query_name"
                + " FROM (" + subQuery + ") as tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                + " LEFT JOIN da_" + userDetails.getTenant()
                + ".entity_query entity_query ON tbl1.entity_query_id=entity_query.id"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                + where
                + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatEntitySample> items = jdbcTemplate.query(queryForItems, new FlatEntitySampleRowMapper(),
                whereValues.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                + " LEFT JOIN da_" + userDetails.getTenant()
                + ".entity_query entity_query ON tbl1.entity_query_id=entity_query.id"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                + where;
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
        SearchResponse<FlatEntitySample> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), items);

        return res;
    }

    public SearchResponse<FlatEntitySample> searchEntitySamplesByDomain(SearchRequestWithJoin searchRequest,
            String domainId, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin,
            UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        if (where.isEmpty()) {
            where = " WHERE tbl1.system_id in (select tbl3.system_id from da_" + userDetails.getTenant()
                    + ".system_to_domain tbl3 where tbl3.domain_id='" + domainId + "' LIMIT " + Constants.sqlInLimit
                    + ")";
        } else {
            where = where + " AND tbl1.system_id in (select tbl3.system_id from da_" + userDetails.getTenant()
                    + ".system_to_domain tbl3 where tbl3.domain_id='" + domainId + "' LIMIT " + Constants.sqlInLimit
                    + ")";
        }

        String subQuery = "SELECT entity_sample.* FROM da_" + userDetails.getTenant() + ".entity_sample " +
                "join da_" + userDetails.getTenant()
                + ".entity_query on entity_sample.entity_query_id = entity_query.id and entity_query.state = '"
                + (searchRequest.getState() != null ? searchRequest.getState() + "' " : "PUBLISHED' ");
        if (userDetails.getStewardId() != null && searchRequest.getLimitSteward() != null
                && searchRequest.getLimitSteward())
            subQuery = subQuery + QueryHelper.getWhereIdInQuery(ArtifactType.entity_sample, userDetails);
        String queryForItems = "SELECT tbl1.*, system.name as system_name, entity.name as entity_name, entity_query.name as entity_query_name FROM ("
                + subQuery + ") as tbl1 "
                + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_query entity_query ON tbl1.entity_query_id=entity_query.id "
                + join + where
                + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatEntitySample> items = jdbcTemplate.query(queryForItems, new FlatEntitySampleRowMapper(), whereValues.toArray());

        String queryForTotal = "SELECT COUNT(tbl1.id) FROM (" + subQuery + ") as tbl1 "
                + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_query entity_query ON tbl1.entity_query_id=entity_query.id "
                + join + where;
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
        SearchResponse<FlatEntitySample> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), items);

        return res;
    }

    public PaginatedArtifactList<EntitySample> getEntitySampleVersions(String sampleId, Integer offset, Integer limit,
            UserDetails userDetails) {
        int total = jdbcTemplate
                .queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".entity_sample_hist " +
                        "WHERE id = ?", Integer.class, UUID.fromString(sampleId));
        List<EntitySample> resources = jdbcTemplate.query(
                "SELECT * FROM da_" + userDetails.getTenant() + ".entity_sample_hist WHERE id = ? " +
                        "offset ? limit ?",
                new EntitySampleRepository.EntitySampleRowMapper(), UUID.fromString(sampleId), offset, limit);

        PaginatedArtifactList<EntitySample> res = new PaginatedArtifactList<>(
                resources, offset, limit, total, "/v1/samples/" + sampleId + "/versions");
        return res;
    }

    public SearchResponse<FlatEntitySampleProperty> searchSampleProperties(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "SELECT entity_sample_property.* FROM da_" + userDetails.getTenant()
                + ".entity_sample_property ";
        if (userDetails.getStewardId() != null && searchRequest.getLimitSteward() != null
                && searchRequest.getLimitSteward())
            subQuery = subQuery + QueryHelper.getWhereIdInQuery(ArtifactType.entity_sample_property, userDetails);
        String queryForItems = "SELECT tbl1.id,tbl1.name,tbl1.description,tbl1.path_type,tbl1.path,tbl1.entity_sample_id,"
                + "tbl1.history_start,tbl1.history_end,tbl1.version_id,tbl1.created,tbl1.creator,tbl1.property_type,"
                + "tbl1.modified,tbl1.modifier, entity_attribute.name AS entity_attribute_name, entity_attribute.id AS entity_attribute_id"
                + " FROM (" + subQuery + ") as tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant()
                + ".entity_attribute_to_sample_property easp ON easp.entity_sample_property_id=tbl1.id"
                + " LEFT JOIN da_" + userDetails.getTenant()
                + ".entity_attribute entity_attribute ON easp.entity_attribute_id=entity_attribute.id "
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatEntitySampleProperty> items = jdbcTemplate.query(queryForItems,
                new EntitySampleRepository.FlatEntitySamplePropertyRowMapper(), whereValues.toArray());

        String queryForTotal = "SELECT COUNT(tbl1.id) FROM (" + subQuery + ") as tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant()
                + ".entity_attribute_to_sample_property easp ON easp.entity_sample_property_id=tbl1.id"
                + " LEFT JOIN da_" + userDetails.getTenant()
                + ".entity_attribute entity_attribute ON easp.entity_attribute_id=entity_attribute.id "
                + where;
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

        SearchResponse<FlatEntitySampleProperty> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), items);

        return res;
    }

    public SearchResponse<FlatEntitySampleDQRule> searchSampleDQRules(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "SELECT entity_sample_to_dq_rule.* FROM da_" + userDetails.getTenant()
                + ".entity_sample_to_dq_rule ";

        String queryForItems = subQuery
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatEntitySampleDQRule> items = jdbcTemplate.query(queryForItems,
                new EntitySampleRepository.FlatEntitySampleDQRuleRowMapper(), whereValues.toArray());

        String queryForTotal = "SELECT COUNT(1) FROM (" + subQuery + ") as tbl1 ";
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

        SearchResponse<FlatEntitySampleDQRule> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), items);

        return res;
    }

    public List<EntitySampleDQRule> getSampleDQRulesByEntitySample(String sampleId, UserDetails userDetails) {
        return jdbcTemplate.query(
                "SELECT * FROM da_"
                        + userDetails.getTenant() + ".entity_sample_to_dq_rule WHERE entity_sample_id=?",
                new EntitySampleDQRuleRowMapper(), UUID.fromString(sampleId));
    }

    public List<EntitySampleDQRule> getSampleDQRulesByIndicator(String indicatorId, UserDetails userDetails) {
        return jdbcTemplate.query(
                "SELECT * FROM da_"
                        + userDetails.getTenant() + ".entity_sample_to_dq_rule WHERE indicator_id=?",
                new EntitySampleDQRuleRowMapper(), UUID.fromString(indicatorId));
    }

    public List<EntitySampleDQRule> getSampleDQRulesByProduct(String productId, UserDetails userDetails) {
        return jdbcTemplate.query(
                "SELECT * FROM da_"
                        + userDetails.getTenant() + ".entity_sample_to_dq_rule WHERE product_id=?",
                new EntitySampleDQRuleRowMapper(), UUID.fromString(productId));
    }

    public List<EntitySampleDQRule> getSampleDQRulesByAsset(String assetId, UserDetails userDetails) {
        return jdbcTemplate.query(
                "SELECT * FROM da_"
                        + userDetails.getTenant() + ".entity_sample_to_dq_rule WHERE asset_id=?",
                new EntitySampleDQRuleRowMapper(), UUID.fromString(assetId));
    }

    public EntitySampleDQRule getSampleDQRule(String id, UserDetails userDetails) {
        EntitySampleDQRule entitySampleDQRule = jdbcTemplate
                .query("SELECT * FROM da_" + userDetails.getTenant() + ".entity_sample_to_dq_rule WHERE id=?",
                        new EntitySampleDQRuleRowMapper(), UUID.fromString(id))
                .stream().findFirst().orElse(null);

        return entitySampleDQRule;
    }

    public boolean existsSampleDQRuleByDQRuleId(String dqRuleId,
            UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() +
                        ".entity_sample_to_dq_rule " +
                        "WHERE id = ?) AS EXISTS",
                Boolean.class, UUID.fromString(dqRuleId));
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public EntitySampleDQRule createSampleDQRule(String sampleId,
            UpdatableEntitySampleDQRule entitySampleDQRule, UserDetails userDetails) throws LottabyteException {
        UUID id = entitySampleDQRule.getId() == null ? UUID.randomUUID() : UUID.fromString(entitySampleDQRule.getId());

        EntitySampleDQRule esp = new EntitySampleDQRule(entitySampleDQRule);
        esp.setId(id.toString());
        LocalDateTime now = LocalDateTime.now();
        esp.setCreatedAt(now);
        esp.setModifiedAt(now);
        esp.setCreatedBy(userDetails.getUid());
        esp.setModifiedBy(userDetails.getUid());

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                + ".entity_sample_to_dq_rule (id, entity_sample_id, dq_rule_id, settings, created, creator, modified, modifier, disabled, indicator_id, product_id, send_mail) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                id, UUID.fromString(sampleId), UUID.fromString(entitySampleDQRule.getDqRuleId()),
                entitySampleDQRule.getSettings(), now, userDetails.getUid(), now, userDetails.getUid(),
                entitySampleDQRule.getDisabled(),
                entitySampleDQRule.getIndicatorId() == null ? entitySampleDQRule.getIndicatorId()
                        : UUID.fromString(entitySampleDQRule.getIndicatorId()),
                entitySampleDQRule.getProductId() == null ? entitySampleDQRule.getProductId()
                        : UUID.fromString(entitySampleDQRule.getProductId()),
                entitySampleDQRule.getSendMail());

        return esp;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public EntitySampleDQRule updateSampleDQRule(String ruleId,
            UpdatableEntitySampleDQRule entitySampleDQRule, UserDetails userDetails) throws LottabyteException {
        EntitySampleDQRule esp = new EntitySampleDQRule(entitySampleDQRule);
        esp.setId(ruleId);

        LocalDateTime now = LocalDateTime.now();
        esp.setModifiedAt(now);
        esp.setModifiedBy(userDetails.getUid());

        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (entitySampleDQRule.getDqRuleId() != null) {
            sets.add("dq_rule_id=?");
            args.add(UUID.fromString(entitySampleDQRule.getDqRuleId()));
        }
        if (entitySampleDQRule.getDisabled() != null) {
            sets.add("disabled=?");
            args.add(entitySampleDQRule.getDisabled());
        }
        if (entitySampleDQRule.getProductId() != null) {
            sets.add("product_id=?");
            args.add(entitySampleDQRule.getProductId());
        }
        if (entitySampleDQRule.getIndicatorId() != null) {
            sets.add("indicator_id=?");
            args.add(entitySampleDQRule.getIndicatorId());
        }
        if (entitySampleDQRule.getSendMail() != null) {
            sets.add("send_mail=?");
            args.add(entitySampleDQRule.getSendMail());
        }
        if (entitySampleDQRule.getSettings() != null) {
            sets.add("settings=?");
            args.add(entitySampleDQRule.getSettings());
        }
        if (entitySampleDQRule.getEntitySampleId() != null) {
            sets.add("entity_sample_id=?");
            args.add(UUID.fromString(entitySampleDQRule.getEntitySampleId()));
        }
        if (!sets.isEmpty()) {
            sets.add("modified=?");
            sets.add("modifier=?");
            args.add(esp.getModifiedAt());
            args.add(esp.getModifiedBy());
            args.add(UUID.fromString(esp.getId()));

            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".entity_sample_to_dq_rule SET "
                    + StringUtils.join(sets, ", ") + " WHERE id=?", args.toArray());

        }
        return esp;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void deleteSampleDQRule(String dqRuleId, Boolean force, UserDetails userDetails) {

        jdbcTemplate.update(
                "DELETE FROM da_" + userDetails.getTenant()
                        + ".entity_sample_to_dq_rule WHERE id=?",
                UUID.fromString(dqRuleId));

    }

    public List<String> getDomainIdsBySystemId(String systemId, UserDetails userDetails) {
        return jdbcTemplate.queryForList("SELECT domain_id FROM da_" + userDetails.getTenant()
                + ".system_to_domain sd WHERE sd.system_id=?", String.class, UUID.fromString(systemId));
    }

    public void updateDQRuleLink(String id, EntitySampleDQRule entitySampleDQRule, UserDetails userDetails) {
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".entity_sample_to_dq_rule SET settings=?, modified=?, modifier=?, disabled=?, send_mail=? WHERE id=?",
                entitySampleDQRule.getEntity().getSettings(), now, userDetails.getUid(), entitySampleDQRule.getEntity().getDisabled(),
                entitySampleDQRule.getEntity().getSendMail(), UUID.fromString(id));
    }

    public void removeDQRuleLink(String id, UserDetails userDetails) {
        String query = "DELETE FROM da_" + userDetails.getTenant() + ".entity_sample_to_dq_rule WHERE id = ?";
        jdbcTemplate.update(query, UUID.fromString(id));
    }
}
