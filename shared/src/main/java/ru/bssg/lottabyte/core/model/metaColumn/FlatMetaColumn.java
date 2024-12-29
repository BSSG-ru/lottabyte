package ru.bssg.lottabyte.core.model.metaColumn;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

import java.util.UUID;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper = true)
public class FlatMetaColumn extends FlatModeledObject {
    private UUID metaObjectId;
    private String metaObjectName;
    private String schemaName;
    private Boolean isKey;
    private String columnType;
    private UUID metaDatabaseId;
    private Integer versionId;
    private UUID entityId;
    private String entityAttributeName;
}
