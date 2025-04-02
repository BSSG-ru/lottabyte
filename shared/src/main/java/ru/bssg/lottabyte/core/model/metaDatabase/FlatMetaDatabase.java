package ru.bssg.lottabyte.core.model.metaDatabase;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.relation.Relation;
import ru.bssg.lottabyte.core.model.tag.Tag;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper = true)
public class FlatMetaDatabase extends FlatModeledObject {
    private String driverClassName;
    private String jdbcUrl;
    private UUID systemId;
    private String state;
    private List<Relation> tasks;
}
