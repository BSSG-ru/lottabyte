package ru.bssg.lottabyte.core.model.token;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper=false)
public class BlackListTokenEntity extends Entity {
    private String apiTokenId;
    private String cause;

    public BlackListTokenEntity() {
        super(ArtifactType.black_list_token);
    }

}
