package ru.bssg.lottabyte.core.model.qualityTask;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper=false)
public class FlatQualityTask extends FlatModeledObject {

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

    public FlatQualityTask(QualityTask d) {
        super(d.getFlatModeledObject());
        this.runId = d.getEntity().getRunId();
        this.parentRunId = d.getEntity().getParentRunId();
        this.eventType = d.getEntity().getEventType();
        this.eventTime = d.getEntity().getEventTime();
        this.fullName = d.getEntity().getFullName();
        this.systemProducer = d.getEntity().getSystemProducer();
        this.inputName = d.getEntity().getInputName();
        this.inputAssetName = d.getEntity().getInputAssetName();
        this.inputId = d.getEntity().getInputId();
        this.inputSystemId = d.getEntity().getInputSystemId();
        this.inputSystemName = d.getEntity().getInputSystemName();
        this.outputName = d.getEntity().getOutputName();
        this.outputAssetName = d.getEntity().getOutputAssetName();
        this.outputId = d.getEntity().getOutputId();
        this.outputSystemId = d.getEntity().getOutputSystemId();
        this.outputSystemName = d.getEntity().getOutputSystemName();
        this.stateName = d.getEntity().getStateName();
        this.assertionMsg = d.getEntity().getAssertionMsg();
        this.state = d.getEntity().getState();
        this.producer = d.getEntity().getProducer();
        this.outputAssetDomainName = d.getEntity().getOutputAssetDomainName();
        this.inputAssetDomainName = d.getEntity().getOutputAssetDomainName();
        this.outputAssetDomainId = d.getEntity().getOutputAssetDomainId();
        this.inputAssetDomainId = d.getEntity().getInputAssetDomainId();
        this.outputAssetId = d.getEntity().getOutputAssetId();
        this.inputAssetId = d.getEntity().getInputAssetId();
        this.stateNameLocal = d.getEntity().getStateNameLocal();
        this.stateLocal = d.getEntity().getStateLocal();

    }
}
