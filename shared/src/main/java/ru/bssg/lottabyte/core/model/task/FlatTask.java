package ru.bssg.lottabyte.core.model.task;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=false)
public class FlatTask extends FlatModeledObject {
    private String queryId;
    private String systemConnectionId;
    private String queryName;
    private String systemConnectionName;
    private String taskState;
    private LocalDateTime lastUpdated;

    public FlatTask(Task d) {
        super(d.getFlatModeledObject());
        this.queryId = d.getEntity().getQueryId();
        this.systemConnectionId = d.getEntity().getSystemConnectionId();
    }
}
