package ru.bssg.lottabyte.core.model.etl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.IFlatModeledObjectWithTags;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper = true)
public class FlatETL extends FlatModeledObject implements IFlatModeledObjectWithTags {
    private String etlTypeId;
    private String etlTypeName;
    private String systemId;
    private String systemName;
    private ArtifactState state;
    private String workflowState;
    private String workflowTaskId;
    private List<String> tags;
    private String code;
    private String algorithm;

    public FlatETL(ETL etl) {
        super(etl.getFlatModeledObject());
        this.etlTypeId = etl.getEntity().getEtlTypeId();
        this.systemId = etl.getEntity().getSystemId();
        this.code = etl.getEntity().getCode();
        this.algorithm = etl.getEntity().getAlgorithm();
    }
}
