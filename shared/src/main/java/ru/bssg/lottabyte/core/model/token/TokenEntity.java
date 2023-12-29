package ru.bssg.lottabyte.core.model.token;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper=false)
public class TokenEntity extends Entity {
    private LocalDateTime validTill;
    private String tenant;

    public TokenEntity() {
        super(ArtifactType.token);
    }

}
