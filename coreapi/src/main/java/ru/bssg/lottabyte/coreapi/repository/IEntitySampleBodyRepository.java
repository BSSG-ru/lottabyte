package ru.bssg.lottabyte.coreapi.repository;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.entitySample.UpdatableEntitySampleEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

public interface IEntitySampleBodyRepository {
    String getEntitySampleBodyById(String entityId, UserDetails userDetails) throws LottabyteException;
    void createEntitySampleBody(String customAttributeDefElementId, String sampleBody, UserDetails userDetails) throws LottabyteException;
    void deleteEntitySampleBodyById(String sampleBodyId, UserDetails userDetails) throws LottabyteException;
}
