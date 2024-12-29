package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.dal.FlatItemRowMapper;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.businessEntity.BusinessEntity;
import ru.bssg.lottabyte.core.model.dataasset.DataAsset;
import ru.bssg.lottabyte.core.model.dataasset.DataAssetEntity;
import ru.bssg.lottabyte.core.model.dataasset.FlatDataAsset;
import ru.bssg.lottabyte.core.model.dataentity.*;
import ru.bssg.lottabyte.core.model.domain.DomainEntity;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;
import ru.bssg.lottabyte.core.model.entitySample.UpdatableEntitySampleDQRule;
import ru.bssg.lottabyte.core.model.indicator.Indicator;
import ru.bssg.lottabyte.core.model.product.*;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.util.JDBCUtil;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static ru.bssg.lottabyte.coreapi.util.QueryHelper.getSearchSQLParts;

@Repository
@Slf4j
public class ProductRepository extends WorkflowableRepository<Product> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = { "domain_id","entity_query_id", "problem", "consumer", "value", "finance_source", "link",
    "limits", "limits_internal", "roles" };

    public ProductRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.product.name(), extFields);
        super.setMapper(new ProductRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class FlatProductRowMapper implements RowMapper<FlatProduct> {
        @Override
        public FlatProduct mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatProduct flatProduct = new FlatProduct();
            try {
                ProductEntity productEntity = new ProductEntity();
                productEntity.setName(rs.getString("name"));
                productEntity.setDescription(rs.getString("description"));
                productEntity.setDomainId(rs.getString("domain_id"));
                productEntity.setEntityQueryId(rs.getString("entity_query_id"));
                productEntity.setProblem(rs.getString("problem"));
                productEntity.setConsumer(rs.getString("consumer"));
                productEntity.setValue(rs.getString("value"));
                productEntity.setFinanceSource(rs.getString("finance_source"));
                if (rs.getObject("short_description") != null)
                    productEntity.setShortDescription(rs.getString("short_description"));

                flatProduct = new FlatProduct(
                        new Product(productEntity, new WorkflowableMetadata(rs, productEntity.getArtifactType())));
                flatProduct.setDomainName(rs.getString("domain_name"));
                return flatProduct;
            } catch (Exception e) {
                return flatProduct;
            }
        }
    }

    private static class FlatProductTypeRowMapper implements RowMapper<FlatProductType> {
        @Override
        public FlatProductType mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatProductType flatProductType = new FlatProductType();
            try {
                ProductTypeEntity productTypeEntity = new ProductTypeEntity();
                productTypeEntity.setName(rs.getString("name"));
                productTypeEntity.setDescription(rs.getString("description"));
                productTypeEntity.setArtifactType(ArtifactType.product_type);

                ProductType pt = new ProductType(productTypeEntity,
                        new Metadata(rs, productTypeEntity.getArtifactType()));
                flatProductType = new FlatProductType(pt);
                return flatProductType;
            } catch (Exception e) {
                return flatProductType;
            }
        }
    }

    private static class FlatProductSupplyVariantRowMapper implements RowMapper<FlatProductSupplyVariant> {
        @Override
        public FlatProductSupplyVariant mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatProductSupplyVariant flatProductSupplyVariant = new FlatProductSupplyVariant();
            try {
                ProductSupplyVariantEntity productSupplyVariantEntity = new ProductSupplyVariantEntity();
                productSupplyVariantEntity.setName(rs.getString("name"));
                productSupplyVariantEntity.setDescription(rs.getString("description"));
                productSupplyVariantEntity.setArtifactType(ArtifactType.product_supply_variant);

                ProductSupplyVariant pt = new ProductSupplyVariant(productSupplyVariantEntity,
                        new Metadata(rs, productSupplyVariantEntity.getArtifactType()));
                flatProductSupplyVariant = new FlatProductSupplyVariant(pt);
                return flatProductSupplyVariant;
            } catch (Exception e) {
                return flatProductSupplyVariant;
            }
        }
    }

    static class ProductRowMapper implements RowMapper<Product> {
        public static Product mapProductRow(ResultSet rs) throws SQLException {
            ProductEntity productEntity = new ProductEntity();
            productEntity.setName(rs.getString("name"));
            productEntity.setDescription(rs.getString("description"));
            productEntity.setVersionId(rs.getInt("version_id"));
            productEntity.setDomainId(rs.getString("domain_id"));
            productEntity.setEntityQueryId(rs.getString("entity_query_id"));
            productEntity.setProblem(rs.getString("problem"));
            productEntity.setConsumer(rs.getString("consumer"));
            productEntity.setValue(rs.getString("value"));
            productEntity.setFinanceSource(rs.getString("finance_source"));
            productEntity.setLink(rs.getString("link"));
            productEntity.setLimits(rs.getString("limits"));
            productEntity.setLimits_internal(rs.getString("limits_internal"));
            productEntity.setRoles(rs.getString("roles"));
            if (rs.getObject("short_description") != null)
                productEntity.setShortDescription(rs.getString("short_description"));

            return new Product(productEntity, new WorkflowableMetadata(rs, productEntity.getArtifactType()));
        }

        @Override
        public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
            return mapProductRow(rs);
        }
    }

    static class ProductTypeRowMapper implements RowMapper<ProductType> {
        public static ProductType mapProductTypeRow(ResultSet rs) throws SQLException {
            ProductTypeEntity productTypeEntity = new ProductTypeEntity();
            productTypeEntity.setName(rs.getString("name"));
            productTypeEntity.setDescription(rs.getString("description"));

            return new ProductType(productTypeEntity, new Metadata(rs, productTypeEntity.getArtifactType()));
        }

        @Override
        public ProductType mapRow(ResultSet rs, int rowNum) throws SQLException {
            return mapProductTypeRow(rs);
        }
    }

    static class ProductSupplyVariantRowMapper implements RowMapper<ProductSupplyVariant> {
        public static ProductSupplyVariant mapProductSupplyVariantRow(ResultSet rs) throws SQLException {
            ProductSupplyVariantEntity productSupplyVariantEntity = new ProductSupplyVariantEntity();
            productSupplyVariantEntity.setName(rs.getString("name"));
            productSupplyVariantEntity.setDescription(rs.getString("description"));

            return new ProductSupplyVariant(productSupplyVariantEntity,
                    new Metadata(rs, productSupplyVariantEntity.getArtifactType()));
        }

        @Override
        public ProductSupplyVariant mapRow(ResultSet rs, int rowNum) throws SQLException {
            return mapProductSupplyVariantRow(rs);
        }
    }

    public String createProduct(UpdatableProductEntity product, String workflowTaskId, UserDetails userDetails) {
        UUID newId = product.getId() != null ? UUID.fromString(product.getId()) : UUID.randomUUID();

        Timestamp ts = new Timestamp(new Date().getTime());

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                + ".product (id, \"name\", description, short_description, history_start, history_end, version_id, created, creator, modified, modifier, state, workflow_task_id, domain_id, problem, consumer, value, finance_source, link, limits,   limits_internal, roles,entity_query_id) "
                +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                newId, product.getName(), product.getDescription(), product.getShortDescription(),
                ts, ts, 0, ts, userDetails.getUid(), ts, userDetails.getUid(), ArtifactState.DRAFT.toString(),
                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                product.getDomainId() == null ? null : UUID.fromString(product.getDomainId()),
                product.getProblem(), product.getConsumer(), product.getValue(), product.getFinanceSource(),
                product.getLink(), product.getLimits(), product.getLimits_internal(), product.getRoles(),product.getEntityQueryId());

        return newId.toString();
    }

    public void updateProduct(String productId, UpdatableProductEntity productEntity, boolean updateNulls, UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".product SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));
        if (updateNulls || productEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(productEntity.getName());
        }
        if (updateNulls || productEntity.getDescription() != null) {
            sets.add("description = ?");
            params.add(productEntity.getDescription());
        }
        if (updateNulls || productEntity.getShortDescription() != null) {
            sets.add("short_description = ?");
            params.add(productEntity.getShortDescription());
        }
        if (updateNulls || productEntity.getDomainId() != null) {
            sets.add("domain_id = ?");
            params.add((productEntity.getDomainId() == null || productEntity.getDomainId().isEmpty()) ?
                    null : UUID.fromString(productEntity.getDomainId()));
        }
        if (updateNulls || productEntity.getEntityQueryId() != null) {
            sets.add("entity_query_id = ?");
            params.add((productEntity.getEntityQueryId() == null || productEntity.getEntityQueryId().isEmpty()) ?
                    null : UUID.fromString(productEntity.getEntityQueryId()));
        }
        if (updateNulls || productEntity.getProblem() != null) {
            sets.add("problem = ?");
            params.add(productEntity.getProblem());
        }
        if (updateNulls || productEntity.getConsumer() != null) {
            sets.add("consumer = ?");
            params.add(productEntity.getConsumer());
        }
        if (updateNulls || productEntity.getValue() != null) {
            sets.add("value = ?");
            params.add(productEntity.getValue());
        }
        if (updateNulls || productEntity.getFinanceSource() != null) {
            sets.add("finance_source = ?");
            params.add(productEntity.getFinanceSource());
        }
        if (updateNulls || productEntity.getLink() != null) {
            sets.add("link = ?");
            params.add(productEntity.getLink());
        }
        if (updateNulls || productEntity.getLimits() != null) {
            sets.add("limits = ?");
            params.add(productEntity.getLimits());
        }
        if (updateNulls || productEntity.getLimits_internal() != null) {
            sets.add("limits_internal = ?");
            params.add(productEntity.getLimits_internal());
        }
        if (updateNulls || productEntity.getRoles() != null) {
            sets.add("roles = ?");
            params.add(productEntity.getRoles());
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
        }
        query += " WHERE id = ?";
        params.add(UUID.fromString(productId));
        jdbcTemplate.update(query, params.toArray());
    }

    public String publishProductDraft(String draftProductId, String publishedProductId, UserDetails userDetails) {
        String res = null;
        if (publishedProductId != null) {
            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant()
                    + ".product e SET name = draft.name, description = draft.description, short_description = draft.short_description, entity_query_id = draft.entity_query_id, domain_id = draft.domain_id, problem = draft.problem, consumer = draft.consumer,"
                    + " value = draft.value, finance_source = draft.finance_source, link = draft.link, limits = draft.limits, limits_internal = draft.limits_internal, roles = draft.roles,"
                    + " ancestor_draft_id = draft.id, modified = draft.modified, modifier = draft.modifier "
                    + " from (select id, name, description, short_description, modified, modifier, domain_id, entity_query_id, problem, consumer, value, finance_source, link, limits, limits_internal, roles FROM da_"
                    + userDetails.getTenant() + ".product) as draft where e.id = ? and draft.id = ?",
                    UUID.fromString(publishedProductId), UUID.fromString(draftProductId));
            res = publishedProductId;
        } else {
            UUID newId = UUID.randomUUID();
            jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                    + ".product (id, name, description, short_description, state, workflow_task_id, "
                    + "published_id, published_version_id, ancestor_draft_id, created, creator, modified, modifier, domain_id, problem, consumer, value, finance_source, link, limits, limits_internal, roles,entity_query_id) "
                    + "SELECT ?, name, description, short_description, ?, ?, ?, ?, ?, created, creator, modified, modifier, domain_id, problem, consumer, value, finance_source, link, limits, limits_internal, roles ,entity_query_id "
                    + "FROM da_" + userDetails.getTenant() + ".product where id = ?",
                    newId, ArtifactState.PUBLISHED.toString(), null, null, null,
                    UUID.fromString(draftProductId), UUID.fromString(draftProductId));
            res = newId.toString();
        }
        jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".product set state = ? where id = ?",
                ArtifactState.DRAFT_HISTORY.toString(), UUID.fromString(draftProductId));
        return res;
    }

    public String createProductDraft(String publishedProductId, String draftId, String workflowTaskId,
            UserDetails userDetails) {
        UUID newId = draftId != null ? UUID.fromString(draftId) : UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                + ".product (id, name, description, short_description, state, workflow_task_id, published_id, published_version_id, created, creator, modified, modifier, domain_id, problem, consumer, value, finance_source, link, limits, limits_internal, roles,entity_query_id) "
                +
                "SELECT ?, name, description, short_description, ?, ?, id, version_id, created, creator, modified, modifier, domain_id, problem, consumer, value, finance_source, link, limits, limits_internal, roles, entity_query_id FROM da_"
                + userDetails.getTenant() + ".product where id = ?",
                newId, ArtifactState.DRAFT.toString(),
                workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                UUID.fromString(publishedProductId));
        return newId.toString();
    }

    public SearchResponse<FlatProduct> searchProducts(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, "tbl1.domain_id", true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "select product.*, true as has_access from da_" + userDetails.getTenant() + ".product ";
        if (userDetails.getStewardId() != null && searchRequest.getLimitSteward() != null
                && searchRequest.getLimitSteward()) {
            subQuery = subQuery + QueryHelper.getWhereIdInQuery(ArtifactType.product, userDetails);
        }
        subQuery = "SELECT sq.*, wft.workflow_state FROM (" + subQuery + ") as sq left join da_"
                + userDetails.getTenant() + ".workflow_task wft "
                + " on sq.workflow_task_id = wft.id ";
        subQuery = "SELECT distinct sq.*, r.indicators, ea.entity_attributes, t.tags, pt.product_types from ("
                + subQuery + ") sq left join ("
                + "select r.source_id, string_agg(i.name, ',') as indicators from da_" + userDetails.getTenant()
                + ".indicator i "
                + "join da_" + userDetails.getTenant() + ".reference r on r.target_id = i.id group by r.source_id"
                + ") r on r.source_id = sq.id left join ("
                + "select r.source_id, string_agg(ea.name, ',') as entity_attributes from da_" + userDetails.getTenant()
                + ".entity_attribute ea "
                + "join da_" + userDetails.getTenant() + ".reference r on r.target_id = ea.id "
                + "group by r.source_id "
                + ") ea on ea.source_id = sq.id "
                + "left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id "
                + "left join (select rpt.source_id, string_agg(pt.name, ',') as product_types from da_"
                + userDetails.getTenant() + ".product_type pt join da_" + userDetails.getTenant()
                + ".reference rpt on rpt.target_id=pt.id group by rpt.source_id) pt on pt.source_id=sq.id ";

        String queryForItems = "SELECT tbl1.*, domain.name AS domain_name FROM (" + subQuery + ") as tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id "
                + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatProduct> flatItems = jdbcTemplate.query(queryForItems, new FlatProductRowMapper(), whereValues.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id "
                + where;
        Integer total = jdbcTemplate.queryForObject(queryForTotal, Integer.class, whereValues.toArray());

        SearchResponse<FlatProduct> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public SearchResponse<FlatProductType> searchProductTypes(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "select product_type.*, true as has_access from da_" + userDetails.getTenant()
                + ".product_type ";

        subQuery = "SELECT sq.* FROM (" + subQuery + ") as sq ";

        String queryForItems = "SELECT * FROM (" + subQuery + ") as tbl1 " + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatProductType> flatItems = jdbcTemplate.query(queryForItems, new FlatProductTypeRowMapper(),
                whereValues.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") tbl1 " + where;
        Integer total = jdbcTemplate.queryForObject(queryForTotal, Integer.class, whereValues.toArray());

        SearchResponse<FlatProductType> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public SearchResponse<FlatProductSupplyVariant> searchProductSupplyVariants(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "select product_supply_variant.*, true as has_access from da_" + userDetails.getTenant()
                + ".product_supply_variant ";

        subQuery = "SELECT sq.* FROM (" + subQuery + ") as sq ";

        String queryForItems = "SELECT * FROM (" + subQuery + ") as tbl1 " + where +
                " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatProductSupplyVariant> flatItems = jdbcTemplate.query(queryForItems,
                new FlatProductSupplyVariantRowMapper(), whereValues.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") tbl1 " + where;
        Integer total = jdbcTemplate.queryForObject(queryForTotal, Integer.class, whereValues.toArray());

        SearchResponse<FlatProductSupplyVariant> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public ProductType getProductTypeById(String id, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".product_type WHERE id=?",
                new ProductTypeRowMapper(), UUID.fromString(id)).stream().findFirst().orElse(null);
    }

    public ProductSupplyVariant getProductSupplyVariantById(String id, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".product_supply_variant WHERE id=?",
                new ProductSupplyVariantRowMapper(), UUID.fromString(id)).stream().findFirst().orElse(null);
    }

    public List<Indicator> getIndicatorsByProductId(String productId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT i.* FROM da_" + userDetails.getTenant() + ".indicator i "
                + " join da_" + userDetails.getTenant() + ".reference r on r.target_id = i.id "
                + " where r.source_id = ?", new IndicatorRepository.IndicatorRowMapper(), UUID.fromString(productId));
    }

    public List<Product> getProductsByProductId(String productId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT i.* FROM da_" + userDetails.getTenant() + ".product i "
                + " join da_" + userDetails.getTenant() + ".reference r on r.target_id = i.id "
                + " where r.source_id = ?", new ProductRepository.ProductRowMapper(), UUID.fromString(productId));
    }

    public List<ProductType> getProductTypesByProductId(String productId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT pt.* FROM da_" + userDetails.getTenant() + ".product_type pt "
                + " join da_" + userDetails.getTenant() + ".reference r on r.target_id = pt.id "
                + " where r.source_id = ?", new ProductTypeRowMapper(), UUID.fromString(productId));
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public EntitySampleDQRule createDQRule(String productId,
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
                + ".entity_sample_to_dq_rule (id,  dq_rule_id, settings, created, creator, modified, modifier, disabled,  product_id, send_mail) VALUES (?,?,?,?,?,?,?,?,?,?)",
                id, UUID.fromString(entitySampleDQRule.getDqRuleId()),
                entitySampleDQRule.getSettings(), now, userDetails.getUid(), now, userDetails.getUid(),
                entitySampleDQRule.getDisabled(),
                UUID.fromString(productId),
                entitySampleDQRule.getSendMail());

        return esp;
    }

    public void removeDQRule(String id, UserDetails userDetails) {
        String query = "DELETE FROM da_" + userDetails.getTenant() + ".entity_sample_to_dq_rule WHERE id = ?";
        jdbcTemplate.update(query, UUID.fromString(id));
    }

    public void addDQRule(String productId,
            EntitySampleDQRule entitySampleDQRule, UserDetails userDetails) {
        // UUID id = entitySampleDQRule.getId() == null ? UUID.randomUUID() :
        // UUID.fromString(entitySampleDQRule.getId());
        UUID id = UUID.randomUUID();

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                + ".entity_sample_to_dq_rule (id,  dq_rule_id, settings, created, creator, modified, modifier, disabled,  product_id, send_mail) VALUES (?,?,?,?,?,?,?,?,?,?)",
                id, UUID.fromString(entitySampleDQRule.getEntity().getDqRuleId()),
                entitySampleDQRule.getEntity().getSettings(), now, userDetails.getUid(), now, userDetails.getUid(),
                entitySampleDQRule.getEntity().getDisabled(),
                UUID.fromString(productId),
                entitySampleDQRule.getEntity().getSendMail());

    }

    public List<ProductSupplyVariant> getProductSupplyVariantsByProductId(String productId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT psv.* FROM da_" + userDetails.getTenant() + ".product_supply_variant psv "
                + " join da_" + userDetails.getTenant() + ".reference r on r.target_id = psv.id "
                + " where r.source_id = ?", new ProductSupplyVariantRowMapper(), UUID.fromString(productId));
    }

    public boolean existsProductWithDomain(String domainId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".product " +
                "WHERE domain_id is not null and domain_id = ? and state = ?) AS EXISTS",
                Boolean.class, UUID.fromString(domainId), ArtifactState.PUBLISHED.toString());
    }

    public boolean existsProductWithQuery(String queryId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".product " +
                        "WHERE entity_query_id is not null and entity_query_id = ? and state = ?) AS EXISTS",
                Boolean.class, UUID.fromString(queryId), ArtifactState.PUBLISHED.toString());
    }

    public List<BusinessEntity> getTermLinksById(String productId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT be.* FROM da_" + userDetails.getTenant() + ".business_entity be "
                + " join da_" + userDetails.getTenant() + ".reference r on r.target_id = be.id "
                + " where r.source_id = ? AND r.reference_type='PRODUCT_TO_BUSINESS_ENTITY_LINK'", new BusinessEntityRepository.BusinessEntityRowMapper(), UUID.fromString(productId));
    }

    public void updateDQRule(String productId, String ruleId, EntitySampleDQRule entitySampleDQRule, UserDetails userDetails) {
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".entity_sample_to_dq_rule SET settings=?, modified=?, modifier=?, disabled=?, send_mail=? WHERE dq_rule_id=? AND product_id=?",
                entitySampleDQRule.getEntity().getSettings(), now, userDetails.getUid(), entitySampleDQRule.getEntity().getDisabled(),
                entitySampleDQRule.getEntity().getSendMail(), UUID.fromString(ruleId), UUID.fromString(productId));
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public EntitySampleDQRule createDQRuleLink(String productId,
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
                        + ".entity_sample_to_dq_rule (id, dq_rule_id, settings, created, creator, modified, modifier, disabled, product_id, send_mail, history_id, published_id, ancestor_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id, UUID.fromString(entitySampleDQRule.getDqRuleId()),
                entitySampleDQRule.getSettings(), now, userDetails.getUid(), now, userDetails.getUid(),
                entitySampleDQRule.getDisabled(),
                UUID.fromString(productId),
                entitySampleDQRule.getSendMail(),
                entitySampleDQRule.getHistoryId(),
                UUID.fromString(entitySampleDQRule.getPublishedId()),
                entitySampleDQRule.getAncestorId() == null ? null : UUID.fromString(entitySampleDQRule.getAncestorId()));

        return esp;
    }

    public void addDQRuleLink(String productId, String publishedId,
                              EntitySampleDQRule entitySampleDQRule, UserDetails userDetails) {

        UUID id = UUID.randomUUID();

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                        + ".entity_sample_to_dq_rule (id,  dq_rule_id, settings, created, creator, modified, modifier, disabled,  product_id, send_mail, history_id, published_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                id, UUID.fromString(entitySampleDQRule.getEntity().getDqRuleId()),
                entitySampleDQRule.getEntity().getSettings(), now, userDetails.getUid(), now, userDetails.getUid(),
                entitySampleDQRule.getEntity().getDisabled(),
                UUID.fromString(productId),
                entitySampleDQRule.getEntity().getSendMail(), entitySampleDQRule.getEntity().getHistoryId(),
                publishedId == null ? null : UUID.fromString(publishedId));

    }
}
