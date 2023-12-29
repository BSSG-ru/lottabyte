package ru.bssg.lottabyte.core.model.datatype;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper = true)
public class FlatDataType extends FlatModeledObject {
    public FlatDataType(DataType s) {
        super(s.getFlatModeledObject());
    }
}
