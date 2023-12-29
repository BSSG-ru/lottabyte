package ru.bssg.lottabyte.core.model.dataentity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.FlatRelation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=false)
public class FlatDataEntity extends FlatModeledObject {
    private String entityFolderId;
    private List<FlatRelation> systems;
    private List<FlatRelation> domains;
    private List<String> tags;
    private ArtifactState state;
    private String workflowState;
    private String workflowTaskId;
    private String businessEntityId;

    /*public FlatDataEntity(DataEntity de) {
        super(de.getFlatModeledObject());
        this.entityFolderId = de.getEntity().getEntityFolderId();
        this.systemIds = de.getEntity().getSystemIds();
        this.tags = this.getEmptyListIfNull(de.getMetadata().getTags()).stream()
                .map(x -> x.getName()).collect(Collectors.toList());
        this.domainIds = de.getDomainIds();
    }*/

    private static <T2> List<T2> getEmptyListIfNull(List<T2> list) {
        return (List)(list == null ? new ArrayList() : list);
    }
}
