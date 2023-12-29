package ru.bssg.lottabyte.coreapi.util;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

@Slf4j
public class QueryHelper {

    public static String getWhereIdInQuery(ArtifactType artifactType, UserDetails userDetails) {
        StringBuilder sb = new StringBuilder();
        sb.append("where ");
        if (artifactType == ArtifactType.task) {
            sb.append("(" + artifactType.getText() + ".query_id is null or " + artifactType.getText() + ".query_id in (");
            sb.append("select entity_query.id from da_" + userDetails.getTenant() + ".entity_query ");
            sb.append(getJoinQuery(ArtifactType.entity_query, userDetails));
            sb.append(")) and (" + artifactType.getText() + ".system_connection_id is null or " + artifactType.getText() + ".system_connection_id in (");
            sb.append("select system_connection.id from da_" + userDetails.getTenant() + ".system_connection ");
            sb.append(getJoinQuery(ArtifactType.system_connection, userDetails));
            sb.append(")) ");
        }
        if (artifactType == ArtifactType.entity_sample_property) {
            sb.append("(" + artifactType.getText() + ".entity_sample_id in (");
            sb.append("select entity_sample.id from da_" + userDetails.getTenant() + ".entity_sample ");
            sb.append(getJoinQuery(ArtifactType.entity_sample, userDetails));
            sb.append(")) ");
        }
        if (artifactType == ArtifactType.entity_sample) {
            sb.append("(" + artifactType.getText() + ".entity_query_id is null or " + artifactType.getText() + ".entity_query_id in (");
            sb.append("select entity_query.id from da_" + userDetails.getTenant() + ".entity_query ");
            sb.append(getJoinQuery(ArtifactType.entity_query, userDetails));
            sb.append(")) and ");
        }
        if (artifactType == ArtifactType.data_asset) {
            sb.append("(" + artifactType.getText() + ".domain_id in (");
            sb.append("select domain.id from da_" + userDetails.getTenant() + ".domain ");
            sb.append(getJoinQuery(ArtifactType.domain, userDetails));
            //sb.append(" WHERE domain.state='PUBLISHED')) and ");
            sb.append(" WHERE domain.state='PUBLISHED')) ");
        }
        if (artifactType == ArtifactType.indicator || artifactType == ArtifactType.business_entity || artifactType == ArtifactType.product) {
            sb.append("(" + artifactType.getText() + ".domain_id in (");
            sb.append("select domain.id from da_" + userDetails.getTenant() + ".domain ");
            sb.append(getJoinQuery(ArtifactType.domain, userDetails));
            sb.append(")) ");
        }
        if (artifactType == ArtifactType.entity_query //|| artifactType == ArtifactType.data_asset
                || artifactType == ArtifactType.entity_sample) {
            //sb.append("select entity_query.* from da_" + userDetails.getTenant() + ".entity_query ");
            sb.append("(" + artifactType.getText() + ".entity_id is null or " + artifactType.getText() + ".entity_id in (");
            sb.append("select entity.id from da_" + userDetails.getTenant() + ".entity ");
            sb.append(getJoinQuery(ArtifactType.entity, userDetails)).append(" and entity.state='PUBLISHED' ");
            sb.append(")) and (" + artifactType.getText() + ".system_id is null or " + artifactType.getText() + ".system_id in (");
            sb.append("select system.id from da_" + userDetails.getTenant() + ".system ");
            sb.append(getJoinQuery(ArtifactType.system, userDetails)).append(" and system.state='PUBLISHED' ");
            sb.append("))");
        }
        return sb.toString();
    }

