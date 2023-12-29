package ru.bssg.lottabyte.coreapi.service.flowable;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.service.*;

@Service("RemoveArtifactService")
@Slf4j
public class RemoveArtifactService implements JavaDelegate {

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
        log.info("RemoveArtifactService called");
        String artifactId = (String) execution.getVariable("artifact_id");
        log.info("Artifact id is " + artifactId);
        ArtifactType artifactType = ArtifactType.fromString((String) execution.getVariable("artifact_type"));
        ArtifactType[] entities = { ArtifactType.domain, ArtifactType.entity, ArtifactType.business_entity,
                ArtifactType.product, ArtifactType.indicator, ArtifactType.data_asset, ArtifactType.system,
                ArtifactType.entity_query, ArtifactType.dq_rule };
        if (Arrays.asList(entities).contains(artifactType)) {
            UserDetails ud = new UserDetails();
            ud.setUid((String) execution.getVariable("ud_uid"));
            ud.setTenant((String) execution.getVariable("ud_tenant"));
            ud.setStewardId((String) execution.getVariable("ud_stewardid"));
            try {
                switch (artifactType) {
                    case domain:
                        domainService.wfApproveRemoval(artifactId, ud);
                        break;
                    case entity:
                        entityService.wfApproveRemoval(artifactId, ud);
                        break;
                    case business_entity:
                        businessEntityService.wfApproveRemoval(artifactId, ud);
                        break;
                    case product:
                        productService.wfApproveRemoval(artifactId, ud);
                        break;
                    case indicator:
                        indicatorService.wfApproveRemoval(artifactId, ud);
                        break;
                    case data_asset:
                        dataAssetService.wfApproveRemoval(artifactId, ud);
                        break;
                    case system:
                        systemService.wfApproveRemoval(artifactId, ud);
                        break;
                    case entity_query:
                        entityQueryService.wfApproveRemoval(artifactId, ud);
                        break;
                    case dq_rule:
                        dqRuleService.wfApproveRemoval(artifactId, ud);
                        break;
                }

            } catch (Exception e) {
                throw new BpmnError(e.getMessage());
            }
        } else {
            throw new BpmnError("Artifact type remove not implemented");
        }
    }
}
