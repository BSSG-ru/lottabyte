package ru.bssg.lottabyte.core.model.qualityTask;

import java.sql.Timestamp;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

@Data
@EqualsAndHashCode(callSuper=false)
public class QualityTaskEntity extends Entity {

    private String runId;
    private String parentRunId;
    private String eventType;
    private Timestamp eventTime;
    private String fullName;
    private String systemProducer;
    private String inputName;
    private String inputAssetName;
    private String inputId;
    private String inputSystemId;
    private String inputSystemName;
    private String outputName;
    private String outputAssetName;
    private String outputId;
    private String outputSystemId;
    private String outputSystemName;
    private String stateName;
    private String assertionMsg;
    private String state;
    private String stateNameLocal;
    private String stateLocal;
    private String producer;
    private String outputAssetDomainName;
    private String inputAssetDomainName;
    private String outputAssetDomainId;
    private String inputAssetDomainId;
    private String outputAssetId;
    private String inputAssetId;

    public QualityTaskEntity() {
        super(ArtifactType.qualityTask);
    }

}
