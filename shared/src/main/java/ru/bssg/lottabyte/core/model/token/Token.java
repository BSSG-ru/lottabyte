package ru.bssg.lottabyte.core.model.token;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class Token extends ModeledObject<TokenEntity> {

    public Token() throws LottabyteException {
    }

    public Token(TokenEntity entity) throws LottabyteException {
        super(entity);
    }

    public Token(TokenEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.token);
    }

}
