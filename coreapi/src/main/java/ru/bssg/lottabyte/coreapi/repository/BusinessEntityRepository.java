package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.businessEntity.*;
import ru.bssg.lottabyte.core.model.datatype.DataType;
import ru.bssg.lottabyte.core.model.datatype.DataTypeEntity;
import ru.bssg.lottabyte.core.model.datatype.FlatDataType;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Repository
@Slf4j
public class BusinessEntityRepository extends WorkflowableRepository<BusinessEntity> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = { "tech_name", "definition", "regulation", "alt_names", "domain_id","parent_id", "formula", "examples", "link", "datatype_id", "limits", "roles" };

    public BusinessEntityRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.business_entity.name(), extFields);
        super.setMapper(new BusinessEntityRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class BusinessEntityRowMapper implements RowMapper<BusinessEntity> {
        @Override
        public BusinessEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            BusinessEntityEntity businessEntityEntity = new BusinessEntityEntity();
            businessEntityEntity.setName(rs.getString("name"));
            businessEntityEntity.setTechName(rs.getString("tech_name"));
            businessEntityEntity.setDefinition(rs.getString("definition"));
            businessEntityEntity.setRegulation(rs.getString("regulation"));
            if (rs.getArray("alt_names") != null) {
                String[] array = (String[]) rs.getArray("alt_names").getArray();
                businessEntityEntity.setAltNames(new ArrayList<>(Arrays.asList(array)));
            }
            businessEntityEntity.setDomainId(rs.getString("domain_id"));
            businessEntityEntity.setFormula(rs.getString("formula"));
            businessEntityEntity.setExamples(rs.getString("examples"));
            businessEntityEntity.setLink(rs.getString("link"));
            businessEntityEntity.setDatatypeId(rs.getString("datatype_id"));
            businessEntityEntity.setLimits(rs.getString("limits"));
            businessEntityEntity.setRoles(rs.getString("roles"));
            businessEntityEntity.setParentId(rs.getString("parent_id"));
            return new BusinessEntity(businessEntityEntity,
                    new WorkflowableMetadata(rs, businessEntityEntity.getArtifactType()));
        }
    }

    public static class FlatBusinessEntityRowMapper implements RowMapper<FlatBusinessEntity> {
        @Override
        public FlatBusinessEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatBusinessEntity fi = new FlatBusinessEntity();
            fi.setId(rs.getString("id"));
            fi.setName(rs.getString("name"));
            fi.setVersionId(rs.getInt("version_id"));
            fi.setModified(rs.getTimestamp("modified").toLocalDateTime());
            fi.setTechName(rs.getString("tech_name"));
            fi.setDefinition(rs.getString("definition"));
            fi.setRegulation(rs.getString("regulation"));
            if (rs.getArray("alt_names") != null) {
                String[] array = (String[]) rs.getArray("alt_names").getArray();
                fi.setAltNames(new ArrayList<>(Arrays.asList(array)));
            } else
                fi.setAltNames(new ArrayList<>());
            fi.setState(ArtifactState.valueOf(rs.getString("state")));
            fi.setWorkflowTaskId(rs.getString("workflow_task_id"));
            fi.setDomainId(rs.getString("domain_id"));
            fi.setDomainName(rs.getString("domain_name"));
            fi.setParentId(rs.getString("parent_id"));
            return fi;
        }
    }



    public String createBusinessEntity(UpdatableBusinessEntityEntity newBusinessEntityEntity, String workflowTaskId,
            UserDetails userDetails) {
        UUID newId = newBusinessEntityEntity.getId() != null ? UUID.fromString(newBusinessEntityEntity.getId())
                : UUID.randomUUID();

        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        String altNamesString = newBusinessEntityEntity.getAltNames() != null
                ? String.join(",", newBusinessEntityEntity.getAltNames())
                : null;

        String query = "INSERT INTO da_" + userDetails.getTenant() + ".business_entity " +
                "(id, \"name\", tech_name, definition, regulation, alt_names, state, workflow_task_id, created, creator, modified, modifier, domain_id, formula, examples, link, datatype_id, limits, roles) "
                +
                "VALUES(?, ?, ?, ?, ?, string_to_array(?,','), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(query, newId, newBusinessEntityEntity.getName(),
                newBusinessEntityEntity.getTechName(),
                newBusinessEntityEntity.getDefinition(),
                newBusinessEntityEntity.getRegulation(),
                altNamesString,
                ArtifactState.DRAFT.toString(),
                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                ts, userDetails.getUid(), ts, userDetails.getUid(), newBusinessEntityEntity.getDomainId() == null ? null
                        : UUID.fromString(newBusinessEntityEntity.getDomainId()),
                newBusinessEntityEntity.getFormula(), newBusinessEntityEntity.getExamples(), newBusinessEntityEntity.getLink(),
                newBusinessEntityEntity.getDatatypeId() == null ? null : UUID.fromString(newBusinessEntityEntity.getDatatypeId()),
                newBusinessEntityEntity.getLimits(), newBusinessEntityEntity.getRoles());
        return newId.toString();
    }

    public void patchBusinessEntity(String businessEntityId, UpdatableBusinessEntityEntity businessEntityEntity,
            UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".business_entity SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new Date().getTime()));
        if (businessEntityEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(businessEntityEntity.getName());
        }
        if (businessEntityEntity.getDefinition() != null) {
            sets.add("definition = ?");
            params.add(businessEntityEntity.getDefinition());
        }
        if (businessEntityEntity.getTechName() != null) {
            sets.add("tech_name = ?");
            params.add(businessEntityEntity.getTechName());
        }
        if (businessEntityEntity.getRegulation() != null) {
            sets.add("regulation = ?");
            params.add(businessEntityEntity.getRegulation());
        }
        if (businessEntityEntity.getAltNames() != null) {
            sets.add("alt_names = string_to_array(?,',')");
            String altNamesString = String.join(",", businessEntityEntity.getAltNames());
            params.add(altNamesString);
        }
        if (businessEntityEntity.getDomainId() != null) {
            sets.add("domain_id = ?");
            params.add(businessEntityEntity.getDomainId().isEmpty() ? null
                    : UUID.fromString(businessEntityEntity.getDomainId()));
        }
        if (businessEntityEntity.getParentId() != null) {
            sets.add("parent_id = ?");
            params.add(businessEntityEntity.getParentId().isEmpty() ? null : UUID.fromString(businessEntityEntity.getParentId()));
        }
        if (businessEntityEntity.getFormula() != null) {
            sets.add("formula = ?");
            params.add(businessEntityEntity.getFormula());
        }
        if (businessEntityEntity.getExamples() != null) {
            sets.add("examples = ?");
            params.add(businessEntityEntity.getExamples());
        }
        if (businessEntityEntity.getLink() != null) {
            sets.add("link = ?");
            params.add(businessEntityEntity.getLink());
        }
        if (businessEntityEntity.getDatatypeId() != null) {
            sets.add("datatype_id = ?");
            params.add(businessEntityEntity.getDatatypeId().isEmpty() ? null : UUID.fromString(businessEntityEntity.getDatatypeId()));
        }
        if (businessEntityEntity.getLimits() != null) {
            sets.add("limits = ?");
            params.add(businessEntityEntity.getLimits());
        }
        if (businessEntityEntity.getRoles() != null) {
            sets.add("roles = ?");
            params.add(businessEntityEntity.getRoles());
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(businessEntityId));
            jdbcTemplate.update(query, params.toArray());
        }
    }

    public SearchResponse<FlatBusinessEntity> searchBusinessEntity(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, "tbl1.domain_id", true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "select * from da_" + userDetails.getTenant() + ".business_entity ";
        if (userDetails.getStewardId() != null && searchRequest.getLimitSteward() != null
                && searchRequest.getLimitSteward()) {
            subQuery = subQuery + QueryHelper.getWhereIdInQuery(ArtifactType.business_entity, userDetails);
        }

        subQuery = "SELECT sq.*, wft.workflow_state, t.tags, syn.synonyms, qwe.be_links FROM (" + subQuery + ") as sq left join da_"
                + userDetails.getTenant() + ".workflow_task wft "
                + " on sq.workflow_task_id = wft.id "
                + "left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id "

                + "left join (select rsyn.source_id, string_agg(syn.name, ',') as synonyms from da_" + userDetails.getTenant() + ".business_entity syn join da_" + userDetails.getTenant() + ".reference rsyn on rsyn.reference_type='BUSINESS_ENTITY_TO_BUSINESS_ENTITY' AND rsyn.target_id=syn.id group by rsyn.source_id) syn on syn.source_id=sq.id "
                + "left join (select rlnk.source_id, string_agg(qwe.name, ',') as be_links from da_" + userDetails.getTenant() + ".business_entity qwe join da_" + userDetails.getTenant() + ".reference rlnk on rlnk.reference_type='BUSINESS_ENTITY_TO_BUSINESS_ENTITY_LINK' AND rlnk.target_id=qwe.id group by rlnk.source_id) qwe on qwe.source_id=sq.id ";

        String queryForItems = "SELECT distinct tbl1.*, domain.name AS domain_name FROM (" + subQuery + ") tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id "
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatBusinessEntity> flatItems = jdbcTemplate.query(queryForItems, new FlatBusinessEntityRowMapper(),
                whereValues.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id "
                + where;
        Long total = jdbcTemplate.queryForObject(queryForTotal, Long.class, whereValues.toArray());

        SearchResponse<FlatBusinessEntity> res = new SearchResponse<>(total.intValue(), searchRequest.getLimit(),
                searchRequest.getOffset(), flatItems);

        return res;
    }

    public List<BusinessEntity> getSynonymsByBEId(String beId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT be.* FROM da_" + userDetails.getTenant() + ".business_entity be "
                + " join da_" + userDetails.getTenant() + ".reference r on r.target_id = be.id "
                + " where r.source_id = ? AND r.reference_type='BUSINESS_ENTITY_TO_BUSINESS_ENTITY'", new BusinessEntityRowMapper(), UUID.fromString(beId));
    }

    public List<BusinessEntity> getBELinksByBEId(String beId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT be.* FROM da_" + userDetails.getTenant() + ".business_entity be "
                + " join da_" + userDetails.getTenant() + ".reference r on r.target_id = be.id "
                + " where r.source_id = ? AND r.reference_type='BUSINESS_ENTITY_TO_BUSINESS_ENTITY_LINK'", new BusinessEntityRowMapper(), UUID.fromString(beId));
    }

    public boolean existsBusinessEntityWithDomain(String domainId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".business_entity " +
                        "WHERE domain_id is not null and domain_id = ? and state = ?) AS EXISTS",
                Boolean.class, UUID.fromString(domainId), ArtifactState.PUBLISHED.toString());
    }

}