    public static String getJoinQuery(ArtifactType artifactType, UserDetails userDetails) {
        String tenant = userDetails.getTenant();
        StringBuilder sb = new StringBuilder();
        if (artifactType == ArtifactType.data_asset)
            sb.append(" left join da_" + tenant + ".entity on data_asset.entity_id = entity.id and entity.state='PUBLISHED' "
                    + " left join da_" + tenant + ".system on data_asset.system_id = system.id and system.state='PUBLISHED' "
                    + " join da_" + tenant + ".domain on data_asset.domain_id = domain.id and domain.state='PUBLISHED' ");
                    //+ " left join da_" + tenant + ".entity_to_system e2s on e2s.entity_id = entity.id and e2s.system_id = system.id "
                    //+ " join da_" + tenant + ".system_to_domain s2d on s2d.system_id = system.id and s2d.domain_id = domain.id ");
        if (artifactType == ArtifactType.product || artifactType == ArtifactType.indicator || artifactType == ArtifactType.business_entity)
            sb.append(" join da_" + tenant + ".domain on " + artifactType.getText() + ".domain_id = domain.id and domain.state='PUBLISHED' ");
        if (artifactType == ArtifactType.task)
            sb.append(" join da_" + tenant + ".entity_query on entity_query.id = task.query_id and entity_query.state='PUBLISHED' ");
        if (artifactType == ArtifactType.system_connection)
            sb.append(" join da_" + userDetails.getTenant() + ".system on system_connection.system_id = system.id and system.state='PUBLISHED' ");
        if (artifactType == ArtifactType.entity_sample_property)
            sb.append(" join da_" + tenant + ".entity_sample on entity_sample_property.entity_sample_id = entity_sample.id ");
        if (artifactType == ArtifactType.entity_sample || artifactType == ArtifactType.entity_sample_property)
            sb.append(" join da_" + tenant + ".entity on entity_sample.entity_id = entity.id and entity.state='PUBLISHED' "
                    + " join da_" + tenant + ".system s2 on s2.id = entity_sample.system_id and s2.state='PUBLISHED' " +
                    "   join da_" + tenant + ".entity_query on entity_query.id = entity_sample.entity_query_id and entity_query.state='PUBLISHED' ");
        if (artifactType == ArtifactType.entity_query || artifactType == ArtifactType.task)
            sb.append(" join da_" + tenant + ".entity on entity_query.entity_id = entity.id and entity.state='PUBLISHED' "
                    + " join da_" + tenant + ".system s2 on s2.id = entity_query.system_id and s2.state='PUBLISHED' ");
        if (artifactType == ArtifactType.entity || artifactType == ArtifactType.entity_query ||
                artifactType == ArtifactType.entity_sample || artifactType == ArtifactType.entity_sample_property || artifactType == ArtifactType.task)
            sb.append("join da_" + tenant + ".entity_to_system on entity_to_system.entity_id = entity.id "
                    + "join da_" + tenant + ".system on system.id = entity_to_system.system_id and system.state='PUBLISHED' ");
        if (artifactType == ArtifactType.system || artifactType == ArtifactType.entity ||
                artifactType == ArtifactType.entity_query || artifactType == ArtifactType.system_connection ||
                artifactType == ArtifactType.entity_sample || artifactType == ArtifactType.entity_sample_property || artifactType == ArtifactType.task)
            sb.append("join da_" + tenant + ".system_to_domain on system_to_domain.system_id = system.id "
                    + "join da_" + tenant + ".domain on system_to_domain.domain_id = domain.id and domain.state='PUBLISHED' ");
        if (artifactType == ArtifactType.domain || artifactType == ArtifactType.system || artifactType == ArtifactType.system_connection ||
                artifactType == ArtifactType.entity || artifactType == ArtifactType.entity_query ||
                artifactType == ArtifactType.entity_sample || artifactType == ArtifactType.entity_sample_property ||
                artifactType == ArtifactType.data_asset || artifactType == ArtifactType.task || artifactType == ArtifactType.product
                || artifactType == ArtifactType.business_entity || artifactType == ArtifactType.indicator
        )
            sb.append("join da_" + tenant + ".steward_to_domain on steward_to_domain.domain_id = domain.id "
                    + "join da_" + tenant + ".steward on steward_to_domain.steward_id = steward.id and steward.id = "
                    + "'" + userDetails.getStewardId() + "' ");
        log.info("FINAL JOIN QUERY: " + sb.toString());
        return sb.toString();
    }

}
