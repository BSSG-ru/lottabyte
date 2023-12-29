package ru.bssg.lottabyte.coreapi.service.flowable;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.businessEntity.BusinessEntity;
import ru.bssg.lottabyte.core.model.dataasset.DataAsset;
import ru.bssg.lottabyte.core.model.dataentity.DataEntity;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.dqRule.DQRule;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;
import ru.bssg.lottabyte.core.model.indicator.Indicator;
import ru.bssg.lottabyte.core.model.product.Product;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.service.*;

@Service("PublishArtifactService")
@Slf4j
public class PublishArtifactService implements JavaDelegate {

    @Autowired
    private DomainService domainService;
    @Autowired
    private EntityService entityService;
    @Autowired
    private BusinessEntityService businessEntityService;
    @Autowired
    private ProductService productService;
    @Autowired
    private IndicatorService indicatorService;
    @Autowired
    private DataAssetService dataAssetService;
    @Autowired
    private SystemService systemService;
    @Autowired
    private EntityQueryService entityQueryService;
    @Autowired
    private DQRuleService dqRuleService;

    public void execute(DelegateExecution execution) {
        log.info("PublishArtifactService called");
        String artifactId = (String) execution.getVariable("artifact_id");
        log.info("Artifact id is " + artifactId);
        ArtifactType artifactType = ArtifactType.fromString((String) execution.getVariable("artifact_type"));
        ArtifactType[] entities = { ArtifactType.domain, ArtifactType.entity, ArtifactType.business_entity,
                ArtifactType.product, ArtifactType.indicator, ArtifactType.data_asset, ArtifactType.system,
                ArtifactType.entity_query, ArtifactType.dq_rule};
        if (Arrays.asList(entities).contains(artifactType)) {
            UserDetails ud = new UserDetails();
            ud.setUid((String) execution.getVariable("ud_uid"));
            ud.setTenant((String) execution.getVariable("ud_tenant"));
            ud.setStewardId((String) execution.getVariable("ud_stewardid"));
            try {
                switch (artifactType) {
                    case domain:
                        Domain d = domainService.wfPublish(artifactId, ud);
                        execution.setVariable("result_artifact_id", d.getMetadata().getId());
                        break;
                    case entity:
                        DataEntity e = entityService.wfPublish(artifactId, ud);
                        execution.setVariable("result_artifact_id", e.getMetadata().getId());
                        break;
                    case business_entity:
                        BusinessEntity b = businessEntityService.wfPublish(artifactId, ud);
                        execution.setVariable("result_artifact_id", b.getMetadata().getId());
                        break;
                    case product:
                        Product p = productService.wfPublish(artifactId, ud);
                        execution.setVariable("result_artifact_id", p.getMetadata().getId());
                        break;
                    case indicator:
                        Indicator i = indicatorService.wfPublish(artifactId, ud);
                        execution.setVariable("result_artifact_id", i.getMetadata().getId());
                        break;
                    case data_asset:
                        DataAsset da = dataAssetService.wfPublish(artifactId, ud);
                        execution.setVariable("result_artifact_id", da.getMetadata().getId());
                        break;
                    case system:
                        System s = systemService.wfPublish(artifactId, ud);
                        execution.setVariable("result_artifact_id", s.getMetadata().getId());
                        break;
                    case entity_query:
                        EntityQuery eq = entityQueryService.wfPublish(artifactId, ud);
                        execution.setVariable("result_artifact_id", eq.getMetadata().getId());
                        break;
                    case dq_rule:
                        DQRule dqr = dqRuleService.wfPublish(artifactId, ud);
                        execution.setVariable("result_artifact_id", dqr.getMetadata().getId());
                        break;
                }

            } catch (Exception e) {
                throw new BpmnError(e.getMessage());
            }
        } else {
            throw new BpmnError("Artifact type publish not implemented");
        }
    }

}
