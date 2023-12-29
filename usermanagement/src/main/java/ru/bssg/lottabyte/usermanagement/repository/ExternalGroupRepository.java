package ru.bssg.lottabyte.usermanagement.repository;

import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.model.group.ExternalGroup;
import ru.bssg.lottabyte.core.usermanagement.model.group.UpdatableExternalGroupEntity;

public interface ExternalGroupRepository {
    void updateExternalGroup(String groupId, UpdatableExternalGroupEntity updatableExternalGroupEntity);
    ExternalGroup getExternalGroupById(String groupId, UserDetails userDetails);
    void deleteExternalGroup(String groupId, UserDetails userDetails);
    Integer createExternalGroup(UpdatableExternalGroupEntity updatableExternalGroupEntity, UserDetails userDetails);
}
