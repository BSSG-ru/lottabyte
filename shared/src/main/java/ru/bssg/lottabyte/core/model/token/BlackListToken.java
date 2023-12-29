package ru.bssg.lottabyte.core.model.token;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class BlackListToken extends ModeledObject<BlackListTokenEntity> {

    public BlackListToken() throws LottabyteException {
    }

    public BlackListToken(BlackListTokenEntity entity) throws LottabyteException {
        super(entity);
    }

    public BlackListToken(BlackListTokenEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.black_list_token);
    }

}
