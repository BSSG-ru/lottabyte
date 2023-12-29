package ru.bssg.lottabyte.usermanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.externalGroup.SearchableExternalGroup;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchColumnForJoin;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.group.ExternalGroup;
import ru.bssg.lottabyte.core.usermanagement.model.group.UpdatableExternalGroupEntity;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.usermanagement.repository.ExternalGroupRepository;
import ru.bssg.lottabyte.usermanagement.repository.TenantRepostiory;
import ru.bssg.lottabyte.usermanagement.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExternalGroupService {
    private final ExternalGroupRepository externalGroupRepository;

    @Autowired
    public ExternalGroupService(ExternalGroupRepository externalGroupRepository) {
        this.externalGroupRepository = externalGroupRepository;
    }
    public ExternalGroup getExternalGroupById(String groupId, UserDetails userDetails) {
        return externalGroupRepository.getExternalGroupById(groupId, userDetails);
    }

    public ExternalGroup createExternalGroup(UpdatableExternalGroupEntity updatableExternalGroupEntity, UserDetails userDetails) {
        Integer id = externalGroupRepository.createExternalGroup(updatableExternalGroupEntity, userDetails);
        return getExternalGroupById(String.valueOf(id), userDetails);
    }

    public ExternalGroup updateExternalGroup(String groupId, UpdatableExternalGroupEntity updatableExternalGroupEntity, UserDetails userDetails) {
        externalGroupRepository.updateExternalGroup(groupId, updatableExternalGroupEntity);
        return getExternalGroupById(groupId, userDetails);
    }
    public ArchiveResponse deleteExternalGroup(String groupId, UserDetails userDetails) {
        externalGroupRepository.deleteExternalGroup(groupId, userDetails);
        ArchiveResponse archiveResponse = new ArchiveResponse();
        archiveResponse.setArchivedGuids(Collections.singletonList(groupId));
        return archiveResponse;
    }

    public SearchableExternalGroup getSearchableArtifact(ExternalGroup externalGroup, UserDetails userDetails) {
        SearchableExternalGroup searchableExternalGroup = new SearchableExternalGroup();
        searchableExternalGroup.setId(externalGroup.getMetadata().getId());
        searchableExternalGroup.setVersionId(externalGroup.getMetadata().getVersionId());
        searchableExternalGroup.setName(externalGroup.getMetadata().getName());
        searchableExternalGroup.setDescription(externalGroup.getEntity().getDescription());
        searchableExternalGroup.setModifiedBy(externalGroup.getMetadata().getModifiedBy());
        searchableExternalGroup.setModifiedAt(externalGroup.getMetadata().getModifiedAt());
        searchableExternalGroup.setArtifactType(externalGroup.getMetadata().getArtifactType());
        searchableExternalGroup.setEffectiveStartDate(externalGroup.getMetadata().getEffectiveStartDate());
        searchableExternalGroup.setEffectiveEndDate(externalGroup.getMetadata().getEffectiveEndDate());
        if (externalGroup.getMetadata().getTags() == null)
            searchableExternalGroup.setTags(new ArrayList<>());
        else
            searchableExternalGroup.setTags((externalGroup.getMetadata().getTags()).stream()
                .map(x -> x.getName()).collect(Collectors.toList()));

        searchableExternalGroup.setPermissions(externalGroup.getEntity().getPermissions());
        searchableExternalGroup.setAttributes(externalGroup.getEntity().getAttributes());
        searchableExternalGroup.setTenant(externalGroup.getEntity().getTenant());
        searchableExternalGroup.setUserRoles(externalGroup.getEntity().getUserRoles());
        return searchableExternalGroup;
    }
}
