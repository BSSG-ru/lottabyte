package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.ca.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.util.JDBCUtil;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Repository
@Slf4j
public class CustomAttributeDefinitionRepository extends GenericArtifactRepository<CustomAttributeDefinition> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = {  };

    public CustomAttributeDefinitionRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.custom_attribute_definition.name(), extFields);
        super.setMapper(new CustomAttributeDefinitionRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    private class CustomAttributeRowMapper implements RowMapper<CustomAttributeRecord> {
        @Override
        public CustomAttributeRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            CustomAttributeRecord caRec = new CustomAttributeRecord();
            caRec.setCustomAttributeDefinitionId(rs.getString("definition_id"));
            caRec.setObjectType(rs.getString("object_type"));
            caRec.setObjectId(rs.getString("object_id"));
            Timestamp date_value = rs.getTimestamp("date_value");
            caRec.setDataValue(date_value == null ? null : date_value.toLocalDateTime());
            caRec.setNumberValue(rs.getDouble("number_value"));
            caRec.setTextValue(rs.getString("text_value"));
            caRec.setDefElementId(rs.getString("def_element_id"));
            caRec.setId(rs.getString("id"));
            return caRec;
        }
    }

    private class CustomAttributeDefinitionRowMapper implements RowMapper<CustomAttributeDefinition> {
        @Override
        public CustomAttributeDefinition mapRow(ResultSet rs, int rowNum) throws SQLException {

            Metadata metadata = new Metadata();
            metadata.setId(rs.getString("id"));
            metadata.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            metadata.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            metadata.setCreatedBy(rs.getString("creator"));
            metadata.setModifiedBy(rs.getString("modifier"));
            metadata.setVersionId(rs.getInt("version_id"));
            metadata.setEffectiveStartDate(rs.getTimestamp("history_start").toLocalDateTime());
            metadata.setEffectiveEndDate(rs.getTimestamp("history_end").toLocalDateTime());

            CustomAttributeDefinitionEntity customAttributeDefinitionEntity = new CustomAttributeDefinitionEntity();
            customAttributeDefinitionEntity.setMaximum(JDBCUtil.getInt(rs,"maximum"));
            customAttributeDefinitionEntity.setMinimum(JDBCUtil.getInt(rs,"minimum"));
            customAttributeDefinitionEntity.setMaxLength(JDBCUtil.getInt(rs,"max_length"));
            customAttributeDefinitionEntity.setMinLength(JDBCUtil.getInt(rs,"min_length"));
            customAttributeDefinitionEntity.setType(AttributeType.valueOf(rs.getString("type")));
            customAttributeDefinitionEntity.setPlaceholder(rs.getString("placeholder"));
            customAttributeDefinitionEntity.setDefaultValue(rs.getString("default_value"));
            customAttributeDefinitionEntity.setMultipleValues(rs.getBoolean("multiple_values"));
            customAttributeDefinitionEntity.setRequired(rs.getBoolean("required"));
            customAttributeDefinitionEntity.setName(rs.getString("name"));
            customAttributeDefinitionEntity.setDescription(rs.getString("description"));
            customAttributeDefinitionEntity.setArtifactType(ArtifactType.custom_attribute_definition);

            return new CustomAttributeDefinition(customAttributeDefinitionEntity, metadata);
        }
    }

    private class CustomAttributeDefElementRowMapper implements RowMapper<CustomAttributeDefElement> {
        @Override
        public CustomAttributeDefElement mapRow(ResultSet rs, int rowNum) throws SQLException {
            Metadata metadata = new Metadata();
            metadata.setId(rs.getString("id"));
            metadata.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            metadata.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            metadata.setCreatedBy(rs.getString("creator"));
            metadata.setModifiedBy(rs.getString("modifier"));

            CustomAttributeDefElementEntity customAttributeDefElementEntity = new CustomAttributeDefElementEntity();
            customAttributeDefElementEntity.setDefinitionId(rs.getString("definition_id"));
            customAttributeDefElementEntity.setName(rs.getString("name"));
            customAttributeDefElementEntity.setDescription(rs.getString("description"));
            customAttributeDefElementEntity.setArtifactType(ArtifactType.custom_attribute_defelement);

            return new CustomAttributeDefElement(customAttributeDefElementEntity, metadata);
        }
    }

    // Definition

    public PaginatedArtifactList<CustomAttributeDefinition> getAllCustomAttributeDefinitionPaginated(String artifactType, Integer offset, Integer limit, String tenant) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + tenant + ".custom_attribute_definition " +
                        (artifactType != null ? " WHERE ? = ANY(artifact_types) " : ""), Integer.class,
                artifactType != null ? new Object[]{artifactType} : new Object[]{} );
        String query = "SELECT * FROM da_" + tenant + ".custom_attribute_definition " +
                (artifactType != null ? " WHERE ? = ANY(artifact_types) " : "") +
                "offset ? limit ? ";
        List<Object> params = new ArrayList<>();
        if (artifactType != null) params.add(artifactType);
        params.add(offset);
        params.add(limit);
        List<CustomAttributeDefinition> customAttributeDefinitionList = jdbcTemplate.query(query, new CustomAttributeDefinitionRowMapper(), params.toArray());

        PaginatedArtifactList<CustomAttributeDefinition> paginatedArtifactList = new PaginatedArtifactList<>(
                customAttributeDefinitionList, offset, limit, total, "/v1/custom_attribute/definition");
        return paginatedArtifactList;
    }

    public String createCustomAttributeDefinition(UpdatableCustomAttributeDefinitionEntity newCustomAttributeDefinitionEntity, UserDetails userDetails) {
        UUID uuidForSystem = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".custom_attribute_definition " +
                        "(id, \"name\", description, \"type\", multiple_values, default_value, placeholder, minimum, maximum, min_length, max_length, required, created, creator, modified, modifier) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                uuidForSystem, newCustomAttributeDefinitionEntity.getName(), newCustomAttributeDefinitionEntity.getDescription(), newCustomAttributeDefinitionEntity.getType().toString(), newCustomAttributeDefinitionEntity.getMultipleValues(),
                newCustomAttributeDefinitionEntity.getDefaultValue(), newCustomAttributeDefinitionEntity.getPlaceholder(), newCustomAttributeDefinitionEntity.getMinimum(), newCustomAttributeDefinitionEntity.getMaximum(), newCustomAttributeDefinitionEntity.getMinLength(),
                newCustomAttributeDefinitionEntity.getMaxLength(), newCustomAttributeDefinitionEntity.getRequired(), new Timestamp(new java.util.Date().getTime()), userDetails.getUid(), new Timestamp(new java.util.Date().getTime()), userDetails.getUid());

        return uuidForSystem.toString();
    }

    public void patchCustomAttributeDefinition(String customAttributeDefinitionId, UpdatableCustomAttributeDefinitionEntity customAttributeDefinitionEntity,
                                               UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".custom_attribute_definition SET modifier = ?, modified = ? ";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));
        if (customAttributeDefinitionEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(customAttributeDefinitionEntity.getName());
        }
        if (customAttributeDefinitionEntity.getDescription() != null) {
            sets.add("description = ?");
            params.add(customAttributeDefinitionEntity.getDescription());
        }
        if (customAttributeDefinitionEntity.getMultipleValues() != null) {
            sets.add("multiple_values = ?");
            params.add(customAttributeDefinitionEntity.getMultipleValues());
        }
        if (customAttributeDefinitionEntity.getDefaultValue() != null) {
            sets.add("default_value = ?");
            params.add(customAttributeDefinitionEntity.getDefaultValue());
        }
        if (customAttributeDefinitionEntity.getPlaceholder() != null) {
            sets.add("placeholder = ?");
            params.add(customAttributeDefinitionEntity.getPlaceholder());
        }
        if (customAttributeDefinitionEntity.getMinimum() != null) {
            sets.add("\"minimum\" = ?");
            params.add(customAttributeDefinitionEntity.getMinimum());
        }
        if (customAttributeDefinitionEntity.getMaximum() != null) {
            sets.add("\"maximum\" = ?");
            params.add(customAttributeDefinitionEntity.getMaximum());
        }
        if (customAttributeDefinitionEntity.getMinLength() != null) {
            sets.add("\"min_length\" = ?");
            params.add(customAttributeDefinitionEntity.getMinLength());
        }
        if (customAttributeDefinitionEntity.getMaxLength() != null) {
            sets.add("\"max_length\" = ?");
            params.add(customAttributeDefinitionEntity.getMaxLength());
        }
        if (customAttributeDefinitionEntity.getRequired() != null) {
            sets.add("\"required\" = ?");
            params.add(customAttributeDefinitionEntity.getRequired());
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(customAttributeDefinitionId));
            jdbcTemplate.update(query, params.toArray());
        }
    }

    public void deleteCustomAttributeDefinition(String customAttributeDefinitionId, String tenant) {
        String query = "DELETE FROM da_" + tenant + ".custom_attribute_definition\n" +
                "WHERE id=?;";
        jdbcTemplate.update(query, UUID.fromString(customAttributeDefinitionId));
    }

    // Def Element

    public CustomAttributeDefElement getCustomAttributeDefElementById(String customAttributeDefElementId, UserDetails userDetails) {
        if (customAttributeDefElementId == null || customAttributeDefElementId.isEmpty())
            return null;

        List<CustomAttributeDefElement> customAttributeDefElementList = jdbcTemplate.query("SELECT id, \"name\", description, definition_id, history_start, history_end, version_id, created, creator, modified, modifier " +
                        "FROM da_" + userDetails.getTenant() + ".custom_attribute_defelement  " +
                        "where id = ?;",
                new CustomAttributeDefElementRowMapper(), UUID.fromString(customAttributeDefElementId));

        return customAttributeDefElementList.stream().findFirst().orElse(null);
    }

    public PaginatedArtifactList<CustomAttributeDefElement> getAllCustomAttributeDefElementPaginated(String definitionId, Integer offset, Integer limit, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".custom_attribute_defelement " +
                        (definitionId != null ? " WHERE definition_id = ? " : ""), Integer.class,
                (definitionId != null ? new Object[]{UUID.fromString(definitionId)} : new Object[]{}));
        String query = "SELECT * FROM da_" + userDetails.getTenant() + ".custom_attribute_defelement " +
                (definitionId != null ? " WHERE definition_id = ? " : "") +
                "offset ? limit ? ";
        List<CustomAttributeDefElement> customAttributeDefElementList = jdbcTemplate.query(query, new CustomAttributeDefElementRowMapper(),
                (definitionId != null ? new Object[]{UUID.fromString(definitionId), offset, limit} : new Object[]{offset, limit}));

        PaginatedArtifactList<CustomAttributeDefElement> paginatedArtifactList = new PaginatedArtifactList<>(
                customAttributeDefElementList, offset, limit, total, "/v1/custom_attribute/defelement");
        return paginatedArtifactList;
    }

    public boolean existsCustomAttributeDefElement(String definitionId, String defElementId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".custom_attribute_defelement " +
                        "WHERE definition_id = ? and id = ?) AS EXISTS",
                Boolean.class, UUID.fromString(definitionId), UUID.fromString(defElementId));
    }

    public boolean existsCustomAttributeDefElement(String defElementId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".custom_attribute_defelement " +
                        "WHERE id = ?) AS EXISTS",
                Boolean.class, UUID.fromString(defElementId));
    }

    public String createCustomAttributeDefElement(UpdatableCustomAttributeDefElementEntity newCustomAttributeDefElementEntity, UserDetails userDetails) {
        UUID newUuid = UUID.randomUUID();
        Timestamp timestamp = new Timestamp(new java.util.Date().getTime());
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".custom_attribute_defelement " +
                        "(id, \"name\", description, definition_id, created, creator, modified, modifier)\n" +
                        "VALUES(?, ?, ?, ?, ?, ?, ?, ?);",
                newUuid, newCustomAttributeDefElementEntity.getName(), newCustomAttributeDefElementEntity.getDescription(),
                UUID.fromString(newCustomAttributeDefElementEntity.getDefinitionId()), timestamp, userDetails.getUid(), timestamp, userDetails.getUid());
        return newUuid.toString();
    }

    public String createCustomAttributeDefElement(CustomAttributeEnumValue ev, String definitionId, UserDetails userDetails) {
        UpdatableCustomAttributeDefElementEntity e = new UpdatableCustomAttributeDefElementEntity();
        e.setName(ev.getName());
        e.setDescription(ev.getDescription());
        e.setDefinitionId(definitionId);
        return createCustomAttributeDefElement(e, userDetails);
    }

    public void patchCustomAttributeDefElement(String customAttributeDefElementId, UpdatableCustomAttributeDefElementEntity customAttributeDefElementEntity,
                                               UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".custom_attribute_defelement SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));
        if(customAttributeDefElementEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(customAttributeDefElementEntity.getName());
        }
        if(customAttributeDefElementEntity.getDescription() != null) {
            sets.add("description = ?");
            params.add(customAttributeDefElementEntity.getDescription());
        }
        if(customAttributeDefElementEntity.getDefinitionId() != null) {
            sets.add("definition_id = ?");
            params.add(UUID.fromString(customAttributeDefElementEntity.getDefinitionId()));
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(customAttributeDefElementId));
            jdbcTemplate.update(query, params.toArray());
        }
    }

    public void deleteCustomAttributeDefElementsByDefinitionId(String definintionId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".custom_attribute_defelement WHERE definition_id = ?",
                UUID.fromString(definintionId));
    }

    public void deleteCustomAttributeDefElementById(String customAttributeDefElementId, UserDetails userDetails) {
        String query = "DELETE FROM da_" + userDetails.getTenant() + ".custom_attribute_defelement " +
                "WHERE id=?;";
        jdbcTemplate.update(query, UUID.fromString(customAttributeDefElementId));
    }

    // Custom Attribute

    public List<CustomAttributeRecord> getCustomAttributeRecordsByObjectId(String objectId, String tenant) {
        return jdbcTemplate.query("SELECT id, definition_id, object_id, object_type, date_value, number_value, text_value, def_element_id, history_start, history_end, version_id, created, creator, modified, modifier " +
                        "FROM da_" + tenant + ".custom_attribute  " +
                        "where object_id = ?;",
                new CustomAttributeRowMapper(), UUID.fromString(objectId));
    }

    public void modifyCustomAttribute(String customAttributeId, CustomAttributeDefinition caDef, Object newValue, UserDetails userDetails) {
        String query = "UPDATE da_" + userDetails.getTenant() + ".custom_attribute SET modified = ?, modifier = ? ";
        if (caDef.getEntity().getType().equals(AttributeType.Enumerated)) {
            query += ", def_element_id = ? ";
        } else if (caDef.getEntity().getType().equals(AttributeType.String)) {
            query += ", text_value = ? ";
        } else if (caDef.getEntity().getType().equals(AttributeType.Date)) {
            query += ", date_value = ? ";
        } else if (caDef.getEntity().getType().equals(AttributeType.Numeric)) {
            query += ", number_value = ? ";
        }
        query += " WHERE id = ?";
        jdbcTemplate.update(query, new Timestamp(new java.util.Date().getTime()), userDetails.getUid(),
                newValue, UUID.fromString(customAttributeId));
    }

    public void copyCustomAttributes(String sourceId, String targetId, ArtifactType targetType,
                                     UserDetails userDetails) {
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".custom_attribute "
            + "(id, definition_id, object_id, object_type, def_element_id, text_value, date_value, number_value,"
            + "created, creator, modified, modifier) select "
            + "gen_random_uuid(), definition_id, ?, ?, def_element_id, text_value, date_value, number_value,"
            + "created, creator, modified, modifier from da_" + userDetails.getTenant() + ".custom_attribute "
            + "where object_id = ?", UUID.fromString(targetId), targetType.getText(), UUID.fromString(sourceId));
    }

    public List<String> createCustomAttribute(CustomAttribute ca, CustomAttributeDefinition caDef,
                                              String objectId, String objectType, UserDetails userDetails) {
        List<String> res = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (CustomAttributeValue customAttributeValue : ca.getValues()) {
            UUID newId = java.util.UUID.randomUUID();
            Timestamp ts = new Timestamp(new java.util.Date().getTime());
            Object value = null;
            String query = "INSERT INTO da_" + userDetails.getTenant() + ".custom_attribute " +
                    "(id, definition_id, object_id, object_type ";
            if (caDef.getEntity().getType().equals(AttributeType.Enumerated)) {
                if (customAttributeValue.getValue() instanceof LinkedHashMap) {
                    value = UUID.fromString(((LinkedHashMap) customAttributeValue.getValue()).get("id").toString());
                    query += ", def_element_id ";
                } else if (customAttributeValue.getValue() instanceof CustomAttributeEnumValue) {
                    value = UUID.fromString(((CustomAttributeEnumValue) customAttributeValue.getValue()).getId());
                    query += ", def_element_id ";
                }
            } else if (caDef.getEntity().getType().equals(AttributeType.String)) {
                value = (String) customAttributeValue.getValue();
                query += ", text_value ";
            } else if (caDef.getEntity().getType().equals(AttributeType.Date)) {
                try { value = sdf.parse((String)customAttributeValue.getValue()); } //LocalDateTime.parse((String) customAttributeValue.getValue());
                catch (ParseException e) { log.error(e.getMessage(), e); }
                query += ", date_value ";
            } else if (caDef.getEntity().getType().equals(AttributeType.Numeric)) {
                value = customAttributeValue.getValue() == null ? null : Double.parseDouble(customAttributeValue.getValue().toString());
                query += ", number_value ";
            }
            query += ", created, creator, modified, modifier) " +
                    " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(query, newId, UUID.fromString(caDef.getMetadata().getId()), UUID.fromString(objectId), objectType,
                    value, ts, userDetails.getUid(), ts, userDetails.getUid());
            res.add(newId.toString());
        }
        return res;
    }

    private String getCaValValue(Object value) {
        return value instanceof CustomAttributeEnumValue ?
                ((CustomAttributeEnumValue) value).getId() :
                ((LinkedHashMap) value).get("id").toString();
    }

    public void patchCustomAttribute(CustomAttribute ca, CustomAttributeDefinition caDef,
                                     List<CustomAttribute> currentCa, String objectId, String objectType,
                                     UserDetails userDetails) {
        if (ca.getValues().isEmpty()) {
            // New values are empty, remove current values of CA
            deleteCustomAttributesByArtifactIdAndDefinitionId(objectId, caDef.getId(), userDetails);
        } else {
            if (currentCa == null || currentCa.isEmpty()) {
                // Old values are empty, add CA
                createCustomAttribute(ca, caDef, objectId, objectType, userDetails);
            } else {
                CustomAttribute current = currentCa.stream().filter(x -> x.getCustomAttributeDefinitionId().equals(ca.getCustomAttributeDefinitionId()))
                        .findFirst().orElse(null);
                for (CustomAttributeValue caVal : ca.getValues()) {
                    if (current != null) {
                        if (!caDef.getEntity().getMultipleValues()) {
                            if (caDef.getEntity().getType().equals(AttributeType.Enumerated)) {
                                String value = ((LinkedHashMap) caVal.getValue()).get("id").toString();
                                String currentVal = ((LinkedHashMap) current.getValues().get(0).getValue())
                                        .get("id").toString();
                                if (!currentVal.equals(value))
                                    modifyCustomAttribute(current.getValues().get(0).getCustomAttributeId(),
                                            caDef, UUID.fromString(value), userDetails);
                            } else {
                                if (!current.getValues().get(0).getValue().equals(caVal.getValue()))
                                    modifyCustomAttribute(current.getValues().get(0).getCustomAttributeId(),
                                            caDef, caVal.getValue(), userDetails);
                            }
                        } else {
                            // Multiple values
                            if (caDef.getEntity().getType().equals(AttributeType.Enumerated)) {
                                String value = caVal.getValue() instanceof CustomAttributeEnumValue ?
                                        ((CustomAttributeEnumValue) caVal.getValue()).getId() :
                                        ((LinkedHashMap) caVal.getValue()).get("id").toString();
                                if (!current.getValues().stream().map(x -> ((CustomAttributeEnumValue)x.getValue()).getId())
                                        .anyMatch(y -> y.equals(value))) {
                                    CustomAttribute n = new CustomAttribute();
                                    n.setValues(Collections.singletonList(caVal));
                                    createCustomAttribute(n, caDef, objectId, objectType, userDetails);
                                }
                            } else {
                                if (!current.getValues().stream().anyMatch(x -> x.getValue().equals(caVal.getValue()))) {
                                    CustomAttribute n = new CustomAttribute();
                                    n.setValues(Collections.singletonList(caVal));
                                    createCustomAttribute(n, caDef, objectId, objectType, userDetails);
                                }
                            }
                        }
                    } else {

                    }
                }
                if (current != null && caDef.getEntity().getMultipleValues()) {
                    for (CustomAttributeValue caCurrVal : current.getValues()) {
                        if (caDef.getEntity().getType().equals(AttributeType.Enumerated)) {
                            String currentVal = ((CustomAttributeEnumValue) caCurrVal.getValue()).getId();
                            if (!ca.getValues().stream().map(x -> getCaValValue(x.getValue()))
                                    .anyMatch(y -> y.equals(currentVal)))
                                deleteCustomAttributeById(caCurrVal.getCustomAttributeId(), userDetails);
                        } else {
                            if (!ca.getValues().stream().anyMatch(x -> x.getValue().equals(caCurrVal.getValue())))
                                deleteCustomAttributeById(caCurrVal.getCustomAttributeId(), userDetails);
                        }
                    }
                }

            }
        }
    }

    public void deleteCustomAttributesByDefinitionId(String definitionId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".custom_attribute WHERE definition_id = ?",
                UUID.fromString(definitionId));
    }

    public void deleteAllCustomAttributesByArtifactId(String artifactId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".custom_attribute where object_id = ?",
                UUID.fromString(artifactId));
    }

    public void deleteCustomAttributesByArtifactIdAndId(String artifactId, String id, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".custom_attribute where object_id = ? and id = ?",
                UUID.fromString(artifactId), UUID.fromString(id));
    }

    public void deleteCustomAttributesByArtifactIdAndDefinitionId(String artifactId, String definitionId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".custom_attribute where object_id = ? and definition_id = ?",
                UUID.fromString(artifactId), UUID.fromString(definitionId));
    }

    public void deleteCustomAttributesByDefElementId(String defElementId, String definitionId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".custom_attribute where definition_id = ? " +
                " and def_element_id = ?", UUID.fromString(definitionId), UUID.fromString(defElementId));
    }

    public void deleteCustomAttributeById(String id, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".custom_attribute WHERE id = ?",
                UUID.fromString(id));
    }


}
