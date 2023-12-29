package ru.bssg.lottabyte.core.model.dataasset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.ca.CustomAttribute;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=false)
public class FlatDataAsset extends FlatModeledObject {
    private String systemId;
    private String domainId;
    private String entityId;
    private String systemName;
    private String domainName;
    private String entityName;
    private Integer rowsCount;
    private Integer dataSize;
    private Boolean hasQuery;
    private Boolean hasSample;
    private Boolean hasStatistics;
    private List<CustomAttribute> customAttributes;
    private ArtifactState state;
    private String workflowState;
    private String workflowTaskId;
    private List<String> tags;

    public FlatDataAsset(DataAsset da) {
        super(da.getFlatModeledObject());
        this.systemId = da.getEntity().getSystemId();
        this.domainId = da.getEntity().getDomainId();
        this.entityId = da.getEntity().getEntityId();
        this.rowsCount = da.getEntity().getRowsCount();
        this.dataSize = da.getEntity().getDataSize();
        this.hasQuery = da.getEntity().getHasQuery();
        this.hasSample = da.getEntity().getHasSample();
        this.hasStatistics = da.getEntity().getHasStatistics();
        this.customAttributes = da.getEntity().getCustomAttributes();
    }
}
