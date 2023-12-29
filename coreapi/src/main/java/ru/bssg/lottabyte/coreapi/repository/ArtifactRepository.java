package ru.bssg.lottabyte.coreapi.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.tools.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.domain.FlatDomain;
import ru.bssg.lottabyte.core.model.reference.ReferenceType;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchRequest;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.ui.model.dashboard.DashboardEntity;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelData;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelLinkData;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelNodeData;
import ru.bssg.lottabyte.core.ui.model.gojs.UpdatableGojsModelData;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.service.WorkflowService;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ArtifactRepository {

    private final JdbcTemplate jdbcTemplate;
    private final WorkflowService workflowService;

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
                    artifactType.getText() + " where id = ?", String.class, UUID.fromString(artifactId));
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

    public void generateArtifactModel(String artifactId, String artifactType, UserDetails userDetails) {
        ArtifactType at = ArtifactType.fromString(artifactType);

        List<String> nodeIds = new ArrayList<>();
        List<String> linkedNodeIds = new ArrayList<>();
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

        // jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() +
        // ".model_nodes (id, artifact_id, node_id, node_artifact_type, parent_node_id,
        // loc) (SELECT ?,?,id,?,NULL,'-200 -50' FROM da_"
        // + userDetails.getTenant() + "." + artifactType + " WHERE id=?)",
        // UUID.randomUUID(), UUID.fromString(artifactId), artifactType,
        // UUID.fromString(artifactId));

        jdbcTemplate.query("SELECT from_node_id, to_node_id FROM da_" + userDetails.getTenant()
                + ".model_links WHERE artifact_id=?", new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        linkedNodeIds.add(rs.getString("from_node_id") + rs.getString("to_node_id"));
                    }
                }, UUID.fromString(artifactId));

        String nodeId;
        switch (at) {
            case entity_query:
                List<String> esIds = jdbcTemplate.queryForList(
                        "SELECT id FROM da_" + userDetails.getTenant() + ".entity_sample WHERE entity_query_id=?",
                        String.class, UUID.fromString(artifactId));
                if (!esIds.isEmpty()) {

                    for (String esId : esIds) {
                        if (!nodeIds.contains(esId)) {
                            nodeIds.add(esId);
                            cols.get(6).add(new ArtifactModelNode(esId, "entity_sample", null, null));
                        }

                        addArtifactModelLink(linkedNodeIds, artifactId, esId, artifactId, userDetails);
                    }

                    jdbcTemplate.query(
                            "SELECT da.id AS asset_id, i.id AS indicator_id, p.id AS product_id, p.domain_id, es.id AS sample_id FROM da_"
                                    + userDetails.getTenant() + ".entity_sample es JOIN da_" + userDetails.getTenant()
                                    + ".data_asset da ON es.entity_id=da.entity_id AND es.system_id=da.system_id AND da.state='PUBLISHED' "
                                    + " LEFT JOIN da_" + userDetails.getTenant()
                                    + ".reference r ON r.reference_type='INDICATOR_TO_DATA_ASSET' AND r.target_id=da.id LEFT JOIN da_"
                                    + userDetails.getTenant()
                                    + ".indicator i ON r.source_id=i.id AND i.state='PUBLISHED'"
                                    + " LEFT JOIN da_" + userDetails.getTenant()
                                    + ".reference r2 ON r2.target_id=i.id LEFT JOIN da_" + userDetails.getTenant()
                                    + ".product p ON r2.source_id=p.id AND p.state='PUBLISHED' AND r2.reference_type='PRODUCT_TO_INDICATOR'"
                                    + " WHERE es.id IN ('" + StringUtils.join(esIds.toArray(), "','") + "')",
                            new RowCallbackHandler() {
                                @Override
                                public void processRow(ResultSet rs) throws SQLException {
                                    String assetId = rs.getString("asset_id");
                                    String indicatorId = rs.getString("indicator_id");
                                    String productId = rs.getString("product_id");
                                    String domainId = rs.getString("domain_id");
                                    String sampleId = rs.getString("sample_id");

                                    if (assetId != null && !nodeIds.contains(assetId)) {
                                        nodeIds.add(assetId);
                                        cols.get(7).add(new ArtifactModelNode(assetId, "data_asset", null, null));
                                    }

                                    if (indicatorId != null && !nodeIds.contains(indicatorId)) {
                                        nodeIds.add(indicatorId);
                                        cols.get(8).add(new ArtifactModelNode(indicatorId, "indicator", null, null));
                                    }

                                    if (productId != null && !nodeIds.contains(productId)) {
                                        nodeIds.add(productId);
                                        cols.get(9).add(new ArtifactModelNode(productId, "product", null, null));

                                    }

                                    if (domainId != null && !nodeIds.contains(domainId)) {
                                        nodeIds.add(domainId);
                                        cols.get(10).add(new ArtifactModelNode(domainId, "domain", null, null));

                                    }

                                    if (assetId != null) {
                                        addArtifactModelLink(linkedNodeIds, artifactId, assetId, sampleId, userDetails);
                                        if (indicatorId != null) {
                                            addArtifactModelLink(linkedNodeIds, artifactId, indicatorId, assetId,
                                                    userDetails);
                                            if (productId != null) {
                                                addArtifactModelLink(linkedNodeIds, artifactId, productId, indicatorId,
                                                        userDetails);
                                                if (domainId != null)
                                                    addArtifactModelLink(linkedNodeIds, artifactId, domainId, productId,
                                                            userDetails);
                                            }
                                        }
                                    }
                                }
                            });
                }

                nodeId = jdbcTemplate.queryForObject(
                        "SELECT system_id FROM da_" + userDetails.getTenant() + ".entity_query WHERE id=?",
                        String.class, UUID.fromString(artifactId));
                if (nodeId != null && !nodeIds.contains(nodeId)) {
                    nodeIds.add(nodeId);

                    cols.get(3).add(new ArtifactModelNode(nodeId, "system", null, null));

                    addArtifactModelLink(linkedNodeIds, artifactId, artifactId, nodeId, userDetails);
                }
                break;
            case entity:
                jdbcTemplate.query(
                        "SELECT da.id AS asset_id, i.id AS indicator_id, p.id AS product_id, p.domain_id FROM da_"
                                + userDetails.getTenant() + ".entity e JOIN da_" + userDetails.getTenant()
                                + ".data_asset da ON da.entity_id=e.id AND da.state='PUBLISHED' "
                                + " LEFT JOIN da_" + userDetails.getTenant()
                                + ".reference r ON r.reference_type='INDICATOR_TO_DATA_ASSET' AND r.target_id=da.id LEFT JOIN da_"
                                + userDetails.getTenant() + ".indicator i ON r.source_id=i.id AND i.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant()
                                + ".reference r2 ON r2.target_id=i.id LEFT JOIN da_" + userDetails.getTenant()
                                + ".product p ON r2.source_id=p.id AND p.state='PUBLISHED' AND r2.reference_type='PRODUCT_TO_INDICATOR'"
                                + " WHERE e.id=?",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String assetId = rs.getString("asset_id");
                                String indicatorId = rs.getString("indicator_id");
                                String productId = rs.getString("product_id");
                                String domainId = rs.getString("domain_id");

                                if (assetId != null && !nodeIds.contains(assetId)) {
                                    nodeIds.add(assetId);
                                    cols.get(6).add(new ArtifactModelNode(assetId, "data_asset", null, null));
                                }

                                if (indicatorId != null && !nodeIds.contains(indicatorId)) {
                                    nodeIds.add(indicatorId);
                                    cols.get(7).add(new ArtifactModelNode(indicatorId, "indicator", null, null));
                                }

                                if (productId != null && !nodeIds.contains(productId)) {
                                    nodeIds.add(productId);
                                    cols.get(8).add(new ArtifactModelNode(productId, "product", null, null));

                                }

                                if (domainId != null && !nodeIds.contains(domainId)) {
                                    nodeIds.add(domainId);
                                    cols.get(9).add(new ArtifactModelNode(domainId, "domain", null, null));

                                }

                                if (assetId != null) {
                                    addArtifactModelLink(linkedNodeIds, artifactId, assetId, artifactId, userDetails);
                                    if (indicatorId != null) {
                                        addArtifactModelLink(linkedNodeIds, artifactId, indicatorId, assetId,
                                                userDetails);
                                        if (productId != null) {
                                            addArtifactModelLink(linkedNodeIds, artifactId, productId, indicatorId,
                                                    userDetails);
                                            if (domainId != null)
                                                addArtifactModelLink(linkedNodeIds, artifactId, domainId, productId,
                                                        userDetails);
                                        }
                                    }
                                }
                            }
                        },
                        UUID.fromString(artifactId));
                break;
            case entity_sample:
                jdbcTemplate.query(
                        "SELECT da.id AS asset_id, i.id AS indicator_id, p.id AS product_id, p.domain_id FROM da_"
                                + userDetails.getTenant() + ".entity_sample es JOIN da_" + userDetails.getTenant()
                                + ".data_asset da ON es.entity_id=da.entity_id AND es.system_id=da.system_id AND da.state='PUBLISHED' "
                                + " LEFT JOIN da_" + userDetails.getTenant()
                                + ".reference r ON r.reference_type='INDICATOR_TO_DATA_ASSET' AND r.target_id=da.id LEFT JOIN da_"
                                + userDetails.getTenant() + ".indicator i ON r.source_id=i.id AND i.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant()
                                + ".reference r2 ON r2.target_id=i.id LEFT JOIN da_" + userDetails.getTenant()
                                + ".product p ON r2.source_id=p.id AND p.state='PUBLISHED' AND r2.reference_type='PRODUCT_TO_INDICATOR'"
                                + " WHERE es.id=?",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String assetId = rs.getString("asset_id");
                                String indicatorId = rs.getString("indicator_id");
                                String productId = rs.getString("product_id");
                                String domainId = rs.getString("domain_id");

                                if (assetId != null && !nodeIds.contains(assetId)) {
                                    nodeIds.add(assetId);
                                    cols.get(6).add(new ArtifactModelNode(assetId, "data_asset", null, null));
                                }

                                if (indicatorId != null && !nodeIds.contains(indicatorId)) {
                                    nodeIds.add(indicatorId);
                                    cols.get(7).add(new ArtifactModelNode(indicatorId, "indicator", null, null));
                                }

                                if (productId != null && !nodeIds.contains(productId)) {
                                    nodeIds.add(productId);
                                    cols.get(8).add(new ArtifactModelNode(productId, "product", null, null));

                                }

                                if (domainId != null && !nodeIds.contains(domainId)) {
                                    nodeIds.add(domainId);
                                    cols.get(9).add(new ArtifactModelNode(domainId, "domain", null, null));

                                }

                                if (assetId != null) {
                                    addArtifactModelLink(linkedNodeIds, artifactId, assetId, artifactId, userDetails);
                                    if (indicatorId != null) {
                                        addArtifactModelLink(linkedNodeIds, artifactId, indicatorId, assetId,
                                                userDetails);
                                        if (productId != null) {
                                            addArtifactModelLink(linkedNodeIds, artifactId, productId, indicatorId,
                                                    userDetails);
                                            if (domainId != null)
                                                addArtifactModelLink(linkedNodeIds, artifactId, domainId, productId,
                                                        userDetails);
                                        }
                                    }
                                }
                            }
                        },
                        UUID.fromString(artifactId));

                jdbcTemplate.query("SELECT es.entity_query_id, eq.system_id FROM da_" + userDetails.getTenant()
                        + ".entity_sample es JOIN da_"
                        + userDetails.getTenant() + ".entity_query eq ON es.entity_query_id=eq.id WHERE es.id=?",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String qId = rs.getString("entity_query_id");
                                String sId = rs.getString("system_id");

                                if (qId != null && !nodeIds.contains(qId)) {
                                    nodeIds.add(qId);

                                    cols.get(3).add(new ArtifactModelNode(qId, "entity_query", null, null));

                                    addArtifactModelLink(linkedNodeIds, artifactId, artifactId, qId, userDetails);
                                }

                                if (qId != null && sId != null && !nodeIds.contains(sId)) {
                                    nodeIds.add(sId);

                                    cols.get(2).add(new ArtifactModelNode(sId, "system", null, null));

                                    addArtifactModelLink(linkedNodeIds, artifactId, qId, sId, userDetails);
                                }
                            }
                        }, UUID.fromString(artifactId));
                break;
            case data_asset:
                jdbcTemplate.query("SELECT i.id AS indicator_id, p.id AS product_id, p.domain_id FROM da_"
                        + userDetails.getTenant() + ".reference r JOIN da_" + userDetails.getTenant()
                        + ".indicator i ON r.source_id=i.id AND i.state='PUBLISHED'"
                        + " LEFT JOIN da_" + userDetails.getTenant()
                        + ".reference r2 ON r2.target_id=i.id LEFT JOIN da_" + userDetails.getTenant()
                        + ".product p ON r2.source_id=p.id AND p.state='PUBLISHED' AND r2.reference_type='PRODUCT_TO_INDICATOR'"
                        + " WHERE r.target_id=? AND r.reference_type='INDICATOR_TO_DATA_ASSET' ",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String indicatorId = rs.getString("indicator_id");
                                String productId = rs.getString("product_id");
                                String domainId = rs.getString("domain_id");

                                if (indicatorId != null && !nodeIds.contains(indicatorId)) {
                                    nodeIds.add(indicatorId);
                                    cols.get(6).add(new ArtifactModelNode(indicatorId, "indicator", null, null));

                                }

                                if (productId != null && !nodeIds.contains(productId)) {
                                    nodeIds.add(productId);
                                    cols.get(7).add(new ArtifactModelNode(productId, "product", null, null));

                                }

                                if (domainId != null && !nodeIds.contains(domainId)) {
                                    nodeIds.add(domainId);
                                    cols.get(8).add(new ArtifactModelNode(domainId, "domain", null, null));

                                }

                                if (indicatorId != null) {
                                    addArtifactModelLink(linkedNodeIds, artifactId, indicatorId, artifactId,
                                            userDetails);
                                    if (productId != null) {
                                        addArtifactModelLink(linkedNodeIds, artifactId, productId, indicatorId,
                                                userDetails);
                                        if (domainId != null)
                                            addArtifactModelLink(linkedNodeIds, artifactId, domainId, productId,
                                                    userDetails);
                                    }
                                }
                            }
                        }, UUID.fromString(artifactId));

                jdbcTemplate.query("SELECT es.id, es.system_id, es.entity_query_id FROM da_" + userDetails.getTenant()
                        + ".entity_sample es JOIN da_" + userDetails.getTenant()
                        + ".data_asset da ON es.entity_id = da.entity_id AND es.system_id=da.system_id WHERE da.id=?",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String sampleId = rs.getString("id");
                                String systemId = rs.getString("system_id");
                                String queryId = rs.getString("entity_query_id");

                                if (sampleId != null && !nodeIds.contains(sampleId)) {
                                    nodeIds.add(sampleId);
                                    cols.get(3).add(new ArtifactModelNode(sampleId, "entity_sample", null, null));
                                }
                                if (queryId != null && !nodeIds.contains(queryId)) {
                                    nodeIds.add(queryId);
                                    cols.get(2).add(new ArtifactModelNode(queryId, "entity_query", null, null));
                                }
                                if (systemId != null && !nodeIds.contains(systemId)) {
                                    nodeIds.add(systemId);
                                    cols.get(1).add(new ArtifactModelNode(systemId, "system", null, null));
                                }

                                if (sampleId != null) {
                                    addArtifactModelLink(linkedNodeIds, artifactId, artifactId, sampleId, userDetails);

                                    if (queryId != null) {
                                        addArtifactModelLink(linkedNodeIds, artifactId, sampleId, queryId, userDetails);

                                        if (systemId != null) {
                                            addArtifactModelLink(linkedNodeIds, artifactId, queryId, systemId,
                                                    userDetails);

                                        }
                                    }
                                }
                            }
                        }, UUID.fromString(artifactId));

                nodeId = jdbcTemplate.queryForObject(
                        "SELECT entity_id FROM da_" + userDetails.getTenant() + ".data_asset WHERE id=?", String.class,
                        UUID.fromString(artifactId));
                if (nodeId != null && !nodeIds.contains(nodeId)) {
                    nodeIds.add(nodeId);
                    cols.get(3).add(new ArtifactModelNode(nodeId, "entity", null, null));

                    for (String attrId : jdbcTemplate.queryForList(
                            "SELECT id FROM da_" + userDetails.getTenant() + ".entity_attribute WHERE entity_id=?",
                            String.class, UUID.fromString(nodeId))) {
                        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                                + ".model_nodes (id, artifact_id, node_id, parent_node_id, node_artifact_type) VALUES (?,?,?,?,'entity_attribute')",
                                UUID.randomUUID(), UUID.fromString(artifactId), UUID.fromString(attrId),
                                UUID.fromString(nodeId));
                    }

                    addArtifactModelLink(linkedNodeIds, artifactId, artifactId, nodeId, userDetails);
                }

                break;
            case domain:
                jdbcTemplate.query(
                        "SELECT p.id AS product_id, r2.target_id AS indicator_id, da.id AS asset_id, da.entity_id, es.id AS sample_id, es.entity_query_id, es.system_id FROM da_" + userDetails.getTenant() + ".product p LEFT JOIN da_"
                                + userDetails.getTenant()
                                + ".reference r2 ON r2.source_id=p.id AND r2.reference_type='PRODUCT_TO_INDICATOR' AND p.state='PUBLISHED' LEFT JOIN da_"
                                + userDetails.getTenant()
                                + ".indicator i ON r2.reference_type='PRODUCT_TO_INDICATOR' AND r2.target_id=i.id AND i.state='PUBLISHED' LEFT JOIN da_"
                                + userDetails.getTenant()
                                + ".reference r ON r.source_id=i.id AND r.reference_type='INDICATOR_TO_DATA_ASSET' LEFT JOIN da_"
                                + userDetails.getTenant()
                                + ".data_asset da ON r.target_id=da.id AND da.state='PUBLISHED' LEFT JOIN da_"
                                + userDetails.getTenant()
                                + ".entity_sample es ON da.system_id=es.system_id AND da.entity_id=es.entity_id WHERE p.domain_id=? AND p.state='PUBLISHED'",
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

                                if (productId != null && !nodeIds.contains(productId)) {
                                    nodeIds.add(productId);
                                    cols.get(4).add(new ArtifactModelNode(productId, "product", null, null));
                                }
                                if (indicatorId != null && !nodeIds.contains(indicatorId)) {
                                    nodeIds.add(indicatorId);
                                    cols.get(4).add(new ArtifactModelNode(indicatorId, "indicator", null, null));
                                }
                                if (assetId != null && !nodeIds.contains(assetId)) {
                                    nodeIds.add(assetId);
                                    cols.get(3).add(new ArtifactModelNode(assetId, "data_asset", null, null));
                                }
                                if (sampleId != null && !nodeIds.contains(sampleId)) {
                                    nodeIds.add(sampleId);
                                    cols.get(2).add(new ArtifactModelNode(sampleId, "entity_sample", null, null));
                                }
                                if (entityId != null && !nodeIds.contains(entityId)) {
                                    nodeIds.add(entityId);
                                    cols.get(2).add(new ArtifactModelNode(entityId, "entity", null, null));

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
                                    cols.get(1).add(new ArtifactModelNode(queryId, "entity_query", null, null));
                                }
                                if (systemId != null && !nodeIds.contains(systemId)) {
                                    nodeIds.add(systemId);
                                    cols.get(0).add(new ArtifactModelNode(systemId, "system", null, null));
                                }

                                if (productId != null) {
                                    addArtifactModelLink(linkedNodeIds, artifactId, artifactId, productId, userDetails);

                                    if (indicatorId != null) {
                                        addArtifactModelLink(linkedNodeIds, artifactId, productId, indicatorId,
                                                userDetails);

                                        if (assetId != null) {
                                            addArtifactModelLink(linkedNodeIds, artifactId, indicatorId, assetId,
                                                    userDetails);

                                            if (entityId != null) {
                                                addArtifactModelLink(linkedNodeIds, artifactId, assetId, entityId,
                                                        userDetails);
                                            }

                                            if (sampleId != null) {
                                                addArtifactModelLink(linkedNodeIds, artifactId, assetId, sampleId,
                                                        userDetails);

                                                if (queryId != null) {
                                                    addArtifactModelLink(linkedNodeIds, artifactId, sampleId, queryId,
                                                            userDetails);

                                                    if (systemId != null) {
                                                        addArtifactModelLink(linkedNodeIds, artifactId, queryId,
                                                                systemId, userDetails);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }, UUID.fromString(artifactId));
                break;
            case product:
                nodeId = jdbcTemplate.queryForObject(
                        "SELECT domain_id FROM da_" + userDetails.getTenant() + ".product WHERE id=?", String.class,
                        UUID.fromString(artifactId));
                if (nodeId != null && !nodeIds.contains(nodeId)) {
                    nodeIds.add(nodeId);
                    cols.get(6).add(new ArtifactModelNode(nodeId, "domain", null, null));

                    addArtifactModelLink(linkedNodeIds, artifactId, nodeId, artifactId, userDetails);
                }
                jdbcTemplate.query(
                        "SELECT r2.target_id AS indicator_id, da.id AS asset_id, da.entity_id, es.id AS sample_id, es.entity_query_id, es.system_id FROM da_"
                                + userDetails.getTenant()
                                + ".reference r2 LEFT JOIN da_" + userDetails.getTenant()
                                + ".indicator i ON r2.reference_type='PRODUCT_TO_INDICATOR' AND r2.target_id=i.id AND i.state='PUBLISHED' LEFT JOIN da_"
                                + userDetails.getTenant()
                                + ".reference r ON r.source_id=i.id AND r.reference_type='INDICATOR_TO_DATA_ASSET' LEFT JOIN da_"
                                + userDetails.getTenant()
                                + ".data_asset da ON r.target_id=da.id AND da.state='PUBLISHED' LEFT JOIN da_"
                                + userDetails.getTenant()
                                + ".entity_sample es ON da.system_id=es.system_id AND da.entity_id=es.entity_id WHERE r2.source_id=? AND r2.reference_type='PRODUCT_TO_INDICATOR'",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String indicatorId = rs.getString("indicator_id");
                                String assetId = rs.getString("asset_id");
                                String entityId = rs.getString("entity_id");
                                String sampleId = rs.getString("sample_id");
                                String queryId = rs.getString("entity_query_id");
                                String systemId = rs.getString("system_id");

                                if (indicatorId != null && !nodeIds.contains(indicatorId)) {
                                    nodeIds.add(indicatorId);
                                    cols.get(4).add(new ArtifactModelNode(indicatorId, "indicator", null, null));
                                }
                                if (assetId != null && !nodeIds.contains(assetId)) {
                                    nodeIds.add(assetId);
                                    cols.get(3).add(new ArtifactModelNode(assetId, "data_asset", null, null));
                                }
                                if (sampleId != null && !nodeIds.contains(sampleId)) {
                                    nodeIds.add(sampleId);
                                    cols.get(2).add(new ArtifactModelNode(sampleId, "entity_sample", null, null));
                                }
                                if (entityId != null && !nodeIds.contains(entityId)) {
                                    nodeIds.add(entityId);
                                    cols.get(2).add(new ArtifactModelNode(entityId, "entity", null, null));

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
                                    cols.get(1).add(new ArtifactModelNode(queryId, "entity_query", null, null));
                                }
                                if (systemId != null && !nodeIds.contains(systemId)) {
                                    nodeIds.add(systemId);
                                    cols.get(0).add(new ArtifactModelNode(systemId, "system", null, null));
                                }

                                if (indicatorId != null) {
                                    addArtifactModelLink(linkedNodeIds, artifactId, artifactId, indicatorId,
                                            userDetails);

                                    if (assetId != null) {
                                        addArtifactModelLink(linkedNodeIds, artifactId, indicatorId, assetId,
                                                userDetails);

                                        if (entityId != null) {
                                            addArtifactModelLink(linkedNodeIds, artifactId, assetId, entityId,
                                                    userDetails);
                                        }

                                        if (sampleId != null) {
                                            addArtifactModelLink(linkedNodeIds, artifactId, assetId, sampleId,
                                                    userDetails);

                                            if (queryId != null) {
                                                addArtifactModelLink(linkedNodeIds, artifactId, sampleId, queryId,
                                                        userDetails);

                                                if (systemId != null) {
                                                    addArtifactModelLink(linkedNodeIds, artifactId, queryId, systemId,
                                                            userDetails);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }, UUID.fromString(artifactId));
                break;
            case indicator:

                jdbcTemplate.query("SELECT p.id AS product_id, p.domain_id FROM da_" + userDetails.getTenant()
                        + ".reference r JOIN da_" + userDetails.getTenant()
                        + ".product p ON r.source_id=p.id AND p.state='PUBLISHED' WHERE r.target_id=? AND r.reference_type='PRODUCT_TO_INDICATOR'",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String productId = rs.getString("product_id");
                                String domainId = rs.getString("domain_id");

                                if (productId != null && !nodeIds.contains(productId)) {
                                    nodeIds.add(productId);
                                    cols.get(6).add(new ArtifactModelNode(productId, "product", null, null));

                                }

                                if (domainId != null && !nodeIds.contains(domainId)) {
                                    nodeIds.add(domainId);
                                    cols.get(7).add(new ArtifactModelNode(domainId, "domain", null, null));

                                }

                                if (productId != null) {
                                    addArtifactModelLink(linkedNodeIds, artifactId, productId, artifactId, userDetails);

                                    if (domainId != null)
                                        addArtifactModelLink(linkedNodeIds, artifactId, domainId, productId,
                                                userDetails);
                                }
                            }
                        }, UUID.fromString(artifactId));

                jdbcTemplate.query(
                        "SELECT da.id AS asset_id, da.entity_id, es.id AS sample_id, es.entity_query_id, es.system_id FROM da_"
                                + userDetails.getTenant() + ".reference r JOIN da_" + userDetails.getTenant()
                                + ".data_asset da ON r.reference_type='INDICATOR_TO_DATA_ASSET' AND r.target_id=da.id AND da.state='PUBLISHED' LEFT JOIN da_"
                                + userDetails.getTenant()
                                + ".entity_sample es ON da.system_id=es.system_id AND da.entity_id=es.entity_id WHERE r.source_id=?",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String assetId = rs.getString("asset_id");
                                String entityId = rs.getString("entity_id");
                                String sampleId = rs.getString("sample_id");
                                String queryId = rs.getString("entity_query_id");
                                String systemId = rs.getString("system_id");

                                if (assetId != null && !nodeIds.contains(assetId)) {
                                    nodeIds.add(assetId);
                                    cols.get(4).add(new ArtifactModelNode(assetId, "data_asset", null, null));
                                }
                                if (sampleId != null && !nodeIds.contains(sampleId)) {
                                    nodeIds.add(sampleId);
                                    cols.get(3).add(new ArtifactModelNode(sampleId, "entity_sample", null, null));
                                }
                                if (entityId != null && !nodeIds.contains(entityId)) {
                                    nodeIds.add(entityId);
                                    cols.get(3).add(new ArtifactModelNode(entityId, "entity", null, null));

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
                                    cols.get(2).add(new ArtifactModelNode(queryId, "entity_query", null, null));
                                }
                                if (systemId != null && !nodeIds.contains(systemId)) {
                                    nodeIds.add(systemId);
                                    cols.get(1).add(new ArtifactModelNode(systemId, "system", null, null));
                                }

                                if (assetId != null) {
                                    addArtifactModelLink(linkedNodeIds, artifactId, artifactId, assetId, userDetails);

                                    if (entityId != null) {
                                        addArtifactModelLink(linkedNodeIds, artifactId, assetId, entityId, userDetails);
                                    }

                                    if (sampleId != null) {
                                        addArtifactModelLink(linkedNodeIds, artifactId, assetId, sampleId, userDetails);

                                        if (queryId != null) {
                                            addArtifactModelLink(linkedNodeIds, artifactId, sampleId, queryId,
                                                    userDetails);

                                            if (systemId != null) {
                                                addArtifactModelLink(linkedNodeIds, artifactId, queryId, systemId,
                                                        userDetails);
                                            }
                                        }
                                    }
                                }
                            }
                        }, UUID.fromString(artifactId));

                break;

            case system:

                jdbcTemplate.query(
                        "SELECT eq.id AS query_id, es.id AS sample_id, da.id AS asset_id, i.id AS indicator_id, p.id AS product_id, p.domain_id FROM da_"
                                + userDetails.getTenant() + ".entity_query eq "
                                + "LEFT JOIN da_" + userDetails.getTenant()
                                + ".entity_sample es ON es.entity_query_id=eq.id "
                                + "LEFT JOIN da_" + userDetails.getTenant()
                                + ".data_asset da ON es.entity_id=da.entity_id AND es.system_id=da.system_id AND da.state='PUBLISHED' "
                                + " LEFT JOIN da_" + userDetails.getTenant()
                                + ".reference r ON r.reference_type='INDICATOR_TO_DATA_ASSET' AND r.target_id=da.id LEFT JOIN da_"
                                + userDetails.getTenant() + ".indicator i ON r.source_id=i.id AND i.state='PUBLISHED'"
                                + " LEFT JOIN da_" + userDetails.getTenant()
                                + ".reference r2 ON r2.target_id=i.id LEFT JOIN da_" + userDetails.getTenant()
                                + ".product p ON r2.source_id=p.id AND p.state='PUBLISHED' AND r2.reference_type='PRODUCT_TO_INDICATOR'"
                                + "WHERE eq.system_id=? AND eq.state='PUBLISHED'",
                        new RowCallbackHandler() {
                            @Override
                            public void processRow(ResultSet rs) throws SQLException {
                                String assetId = rs.getString("asset_id");
                                String indicatorId = rs.getString("indicator_id");
                                String productId = rs.getString("product_id");
                                String domainId = rs.getString("domain_id");
                                String sampleId = rs.getString("sample_id");
                                String queryId = rs.getString("query_id");

                                if (queryId != null && !nodeIds.contains(queryId)) {
                                    nodeIds.add(queryId);
                                    cols.get(6).add(new ArtifactModelNode(queryId, "entity_query", null, null));
                                }

                                if (sampleId != null && !nodeIds.contains(sampleId)) {
                                    nodeIds.add(sampleId);
                                    cols.get(7).add(new ArtifactModelNode(sampleId, "entity_sample", null, null));
                                }

                                if (assetId != null && !nodeIds.contains(assetId)) {
                                    nodeIds.add(assetId);
                                    cols.get(8).add(new ArtifactModelNode(assetId, "data_asset", null, null));
                                }

                                if (indicatorId != null && !nodeIds.contains(indicatorId)) {
                                    nodeIds.add(indicatorId);
                                    cols.get(9).add(new ArtifactModelNode(indicatorId, "indicator", null, null));
                                }

                                if (productId != null && !nodeIds.contains(productId)) {
                                    nodeIds.add(productId);
                                    cols.get(10).add(new ArtifactModelNode(productId, "product", null, null));

                                }

                                if (domainId != null && !nodeIds.contains(domainId)) {
                                    nodeIds.add(domainId);
                                    cols.get(10).add(new ArtifactModelNode(domainId, "domain", null, null));

                                }

                                if (queryId != null) {
                                    addArtifactModelLink(linkedNodeIds, artifactId, queryId, artifactId, userDetails);
                                    if (sampleId != null) {
                                        addArtifactModelLink(linkedNodeIds, artifactId, sampleId, queryId, userDetails);
                                        if (assetId != null) {
                                            addArtifactModelLink(linkedNodeIds, artifactId, assetId, sampleId,
                                                    userDetails);
                                            if (indicatorId != null) {
                                                addArtifactModelLink(linkedNodeIds, artifactId, indicatorId, assetId,
                                                        userDetails);
                                                if (productId != null) {
                                                    addArtifactModelLink(linkedNodeIds, artifactId, productId,
                                                            indicatorId, userDetails);
                                                    if (domainId != null)
                                                        addArtifactModelLink(linkedNodeIds, artifactId, domainId,
                                                                productId, userDetails);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }, UUID.fromString(artifactId));

                break;
        }

        List<String> allNodeIds = new ArrayList<>();

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
                    boolean nodeExists = jdbcTemplate.queryForObject(
                            "SELECT COUNT(id) FROM da_" + userDetails.getTenant()
                                    + ".model_nodes WHERE artifact_id=? AND node_id=?",
                            Integer.class,
                            UUID.fromString(artifactId), UUID.fromString(cols.get(c).get(i).getNodeId())) > 0;

                    if (nodeExists) {
                        jdbcTemplate.update(
                                "UPDATE da_" + userDetails.getTenant()
                                        + ".model_nodes SET loc=? WHERE artifact_id=? AND node_id=?",
                                locX + " " + locY, UUID.fromString(artifactId),
                                UUID.fromString(cols.get(c).get(i).getNodeId()));
                        jdbcTemplate.update("UPDATE da_" + userDetails.getTenant()
                                + ".model_links SET points=NULL WHERE artifact_id=? AND (from_node_id=? OR to_node_id=?)",
                                UUID.fromString(artifactId),
                                UUID.fromString(cols.get(c).get(i).getNodeId()),
                                UUID.fromString(cols.get(c).get(i).getNodeId()));
                    } else {

                        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant()
                                + ".model_nodes (id, artifact_id, artifact_type, node_id, node_artifact_type, loc) VALUES (?,?,?,?,?,?)",
                                UUID.randomUUID(), UUID.fromString(artifactId), artifactType,
                                UUID.fromString(cols.get(c).get(i).getNodeId()), cols.get(c).get(i).getArtifactType(),
                                locX + " " + locY);
                    }

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

        }

        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".model_links WHERE from_node_id NOT IN ('"
                + StringUtils.join(allNodeIds.toArray(), "','")
                + "') OR to_node_id NOT IN ('" + StringUtils.join(allNodeIds.toArray(), "','") + "')");
        jdbcTemplate.update(
                "DELETE FROM da_" + userDetails.getTenant() + ".model_nodes WHERE artifact_id=? AND node_id NOT IN ('"
                        + StringUtils.join(allNodeIds.toArray(), "','") + "')",
                UUID.fromString(artifactId));
    }

    public GojsModelData getArtifactModel(String artifactId, String artifactType, UserDetails userDetails) {
        GojsModelData res = new GojsModelData();
        List<GojsModelNodeData> nodes = new ArrayList<>();
        List<GojsModelLinkData> links = new ArrayList<>();

        // clearModels(userDetails);

        // if (jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" +
        // userDetails.getTenant() + ".model_nodes WHERE artifact_id=?", Integer.class,
        // UUID.fromString(artifactId)) == 0)
        generateArtifactModel(artifactId, artifactType, userDetails);

        jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant()
                + ".model_nodes WHERE artifact_id=? ORDER BY parent_node_id DESC", new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        GojsModelNodeData nd = new GojsModelNodeData();
                        nd.setId(rs.getString("node_id"));
                        try {
                            String name = jdbcTemplate.queryForObject(
                                    "SELECT name FROM da_" + userDetails.getTenant() + "."
                                            + rs.getString("node_artifact_type") + " WHERE id=?",
                                    String.class, UUID.fromString(rs.getString("node_id")));
                            if (name.length() > 40)
                                name = name.substring(0, 40) + "...";
                            nd.setName(name);
                            nd.setText(name);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            nd.setName("???");
                            nd.setText("???");
                        }
                        nd.setType("defaultNodeType");
                        nd.setZOrder(1);
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

                        nd.setLoc(rs.getString("loc"));
                        nd.setArtifactType(rs.getString("node_artifact_type"));

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
            String artifactType, String artifactId, UserDetails userDetails) {
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
                        "SELECT id, name, description, workflow_task_id, created, 'domain' as artifact_type, modified FROM da_"
                                + userDetails.getTenant() + ".domain WHERE state='DRAFT'");
                if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
                    sb.append(" AND id IN ('"
                            + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "') ");
                }
                break;
            case system:
                sb.append(
                        "SELECT id, name, description, workflow_task_id, created, 'system' as artifact_type, modified FROM da_"
                                + userDetails.getTenant() + ".system WHERE state='DRAFT'");
                if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
                    sb.append(" AND id IN (SELECT system_id FROM da_" + userDetails.getTenant()
                            + ".system_to_domain WHERE domain_id IN ('"
                            + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "')) ");
                }
                break;
            case entity:
                sb.append(
                        "SELECT id, name, description, workflow_task_id, created, 'entity' as artifact_type, modified FROM da_"
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
                        "SELECT id, name, description, workflow_task_id, created, 'entity_query' as artifact_type, modified FROM da_"
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
                        "SELECT id, name, description, workflow_task_id, created, 'data_asset' as artifact_type, modified FROM da_"
                                + userDetails.getTenant() + ".data_asset WHERE state='DRAFT'");
                if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
                    sb.append(" AND domain_id IN ('"
                            + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "') ");
                }
                break;
            case indicator:
                sb.append(
                        "SELECT id, name, description, workflow_task_id, created, 'indicator' as artifact_type, modified FROM da_"
                                + userDetails.getTenant() + ".indicator WHERE state='DRAFT'");
                if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
                    sb.append(" AND domain_id IN ('"
                            + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "') ");
                }
                break;
            case business_entity:
                sb.append(
                        "SELECT id, name, description, workflow_task_id, created, 'business_entity' as artifact_type, modified FROM da_"
                                + userDetails.getTenant() + ".business_entity WHERE state='DRAFT'");
                if (userDetails.getUserDomains() != null && !userDetails.getUserDomains().isEmpty()) {
                    sb.append(" AND domain_id IN ('"
                            + org.apache.commons.lang3.StringUtils.join(userDetails.getUserDomains(), "','") + "') ");
                }
                break;
            case product:
                sb.append(
                        "SELECT id, name, description, workflow_task_id, created, 'product' as artifact_type, modified FROM da_"
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
}
