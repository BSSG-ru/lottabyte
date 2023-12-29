package ru.bssg.lottabyte.core.model.enumeration;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper = true)
public class FlatEnumeration extends FlatModeledObject {
    private List<String> variants;

    public FlatEnumeration(Enumeration s) {
        super(s.getFlatModeledObject());
        this.variants = s.getEntity().getVariants();
    }
}
