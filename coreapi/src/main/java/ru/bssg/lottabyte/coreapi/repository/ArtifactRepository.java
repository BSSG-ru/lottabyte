package ru.bssg.lottabyte.coreapi.repository;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.tools.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.domain.FlatDomain;
import ru.bssg.lottabyte.core.model.reference.ReferenceType;
import ru.bssg.lottabyte.core.model.relation.Relation;
import ru.bssg.lottabyte.core.model.tag.Tag;
import ru.bssg.lottabyte.core.model.tag.TagEntity;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.ui.model.dashboard.DashboardEntity;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelData;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelLinkData;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelNodeData;
import ru.bssg.lottabyte.core.ui.model.gojs.UpdatableGojsModelData;
import ru.bssg.lottabyte.core.usermanagement.model.Language;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.config.ApplicationConfig;
import ru.bssg.lottabyte.coreapi.service.TagService;
import ru.bssg.lottabyte.coreapi.service.WorkflowService;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.bssg.lottabyte.coreapi.util.QueryHelper.getSearchSQLParts;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ArtifactRepository {

    private final JdbcTemplate jdbcTemplate;
    private final WorkflowService workflowService;
    private final TagService tagService;
    private final TagRepository tagRepository;
    private final AmazonS3 amazonS3;
    private final ApplicationConfig applicationConfig;

    public Integer getSettingsCount(String type, UserDetails userDetails) {
        Integer res = 0;
        switch (type) {
            case "users":
                res = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usermgmt.platform_users WHERE tenant=?", Integer.class, userDetails.getTenant());
                break;
            case "system_connections":
                res = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".system_connection", Integer.class);
                break;
            case "roles":
                res = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM usermgmt.user_roles WHERE tenant=?", Integer.class, userDetails.getTenant());
                break;
            case "groups":
                res = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM usermgmt.external_groups WHERE tenant=?", Integer.class, userDetails.getTenant());
                break;
            case "workflows":
                res = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".workflow_settings", Integer.class);
                break;
        }

        return res;
    }

    public Integer getArtifactsCount(String artifactType, Boolean limitSteward, UserDetails userDetails) {

        if (artifactType.equals("draft")) {

            List<String> subqueries = new ArrayList<>();
            subqueries.add(getSearchSubQuery(ArtifactType.domain, userDetails));
            subqueries.add(getSearchSubQuery(ArtifactType.system, userDetails));
            subqueries.add(getSearchSubQuery(ArtifactType.entity, userDetails));
            subqueries.add(getSearchSubQuery(ArtifactType.entity_query, userDetails));
            subqueries.add(getSearchSubQuery(ArtifactType.data_asset, userDetails));
            subqueries.add(getSearchSubQuery(ArtifactType.indicator, userDetails));
            subqueries.add(getSearchSubQuery(ArtifactType.business_entity, userDetails));
            subqueries.add(getSearchSubQuery(ArtifactType.product, userDetails));

            String query = "SELECT * FROM ((" + org.apache.commons.lang3.StringUtils.join(subqueries, ") UNION (")
                    + ")) AS tbl1 ";

            return jdbcTemplate.queryForObject("SELECT COUNT(distinct id) FROM (" + query + ") as tbl2 ",
                    Integer.class);
        } else {

            String state = "";
            String entityQueryJoin = "";
            String domainsFilter = "";
            ArtifactType at = ArtifactType.valueOf(artifactType);

            if (workflowService.isWorkflowEnabled(at)) {
                if (at.equals(ArtifactType.entity_sample)) {
                    state = " and entity_query.STATE = '" + ArtifactState.PUBLISHED + "' ";
                    entityQueryJoin = " join da_" + userDetails.getTenant()
                            + ".entity_query on entity_sample.entity_query_id = entity_query.id";
                }
            } else {
                if (at.equals(ArtifactType.entity_sample)) {
                    state = " where entity_query.STATE = '" + ArtifactState.PUBLISHED + "' ";
                    entityQueryJoin = " join da_" + userDetails.getTenant()
                            + ".entity_query on entity_sample.entity_query_id = entity_query.id";
                }
                if (at.equals(ArtifactType.meta_database)) {
                    state = " where state = '" + ArtifactState.PUBLISHED + "' ";
                }
            }

            if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
                switch (at) {
                    case domain:
                        domainsFilter = " domain.id IN ('"
                                + StringUtils.join(userDetails.getUserDomains().toArray(), "','") + "')";
                        break;
                    case system:
                        domainsFilter = " system.id IN (SELECT system_id FROM da_" + userDetails.getTenant()
                                + ".system_to_domain WHERE domain_id IN('"
                                + StringUtils.join(userDetails.getUserDomains().toArray(), "','") + "'))";
                        break;
                    case task:
                        domainsFilter = " task.query_id IN (SELECT id FROM da_" + userDetails.getTenant()
                                + ".entity_query WHERE system_id IN (SELECT system_id FROM da_"
                                + userDetails.getTenant() + ".system_to_domain WHERE domain_id IN('"
                                + StringUtils.join(userDetails.getUserDomains().toArray(), "','") + "')))";
                        break;
                    case entity:
                        domainsFilter = " entity.id IN (SELECT entity_id FROM da_" + userDetails.getTenant()
                                + ".entity_to_system WHERE system_id IN (SELECT system_id FROM da_"
                                + userDetails.getTenant() + ".system_to_domain WHERE domain_id IN('"
                                + StringUtils.join(userDetails.getUserDomains().toArray(), "','") + "')))";
                        break;
                    case entity_query:
                        domainsFilter = " entity_query.system_id IN (SELECT system_id FROM da_"
                                + userDetails.getTenant() + ".system_to_domain WHERE domain_id IN('"
                                + StringUtils.join(userDetails.getUserDomains().toArray(), "','") + "'))";
                        break;
                    case entity_sample:
                        domainsFilter = " entity_sample.system_id IN (SELECT system_id FROM da_"
                                + userDetails.getTenant() + ".system_to_domain WHERE domain_id IN('"
                                + StringUtils.join(userDetails.getUserDomains().toArray(), "','") + "'))";
                        break;
                    case data_asset:
                        domainsFilter = " data_asset.domain_id IN ('"
                                + StringUtils.join(userDetails.getUserDomains().toArray(), "','") + "')";
                        break;
                    case indicator:
                        domainsFilter = " indicator.domain_id IN ('"
                                + StringUtils.join(userDetails.getUserDomains().toArray(), "','") + "')";
                        break;
                    case business_entity:
                        domainsFilter = " business_entity.domain_id IN ('"
                                + StringUtils.join(userDetails.getUserDomains().toArray(), "','") + "')";
                        break;
                    case product:
                        domainsFilter = " product.domain_id IN ('"
                                + StringUtils.join(userDetails.getUserDomains().toArray(), "','") + "')";
                        break;
                    default:
                        break;
                }
            }

            String query;
            if (limitSteward && userDetails.getStewardId() != null) {
                if (workflowService.isWorkflowEnabled(at)) {
                    query = "SELECT distinct " + at.getText() + ".ID FROM da_" + userDetails.getTenant() + "."
                            + at.getText() + " " +
                            QueryHelper.getJoinQuery(at, userDetails) + " WHERE " + at + ".state = 'PUBLISHED' " + state
                            + (domainsFilter.isEmpty() ? "" : (" AND " + domainsFilter));

                } else {
                    if (!domainsFilter.isEmpty()) {
                        if (state.isEmpty())
                            domainsFilter = " where " + domainsFilter;
                        else
                            domainsFilter = " and " + domainsFilter;
                    }
                    query = "SELECT distinct " + at.getText() + ".ID FROM da_" + userDetails.getTenant() + "."
                            + at.getText() + " " +
                            QueryHelper.getJoinQuery(at, userDetails) + state + domainsFilter;

                }
                return jdbcTemplate.queryForObject("SELECT COUNT(ID) FROM (" + query + ") as a", Integer.class);
            } else {
                if (workflowService.isWorkflowEnabled(at)) {
                    query = "SELECT COUNT(" + at.getText() + ".ID) FROM da_" + userDetails.getTenant() +
                            "." + at.getText() + entityQueryJoin + " WHERE state = 'PUBLISHED' " + state
                            + (domainsFilter.isEmpty() ? "" : (" AND " + domainsFilter));

                } else {
                    if (!domainsFilter.isEmpty()) {
                        if (state.isEmpty())
                            domainsFilter = " where " + domainsFilter;
                        else
                            domainsFilter = " and " + domainsFilter;
                    }
                    query = "SELECT COUNT(" + at.getText() + ".ID) FROM da_" + userDetails.getTenant() +
                            "." + at.getText() + entityQueryJoin + state + domainsFilter;
                }
                return jdbcTemplate.queryForObject(query, Integer.class);
            }
        }
    }

    public String getArtifactName(ArtifactType artifactType, String artifactId, UserDetails userDetails) {
        try {
            return jdbcTemplate.queryForObject("SELECT NAME FROM da_" + userDetails.getTenant() + "." +
                    artifactType.getText() + " where id = ? LIMIT 1", String.class, UUID.fromString(artifactId));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public String getArtifactState(String artifactType, String artifactId, UserDetails userDetails) {
        try {
            return jdbcTemplate.queryForObject("SELECT state FROM da_" + userDetails.getTenant() + "." +
                    artifactType + " where id = ?", String.class, UUID.fromString(artifactId));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Boolean existsArtifact(String artifactId, ArtifactType artifactType, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS (select id from da_" + userDetails.getTenant() + "." +
                artifactType.getText() + " where id = ?) as exists", Boolean.class, UUID.fromString(artifactId));
    }

    @AllArgsConstructor
    @Data
    public class ArtifactModelNode {
        protected String nodeId;
        protected String artifactType;
        protected Integer locX;
        protected Integer locY;
        protected String lineageDir;

        public ArtifactModelNode(String nodeId, String artifactType) {
            this(nodeId, artifactType, null, null, "");
        }

        public ArtifactModelNode(String nodeId, String artifactType, String lineageDir) {
            this(nodeId, artifactType, null, null, lineageDir);
        }

        public ArtifactModelNode(String nodeId, String artifactType, Integer locX, Integer locY) {
            this(nodeId, artifactType, locX, locY, "");
        }
    }

    private void addArtifactModelLink(List<String> linkedNodeIds, String artifactId, String fromId, String toId,
            UserDetails userDetails) {
        if (linkedNodeIds.contains(fromId + toId))
            return;
        linkedNodeIds.add(fromId + toId);
        jdbcTemplate.update(
                "INSERT INTO da_" + userDetails.getTenant()
                        + ".model_links (id, artifact_id, from_node_id, to_node_id) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), UUID.fromString(artifactId), UUID.fromString(fromId), UUID.fromString(toId));
    }

    private void addProductLinks(String artifactId, List<String> linkedNodeIds, List<String> productIds, List<String> nodeIds, UserDetails userDetails) {
        if (!productIds.isEmpty()) {
            jdbcTemplate.query("SELECT source_id, target_id FROM da_" + userDetails.getTenant() + ".reference WHERE reference_type='PRODUCT_TO_PRODUCT' AND ("
                    + "source_id IN ('" + StringUtils.join(productIds.toArray(), "','") + "') OR target_id IN ('"
                    + StringUtils.join(productIds.toArray(), "','") + "'))", new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    String srcId = rs.getString("source_id");
                    String tgtId = rs.getString("target_id");
                    if (nodeIds.contains(srcId) && nodeIds.contains(tgtId))
                        addArtifactModelLink(linkedNodeIds, artifactId, srcId, tgtId, userDetails);
                }
            });
        }
    }

    public void generateArtifactModel(String artifactId, String artifactType, UserDetails userDetails) {
        ArtifactType at = ArtifactType.fromString(artifactType);

        List<String> nodeIds = new ArrayList<>();
        List<String> linkedNodeIds = new ArrayList<>();
        List<List<ArtifactModelNode>> rowsTop = new ArrayList<>();
        rowsTop.add(new ArrayList<>());
        rowsTop.add(new ArrayList<>());
        rowsTop.add(new ArrayList<>());
        List<List<ArtifactModelNode>> rowsBottom = new ArrayList<>();
        rowsBottom.add(new ArrayList<>());
        rowsBottom.add(new ArrayList<>());
        List<List<ArtifactModelNode>> cols = new ArrayList<>();
        cols.add(new ArrayList<>());
        cols.add(new ArrayList<>());
        cols.add(new ArrayList<>());
        cols.add(new ArrayList<>());
        cols.add(new ArrayList<>());
        cols.add(new ArrayList<>());
        cols.add(new ArrayList<>());
        cols.add(new ArrayList<>());
        cols.add(new ArrayList<>());
        cols.add(new ArrayList<>());
        cols.add(new ArrayList<>());

        cols.get(5).add(new ArtifactModelNode(artifactId, artifactType, null, null));
        nodeIds.add(artifactId);

        jdbcTemplate.query("SELECT from_node_id, to_node_id FROM da_" + userDetails.getTenant()
                + ".model_links WHERE artifact_id=?", new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        linkedNodeIds.add(rs.getString("from_node_id") + rs.getString("to_node_id"));
                    }
                }, UUID.fromString(artifactId));

        List<String> productIds = new ArrayList<>();
        Map<String,List<String>> productToQueryIds = new HashMap<>();

        switch (at) {
            case entity_query:
                List<String> esIds = new ArrayList<>();
                jdbcTemplate.query(
                        "SELECT es.id, mo.id AS mo_id, t.query_id FROM da_" + userDetails.getTenant() + ".entity_sample es"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_sample_property esp ON esp.entity_sample_id=es.id"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r ON r.source_id=esp.id AND r.reference_type='SAMPLE_PROPERTY_TO_META_COLUMN'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".meta_column mc ON r.target_id=mc.id"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".meta_object mo ON mc.meta_object_id=mo.id"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r2 ON r2.source_id=mc.meta_database_id AND r2.reference_type='META_DATABASE_TO_TASK'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".task t ON r2.target_id=t.id"
                                + " WHERE es.entity_query_id=?",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String esId = rs.getString("id");
                                String metaObjectId = rs.getString("mo_id");
                                String queryId = rs.getString("query_id");

                                if (!nodeIds.contains(esId)) {
                                    esIds.add(esId);
                                    nodeIds.add(esId);
                                    rowsBottom.get(1).add(new ArtifactModelNode(esId, "entity_sample"));
                                }

                                if (metaObjectId != null && !nodeIds.contains(metaObjectId)) {
                                    nodeIds.add(metaObjectId);
                                    rowsBottom.get(1).add(new ArtifactModelNode(metaObjectId, "meta_object"));
                                }

                                if (queryId != null && !nodeIds.contains(queryId)) {
                                    nodeIds.add(queryId);
                                    cols.get(5).add(new ArtifactModelNode(queryId, "entity_query"));
                                }
                            }
                        }, UUID.fromString(artifactId));
                if (!esIds.isEmpty()) {

                    for (String esId : esIds) {


                        //addArtifactModelLink(linkedNodeIds, artifactId, esId, artifactId, userDetails);
                    }

                    jdbcTemplate.query(
                            "SELECT da.id AS asset_id, i.id AS indicator_id, p.id AS product_id, p.domain_id, be.id AS be_id, p.entity_query_id AS p_eq_id, es.id AS sample_id FROM da_"
                                    + userDetails.getTenant() + ".entity_sample es JOIN da_" + userDetails.getTenant()
                                    + ".data_asset da ON es.entity_id=da.entity_id AND es.system_id=da.system_id AND da.state='PUBLISHED' "
                                    + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r ON r.reference_type='INDICATOR_TO_DATA_ASSET' AND r.target_id=da.id"
                                    + " LEFT JOIN da_" + userDetails.getTenant() + ".indicator i ON r.source_id=i.id AND i.state='PUBLISHED'"
                                    + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r2 ON r2.target_id=i.id"
                                    + " LEFT JOIN da_" + userDetails.getTenant() + ".product p ON r2.source_id=p.id AND p.state='PUBLISHED' AND r2.reference_type='PRODUCT_TO_INDICATOR'"
                                    + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r3 ON r3.source_id=p.id AND r3.reference_type='PRODUCT_TO_BUSINESS_ENTITY_LINK'"
                                    + " LEFT JOIN da_" + userDetails.getTenant() + ".business_entity be ON r3.target_id=be.id AND be.state='PUBLISHED'"
                                    + " WHERE es.id IN ('" + StringUtils.join(esIds.toArray(), "','") + "')",
                            new RowCallbackHandler() {
                                @Override
                                public void processRow(ResultSet rs) throws SQLException {
                                    String assetId = rs.getString("asset_id");
                                    String indicatorId = rs.getString("indicator_id");
                                    String productId = rs.getString("product_id");
                                    String domainId = rs.getString("domain_id");
                                    String sampleId = rs.getString("sample_id");
                                    String beId = rs.getString("be_id");
                                    String prodQueryId = rs.getString("p_eq_id");

                                    if (productId != null && prodQueryId != null) {
                                        if (!productToQueryIds.containsKey(productId))
                                            productToQueryIds.put(productId, new ArrayList<>());
                                        if (!productToQueryIds.get(productId).contains(prodQueryId))
                                            productToQueryIds.get(productId).add(prodQueryId);
                                    }

                                    if (productId != null && !productIds.contains(productId))
                                        productIds.add(productId);

                                    if (assetId != null && !nodeIds.contains(assetId)) {
                                        nodeIds.add(assetId);
                                        cols.get(7).add(new ArtifactModelNode(assetId, "data_asset", "right"));
                                    }

                                    if (indicatorId != null && !nodeIds.contains(indicatorId)) {
                                        nodeIds.add(indicatorId);
                                        cols.get(8).add(new ArtifactModelNode(indicatorId, "indicator", "right"));
                                    }

                                    if (productId != null && !nodeIds.contains(productId)) {
                                        nodeIds.add(productId);
                                        cols.get(9).add(new ArtifactModelNode(productId, "product", "right"));

                                    }

                                    if (beId != null && !nodeIds.contains(beId)) {
                                        nodeIds.add(beId);
                                        rowsTop.get(1).add(new ArtifactModelNode(beId, "business_entity"));
                                    }

                                    if (domainId != null && !nodeIds.contains(domainId)) {
                                        nodeIds.add(domainId);
                                        rowsTop.get(0).add(new ArtifactModelNode(domainId, "domain"));

                                    }

                                    if (assetId != null) {
                                        //addArtifactModelLink(linkedNodeIds, artifactId, assetId, sampleId, userDetails);
                                        if (indicatorId != null) {
                                            //addArtifactModelLink(linkedNodeIds, artifactId, indicatorId, assetId, userDetails);
                                            if (productId != null) {
                                                //addArtifactModelLink(linkedNodeIds, artifactId, productId, indicatorId, userDetails);
                                                if (beId != null) {
                                                    //addArtifactModelLink(linkedNodeIds, artifactId, productId, beId, userDetails);
                                                }
                                                if (domainId != null) {
                                                    //addArtifactModelLink(linkedNodeIds, artifactId, domainId, productId, userDetails);

                                                }
                                            }
                                        }
                                    }

                                }
                            });
                }

                jdbcTemplate.query("SELECT eq.system_id, eq.entity_id, d.id as domain_id FROM da_" + userDetails.getTenant()
                                + ".entity_query eq"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".system_to_domain s2d ON s2d.system_id=eq.system_id"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".domain d ON s2d.domain_id=d.id AND d.state='PUBLISHED'"
                                + " WHERE eq.id=?", new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String systemId = rs.getString("system_id");
                                String domainId = rs.getString("domain_id");
                                String entityId = rs.getString("entity_id");

                                if (systemId != null && !nodeIds.contains(systemId)) {
                                    nodeIds.add(systemId);
                                    cols.get(3).add(new ArtifactModelNode(systemId, "system", "left"));
                                }

                                if (domainId != null && !nodeIds.contains(domainId)) {
                                    nodeIds.add(domainId);
                                    rowsTop.get(0).add(new ArtifactModelNode(domainId, "domain"));
                                }

                                if (entityId != null && !nodeIds.contains(entityId)) {
                                    nodeIds.add(entityId);
                                    rowsTop.get(2).add(new ArtifactModelNode(entityId, "entity"));
                                }

                                if (systemId != null) {
                                    //addArtifactModelLink(linkedNodeIds, artifactId, artifactId, systemId, userDetails);

                                    //if (domainId != null)
                                      //  addArtifactModelLink(linkedNodeIds, artifactId, systemId, domainId, userDetails);
                                }

                                //if (entityId != null)
                                  //  addArtifactModelLink(linkedNodeIds, artifactId, artifactId, entityId, userDetails);
                            }
                        }, UUID.fromString(artifactId));
                break;
            case entity:
                jdbcTemplate.query(
                        "SELECT da.id AS asset_id, i.id AS indicator_id, p.id AS product_id, p.domain_id, be.id AS be_id, p.entity_query_id AS p_eq_id FROM da_"
                                + userDetails.getTenant() + ".entity e JOIN da_" + userDetails.getTenant()
                                + ".data_asset da ON da.entity_id=e.id AND da.state='PUBLISHED' "
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r ON r.reference_type='INDICATOR_TO_DATA_ASSET' AND r.target_id=da.id"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".indicator i ON r.source_id=i.id AND i.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r2 ON r2.target_id=i.id"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".product p ON r2.source_id=p.id AND p.state='PUBLISHED' AND r2.reference_type='PRODUCT_TO_INDICATOR'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".business_entity be ON be.domain_id=p.domain_id AND be.state='PUBLISHED'"
                                + " WHERE e.id=?",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String assetId = rs.getString("asset_id");
                                String indicatorId = rs.getString("indicator_id");
                                String productId = rs.getString("product_id");
                                String domainId = rs.getString("domain_id");
                                String beId = rs.getString("be_id");
                                String prodQueryId = rs.getString("p_eq_id");

                                if (productId != null && prodQueryId != null) {
                                    if (!productToQueryIds.containsKey(productId))
                                        productToQueryIds.put(productId, new ArrayList<>());
                                    if (!productToQueryIds.get(productId).contains(prodQueryId))
                                        productToQueryIds.get(productId).add(prodQueryId);
                                }

                                if (productId != null && !productIds.contains(productId))
                                    productIds.add(productId);

                                if (assetId != null && !nodeIds.contains(assetId)) {
                                    nodeIds.add(assetId);
                                    cols.get(6).add(new ArtifactModelNode(assetId, "data_asset", "right"));
                                }

                                if (indicatorId != null && !nodeIds.contains(indicatorId)) {
                                    nodeIds.add(indicatorId);
                                    cols.get(7).add(new ArtifactModelNode(indicatorId, "indicator", "right"));
                                }

                                if (productId != null && !nodeIds.contains(productId)) {
                                    nodeIds.add(productId);
                                    cols.get(8).add(new ArtifactModelNode(productId, "product", "right"));

                                }

                                if (beId != null && !nodeIds.contains(beId)) {
                                    nodeIds.add(beId);
                                    rowsTop.get(1).add(new ArtifactModelNode(beId, "business_entity"));
                                }

                                if (domainId != null && !nodeIds.contains(domainId)) {
                                    nodeIds.add(domainId);
                                    rowsTop.get(0).add(new ArtifactModelNode(domainId, "domain"));

                                }

                                if (assetId != null) {
                                    //addArtifactModelLink(linkedNodeIds, artifactId, assetId, artifactId, userDetails);
                                    if (indicatorId != null) {
                                        //addArtifactModelLink(linkedNodeIds, artifactId, indicatorId, assetId, userDetails);
                                        if (productId != null) {
                                            //addArtifactModelLink(linkedNodeIds, artifactId, productId, indicatorId, userDetails);
                                            if (domainId != null) {
                                                //addArtifactModelLink(linkedNodeIds, artifactId, domainId, productId, userDetails);
                                                //if (beId != null)
                                                    //addArtifactModelLink(linkedNodeIds, artifactId, beId, domainId, userDetails);
                                            }
                                        }
                                    }
                                }

                                //if (productId != null && prodQueryId != null && nodeIds.contains(productId) && nodeIds.contains(prodQueryId))
                                  //  addArtifactModelLink(linkedNodeIds, artifactId, productId, prodQueryId, userDetails);
                            }
                        },
                        UUID.fromString(artifactId));
                break;
            case entity_sample:
                jdbcTemplate.query(
                        "SELECT da.id AS asset_id, i.id AS indicator_id, p.id AS product_id, p.domain_id, be.id AS be_id, p.entity_query_id AS p_eq_id, be.domain_id as be_domain_id, e.id AS entity_id FROM da_"
                                + userDetails.getTenant() + ".entity_sample es JOIN da_" + userDetails.getTenant()
                                + ".data_asset da ON es.entity_id=da.entity_id AND es.system_id=da.system_id AND da.state='PUBLISHED' "
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r ON r.reference_type='INDICATOR_TO_DATA_ASSET' AND r.target_id=da.id"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".indicator i ON r.source_id=i.id AND i.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r2 ON r2.target_id=i.id"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".product p ON r2.source_id=p.id AND p.state='PUBLISHED' AND r2.reference_type='PRODUCT_TO_INDICATOR'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r3 ON r3.source_id=p.id AND r3.reference_type='PRODUCT_TO_BUSINESS_ENTITY_LINK'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".business_entity be ON r3.target_id=be.id AND be.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity e ON e.id=es.entity_id AND e.state='PUBLISHED'"
                                + " WHERE es.id=?",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String assetId = rs.getString("asset_id");
                                String indicatorId = rs.getString("indicator_id");
                                String productId = rs.getString("product_id");
                                String domainId = rs.getString("domain_id");
                                String beDomainId = rs.getString("be_domain_id");
                                String beId = rs.getString("be_id");
                                String prodQueryId = rs.getString("p_eq_id");
                                String entityId = rs.getString("entity_id");

                                if (productId != null && prodQueryId != null) {
                                    if (!productToQueryIds.containsKey(productId))
                                        productToQueryIds.put(productId, new ArrayList<>());
                                    if (!productToQueryIds.get(productId).contains(prodQueryId))
                                        productToQueryIds.get(productId).add(prodQueryId);
                                }

                                if (productId != null && !productIds.contains(productId))
                                    productIds.add(productId);

                                if (assetId != null && !nodeIds.contains(assetId)) {
                                    nodeIds.add(assetId);
                                    cols.get(6).add(new ArtifactModelNode(assetId, "data_asset", "right"));
                                }

                                if (indicatorId != null && !nodeIds.contains(indicatorId)) {
                                    nodeIds.add(indicatorId);
                                    cols.get(7).add(new ArtifactModelNode(indicatorId, "indicator", "right"));
                                }

                                if (productId != null && !nodeIds.contains(productId)) {
                                    nodeIds.add(productId);
                                    cols.get(8).add(new ArtifactModelNode(productId, "product", "right"));

                                }

                                if (beId != null && !nodeIds.contains(beId)) {
                                    nodeIds.add(beId);
                                    rowsTop.get(1).add(new ArtifactModelNode(beId, "business_entity"));
                                }

                                if (domainId != null && !nodeIds.contains(domainId)) {
                                    nodeIds.add(domainId);
                                    rowsTop.get(0).add(new ArtifactModelNode(domainId, "domain"));
                                }
                                if (beDomainId != null && !nodeIds.contains(beDomainId)) {
                                    nodeIds.add(beDomainId);
                                    rowsTop.get(0).add(new ArtifactModelNode(beDomainId, "domain"));
                                }

                                if (entityId != null && !nodeIds.contains(entityId)) {
                                    nodeIds.add(entityId);
                                    rowsTop.get(2).add(new ArtifactModelNode(entityId, "entity"));
                                }

                                if (assetId != null) {
                                    //addArtifactModelLink(linkedNodeIds, artifactId, assetId, artifactId, userDetails);
                                    if (indicatorId != null) {
                                        //addArtifactModelLink(linkedNodeIds, artifactId, indicatorId, assetId, userDetails);
                                        if (productId != null) {
                                            //addArtifactModelLink(linkedNodeIds, artifactId, productId, indicatorId, userDetails);

                                            if (beId != null) {
                                                //addArtifactModelLink(linkedNodeIds, artifactId, productId, beId, userDetails);

                                                //if (beDomainId != null)
                                                  //  addArtifactModelLink(linkedNodeIds, artifactId, beId, beDomainId, userDetails);
                                            }

                                            if (domainId != null) {
                                                //addArtifactModelLink(linkedNodeIds, artifactId, domainId, productId, userDetails);

                                            }
                                        }
                                    }
                                }
                            }
                        },
                        UUID.fromString(artifactId));

                jdbcTemplate.query("SELECT es.entity_query_id, eq.system_id, d.id as domain_id FROM da_" + userDetails.getTenant()
                        + ".entity_sample es JOIN da_" + userDetails.getTenant() + ".entity_query eq ON es.entity_query_id=eq.id "
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".system_to_domain s2d ON s2d.system_id=eq.system_id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".domain d ON s2d.domain_id=d.id AND d.state='PUBLISHED'"
                        + " WHERE es.id=?",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String qId = rs.getString("entity_query_id");
                                String sId = rs.getString("system_id");
                                String domainId = rs.getString("domain_id");

                                if (domainId != null && !nodeIds.contains(domainId)) {
                                    nodeIds.add(domainId);
                                    rowsTop.get(0).add(new ArtifactModelNode(domainId, "domain"));
                                }

                                if (qId != null && !nodeIds.contains(qId)) {
                                    nodeIds.add(qId);
                                    rowsBottom.get(0).add(new ArtifactModelNode(qId, "entity_query"));
                                }

                                if (qId != null && sId != null && !nodeIds.contains(sId)) {
                                    nodeIds.add(sId);
                                    cols.get(4).add(new ArtifactModelNode(sId, "system", "left"));
                                }

                                if (qId != null) {
                                    //addArtifactModelLink(linkedNodeIds, artifactId, artifactId, qId, userDetails);

                                    if (sId != null) {
                                        //addArtifactModelLink(linkedNodeIds, artifactId, qId, sId, userDetails);

                                        //if (domainId != null)
                                            //addArtifactModelLink(linkedNodeIds, artifactId, sId, domainId, userDetails);
                                    }
                                }
                            }
                        }, UUID.fromString(artifactId));

                jdbcTemplate.query("SELECT DISTINCT mo.id, t.query_id AS query_id FROM da_" + userDetails.getTenant() + ".entity_sample_property esp "
                                + "JOIN da_" + userDetails.getTenant() + ".reference r ON r.source_id=esp.id AND r.reference_type='SAMPLE_PROPERTY_TO_META_COLUMN' "
                                + "JOIN da_" + userDetails.getTenant() + ".meta_column mc ON r.target_id=mc.id "
                                + "JOIN da_" + userDetails.getTenant() + ".meta_object mo ON mc.meta_object_id=mo.id "
                                + "LEFT JOIN da_" + userDetails.getTenant() + ".reference rq ON rq.source_id=mc.meta_database_id AND rq.reference_type='META_DATABASE_TO_TASK' "
                                + "LEFT JOIN da_" + userDetails.getTenant() + ".task t ON rq.target_id=t.id "
                                + "WHERE esp.entity_sample_id=?",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String metaTableId = rs.getString("id");
                                String queryId = rs.getString("query_id");
                                if (metaTableId != null && !nodeIds.contains(metaTableId)) {
                                    nodeIds.add(metaTableId);
                                    cols.get(4).add(new ArtifactModelNode(metaTableId, "meta_object"));
                                }
                                if (queryId != null && !nodeIds.contains(queryId)) {
                                    nodeIds.add(queryId);
                                    rowsBottom.get(0).add(new ArtifactModelNode(queryId, "entity_query"));
                                }
                            }
                        },
                        UUID.fromString(artifactId)
                );
                break;
            case data_asset:
                jdbcTemplate.query("SELECT e.id as e_id, be.id AS be_id, be.domain_id as be_domain_id FROM da_" + userDetails.getTenant() + ".data_asset da "
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".entity e ON da.entity_id=e.id AND e.state='PUBLISHED'"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r5 ON r5.source_id=e.id AND reference_type='DATA_ENTITY_TO_BUSINESS_ENTITY'"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".business_entity be ON be.id=r5.target_id AND be.state='PUBLISHED'"
                        + " WHERE da.id=?"
                        , new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String entityId = rs.getString("e_id");
                                String beId = rs.getString("be_id");
                                String beDomainId = rs.getString("be_domain_id");

                                if (entityId != null && !nodeIds.contains(entityId)) {
                                    nodeIds.add(entityId);
                                    rowsTop.get(2).add(new ArtifactModelNode(entityId, "entity"));
                                }

                                if (beId != null && !nodeIds.contains(beId)) {
                                    nodeIds.add(beId);
                                    rowsTop.get(1).add(new ArtifactModelNode(beId, "business_entity"));
                                }

                                if (beDomainId != null && !nodeIds.contains(beDomainId)) {
                                    nodeIds.add(beDomainId);
                                    rowsTop.get(0).add(new ArtifactModelNode(beDomainId, "domain"));
                                }

                                if (entityId != null) {
                                    //addArtifactModelLink(linkedNodeIds, artifactId, artifactId, entityId, userDetails);

                                    if (beId != null) {
                                        //addArtifactModelLink(linkedNodeIds, artifactId, entityId, beId, userDetails);

                                        //if (beDomainId != null)
                                            //addArtifactModelLink(linkedNodeIds, artifactId, beId, beDomainId, userDetails);
                                    }
                                }
                            }
                        }, UUID.fromString(artifactId));
                jdbcTemplate.query("SELECT i.id AS indicator_id, p.id AS product_id, p.domain_id, be.id AS be_id, p.entity_query_id AS p_eq_id, be2.id AS be2_id, be.domain_id as be_domain_id, be2.domain_id as be2_domain_id FROM da_"
                        + userDetails.getTenant() + ".reference r JOIN da_" + userDetails.getTenant()
                        + ".indicator i ON r.source_id=i.id AND i.state='PUBLISHED'"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r2 ON r2.target_id=i.id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".product p ON r2.source_id=p.id AND p.state='PUBLISHED' AND r2.reference_type='PRODUCT_TO_INDICATOR'"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r3 ON r3.source_id=p.id AND r3.reference_type='PRODUCT_TO_BUSINESS_ENTITY_LINK'"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".business_entity be ON r3.target_id=be.id AND be.state='PUBLISHED'"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r5 ON r5.source_id=i.id AND r5.reference_type='INDICATOR_TO_BUSINESS_ENTITY_LINK'"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".business_entity be2 ON be2.id=r5.target_id AND be2.state='PUBLISHED'"
                        + " WHERE r.target_id=? AND r.reference_type='INDICATOR_TO_DATA_ASSET' ",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String indicatorId = rs.getString("indicator_id");
                                String productId = rs.getString("product_id");
                                String domainId = rs.getString("domain_id");
                                String beId = rs.getString("be_id");
                                String be2Id = rs.getString("be2_id");
                                String prodQueryId = rs.getString("p_eq_id");
                                String beDomainId = rs.getString("be_domain_id");
                                String be2DomainId = rs.getString("be2_domain_id");

                                if (productId != null && prodQueryId != null) {
                                    if (!productToQueryIds.containsKey(productId))
                                        productToQueryIds.put(productId, new ArrayList<>());
                                    if (!productToQueryIds.get(productId).contains(prodQueryId))
                                        productToQueryIds.get(productId).add(prodQueryId);
                                }

                                if (productId != null && !productIds.contains(productId))
                                    productIds.add(productId);

                                if (indicatorId != null && !nodeIds.contains(indicatorId)) {
                                    nodeIds.add(indicatorId);
                                    cols.get(6).add(new ArtifactModelNode(indicatorId, "indicator", "right"));
                                }

                                if (productId != null && !nodeIds.contains(productId)) {
                                    nodeIds.add(productId);
                                    cols.get(7).add(new ArtifactModelNode(productId, "product", "right"));
                                }

                                if (domainId != null && !nodeIds.contains(domainId)) {
                                    nodeIds.add(domainId);
                                    rowsTop.get(0).add(new ArtifactModelNode(domainId, "domain"));
                                }
                                if (beDomainId != null && !nodeIds.contains(beDomainId)) {
                                    nodeIds.add(beDomainId);
                                    rowsTop.get(0).add(new ArtifactModelNode(beDomainId, "domain"));
                                }
                                if (be2DomainId != null && !nodeIds.contains(be2DomainId)) {
                                    nodeIds.add(be2DomainId);
                                    rowsTop.get(0).add(new ArtifactModelNode(be2DomainId, "domain"));
                                }

                                if (beId != null && !nodeIds.contains(beId)) {
                                    nodeIds.add(beId);
                                    rowsTop.get(1).add(new ArtifactModelNode(beId, "business_entity"));
                                }
                                if (be2Id != null && !nodeIds.contains(be2Id)) {
                                    nodeIds.add(be2Id);
                                    rowsTop.get(1).add(new ArtifactModelNode(be2Id, "business_entity"));
                                }

                                if (indicatorId != null) {
                                    //addArtifactModelLink(linkedNodeIds, artifactId, indicatorId, artifactId, userDetails);

                                    if (be2Id != null) {
                                        //addArtifactModelLink(linkedNodeIds, artifactId, indicatorId, be2Id, userDetails);
                                        //if (be2DomainId != null)
                                          //  addArtifactModelLink(linkedNodeIds, artifactId, be2Id, be2DomainId, userDetails);
                                    }

                                    if (productId != null) {
                                        //addArtifactModelLink(linkedNodeIds, artifactId, productId, indicatorId, userDetails);

                                        if (beId != null) {
                                            //addArtifactModelLink(linkedNodeIds, artifactId, productId, beId, userDetails);
                                            //if (beDomainId != null)
                                              //  addArtifactModelLink(linkedNodeIds, artifactId, beId, beDomainId, userDetails);
                                        }

                                        if (domainId != null) {
                                            //addArtifactModelLink(linkedNodeIds, artifactId, domainId, productId, userDetails);

                                        }
                                    }
                                }
                            }
                        }, UUID.fromString(artifactId));

                jdbcTemplate.query("SELECT es.id, es.system_id, es.entity_query_id, mc.meta_object_id FROM da_" + userDetails.getTenant()
                        + ".entity_sample es"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_sample_property esp ON esp.entity_sample_id=es.id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r ON r.source_id=esp.id AND r.reference_type='SAMPLE_PROPERTY_TO_META_COLUMN'"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".meta_column mc ON r.target_id=mc.id"
                        + " JOIN da_" + userDetails.getTenant()
                        + ".data_asset da ON es.entity_id = da.entity_id AND es.system_id=da.system_id WHERE da.id=?",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String sampleId = rs.getString("id");
                                String systemId = rs.getString("system_id");
                                String queryId = rs.getString("entity_query_id");
                                String metaObjectId = rs.getString("meta_object_id");

                                if (sampleId != null && !nodeIds.contains(sampleId)) {
                                    nodeIds.add(sampleId);
                                    rowsBottom.get(1).add(new ArtifactModelNode(sampleId, "entity_sample"));
                                }
                                if (queryId != null && !nodeIds.contains(queryId)) {
                                    nodeIds.add(queryId);
                                    rowsBottom.get(0).add(new ArtifactModelNode(queryId, "entity_query"));
                                }
                                if (systemId != null && !nodeIds.contains(systemId)) {
                                    nodeIds.add(systemId);
                                    cols.get(4).add(new ArtifactModelNode(systemId, "system", "left"));
                                }

                                if (metaObjectId != null && !nodeIds.contains(metaObjectId)) {
                                    nodeIds.add(metaObjectId);
                                    rowsBottom.get(1).add(new ArtifactModelNode(metaObjectId, "meta_object"));
                                }
                            }
                        }, UUID.fromString(artifactId));
                break;
            case domain:
                jdbcTemplate.query("SELECT be.id AS be_id FROM da_" + userDetails.getTenant() + ".business_entity be"
                + " WHERE be.state='PUBLISHED' AND be.domain_id=?", new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        String beId = rs.getString("be_id");
                        if (beId != null && !nodeIds.contains(beId)) {
                            nodeIds.add(beId);
                            rowsTop.get(1).add(new ArtifactModelNode(beId, "business_entity", null, null));
                        }

                        if (beId != null) {
                            //addArtifactModelLink(linkedNodeIds, artifactId, beId, artifactId, userDetails);
                        }
                    }
                }, UUID.fromString(artifactId));
                jdbcTemplate.query(
                        "SELECT p.id AS product_id, r2.target_id AS indicator_id, r5.target_id as be_id, p.entity_query_id AS p_eq_id, da.id AS asset_id, da.entity_id, es.id AS sample_id, es.entity_query_id, es.system_id, mc.meta_object_id FROM da_" + userDetails.getTenant() + ".product p"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r2 ON r2.source_id=p.id AND r2.reference_type='PRODUCT_TO_INDICATOR' AND p.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".indicator i ON r2.reference_type='PRODUCT_TO_INDICATOR' AND r2.target_id=i.id AND i.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r ON r.source_id=i.id AND r.reference_type='INDICATOR_TO_DATA_ASSET'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".data_asset da ON r.target_id=da.id AND da.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r5 ON r5.source_id=da.entity_id AND r5.reference_type='DATA_ENTITY_TO_BUSINESS_ENTITY'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_sample es ON da.system_id=es.system_id AND da.entity_id=es.entity_id"

                                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_sample_property esp ON esp.entity_sample_id=es.id"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r6 ON r6.source_id=esp.id AND r6.reference_type='SAMPLE_PROPERTY_TO_META_COLUMN'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".meta_column mc ON r6.target_id=mc.id"

                                + " WHERE p.domain_id=? AND p.state='PUBLISHED'",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String productId = rs.getString("product_id");
                                String indicatorId = rs.getString("indicator_id");
                                String assetId = rs.getString("asset_id");
                                String entityId = rs.getString("entity_id");
                                String sampleId = rs.getString("sample_id");
                                String queryId = rs.getString("entity_query_id");
                                String systemId = rs.getString("system_id");
                                String prodQueryId = rs.getString("p_eq_id");
                                String metaObjectId = rs.getString("meta_object_id");

                                if (productId != null && prodQueryId != null) {
                                    if (!productToQueryIds.containsKey(productId))
                                        productToQueryIds.put(productId, new ArrayList<>());
                                    if (!productToQueryIds.get(productId).contains(prodQueryId))
                                        productToQueryIds.get(productId).add(prodQueryId);
                                }

                                if (productId != null && !productIds.contains(productId))
                                    productIds.add(productId);

                                if (productId != null && !nodeIds.contains(productId)) {
                                    nodeIds.add(productId);
                                    cols.get(4).add(new ArtifactModelNode(productId, "product", "left"));
                                }
                                if (indicatorId != null && !nodeIds.contains(indicatorId)) {
                                    nodeIds.add(indicatorId);
                                    cols.get(3).add(new ArtifactModelNode(indicatorId, "indicator", "left"));
                                }
                                if (assetId != null && !nodeIds.contains(assetId)) {
                                    nodeIds.add(assetId);
                                    cols.get(2).add(new ArtifactModelNode(assetId, "data_asset", "left"));
                                }
                                if (sampleId != null && !nodeIds.contains(sampleId)) {
                                    nodeIds.add(sampleId);
                                    rowsBottom.get(1).add(new ArtifactModelNode(sampleId, "entity_sample"));
                                }
                                if (metaObjectId != null && !nodeIds.contains(metaObjectId)) {
                                    nodeIds.add(metaObjectId);
                                    rowsBottom.get(1).add(new ArtifactModelNode(metaObjectId, "meta_object"));
                                }
                                if (entityId != null && !nodeIds.contains(entityId)) {
                                    nodeIds.add(entityId);
                                    rowsTop.get(2).add(new ArtifactModelNode(entityId, "entity"));

                                    for (String attrId : jdbcTemplate.queryForList(
                                            "SELECT id FROM da_" + userDetails.getTenant()
                                                    + ".entity_attribute WHERE entity_id=?",
                                            String.class, UUID.fromString(entityId))) {
                                        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                                                + ".model_nodes (id, artifact_id, node_id, parent_node_id, node_artifact_type) VALUES (?,?,?,?,'entity_attribute')",
                                                UUID.randomUUID(), UUID.fromString(artifactId), UUID.fromString(attrId),
                                                UUID.fromString(entityId));
                                    }
                                }
                                if (queryId != null && !nodeIds.contains(queryId)) {
                                    nodeIds.add(queryId);
                                    rowsBottom.get(0).add(new ArtifactModelNode(queryId, "entity_query"));
                                }
                                if (systemId != null && !nodeIds.contains(systemId)) {
                                    nodeIds.add(systemId);
                                    cols.get(1).add(new ArtifactModelNode(systemId, "system", "left"));
                                }
                            }
                        }, UUID.fromString(artifactId));
                break;
            case product:

                List<String> artifactIds = getRelatedProdIdsForProduct(artifactId, userDetails);
                for (String id : artifactIds) {
                    if (!id.equals(artifactId)) {
                        cols.get(5).add(new ArtifactModelNode(id, "product", "left"));
                        nodeIds.add(id);
                    }
                }
                List<String> ascIds = getAscendantProdIdsForProduct(artifactId, userDetails);
                for (String id : ascIds) {
                    if (!id.equals(artifactId)) {
                        cols.get(6).add(new ArtifactModelNode(id, "product", "right"));
                        nodeIds.add(id);
                        artifactIds.add(id);
                    }
                }

                jdbcTemplate.query(
                        "SELECT eq.id as entity_query_id, be.id as be_id, p.entity_query_id AS p_eq_id, s.id as system_id, "
                            + "p2.id AS p2_id, p.id AS p_id, e.id as entity_id, d2.id as domain_id, "
                            + "da.id as data_asset_id, es.id as da_es_id, es_eq.id as es_eq_id, mc.meta_object_id FROM da_" + userDetails.getTenant()
                            + ".product p"

                            + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_query eq ON p.entity_query_id=eq.id AND eq.state='PUBLISHED'"


                            + " LEFT JOIN da_" + userDetails.getTenant() + ".system s ON eq.system_id=s.id AND s.state='PUBLISHED'"
                            + " LEFT JOIN da_" + userDetails.getTenant() + ".reference rp ON rp.source_id=p.id AND rp.reference_type='PRODUCT_TO_PRODUCT'"
                            + " LEFT JOIN da_" + userDetails.getTenant() + ".product p2 ON p2.id=rp.target_id AND p2.state='PUBLISHED'"
                            + " LEFT JOIN da_" + userDetails.getTenant() + ".system_to_domain s2d ON s2d.system_id=s.id"

                            + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r6 ON r6.source_id=p.id AND r6.reference_type='PRODUCT_TO_DATA_ASSET'"
                            + " LEFT JOIN da_" + userDetails.getTenant() + ".data_asset da ON da.id=r6.target_id AND da.state='PUBLISHED'"
                            + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_sample es ON (es.system_id=da.system_id AND es.entity_id=da.entity_id) OR es.entity_query_id=eq.id"
                            + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_query es_eq ON es_eq.id=es.entity_query_id AND es_eq.state='PUBLISHED'"

                            + " LEFT JOIN da_" + userDetails.getTenant() + ".entity e ON (eq.entity_id=e.id OR es.entity_id=e.id OR da.entity_id=e.id OR es_eq.entity_id=e.id) AND e.state='PUBLISHED'"

                            + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r5 ON r5.source_id=p.id AND r5.reference_type='PRODUCT_TO_BUSINESS_ENTITY_LINK'"
                            + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r8 ON r8.source_id=e.id AND r8.reference_type='DATA_ENTITY_TO_BUSINESS_ENTITY'"
                            + " LEFT JOIN da_" + userDetails.getTenant() + ".business_entity be ON (be.id=r5.target_id OR be.id=r8.target_id) AND be.state='PUBLISHED'"

                            + " LEFT JOIN da_" + userDetails.getTenant() + ".domain d2 ON (s2d.domain_id=d2.id OR be.domain_id=d2.id OR p.domain_id=d2.id) AND d2.state='PUBLISHED'"

                            + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_sample_property esp ON esp.entity_sample_id=es.id"
                            + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r ON r.source_id=esp.id AND r.reference_type='SAMPLE_PROPERTY_TO_META_COLUMN'"
                            + " LEFT JOIN da_" + userDetails.getTenant() + ".meta_column mc ON r.target_id=mc.id"

                            + " WHERE p.id IN ('" + StringUtils.join(artifactIds.toArray(), "','") + "')",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String beId = rs.getString("be_id");
                                String prodQueryId = rs.getString("p_eq_id");
                                String queryId = rs.getString("entity_query_id");
                                String entityId = rs.getString("entity_id");
                                String systemId = rs.getString("system_id");
                                String domainId = rs.getString("domain_id");
                                String productId = rs.getString("p_id");
                                String product2Id = rs.getString("p2_id");
                                String dataAssetId = rs.getString("data_asset_id");
                                String assetSampleId = rs.getString("da_es_id");
                                String sampleQueryId = rs.getString("es_eq_id");
                                String metaObjectId = rs.getString("meta_object_id");

                                if (prodQueryId != null) {
                                    if (!productToQueryIds.containsKey(productId))
                                        productToQueryIds.put(productId, new ArrayList<>());
                                    if (!productToQueryIds.get(productId).contains(prodQueryId))
                                        productToQueryIds.get(productId).add(prodQueryId);
                                }
                                if (domainId != null && !nodeIds.contains(domainId)) {
                                    nodeIds.add(domainId);
                                    rowsTop.get(0).add(new ArtifactModelNode(domainId, "domain"));

                                    //addArtifactModelLink(linkedNodeIds, artifactId, productId, domainId, userDetails);
                                }
                                if (beId != null && !nodeIds.contains(beId)) {
                                    nodeIds.add(beId);
                                    rowsTop.get(1).add(new ArtifactModelNode(beId, "business_entity"));
                                }
                                if (queryId != null && !nodeIds.contains(queryId)) {
                                    nodeIds.add(queryId);
                                    rowsBottom.get(0).add(new ArtifactModelNode(queryId, "entity_query"));
                                }
                                if (sampleQueryId != null && !nodeIds.contains(sampleQueryId)) {
                                    nodeIds.add(sampleQueryId);
                                    rowsBottom.get(0).add(new ArtifactModelNode(sampleQueryId, "entity_query"));
                                }
                                if (entityId != null && !nodeIds.contains(entityId)) {
                                    nodeIds.add(entityId);
                                    rowsTop.get(2).add(new ArtifactModelNode(entityId, "entity"));
                                }
                                if (systemId != null && !nodeIds.contains(systemId)) {
                                    nodeIds.add(systemId);
                                    cols.get(2).add(new ArtifactModelNode(systemId, "system", "left"));
                                }

                                if (dataAssetId != null && !nodeIds.contains(dataAssetId)) {
                                    nodeIds.add(dataAssetId);
                                    cols.get(3).add(new ArtifactModelNode(dataAssetId, "data_asset", "left"));
                                }

                                if (assetSampleId != null && !nodeIds.contains(assetSampleId)) {
                                    nodeIds.add(assetSampleId);
                                    rowsBottom.get(1).add(new ArtifactModelNode(assetSampleId, "entity_sample"));
                                }

                                if (metaObjectId != null && !nodeIds.contains(metaObjectId)) {
                                    nodeIds.add(metaObjectId);
                                    rowsBottom.get(1).add(new ArtifactModelNode(metaObjectId, "meta_object"));
                                }

                                if (product2Id != null && nodeIds.contains(product2Id)) {
                                    addArtifactModelLink(linkedNodeIds, artifactId, productId, product2Id, userDetails);
                                }
                            }
                        });

                jdbcTemplate.query(
                        "SELECT r2.source_id as p_id, r2.target_id AS indicator_id, da.id AS asset_id, be.id as be_id, es.id AS sample_id, " +
                                "es.entity_query_id, es.system_id, es.entity_query_id as es_query_id, be.domain_id as domain_id, " +
                                "e.id as entity_id, i2.id as indicator2_id, mc.meta_object_id FROM da_"
                                + userDetails.getTenant() + ".reference r2 LEFT JOIN da_" + userDetails.getTenant()
                                + ".indicator i ON r2.reference_type='PRODUCT_TO_INDICATOR' AND r2.target_id=i.id AND i.state='PUBLISHED'"

                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r8 ON r8.source_id=i.id AND r8.reference_type='INDICATOR_FORMULA_TO_INDICATOR'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".indicator i2 ON i2.id=r8.target_id AND i2.state='PUBLISHED'"

                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r ON (r.source_id=i.id OR r.source_id=i2.id) AND r.reference_type='INDICATOR_TO_DATA_ASSET'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".data_asset da ON r.target_id=da.id AND da.state='PUBLISHED'"

                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r6 ON (r6.source_id=i.id OR r6.source_id=i2.id) AND r6.reference_type='INDICATOR_TO_BUSINESS_ENTITY_LINK'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r9 ON (r9.source_id=i.id OR r9.source_id=i2.id) AND r9.reference_type='INDICATOR_FORMULA_TO_ENTITY'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity e ON (e.id=da.entity_id OR e.id=r9.target_id) AND e.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r5 ON r5.source_id=e.id AND r5.reference_type='DATA_ENTITY_TO_BUSINESS_ENTITY'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".business_entity be ON (be.id=r5.target_id OR be.id=r6.target_id) AND be.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_sample es ON da.system_id=es.system_id AND da.entity_id=es.entity_id "

                                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_sample_property esp ON esp.entity_sample_id=es.id"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r0 ON r0.source_id=esp.id AND r0.reference_type='SAMPLE_PROPERTY_TO_META_COLUMN'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".meta_column mc ON r0.target_id=mc.id"

                                + " WHERE r2.source_id IN ('" + StringUtils.join(artifactIds.toArray(), "','") + "') AND r2.reference_type='PRODUCT_TO_INDICATOR'",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String indicatorId = rs.getString("indicator_id");
                                String indicator2Id = rs.getString("indicator2_id");
                                String assetId = rs.getString("asset_id");
                                String sampleId = rs.getString("sample_id");
                                String sampleQueryId = rs.getString("es_query_id");
                                String beId = rs.getString("be_id");
                                String domainId = rs.getString("domain_id");
                                String entityId = rs.getString("entity_id");
                                String metaObjectId = rs.getString("meta_object_id");

                                if (indicatorId != null && !nodeIds.contains(indicatorId)) {
                                    nodeIds.add(indicatorId);
                                    cols.get(4).add(new ArtifactModelNode(indicatorId, "indicator", "left"));
                                }
                                if (indicator2Id != null && !nodeIds.contains(indicator2Id)) {
                                    nodeIds.add(indicator2Id);
                                    cols.get(4).add(new ArtifactModelNode(indicator2Id, "indicator", "left"));
                                }
                                if (assetId != null && !nodeIds.contains(assetId)) {
                                    nodeIds.add(assetId);
                                    cols.get(3).add(new ArtifactModelNode(assetId, "data_asset", "left"));
                                }
                                if (sampleId != null && !nodeIds.contains(sampleId)) {
                                    nodeIds.add(sampleId);
                                    rowsBottom.get(1).add(new ArtifactModelNode(sampleId, "entity_sample"));
                                }
                                if (metaObjectId != null && !nodeIds.contains(metaObjectId)) {
                                    nodeIds.add(metaObjectId);
                                    rowsBottom.get(1).add(new ArtifactModelNode(metaObjectId, "meta_object"));
                                }
                                if (beId != null && !nodeIds.contains(beId)) {
                                    nodeIds.add(beId);
                                    rowsTop.get(1).add(new ArtifactModelNode(beId, "business_entity"));
                                }
                                if (domainId != null && !nodeIds.contains(domainId)) {
                                    nodeIds.add(domainId);
                                    rowsTop.get(0).add(new ArtifactModelNode(domainId, "domain"));
                                }
                                if (entityId != null && !nodeIds.contains(entityId)) {
                                    nodeIds.add(entityId);
                                    rowsTop.get(2).add(new ArtifactModelNode(entityId, "entity"));
                                }

                                if (sampleQueryId != null && !nodeIds.contains(sampleQueryId)) {
                                    nodeIds.add(sampleQueryId);
                                    rowsBottom.get(0).add(new ArtifactModelNode(sampleQueryId, "entity_query"));
                                }
                            }
                        });
                break;
            case indicator:

                jdbcTemplate.query("SELECT p.id AS product_id, p.domain_id, be.id AS be_id, p.entity_query_id AS p_eq_id FROM da_" + userDetails.getTenant()
                        + ".reference r JOIN da_" + userDetails.getTenant() + ".product p ON r.source_id=p.id AND p.state='PUBLISHED'"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r2 ON r2.source_id=p.id AND r2.reference_type='PRODUCT_TO_BUSINESS_ENTITY_LINK'"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".business_entity be ON r2.target_id=be.id AND be.state='PUBLISHED'"
                        + " WHERE r.target_id=? AND r.reference_type='PRODUCT_TO_INDICATOR'",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String productId = rs.getString("product_id");
                                String domainId = rs.getString("domain_id");
                                String beId = rs.getString("be_id");
                                String prodQueryId = rs.getString("p_eq_id");

                                if (productId != null && prodQueryId != null) {
                                    if (!productToQueryIds.containsKey(productId))
                                        productToQueryIds.put(productId, new ArrayList<>());
                                    if (!productToQueryIds.get(productId).contains(prodQueryId))
                                        productToQueryIds.get(productId).add(prodQueryId);
                                }

                                if (productId != null && !productIds.contains(productId))
                                    productIds.add(productId);

                                if (productId != null && !nodeIds.contains(productId)) {
                                    nodeIds.add(productId);
                                    cols.get(6).add(new ArtifactModelNode(productId, "product", "right"));
                                }

                                if (domainId != null && !nodeIds.contains(domainId)) {
                                    nodeIds.add(domainId);
                                    rowsTop.get(0).add(new ArtifactModelNode(domainId, "domain"));
                                }

                                if (beId != null && !nodeIds.contains(beId)) {
                                    nodeIds.add(beId);
                                    rowsTop.get(1).add(new ArtifactModelNode(beId, "business_entity"));
                                }

                            }
                        }, UUID.fromString(artifactId));

                jdbcTemplate.query(
                        "SELECT da.id AS asset_id, e.id as entity_id, es.id AS sample_id, r5.target_id AS be_id, es.entity_query_id, es.system_id, d2.id as sys_domain_id, i2.id as indicator2_id, mc.meta_object_id FROM da_"
                                + userDetails.getTenant() + ".reference r JOIN da_" + userDetails.getTenant()
                                + ".data_asset da ON r.reference_type='INDICATOR_TO_DATA_ASSET' AND r.target_id=da.id AND da.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r5 ON r5.source_id=da.entity_id AND r5.reference_type='DATA_ENTITY_TO_BUSINESS_ENTITY'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_sample es ON da.system_id=es.system_id AND da.entity_id=es.entity_id"

                                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_sample_property esp ON esp.entity_sample_id=es.id"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r0 ON r0.source_id=esp.id AND r0.reference_type='SAMPLE_PROPERTY_TO_META_COLUMN'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".meta_column mc ON r0.target_id=mc.id"

                                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_query eq ON eq.id=es.entity_query_id "
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".system_to_domain s2d ON s2d.system_id=es.system_id"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".domain d2 ON d2.id=s2d.domain_id AND d2.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r8 ON r8.source_id=r.source_id AND r8.reference_type='INDICATOR_FORMULA_TO_INDICATOR'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".indicator i2 ON i2.id=r8.target_id AND i2.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r9 ON (r9.source_id=r.source_id OR r9.source_id=i2.id) AND r9.reference_type='INDICATOR_FORMULA_TO_ENTITY'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity e ON (e.id=da.entity_id OR e.id=r9.target_id OR e.id=es.entity_id OR e.id=eq.entity_id) AND e.state='PUBLISHED'"
                                + " WHERE r.source_id=?",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String assetId = rs.getString("asset_id");
                                String indicator2Id = rs.getString("indicator2_id");
                                String entityId = rs.getString("entity_id");
                                String sampleId = rs.getString("sample_id");
                                String queryId = rs.getString("entity_query_id");
                                String systemId = rs.getString("system_id");
                                String sysDomainId = rs.getString("sys_domain_id");
                                String metaObjectId = rs.getString("meta_object_id");

                                if (assetId != null && !nodeIds.contains(assetId)) {
                                    nodeIds.add(assetId);
                                    cols.get(4).add(new ArtifactModelNode(assetId, "data_asset", "left"));
                                }
                                if (sampleId != null && !nodeIds.contains(sampleId)) {
                                    nodeIds.add(sampleId);
                                    rowsBottom.get(1).add(new ArtifactModelNode(sampleId, "entity_sample"));
                                }
                                if (metaObjectId != null && !nodeIds.contains(metaObjectId)) {
                                    nodeIds.add(metaObjectId);
                                    rowsBottom.get(1).add(new ArtifactModelNode(metaObjectId, "meta_object"));
                                }
                                if (entityId != null && !nodeIds.contains(entityId)) {
                                    nodeIds.add(entityId);
                                    rowsTop.get(2).add(new ArtifactModelNode(entityId, "entity"));

                                    for (String attrId : jdbcTemplate.queryForList(
                                            "SELECT id FROM da_" + userDetails.getTenant()
                                                    + ".entity_attribute WHERE entity_id=?",
                                            String.class, UUID.fromString(entityId))) {
                                        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                                                + ".model_nodes (id, artifact_id, node_id, parent_node_id, node_artifact_type) VALUES (?,?,?,?,'entity_attribute')",
                                                UUID.randomUUID(), UUID.fromString(artifactId), UUID.fromString(attrId),
                                                UUID.fromString(entityId));
                                    }
                                }
                                if (indicator2Id != null && !nodeIds.contains(indicator2Id)) {
                                    nodeIds.add(indicator2Id);
                                    cols.get(5).add(new ArtifactModelNode(indicator2Id, "indicator"));
                                }
                                if (queryId != null && !nodeIds.contains(queryId)) {
                                    nodeIds.add(queryId);
                                    rowsBottom.get(0).add(new ArtifactModelNode(queryId, "entity_query"));
                                }
                                if (systemId != null && !nodeIds.contains(systemId)) {
                                    nodeIds.add(systemId);
                                    cols.get(1).add(new ArtifactModelNode(systemId, "system", "left"));
                                }
                                if (sysDomainId != null && !nodeIds.contains(sysDomainId)) {
                                    nodeIds.add(sysDomainId);
                                    rowsTop.get(0).add(new ArtifactModelNode(sysDomainId, "domain"));
                                }
                            }
                        }, UUID.fromString(artifactId));

                break;

            case system:
                jdbcTemplate.query(
                        "SELECT eq.id AS query_id, es.id AS sample_id, da.id AS asset_id, be.id AS be_id, p.entity_query_id AS p_eq_id, i.id AS indicator_id, p.id AS product_id, p.domain_id, e.id as entity_id, mc.meta_object_id FROM da_"
                                + userDetails.getTenant() + ".entity_query eq "
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_sample es ON es.entity_query_id=eq.id "
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".data_asset da ON es.entity_id=da.entity_id AND es.system_id=da.system_id AND da.state='PUBLISHED' "
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r ON r.reference_type='INDICATOR_TO_DATA_ASSET' AND r.target_id=da.id"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".indicator i ON r.source_id=i.id AND i.state='PUBLISHED'"

                                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_sample_property esp ON esp.entity_sample_id=es.id"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r0 ON r0.source_id=esp.id AND r0.reference_type='SAMPLE_PROPERTY_TO_META_COLUMN'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".meta_column mc ON r0.target_id=mc.id"

                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r8 ON r8.source_id=i.id AND r8.reference_type='INDICATOR_FORMULA_TO_INDICATOR'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".indicator i2 ON i2.id=r8.target_id AND i2.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r9 ON (r9.source_id=i.id OR r9.source_id=i2.id) AND r9.reference_type='INDICATOR_FORMULA_TO_ENTITY'"

                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r2 ON (r2.target_id=i.id OR r2.target_id=i2.id)"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".product p ON r2.source_id=p.id AND p.state='PUBLISHED' AND r2.reference_type='PRODUCT_TO_INDICATOR'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".entity e ON (e.id=eq.entity_id OR e.id=r9.target_id) AND e.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r5 ON r5.source_id=e.id AND r5.reference_type='DATA_ENTITY_TO_BUSINESS_ENTITY'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".reference r6 ON (r6.source_id=i.id OR r6.source_id=i2.id) AND r6.reference_type='INDICATOR_TO_BUSINESS_ENTITY_LINK'"
                                + " LEFT JOIN da_" + userDetails.getTenant() + ".business_entity be ON (be.id=r5.target_id OR be.id=r6.target_id) AND be.state='PUBLISHED' "

                                + " WHERE eq.system_id=? AND eq.state='PUBLISHED'",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String assetId = rs.getString("asset_id");
                                String indicatorId = rs.getString("indicator_id");
                                String productId = rs.getString("product_id");
                                String domainId = rs.getString("domain_id");
                                String entityId = rs.getString("entity_id");
                                String sampleId = rs.getString("sample_id");
                                String queryId = rs.getString("query_id");
                                String beId = rs.getString("be_id");
                                String prodQueryId = rs.getString("p_eq_id");
                                String metaObjectId = rs.getString("meta_object_id");

                                if (productId != null && prodQueryId != null) {
                                    if (!productToQueryIds.containsKey(productId))
                                        productToQueryIds.put(productId, new ArrayList<>());
                                    if (!productToQueryIds.get(productId).contains(prodQueryId))
                                        productToQueryIds.get(productId).add(prodQueryId);
                                }

                                if (productId != null && !productIds.contains(productId))
                                    productIds.add(productId);

                                if (queryId != null && !nodeIds.contains(queryId)) {
                                    nodeIds.add(queryId);
                                    rowsBottom.get(0).add(new ArtifactModelNode(queryId, "entity_query"));
                                }

                                if (entityId != null && !nodeIds.contains(entityId)) {
                                    nodeIds.add(entityId);
                                    rowsTop.get(2).add(new ArtifactModelNode(entityId, "entity"));
                                }

                                if (sampleId != null && !nodeIds.contains(sampleId)) {
                                    nodeIds.add(sampleId);
                                    rowsBottom.get(1).add(new ArtifactModelNode(sampleId, "entity_sample"));
                                }

                                if (metaObjectId != null && !nodeIds.contains(metaObjectId)) {
                                    nodeIds.add(metaObjectId);
                                    rowsBottom.get(1).add(new ArtifactModelNode(metaObjectId, "meta_object"));
                                }

                                if (assetId != null && !nodeIds.contains(assetId)) {
                                    nodeIds.add(assetId);
                                    cols.get(6).add(new ArtifactModelNode(assetId, "data_asset", "right"));
                                }

                                if (indicatorId != null && !nodeIds.contains(indicatorId)) {
                                    nodeIds.add(indicatorId);
                                    cols.get(7).add(new ArtifactModelNode(indicatorId, "indicator", "right"));
                                }

                                if (productId != null && !nodeIds.contains(productId)) {
                                    nodeIds.add(productId);
                                    cols.get(8).add(new ArtifactModelNode(productId, "product", "right"));

                                }

                                if (domainId != null && !nodeIds.contains(domainId)) {
                                    nodeIds.add(domainId);
                                    rowsTop.get(0).add(new ArtifactModelNode(domainId, "domain"));

                                }

                                if (beId != null && !nodeIds.contains(beId)) {
                                    nodeIds.add(beId);
                                    rowsTop.get(1).add(new ArtifactModelNode(beId, "business_entity"));
                                }

                                if (queryId != null) {
                                    //addArtifactModelLink(linkedNodeIds, artifactId, queryId, artifactId, userDetails);
                                    //if (entityId != null)
                                      //  addArtifactModelLink(linkedNodeIds, artifactId, queryId, entityId, userDetails);
                                    if (sampleId != null) {
                                        //addArtifactModelLink(linkedNodeIds, artifactId, sampleId, queryId, userDetails);
                                        if (assetId != null) {
                                            //addArtifactModelLink(linkedNodeIds, artifactId, assetId, sampleId, userDetails);
                                            if (indicatorId != null) {
                                                //addArtifactModelLink(linkedNodeIds, artifactId, indicatorId, assetId, userDetails);
                                                if (productId != null) {
                                                    //addArtifactModelLink(linkedNodeIds, artifactId, productId, indicatorId, userDetails);
                                                    if (domainId != null) {
                                                        //addArtifactModelLink(linkedNodeIds, artifactId, domainId, productId, userDetails);

                                                        //if (beId != null)
                                                          //  addArtifactModelLink(linkedNodeIds, artifactId, beId, domainId, userDetails);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }, UUID.fromString(artifactId));

                jdbcTemplate.query("SELECT d.id as domain_id FROM da_" + userDetails.getTenant()
                    + ".system_to_domain s2d LEFT JOIN da_" + userDetails.getTenant()
                    + ".domain d ON s2d.domain_id=d.id AND d.state='PUBLISHED'"
                    + " WHERE s2d.system_id=?", new RowCallbackHandler() {
                        @Override
                        public void processRow(ResultSet rs) throws SQLException {
                            String domainId = rs.getString("domain_id");

                            if (domainId != null && !nodeIds.contains(domainId)) {
                                nodeIds.add(domainId);
                                rowsTop.get(0).add(new ArtifactModelNode(domainId, "domain"));
                            }

                            if (domainId != null) {
                                //addArtifactModelLink(linkedNodeIds, artifactId, artifactId, domainId, userDetails);
                            }
                        }
                    }, UUID.fromString(artifactId));
                break;
        }

        addProductLinks(artifactId, linkedNodeIds, productIds, nodeIds, userDetails);
        for (String prodId : productToQueryIds.keySet()) {
            for (String qId : productToQueryIds.get(prodId)) {
                if (nodeIds.contains(prodId) && nodeIds.contains(qId))
                    addArtifactModelLink(linkedNodeIds, artifactId, prodId, qId, userDetails);
            }
        }

        //////

        jdbcTemplate.query("SELECT id as id1, entity_id as id2 FROM da_" + userDetails.getTenant() + ".entity_query eq WHERE eq.state='PUBLISHED'"
                + " UNION SELECT id as id1, system_id as id2 FROM da_" + userDetails.getTenant() + ".entity_query eq WHERE eq.state='PUBLISHED'"
                + " UNION SELECT id as id1, entity_id as id2 FROM da_" + userDetails.getTenant() + ".data_asset da WHERE da.state='PUBLISHED'"
                + " UNION SELECT da.id as id1, es.id as id2 FROM da_" + userDetails.getTenant() + ".data_asset da JOIN da_" + userDetails.getTenant() + ".entity_sample es ON da.system_id=es.system_id AND da.entity_id=es.entity_id WHERE da.state='PUBLISHED' "
                + " UNION SELECT id as id1, entity_id as id2 FROM da_" + userDetails.getTenant() + ".entity_sample es"
                + " UNION SELECT id as id1, entity_query_id as id2 FROM da_" + userDetails.getTenant() + ".entity_sample es"
                + " UNION SELECT id as id1, domain_id as id2 FROM da_" + userDetails.getTenant() + ".business_entity be WHERE be.state='PUBLISHED'"
                + " UNION SELECT id as id1, domain_id as id2 FROM da_" + userDetails.getTenant() + ".product p WHERE p.state='PUBLISHED'"
                + " UNION SELECT id as id1, entity_query_id as id2 FROM da_" + userDetails.getTenant() + ".product p WHERE p.state='PUBLISHED'"
                + " UNION SELECT system_id as id1, domain_id as id2 FROM da_" + userDetails.getTenant() + ".system_to_domain s2d"
                + " UNION SELECT source_id as id1, target_id as id2 FROM da_" + userDetails.getTenant() + ".reference r"
                + " UNION SELECT rm_esp.entity_sample_id AS id1, rm_mc.meta_object_id AS id2 FROM da_" + userDetails.getTenant() + ".reference rm"
                + " JOIN da_" + userDetails.getTenant() + ".entity_sample_property rm_esp ON rm.source_id=rm_esp.id"
                + " JOIN da_" + userDetails.getTenant() + ".meta_column rm_mc ON rm.target_id=rm_mc.id WHERE rm.reference_type='SAMPLE_PROPERTY_TO_META_COLUMN'"

                + " UNION SELECT rq_mo.id AS id1, rq_t.query_id AS id2 FROM da_" + userDetails.getTenant() + ".reference rq "
                + " JOIN da_" + userDetails.getTenant() + ".meta_object rq_mo ON rq_mo.meta_database_id=rq.source_id"
                + " JOIN da_" + userDetails.getTenant() + ".task rq_t ON rq.target_id=rq_t.id WHERE rq.reference_type='META_DATABASE_TO_TASK'"
                ,

                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        String id1 = rs.getString("id1");
                        String id2 = rs.getString("id2");

                        if (id1 != null && !id1.isEmpty() && id2 != null && !id2.isEmpty() && nodeIds.contains(id1) && nodeIds.contains(id2)) {
                            addArtifactModelLink(linkedNodeIds, artifactId, id1, id2, userDetails);
                        }
                    }
                });

        //////


        List<String> allNodeIds = new ArrayList<>();

        for (int r = 0; r < rowsTop.size(); r++) {
            int locX = 500 - rowsTop.get(r).size() * 400 / 2;
            int locY = -200 * (rowsTop.size() - r);

            boolean haveChangesInRow = !rowsTop.get(r).isEmpty()
                    && jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant()
                        + ".model_nodes WHERE artifact_id=? AND node_id IN ('"
                        + StringUtils.join(rowsTop.get(r).stream().map(x -> x.getNodeId()).toArray(), "','") + "')",
                        Integer.class, UUID.fromString(artifactId)) != rowsTop.get(r).size();

            for (int i = 0; i < rowsTop.get(r).size(); i++) {
                allNodeIds.add(rowsTop.get(r).get(i).getNodeId());

                if (haveChangesInRow) {
                    updateModelNode(artifactId, artifactType, rowsTop.get(r).get(i),
                            locX, locY, userDetails);

                    locX += 400;

                }
            }
        }

        int maxLocY = 0;

        for (int c = 0; c < cols.size(); c++) {
            int locX = c * 400 - 1500;
            int locY = 0;

            boolean haveChangesInCol = !cols.get(c).isEmpty()
                    && jdbcTemplate
                            .queryForObject(
                                    "SELECT COUNT(id) FROM da_" + userDetails.getTenant()
                                            + ".model_nodes WHERE artifact_id=? AND node_id IN ('"
                                            + StringUtils.join(cols.get(c).stream().map(x -> x.getNodeId()).toArray(),
                                                    "','")
                                            + "')",
                                    Integer.class, UUID.fromString(artifactId)) != cols.get(c).size();

            for (int i = 0; i < cols.get(c).size(); i++) {

                allNodeIds.add(cols.get(c).get(i).getNodeId());

                if (haveChangesInCol) {

                    updateModelNode(artifactId, artifactType, cols.get(c).get(i),
                            locX, locY, userDetails);

                    locY += 200;
                    if (cols.get(c).get(i).getArtifactType().equals("entity")) {
                        int cnt = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM da_" + userDetails.getTenant()
                                        + ".entity_attribute WHERE entity_id=?",
                                Integer.class, UUID.fromString(cols.get(c).get(i).getNodeId()));
                        if (cnt > 0)
                            locY += 30 * cnt;
                    }
                }
            }

            if (locY > maxLocY)
                maxLocY = locY;
        }

        for (int r = 0; r < rowsBottom.size(); r++) {
            int locX = 500 - rowsBottom.get(r).size() * 400 / 2;
            int locY = maxLocY + 200 + 200 * r;

            boolean haveChangesInRow = !rowsBottom.get(r).isEmpty()
                    && jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant()
                            + ".model_nodes WHERE artifact_id=? AND node_id IN ('"
                            + StringUtils.join(rowsBottom.get(r).stream().map(x -> x.getNodeId()).toArray(), "','") + "')",
                    Integer.class, UUID.fromString(artifactId)) != rowsBottom.get(r).size();

            for (int i = 0; i < rowsBottom.get(r).size(); i++) {
                allNodeIds.add(rowsBottom.get(r).get(i).getNodeId());

                if (haveChangesInRow) {
                    updateModelNode(artifactId, artifactType, rowsBottom.get(r).get(i),
                            locX, locY, userDetails);

                    locX += 400;

                }
            }
        }

        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".model_links WHERE from_node_id NOT IN ('"
                + StringUtils.join(allNodeIds.toArray(), "','")
                + "') OR to_node_id NOT IN ('" + StringUtils.join(allNodeIds.toArray(), "','") + "')");
        jdbcTemplate.update(
                "DELETE FROM da_" + userDetails.getTenant() + ".model_nodes WHERE artifact_id=? AND node_id NOT IN ('"
                        + StringUtils.join(allNodeIds.toArray(), "','") + "')",
                UUID.fromString(artifactId));
    }

    private List<String> getAscendantProdIdsForProduct(String productId, UserDetails userDetails) {
        List<String> res = new ArrayList<>();
        res.add(productId);
        List<String> ids = new ArrayList<>();
        ids.add(productId);
        while (!ids.isEmpty()) {
            List<String> newIds = jdbcTemplate.queryForList("SELECT p.id FROM da_" + userDetails.getTenant() + ".reference r JOIN da_"
                    + userDetails.getTenant() + ".product p ON r.source_id=p.id AND r.reference_type='PRODUCT_TO_PRODUCT' AND p.state='PUBLISHED' WHERE r.target_id IN ('"
                    + StringUtils.join(ids.toArray(), "','") + "')", String.class);
            for (String id : newIds) {
                if (!res.contains(id))
                    res.add(id);
            }
            ids.clear();
            ids.addAll(newIds);
        }

        return res;
    }

    private List<String> getRelatedProdIdsForProduct(String productId, UserDetails userDetails) {
        List<String> res = new ArrayList<>();
        res.add(productId);
        List<String> ids = new ArrayList<>();
        ids.add(productId);
        while (!ids.isEmpty()) {
            List<String> newIds = jdbcTemplate.queryForList("SELECT p.id FROM da_" + userDetails.getTenant() + ".reference r JOIN da_"
                + userDetails.getTenant() + ".product p ON r.target_id=p.id AND r.reference_type='PRODUCT_TO_PRODUCT' AND p.state='PUBLISHED' WHERE r.source_id IN ('"
            + StringUtils.join(ids.toArray(), "','") + "')", String.class);
            for (String id : newIds) {
                if (!res.contains(id))
                    res.add(id);
            }
            ids.clear();
            ids.addAll(newIds);
        }

        return res;
    }

    private void updateModelNode(String artifactId, String artifactType, ArtifactModelNode node, int locX, int locY, UserDetails userDetails) {
        boolean nodeExists = modelNodeExists(artifactId, node.getNodeId(), userDetails);

        if (nodeExists) {
            jdbcTemplate.update(
                    "UPDATE da_" + userDetails.getTenant()
                            + ".model_nodes SET loc=?, lineage_dir=? WHERE artifact_id=? AND node_id=?",
                    locX + " " + locY, node.getLineageDir(), UUID.fromString(artifactId),
                    UUID.fromString(node.getNodeId()));
            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant()
                            + ".model_links SET points=NULL WHERE artifact_id=? AND (from_node_id=? OR to_node_id=?)",
                    UUID.fromString(artifactId),
                    UUID.fromString(node.getNodeId()),
                    UUID.fromString(node.getNodeId()));
        } else {

            jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                            + ".model_nodes (id, artifact_id, artifact_type, node_id, node_artifact_type, loc, lineage_dir) VALUES (?,?,?,?,?,?,?)",
                    UUID.randomUUID(), UUID.fromString(artifactId), artifactType,
                    UUID.fromString(node.getNodeId()), node.getArtifactType(),
                    locX + " " + locY, node.getLineageDir());
        }
    }

    private boolean modelNodeExists(String artifactId, String nodeId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(id) FROM da_" + userDetails.getTenant()
                        + ".model_nodes WHERE artifact_id=? AND node_id=?",
                Integer.class,
                UUID.fromString(artifactId), UUID.fromString(nodeId)) > 0;
    }

    public GojsModelData getArtifactModel(String artifactId, String artifactType, UserDetails userDetails) {
        GojsModelData res = new GojsModelData();
        List<GojsModelNodeData> nodes = new ArrayList<>();
        List<GojsModelLinkData> links = new ArrayList<>();

        // clearModels(userDetails);

        generateArtifactModel(artifactId, artifactType, userDetails);

        jdbcTemplate.query("SELECT n.*, (SELECT string_agg(t.name, '%SP%') FROM da_" + userDetails.getTenant()
                + ".tag t JOIN da_" + userDetails.getTenant() + ".tag_to_artifact ta ON t.id=ta.tag_id AND ta.artifact_id=n.node_id) AS tag_names FROM da_"
                + userDetails.getTenant() + ".model_nodes n WHERE n.artifact_id=? ORDER BY n.parent_node_id DESC", new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        GojsModelNodeData nd = new GojsModelNodeData();
                        nd.setId(rs.getString("node_id"));
                        String pid = rs.getString("parent_node_id");
                        if (pid != null && !pid.isEmpty()) {
                            nd.setParentId(pid);
                            nd.setGroup(pid);
                            nd.setIsGroup(false);
                        } else {
                            nd.setIsGroup(true);
                            nd.setGroup("");
                            nd.setParentId("");
                        }
                        try {
                            final String[] name = new String[1];
                            if (rs.getString("node_artifact_type").equals("meta_object")) {
                                jdbcTemplate.query("SELECT (ms.name || '.' || mo.name) AS name, mo.meta_database_id FROM da_" + userDetails.getTenant()
                                                + ".meta_object mo LEFT JOIN da_" + userDetails.getTenant()
                                                + ".meta_object ms ON mo.parent_id=ms.id WHERE mo.id=?",
                                    new RowCallbackHandler() {
                                        @Override
                                        public void processRow(ResultSet rs) throws SQLException {
                                            name[0] = rs.getString("name");
                                            nd.setParentId(rs.getString("meta_database_id"));
                                        }
                                    },
                                    UUID.fromString(rs.getString("node_id")));
                            } else
                                name[0] = jdbcTemplate.queryForObject(
                                    "SELECT name FROM da_" + userDetails.getTenant() + "."
                                            + rs.getString("node_artifact_type") + " WHERE id=?",
                                    String.class, UUID.fromString(rs.getString("node_id")));
                            if (name[0].length() > 40)
                                name[0] = name[0].substring(0, 40) + "...";
                            nd.setName(name[0]);
                            nd.setText(name[0]);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            nd.setName("???");
                            nd.setText("???");
                        }
                        nd.setType("defaultNodeType");
                        nd.setZOrder(1);
                        nd.setLineageDir(rs.getString("lineage_dir"));


                        nd.setLoc(rs.getString("loc"));
                        nd.setArtifactType(rs.getString("node_artifact_type"));

                        if (rs.getObject("tag_names") != null)
                            nd.setTagNames(rs.getString("tag_names").split("%SP%"));

                        nodes.add(nd);
                    }
                }, UUID.fromString(artifactId));

        jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".model_links WHERE artifact_id=?",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        GojsModelLinkData ld = new GojsModelLinkData();
                        ld.setId(rs.getString("id"));
                        ld.setFrom(rs.getString("from_node_id"));
                        ld.setTo(rs.getString("to_node_id"));
                        ld.setPoints(rs.getString("points"));
                        ld.setZOrder(1);
                        ld.setTags(tagService.getArtifactTags(ld.getId(), userDetails).stream().map(t -> new Relation(t.getId(), t.getName())).collect(Collectors.toList()));

                        links.add(ld);
                    }
                }, UUID.fromString(artifactId));

        res.setNodes(nodes);
        res.setLinks(links);
        return res;
    }

    public GojsModelData getModel(String artifactType, UserDetails userDetails) {
        GojsModelData res = new GojsModelData();
        List<GojsModelNodeData> nodes = new ArrayList<>();
        List<GojsModelLinkData> links = new ArrayList<>();

        jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".entity WHERE state='PUBLISHED'",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        GojsModelNodeData nd = new GojsModelNodeData();
                        nd.setId(rs.getString("id"));
                        nd.setName(rs.getString("name"));
                        nd.setType("defaultNodeType");
                        nd.setZOrder(1);
                        nd.setText(rs.getString("name"));
                        nd.setIsGroup(true);
                        nd.setGroup("");
                        nd.setParentId("");
                        nd.setLoc(rs.getString("loc"));
                        nd.setArtifactType(ArtifactType.entity.getText());

                        nodes.add(nd);
                    }
                });

        jdbcTemplate.query("SELECT ea.* FROM da_" + userDetails.getTenant() + ".entity e JOIN da_"
                + userDetails.getTenant() + ".entity_attribute ea "
                + "ON e.id=ea.entity_id WHERE e.state='PUBLISHED'", new RowCallbackHandler() {
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

                        nodes.add(nd);
                    }
                });

        jdbcTemplate.query("SELECT r.* FROM da_" + userDetails.getTenant() + ".reference r JOIN da_"
                + userDetails.getTenant()
                + ".entity_attribute ea1 ON r.source_id=ea1.id JOIN da_" + userDetails.getTenant()
                + ".entity e1 ON ea1.entity_id=e1.id JOIN da_" + userDetails.getTenant() + ".entity_attribute ea2"
                + " ON r.target_id=ea2.id JOIN da_" + userDetails.getTenant() + ".entity e2 ON ea2.entity_id=e2.id "
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
                    }
                }, ReferenceType.ENTITY_ATTRIBUTE_TO_ENTITY_ATTRIBUTE.name());

        res.setNodes(nodes);
        res.setLinks(links);
        return res;
    }

    public List<GojsModelNodeData> updateArtifactModel(UpdatableGojsModelData updatableGojsModelData,
            String artifactType, String artifactId, UserDetails userDetails) throws LottabyteException {
        if (updatableGojsModelData.getUpdateNodes() != null) {
            for (GojsModelNodeData nodeData : updatableGojsModelData.getUpdateNodes()) {
                if (nodeData.getIsGroup())
                    jdbcTemplate.update(
                            "UPDATE da_" + userDetails.getTenant()
                                    + ".model_nodes SET loc=? WHERE node_id=? AND artifact_id=?",
                            nodeData.getLoc(),
                            UUID.fromString(nodeData.getId()), UUID.fromString(artifactId));
            }
        }

        if (updatableGojsModelData.getUpdateLinks() != null) {
            for (GojsModelLinkData linkData : updatableGojsModelData.getUpdateLinks()) {
                Timestamp ts = new Timestamp(new java.util.Date().getTime());

                boolean linkExists = jdbcTemplate
                        .queryForObject("SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".model_links"
                                + " WHERE id = ? ) as exists", Boolean.class, UUID.fromString((linkData.getId())));

                if (linkExists) {
                    jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".model_links SET points=? WHERE id=?",
                            linkData.getPoints(), UUID.fromString(linkData.getId()));
                } else {
                    jdbcTemplate.update(
                            "INSERT INTO da_" + userDetails.getTenant()
                                    + ".model_links (id, artifact_id, from_node_id, to_node_id, points)"
                                    + " VALUES (?,?,?,?,?)",
                            UUID.fromString(linkData.getId()), UUID.fromString(artifactId),
                            UUID.fromString(linkData.getFrom()),
                            UUID.fromString(linkData.getTo()), linkData.getPoints());

                }

                List<Relation> currTags = jdbcTemplate.query("SELECT t.id, t.name FROM da_" + userDetails.getTenant()
                                + ".tag_to_artifact t2a JOIN da_" + userDetails.getTenant() + ".tag t ON t2a.tag_id=t.id WHERE t2a.artifact_id=?",
                        new RowMapper<Relation>() {
                            @Override
                            public Relation mapRow(ResultSet rs, int rowNum) throws SQLException {
                                return new Relation(rs.getString("id"), rs.getString("name"));
                            }
                        }, UUID.fromString(linkData.getId()));

                for (Relation tag : currTags) {
                    if (linkData.getTags().stream().noneMatch(t -> t.getName().equals(tag.getName())))
                        tagRepository.unlinkTagFromArtifact(tag.getId(), linkData.getId(), userDetails);
                }

                for (Relation tag : linkData.getTags()) {
                    Relation finalTag = tag;
                    if (currTags.stream().noneMatch(t -> t.getName().equals(finalTag.getName()))) {
                        Tag linkTag = tagRepository.getTagByName(tag.getName(), null, userDetails);
                        if (linkTag == null) {
                            TagEntity newTagEntity = new TagEntity();
                            newTagEntity.setName(tag.getName());
                            linkTag = tagRepository.createTag(newTagEntity, userDetails);
                        }
                        if (linkTag != null) {
                            if (!tagRepository.tagIsLinkedToArtifact(linkTag.getId(), linkData.getId(), userDetails))
                                tagRepository.linkTagToArtifact(linkTag.getId(), linkData.getId(), "link", userDetails);
                        }
                    }
                }
            }
        }

        if (updatableGojsModelData.getDeleteLinks() != null) {
            for (String linkId : updatableGojsModelData.getDeleteLinks()) {
                jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".reference WHERE id=?",
                        UUID.fromString(linkId));
            }
        }

        return null;
    }

    public List<GojsModelNodeData> updateModel(UpdatableGojsModelData updatableGojsModelData, UserDetails userDetails) {

        /*
         * if (updatableGojsModelData.getUpdateNodes() != null) {
         * for (GojsModelNodeData nodeData : updatableGojsModelData.getUpdateNodes()) {
         * if (nodeData.getIsGroup())
         * jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + "."+
         * nodeData.getArtifactType() +" SET loc=? WHERE id=?", nodeData.getLoc(),
         * UUID.fromString(nodeData.getId()));
         * }
         * }
         *
         * if (updatableGojsModelData.getUpdateLinks() != null) {
         * for (GojsModelLinkData linkData : updatableGojsModelData.getUpdateLinks()) {
         * Timestamp ts = new Timestamp(new java.util.Date().getTime());
         *
         * boolean linkExists =
         * jdbcTemplate.queryForObject("SELECT EXISTS(SELECT ID FROM da_" +
         * userDetails.getTenant() + ".reference"
         * + " WHERE id = ? ) as exists", Boolean.class,
         * UUID.fromString((linkData.getId())));
         *
         * if (linkExists) {
         * jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() +
         * ".reference SET source_id=?, target_id=?, points=?, modified=?, modifier=?, history_end=? WHERE id=?"
         * ,
         * UUID.fromString(linkData.getFrom()), UUID.fromString(linkData.getTo()),
         * linkData.getPoints(), ts,userDetails.getUid(), ts,
         * UUID.fromString(linkData.getId()));
         * } else {
         * jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() +
         * ".reference (id, source_id, source_artifact_type, target_id, "
         * +
         * "target_artifact_type, reference_type, created, creator, modified, modifier, history_start, history_end, version_id, published_id, points)"
         * + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
         * UUID.fromString(linkData.getId()), UUID.fromString(linkData.getFrom()),
         * ArtifactType.entity_attribute.getText(),
         * UUID.fromString(linkData.getTo()), ArtifactType.entity_attribute.getText(),
         * ReferenceType.ENTITY_ATTRIBUTE_TO_ENTITY_ATTRIBUTE.name(),
         * ts, userDetails.getUid(), ts, userDetails.getUid(), ts, ts, 0,
         * UUID.fromString(linkData.getFrom()), linkData.getPoints());
         *
         * }
         * }
         * }
         *
         * if (updatableGojsModelData.getDeleteLinks() != null) {
         * for (String linkId : updatableGojsModelData.getDeleteLinks()) {
         * jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() +
         * ".reference WHERE id=?", UUID.fromString(linkId));
         * }
         * }
         */

        return null;
    }

    public List<DashboardEntity> getRecommended(UserDetails userDetails) {
        List<DashboardEntity> res = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        Date today = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        Date weekago = cal.getTime();

        if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (UUID id : userDetails.getUserDomains())
                parts.add("a.domain_ids LIKE '%" + id.toString() + "%'");


            jdbcTemplate.query("SELECT * FROM ((SELECT artifact_id, artifact_type, (total_views + 10*today_views + 5*week_views) as rate FROM (SELECT DISTINCT av.artifact_id, av.artifact_type, " +
                    "(SELECT SUM(views) FROM da_" + userDetails.getTenant() + ".artifact_views WHERE artifact_id=av.artifact_id) AS total_views, " +
                    "(SELECT SUM(views) FROM da_" + userDetails.getTenant() + ".artifact_views WHERE artifact_id=av.artifact_id AND view_date >= ?) AS week_views, " +
                    "(SELECT SUM(views) FROM da_" + userDetails.getTenant() + ".artifact_views WHERE artifact_id=av.artifact_id AND view_date = ?) AS today_views " +
                    "FROM da_" + userDetails.getTenant()
                    + ".artifact_views av) q ORDER BY (total_views + 10*today_views + 5*week_views) DESC LIMIT 10)" +
                    " UNION " +
                    "(SELECT artifact_id, artifact_type, (total_views + 10*today_views + 5*week_views) as rate FROM (SELECT DISTINCT av.artifact_id, av.artifact_type, " +
                    "(SELECT SUM(views) FROM da_" + userDetails.getTenant() + ".artifact_views WHERE artifact_id=av.artifact_id) AS total_views, " +
                    "(SELECT SUM(views) FROM da_" + userDetails.getTenant() + ".artifact_views WHERE artifact_id=av.artifact_id AND view_date >= ?) AS week_views, " +
                    "(SELECT SUM(views) FROM da_" + userDetails.getTenant() + ".artifact_views WHERE artifact_id=av.artifact_id AND view_date = ?) AS today_views " +
                    "FROM da_" + userDetails.getTenant()
                    + ".artifact_views av JOIN da_" + userDetails.getTenant() + ".artifacts a ON av.artifact_id=a.id AND (" + StringUtils.join(parts.toArray(), " OR ")
                    + ")) q ORDER BY (total_views + 10*today_views + 5*week_views) DESC LIMIT 10) ORDER BY rate DESC) q2", new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    DashboardEntity de = new DashboardEntity();
                    de.setId(rs.getString("artifact_id"));
                    de.setArtifactType(rs.getString("artifact_type"));
                    de.setName(getArtifactName(ArtifactType.valueOf(de.getArtifactType()), de.getId(), userDetails));
                    res.add(de);
                }
            }, weekago, today, weekago, today);
        } else {
            jdbcTemplate.query("(SELECT artifact_id, artifact_type FROM (SELECT DISTINCT av.artifact_id, av.artifact_type, " +
                    "(SELECT SUM(views) FROM da_" + userDetails.getTenant() + ".artifact_views WHERE artifact_id=av.artifact_id) AS total_views, " +
                    "(SELECT SUM(views) FROM da_" + userDetails.getTenant() + ".artifact_views WHERE artifact_id=av.artifact_id AND view_date >= ?) AS week_views, " +
                    "(SELECT SUM(views) FROM da_" + userDetails.getTenant() + ".artifact_views WHERE artifact_id=av.artifact_id AND view_date = ?) AS today_views " +
                    "FROM da_" + userDetails.getTenant()
                    + ".artifact_views av) q ORDER BY (total_views + 10*today_views + 5*week_views) DESC LIMIT 10)", new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    DashboardEntity de = new DashboardEntity();
                    de.setId(rs.getString("artifact_id"));
                    de.setArtifactType(rs.getString("artifact_type"));
                    de.setName(getArtifactName(ArtifactType.valueOf(de.getArtifactType()), de.getId(), userDetails));
                    res.add(de);
                }
            }, weekago, today);
        }

        /*List<String> sqls = new ArrayList<>();

        for (String at : new String[] {"product", "indictor", "business_entity", "domain"}) {
            sqls.add("SELECT p.id, p.name, AVG(r.rating) * 100 AS rating, '" + at + "' AS artifact_type FROM da_"
                + userDetails.getTenant() + "." + at + " p LEFT JOIN da_" + userDetails.getTenant() + ".rating r "
                + "ON p.id=r.artifact_id WHERE p.state='PUBLISHED' AND r.rating IS NOT NULL GROUP BY p.id, p.name");
        }

        jdbcTemplate.query("SELECT * FROM ("
                        + "(" + StringUtils.join(sqls.toArray(), ") UNION (") + ")"
                        + ") sq ORDER BY rating DESC LIMIT 10",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        DashboardEntity entity = new DashboardEntity();
                        entity.setId(rs.getString("id"));
                        entity.setName(rs.getString("name"));
                        entity.setArtifactType(rs.getString("artifact_type"));
                        Integer rating = rs.getInt("rating");
                        if (rating == null || rating < 100)
                            rating = 100;
                        entity.setWeight(rating);
                        res.add(entity);
                    }
                });*/

        return res;
    }

    public List<DashboardEntity> getPopular(UserDetails userDetails) {
        List<DashboardEntity> res = new ArrayList<>();
        List<String> sqls = new ArrayList<>();

        for (String at : new String[] {"product", "indicator", "business_entity", "domain"}) {
            sqls.add("SELECT p.id, p.name, p.short_description AS description, p.creator, AVG(r.rating) * 100 AS rating, '" + at + "' AS artifact_type FROM da_"
                    + userDetails.getTenant() + "." + at + " p LEFT JOIN da_" + userDetails.getTenant() + ".rating r "
                    + "ON p.id=r.artifact_id WHERE p.state='PUBLISHED' AND r.rating IS NOT NULL GROUP BY p.id, p.name, p.short_description, p.creator");
        }

        jdbcTemplate.query("SELECT * FROM ("
                        + "(" + StringUtils.join(sqls.toArray(), ") UNION (") + ")"
                        + ") sq ORDER BY rating DESC LIMIT 6",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        DashboardEntity entity = new DashboardEntity();
                        entity.setId(rs.getString("id"));
                        entity.setName(rs.getString("name"));
                        entity.setDescription(rs.getString("description"));
                        entity.setRating(rs.getInt("rating") / 100);
                        entity.setArtifactType(rs.getString("artifact_type"));
                        entity.setCreatedBy(rs.getString("creator"));
                        Integer rating = rs.getInt("rating");
                        if (rating == null || rating < 100)
                            rating = 100;
                        entity.setWeight(rating);
                        res.add(entity);
                    }
                });

        return res;
    }

    public List<DashboardEntity> getFavorites(UserDetails userDetails) {
        List<DashboardEntity> res = new ArrayList<>();
        List<String> sqls = new ArrayList<>();

        for (String at : new String[] {"product", "indicator", "business_entity", "domain"}) {
            sqls.add("SELECT p.id, p.name, AVG(r.rating) * 100 AS rating, '" + at + "' AS artifact_type FROM da_"
                    + userDetails.getTenant() + "." + at + " p LEFT JOIN da_" + userDetails.getTenant() + ".rating r "
                    + "ON p.id=r.artifact_id WHERE p.state='PUBLISHED' AND r.rating IS NOT NULL GROUP BY p.id, p.name");
        }

        jdbcTemplate.query("SELECT * FROM ("
                        + "(" + StringUtils.join(sqls.toArray(), ") UNION (") + ")"
                        + ") sq ORDER BY rating DESC LIMIT 10",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        DashboardEntity entity = new DashboardEntity();
                        entity.setId(rs.getString("id"));
                        entity.setName(rs.getString("name"));
                        entity.setArtifactType(rs.getString("artifact_type"));
                        Integer rating = rs.getInt("rating");
                        if (rating == null || rating < 100)
                            rating = 100;
                        entity.setWeight(rating);
                        res.add(entity);
                    }
                });

        return res;
    }

    public List<DashboardEntity> getDashboard(UserDetails userDetails) {
        List<DashboardEntity> res = new ArrayList<>();

        jdbcTemplate.query("SELECT * FROM (SELECT p.id, p.name, AVG(r.rating) * 100 AS rating FROM da_"
                + userDetails.getTenant() + ".product p LEFT JOIN da_" + userDetails.getTenant()
                + ".rating r ON p.id=r.artifact_id WHERE p.state='PUBLISHED' and r.rating is not null GROUP BY p.id, p.name) sq ORDER BY rating DESC LIMIT 10",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        DashboardEntity entity = new DashboardEntity();
                        entity.setId(rs.getString("id"));
                        entity.setName(rs.getString("name"));
                        entity.setArtifactType("product");
                        Integer rating = rs.getInt("rating");
                        if (rating == null || rating < 100)
                            rating = 100;
                        entity.setWeight(rating);
                        res.add(entity);
                    }
                });

        jdbcTemplate.query("SELECT * FROM (SELECT i.id, i.name, AVG(r.rating) * 100 AS rating FROM da_"
                + userDetails.getTenant() + ".indicator i LEFT JOIN da_" + userDetails.getTenant()
                + ".rating r ON i.id=r.artifact_id WHERE i.state='PUBLISHED' and r.rating is not null GROUP BY i.id, i.name) sq ORDER BY rating DESC LIMIT 10",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        DashboardEntity entity = new DashboardEntity();
                        entity.setId(rs.getString("id"));
                        entity.setName(rs.getString("name"));
                        entity.setArtifactType("indicator");
                        Integer rating = rs.getInt("rating");
                        if (rating == null || rating < 100)
                            rating = 100;
                        entity.setWeight(rating);
                        res.add(entity);
                    }
                });

        jdbcTemplate.query("SELECT * FROM (SELECT be.id, be.name, AVG(r.rating) * 100 AS rating FROM da_"
                + userDetails.getTenant() + ".business_entity be LEFT JOIN da_" + userDetails.getTenant()
                + ".rating r ON be.id=r.artifact_id WHERE be.state='PUBLISHED' and r.rating is not null GROUP BY be.id, be.name) sq ORDER BY rating DESC LIMIT 10",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        DashboardEntity entity = new DashboardEntity();
                        entity.setId(rs.getString("id"));
                        entity.setName(rs.getString("name"));
                        entity.setArtifactType("business_entity");
                        Integer rating = rs.getInt("rating");
                        if (rating == null || rating < 100)
                            rating = 100;
                        entity.setWeight(rating);
                        res.add(entity);
                    }
                });

        jdbcTemplate.query("SELECT * FROM (SELECT d.id, d.name, AVG(r.rating) * 100 AS rating FROM da_"
                + userDetails.getTenant() + ".domain d LEFT JOIN da_" + userDetails.getTenant()
                + ".rating r ON d.id=r.artifact_id WHERE d.state='PUBLISHED' and r.rating is not null GROUP BY d.id, d.name) sq ORDER BY rating DESC LIMIT 10",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        DashboardEntity entity = new DashboardEntity();
                        entity.setId(rs.getString("id"));
                        entity.setName(rs.getString("name"));
                        entity.setArtifactType("domain");
                        Integer rating = rs.getInt("rating");
                        if (rating == null || rating < 100)
                            rating = 100;
                        entity.setWeight(rating);
                        res.add(entity);
                    }
                });

        return res;
    }

    public void clearModels(UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".model_nodes");
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".model_links");
    }

    class FlatWFItemObjectRowMapper implements RowMapper<FlatWFItemObject> {
        @Override
        public FlatWFItemObject mapRow(ResultSet rs, int rowNum) throws SQLException {

            FlatWFItemObject obj = new FlatWFItemObject();
            obj.setName(rs.getString("name"));
            obj.setDescription(rs.getString("description"));
            obj.setId(rs.getString("id"));
            obj.setArtifactType(rs.getString("artifact_type"));
            obj.setArtifactTypeName(rs.getString("artifact_type_name"));
            obj.setWorkflowState(rs.getString("state"));
            if (rs.getString("state_name") == null)
                obj.setWorkflowStateName("В работе");
            else
                obj.setWorkflowStateName(rs.getString("state_name"));

            obj.setModified(rs.getTimestamp("modified").toLocalDateTime());
            obj.setWorkflowTaskId(rs.getString("workflow_task_id"));

            return obj;
        }
    }

    private String getSearchSubQuery(ArtifactType artifactType, UserDetails userDetails) {
        StringBuilder sb = new StringBuilder();

        switch (artifactType) {
            case domain:
                sb.append(
                        "SELECT id, name, short_description, description, workflow_task_id, created, 'domain' as artifact_type, modified FROM da_"
                                + userDetails.getTenant() + ".domain WHERE state='DRAFT'");
                if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
                    sb.append(" AND id IN ('"
                            + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "') ");
                }
                break;
            case system:
                sb.append(
                        "SELECT id, name, short_description, description, workflow_task_id, created, 'system' as artifact_type, modified FROM da_"
                                + userDetails.getTenant() + ".system WHERE state='DRAFT'");
                if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
                    sb.append(" AND id IN (SELECT system_id FROM da_" + userDetails.getTenant()
                            + ".system_to_domain WHERE domain_id IN ('"
                            + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "')) ");
                }
                break;
            case entity:
                sb.append(
                        "SELECT id, name, short_description, description, workflow_task_id, created, 'entity' as artifact_type, modified FROM da_"
                                + userDetails.getTenant() + ".entity WHERE state='DRAFT'");
                if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
                    sb.append(" AND id IN (SELECT entity_id FROM da_" + userDetails.getTenant()
                            + ".entity_to_system ets JOIN da_" + userDetails.getTenant()
                            + ".system_to_domain std ON ets.system_id=std.system_id WHERE std.domain_id IN ('"
                            + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "')) ");
                }
                break;
            case entity_query:
                sb.append(
                        "SELECT id, name, short_description, description, workflow_task_id, created, 'entity_query' as artifact_type, modified FROM da_"
                                + userDetails.getTenant() + ".entity_query WHERE state='DRAFT'");
                if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
                    sb.append(" AND entity_id IN (SELECT entity_id FROM da_" + userDetails.getTenant()
                            + ".entity_to_system ets JOIN da_" + userDetails.getTenant()
                            + ".system_to_domain std ON ets.system_id=std.system_id WHERE std.domain_id IN ('"
                            + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "')) ");
                }
                break;
            case data_asset:
                sb.append(
                        "SELECT id, name, short_description, description, workflow_task_id, created, 'data_asset' as artifact_type, modified FROM da_"
                                + userDetails.getTenant() + ".data_asset WHERE state='DRAFT'");
                if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
                    sb.append(" AND domain_id IN ('"
                            + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "') ");
                }
                break;
            case indicator:
                sb.append(
                        "SELECT id, name, short_description, description, workflow_task_id, created, 'indicator' as artifact_type, modified FROM da_"
                                + userDetails.getTenant() + ".indicator WHERE state='DRAFT'");
                if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
                    sb.append(" AND domain_id IN ('"
                            + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "') ");
                }
                break;
            case business_entity:
                sb.append(
                        "SELECT id, name, short_description, description, workflow_task_id, created, 'business_entity' as artifact_type, modified FROM da_"
                                + userDetails.getTenant() + ".business_entity WHERE state='DRAFT'");
                if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
                    sb.append(" AND domain_id IN ('"
                            + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "') ");
                }
                break;
            case product:
                sb.append(
                        "SELECT id, name, short_description, description, workflow_task_id, created, 'product' as artifact_type, modified FROM da_"
                                + userDetails.getTenant() + ".product WHERE state='DRAFT'");
                if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
                    sb.append(" AND domain_id IN ('"
                            + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "') ");
                }
                break;
        }

        return sb.toString();
    }

    public SearchResponse<FlatWFItemObject> searchDrafts(SearchRequest searchRequest, SearchColumn[] searchableColumns,
            UserDetails userDetails) {
        String orderby = "name";
        if (!org.apache.commons.lang3.StringUtils.isEmpty(searchRequest.getSort()))
            orderby = (searchRequest.getSort().contains(".") ? "" : "tbl1.")
                    + searchRequest.getSort().replaceAll("[\\-\\+]", "")
                    + ((searchRequest.getSort().contains("-")) ? " DESC" : " ASC");

        String where = ServiceUtils.buildWhereForSearchRequest(searchRequest, searchableColumns);

        List<String> subqueries = new ArrayList<>();
        subqueries.add(getSearchSubQuery(ArtifactType.domain, userDetails));
        subqueries.add(getSearchSubQuery(ArtifactType.system, userDetails));
        subqueries.add(getSearchSubQuery(ArtifactType.entity, userDetails));
        subqueries.add(getSearchSubQuery(ArtifactType.entity_query, userDetails));
        subqueries.add(getSearchSubQuery(ArtifactType.data_asset, userDetails));
        subqueries.add(getSearchSubQuery(ArtifactType.indicator, userDetails));
        subqueries.add(getSearchSubQuery(ArtifactType.business_entity, userDetails));
        subqueries.add(getSearchSubQuery(ArtifactType.product, userDetails));

        String query = "SELECT tbl1.*, at.name AS artifact_type_name, wt.workflow_state AS state, ws.name AS state_name FROM (("
                + org.apache.commons.lang3.StringUtils.join(subqueries, ") UNION (") + ")) AS tbl1 LEFT JOIN da_"
                + userDetails.getTenant() + ".artifact_type at ON tbl1.artifact_type=at.code LEFT JOIN da_"
                + userDetails.getTenant() + ".workflow_task wt ON tbl1.workflow_task_id=wt.id LEFT JOIN da_"
                + userDetails.getTenant() + ".workflow_state ws ON wt.workflow_state=ws.state " + where + " ORDER BY "
                + orderby;

        List<FlatWFItemObject> flatItems = jdbcTemplate.query(query + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit(), new FlatWFItemObjectRowMapper());

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(distinct id) FROM (" + query + ") as tbl1 ", Long.class);

        SearchResponse<FlatWFItemObject> res = new SearchResponse<>();
        res.setCount(total.intValue());
        res.setLimit(searchRequest.getLimit());
        res.setOffset(searchRequest.getOffset());
        int num = searchRequest.getOffset() + 1;
        for (FlatModeledObject fd : flatItems)
            fd.setNum(num++);

        res.setItems(flatItems);

        return res;
    }

    public Map<String, String> getArtifactTypes(UserDetails userDetails) {
        Map<String, String> types = new HashMap<>();
        jdbcTemplate.query("SELECT code, name FROM da_" + userDetails.getTenant() + ".artifact_type ORDER BY code",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        types.put(rs.getString("code"), rs.getString("name"));
                    }
                });
        return types;
    }

    public String getArtifactType(String code, UserDetails userDetails) {
        return jdbcTemplate.queryForObject(
                "SELECT name FROM da_" + userDetails.getTenant() + ".artifact_type WHERE code=?", String.class, code);
    }

    public void updateModelsWithArtifact(String id, UserDetails userDetails) {
        List<String> artifactIds = new ArrayList<>();
        List<String> artifactTypes = new ArrayList<>();

        jdbcTemplate.query(
                "SELECT artifact_id, artifact_type FROM da_" + userDetails.getTenant() + ".model_nodes WHERE node_id=?",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        String v = rs.getString("artifact_id");
                        if (!artifactIds.contains(v)) {
                            artifactIds.add(v);
                            artifactTypes.add(rs.getString("artifact_type"));
                        }
                    }
                }, UUID.fromString(id));

        for (int i = 0; i < artifactIds.size(); i++) {
            generateArtifactModel(artifactIds.get(i), artifactTypes.get(i), userDetails);
        }
    }

    public void updateModelForArtifact(String artifactId, String artifactType, UserDetails userDetails) {
        generateArtifactModel(artifactId, artifactType, userDetails);
    }

    public SearchResponse<? extends FlatModeledObject> searchRelatedArtifacts(String srcArtifactType, String srcArtifactId, String tgtArtifactType, ArtifactsRelation ar, SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, UserDetails userDetails) {

        String domainIdField = "tbl1.domain_id";
        if (tgtArtifactType.equals("domain"))
            domainIdField = "tbl1.id";
        if (tgtArtifactType.equals("dq_rule") || tgtArtifactType.equals("entity_query") || tgtArtifactType.equals("entity")
            || tgtArtifactType.equals("entity_sample") || tgtArtifactType.equals("system") || tgtArtifactType.equals("entity_attribute")
            || tgtArtifactType.equals("entity_sample_property") || tgtArtifactType.equals("task") || tgtArtifactType.equals("meta_database"))
            domainIdField = null;

        boolean tgtHasWF = !tgtArtifactType.equals("entity_sample") && !tgtArtifactType.equals("entity_attribute")
                && !tgtArtifactType.equals("entity_sample_property") && !tgtArtifactType.equals("task") && !tgtArtifactType.equals("meta_database");

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, domainIdField, tgtHasWF || tgtArtifactType.equals("meta_database"), userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = "";
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String w = null;
        if (ar == null) {
            join = " JOIN da_" + userDetails.getTenant() + ".reference ref9 ON ((ref9.source_id=tbl1.id AND ref9.target_id='" + srcArtifactId
                    + "') OR (ref9.source_id='" + srcArtifactId + "' AND ref9.target_id=tbl1.id))";
            w = "ref9.id IS NOT NULL";
        } else {
            switch (ar.getRelationType()) {
                case ForeignKey:
                    if (ar.getArtifact2Type().toString().equals(tgtArtifactType) && ar.getArtifact2Column() != null)
                        w = "tbl1." + ar.getArtifact2Column() + "='" + srcArtifactId + "'";
                    else if (ar.getArtifact1Type().toString().equals(tgtArtifactType) && ar.getArtifact1Column() != null)
                        w = "tbl1." + ar.getArtifact1Column() + "='" + srcArtifactId + "'";
                    break;
                case CrossTable:
                    if (ar.getArtifact2Type().toString().equals(tgtArtifactType)) {
                        join = " JOIN da_" + userDetails.getTenant() + "." + ar.getCrossTableName() + " crt ON tbl1.id=crt."
                                + ar.getArtifact2Column() + " AND crt." + ar.getArtifact1Column() + "='" + srcArtifactId + "'";
                    } else if (ar.getArtifact1Type().toString().equals(tgtArtifactType)) {
                        join = " JOIN da_" + userDetails.getTenant() + "." + ar.getCrossTableName() + " crt ON tbl1.id=crt."
                                + ar.getArtifact1Column() + " AND crt." + ar.getArtifact2Column() + "='" + srcArtifactId + "'";
                    }
                    break;
                case CustomSQL:
                    w = ar.getWhereSQL().replaceAll("\\{tenant\\}", userDetails.getTenant()).replaceAll("\\{srcArtifactId\\}", "'" + srcArtifactId + "'");
                    join = ar.getJoinSQL().replaceAll("\\{tenant\\}", userDetails.getTenant()).replaceAll("\\{srcArtifactId\\}", "'" + srcArtifactId + "'");
                    break;
            }
        }

        if (w != null) {
            if (where.isEmpty())
                where = " WHERE " + w;
            else
                where += " AND " + w;
        }

        String subQuery = "SELECT d.*, true as has_access FROM da_" + userDetails.getTenant() + "." + tgtArtifactType + " d ";

        if (!tgtHasWF)
            subQuery = "SELECT sq.* FROM (" + subQuery + ") as sq ";
        else
            subQuery = "SELECT sq.*, wft.workflow_state FROM (" + subQuery + ") as sq left join da_" + userDetails.getTenant() + ".workflow_task wft "
                    + " on sq.workflow_task_id = wft.id ";
        subQuery = "SELECT distinct sq.*, t.tags FROM (" + subQuery + ") as sq "
                + "left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_" + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant() + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id ";

        String queryForItems = "SELECT distinct tbl1.* FROM (" + subQuery + ") as tbl1 " + join + where
                + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        log.info("QQQ " + queryForItems);

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1 " + join + where;

        RowMapper mapper = null;
        switch (tgtArtifactType) {
            case "entity_attribute":
                mapper = new EntityRepository.FlatDataEntityAttributeRowMapper();
                queryForItems = "SELECT distinct tbl1.*,eat.name as attribute_type_name, meta_column.id AS meta_column_id, (meta_schema.name || '.' || meta_table.name || '.' || meta_column.name) AS meta_column_name, meta_column.meta_database_id AS meta_database_id FROM (" + subQuery + ") as tbl1 " + join
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_attribute_type eat ON tbl1.attribute_type=eat.id "
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_attribute_to_sample_property ea2sp ON tbl1.id=ea2sp.entity_attribute_id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".reference rz ON ea2sp.entity_sample_property_id=rz.source_id AND rz.reference_type='SAMPLE_PROPERTY_TO_META_COLUMN'"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".meta_column ON rz.target_id=meta_column.id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".meta_object meta_table ON meta_column.meta_object_id=meta_table.id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".meta_object meta_schema ON meta_table.parent_id=meta_schema.id"
                        + where
                        + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                        + searchRequest.getLimit();
                queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1 " + join
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_attribute_type eat ON tbl1.attribute_type=eat.id "
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".entity_attribute_to_sample_property ea2sp ON tbl1.id=ea2sp.entity_attribute_id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".reference rz ON ea2sp.entity_sample_property_id=rz.source_id AND rz.reference_type='SAMPLE_PROPERTY_TO_META_COLUMN'"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".meta_column ON rz.target_id=meta_column.id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".meta_object meta_table ON meta_column.meta_object_id=meta_table.id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".meta_object meta_schema ON meta_table.parent_id=meta_schema.id"
                        + where;
                break;
            case "domain": mapper = new DomainRepository.FlatDomainRowMapper(); break;
            case "system": mapper = new SystemRepository.FlatSystemRowMapper(); break;
            case "entity": mapper = new EntityRepository.FlatDataEntityRowMapper(); break;
            case "entity_query":
                mapper = new EntityQueryRepository.FlatEntityQueryRowMapper();
                queryForItems = "SELECT tbl1.*,system.name as system_name, entity.name AS entity_name FROM (" + subQuery + ") as tbl1 " + join
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                        + ((userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) ?
                        " JOIN da_" + userDetails.getTenant() + ".system sys2 ON tbl1.system_id=sys2.id AND sys2.id IN (SELECT system_id FROM da_" + userDetails.getTenant() + ".system_to_domain WHERE domain_id IN ('" + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "'))"
                        : "")
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                        + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                        + searchRequest.getLimit();
                queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1 " + join
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                        + ((userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) ?
                        " JOIN da_" + userDetails.getTenant() + ".system sys2 ON tbl1.system_id=sys2.id AND sys2.id IN (SELECT system_id FROM da_" + userDetails.getTenant() + ".system_to_domain WHERE domain_id IN ('" + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "'))"
                        : "")
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                        + where;
                break;
            case "entity_sample":
                mapper = new EntitySampleRepository.FlatEntitySampleRowMapper();
                queryForItems = "SELECT tbl1.*, system.name as system_name, entity.name as entity_name, entity_query.name as entity_query_name FROM (" + subQuery + ") as tbl1 " + join
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                        + " LEFT JOIN da_" + userDetails.getTenant()
                        + ".entity_query entity_query ON tbl1.entity_query_id=entity_query.id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                        + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                        + searchRequest.getLimit();
                queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1 " + join
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                        + " LEFT JOIN da_" + userDetails.getTenant()
                        + ".entity_query entity_query ON tbl1.entity_query_id=entity_query.id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                        + where;
                break;
            case "data_asset":
                queryForItems = "SELECT tbl1.*, domain.name AS domain_name, system.name as system_name, entity.name AS entity_name FROM ("
                        + subQuery + ") as tbl1 " + join
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                        + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                        + searchRequest.getLimit();
                queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1 " + join
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".entity entity ON tbl1.entity_id=entity.id "
                        + where;
                mapper = new DataAssetRepository.FlatDataAssetRowMapper();
                break;
            case "business_entity":
                mapper = new BusinessEntityRepository.FlatBusinessEntityRowMapper();
                queryForItems = "SELECT tbl1.*, domain.name AS domain_name, datatype.name AS datatype_name FROM (" + subQuery + ") as tbl1 "
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id "
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".datatype ON tbl1.datatype_id=CAST(datatype.id AS TEXT) "
                        + join + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                        + searchRequest.getLimit();
                queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1 "
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id "
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".datatype ON tbl1.datatype_id=CAST(datatype.id AS TEXT) "
                        + join + where;
                break;
            case "indicator":
                mapper = new IndicatorRepository.FlatIndicatorRowMapper();
                queryForItems = "SELECT tbl1.*, domain.name as domain_name, indicator_type.name as indicator_type_name FROM (" + subQuery + ") as tbl1 " + join
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".indicator_type indicator_type on tbl1.indicator_type_id=indicator_type.id "
                        + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                        + searchRequest.getLimit();
                queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1 " + join
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".domain domain ON tbl1.domain_id=domain.id"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".indicator_type indicator_type on tbl1.indicator_type_id=indicator_type.id "
                        + where;
                break;
            case "product":
                mapper = new ProductRepository.FlatProductRowMapper(); break;
            case "entity_sample_property":
                queryForItems = "SELECT tbl1.*, ea.id AS entity_attribute_id, ea.name AS entity_attribute_name FROM (" + subQuery + ") AS tbl1 " + join
                        //+ " LEFT JOIN da_" + userDetails.getTenant() + ".entity_attribute_to_sample_property ea2sp ON tbl1.id=ea2sp.entity_sample_property_id"
                        //+ " LEFT JOIN da_" + userDetails.getTenant() + ".entity_attribute ea ON ea2sp.entity_attribute_id=ea.id"
                        + where
                        + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT " + searchRequest.getLimit();
                mapper = new EntitySampleRepository.FlatEntitySamplePropertyRowMapper();
                break;
            case "task":
                mapper = new TaskRepository.FlatTaskRowMapper();
                break;
            case "meta_database":
                queryForItems = "SELECT distinct tbl1.* FROM (" + subQuery + ") as tbl1 " + join + where
                        + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                        + searchRequest.getLimit();
                mapper = new MetaDatabaseRepository.FlatMetaDatabaseRowMapper();
                break;
        }

        List<? extends FlatModeledObject> flatItems =
                jdbcTemplate.query(queryForItems, mapper, whereValues.toArray());

        Integer total = jdbcTemplate.queryForObject(queryForTotal, Integer.class, whereValues.toArray());

        SearchResponse<? extends FlatModeledObject> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Data
    @NoArgsConstructor
    public class ServeFileData {
        private byte[] contents;
        private String contentType;
    }

    public UploadedFileData uploadImage(MultipartFile file, String folder) throws LottabyteException {
        if (file == null)
            return null;

        UploadedFileData res = new UploadedFileData();

        String path = applicationConfig.getBucketName();
        UUID fileId = UUID.randomUUID();
        String fileName = fileId + "-" + file.getOriginalFilename();

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(file.getContentType());
        objectMetadata.setContentLength(file.getSize());

        try {
            InputStream stream = new ByteArrayInputStream(file.getBytes());
            amazonS3.putObject(path, folder + "/" + fileName, stream, objectMetadata);

            res.setName(fileName);
            res.setUrl("/v1/artifacts/image/" + fileName);
            res.setSize(file.getSize());
        } catch (AmazonServiceException e) {
            throw new LottabyteException(Message.LBE00036, Language.ru, file.getOriginalFilename());
        } catch (IOException e) {
            throw new LottabyteException(Message.LBE00036, Language.ru, file.getOriginalFilename());
        }

        return res;
    }

    public ServeFileData getImage(String filename, String folder) throws LottabyteException {
        if (filename == null || filename.isEmpty())
            return null;

        try {
            ServeFileData res = new ServeFileData();

            S3Object obj = amazonS3.getObject(applicationConfig.getBucketName(), folder + "/" + filename);
            if (obj != null) {
                res.setContents(obj.getObjectContent().readAllBytes());

                ObjectMetadata meta = obj.getObjectMetadata();
                if (meta != null)
                    res.setContentType(meta.getContentType());

                return res;
            }
        } catch (IOException e) {
            throw new LottabyteException(Message.LBE00035, Language.ru, filename);
        }

        return null;
    }
}
