package ru.bssg.lottabyte.core.model.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=false)
public class FlatSystemConnection extends FlatModeledObject {

    private String connectorName;
    private String systemName;

    public FlatSystemConnection(SystemConnection d) {
        super(d.getFlatModeledObject());
    }
}