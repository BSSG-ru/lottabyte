package ru.bssg.lottabyte.coreapi.service.flowable;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.usermanagement.model.Language;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.service.*;

import java.util.Arrays;

@Service("RestoreArtifactService")
@Slf4j
public class RestoreArtifactService implements JavaDelegate {
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
        log.info("RestoreArtifactService called");
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
            ud.setLanguage( (Language)execution.getVariable("ud_language"));

            try {
                switch (artifactType) {
                    case domain:
                        domainService.wfApproveRestore(artifactId, ud);
                        break;
                    case entity:
                        entityService.wfApproveRestore(artifactId, ud);
                        break;
                    case business_entity:
                        businessEntityService.wfApproveRestore(artifactId, ud);
                        break;
                    case product:
                        productService.wfApproveRestore(artifactId, ud);
                        break;
                    case indicator:
                        indicatorService.wfApproveRestore(artifactId, ud);
                        break;
                    case data_asset:
                        dataAssetService.wfApproveRestore(artifactId, ud);
                        break;
                    case system:
                        systemService.wfApproveRestore(artifactId, ud);
                        break;
                    case entity_query:
                        entityQueryService.wfApproveRestore(artifactId, ud);
                        break;
                    case dq_rule:
                        dqRuleService.wfApproveRestore(artifactId, ud);
                        break;
                }

            } catch (Exception e) {
                throw new BpmnError(e.getMessage());
            }
        } else {
            throw new BpmnError("Artifact type restore not implemented");
        }
    }
}
