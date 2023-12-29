package ru.bssg.lottabyte.core.model.connector;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class ConnectorParamEntity extends Entity {

    private String connectorId;
    private Boolean required;
    private Integer showOrder;
    private String displayName;
    private String example;
    private ConnectorParamType paramType;
    private List<String> enumValues;

    public ConnectorParamEntity() {
        super(ArtifactType.connector_param);
    }

}
