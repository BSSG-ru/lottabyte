package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.enumeration.Enumeration;
import ru.bssg.lottabyte.core.model.enumeration.FlatEnumeration;
import ru.bssg.lottabyte.core.model.enumeration.SearchableEnumeration;
import ru.bssg.lottabyte.core.model.enumeration.UpdatableEnumerationEntity;
import ru.bssg.lottabyte.core.model.tag.Tag;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchColumnForJoin;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.EnumerationRepository;
import ru.bssg.lottabyte.coreapi.util.Helper;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnumerationService {
    private final EnumerationRepository enumerationRepository;
    private final TagService tagService;
    private final SearchColumn[] searchableColumns = {
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
            new SearchColumn("variants", SearchColumn.ColumnType.Array)
    };

    private final SearchColumnForJoin[] joinColumns = {
            new SearchColumnForJoin("domain_id", "system_to_domain", SearchColumn.ColumnType.UUID, "id", "system_id")
    };

    public Enumeration getEnumerationById(String enumerationId, UserDetails userDetails) throws LottabyteException {
        Enumeration enumeration = enumerationRepository.getById(enumerationId, userDetails);
        if (enumeration == null)
            throw new LottabyteException(Message.LBE02601, userDetails.getLanguage(), enumerationId);

        return enumeration;
    }

    public PaginatedArtifactList<Enumeration> getEnumerationPaginated(Integer offset, Integer limit, UserDetails userDetails) {
        return enumerationRepository.getAllPaginated(offset, limit, "/v1/enumeration", userDetails);
    }

    public Enumeration createEnumeration(UpdatableEnumerationEntity newEnumerationEntity, UserDetails userDetails) throws LottabyteException {
        if (newEnumerationEntity.getName() == null || newEnumerationEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE02602, userDetails.getLanguage(), newEnumerationEntity.getName());
        if(newEnumerationEntity.getVariants() == null || newEnumerationEntity.getVariants().isEmpty())
            throw new LottabyteException(Message.LBE02604, userDetails.getLanguage());
        Set<String> set = new HashSet<>(newEnumerationEntity.getVariants());
        if(set.size() < newEnumerationEntity.getVariants().size())
            throw new LottabyteException(Message.LBE02603, userDetails.getLanguage(), newEnumerationEntity.getVariants());

        String enumerationId = enumerationRepository.createEnumeration(newEnumerationEntity, userDetails);
        return getEnumerationById(enumerationId, userDetails);
    }

    public Enumeration patchEnumeration(String enumerationId, UpdatableEnumerationEntity enumerationEntity, UserDetails userDetails) throws LottabyteException {
        if (enumerationId != null)
            getEnumerationById(enumerationId, userDetails);
        if (enumerationEntity.getName() != null && enumerationEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE02602, userDetails.getLanguage(), enumerationEntity.getName());
        if(enumerationEntity.getVariants() != null){
            if(enumerationEntity.getVariants().isEmpty())
                throw new LottabyteException(Message.LBE02604, userDetails.getLanguage());
            Set<String> set = new HashSet<>(enumerationEntity.getVariants());
            if(set.size() < enumerationEntity.getVariants().size())
                throw new LottabyteException(Message.LBE02603, userDetails.getLanguage(), enumerationEntity.getVariants());
        }

        enumerationRepository.patchEnumeration(enumerationId, enumerationEntity, userDetails);
        return getEnumerationById(enumerationId, userDetails);
    }

    public ArchiveResponse deleteEnumeration(String enumerationId, UserDetails userDetails) throws LottabyteException {
        if (enumerationId != null)
            getEnumerationById(enumerationId, userDetails);

        enumerationRepository.deleteById(enumerationId, userDetails);
        ArchiveResponse archiveResponse = new ArchiveResponse();
        archiveResponse.setArchivedGuids(Collections.singletonList(enumerationId));
        return archiveResponse;
    }

    public SearchResponse<FlatEnumeration> searchEnumeration(SearchRequestWithJoin request, UserDetails userDetails) throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        return enumerationRepository.searchEnumeration(request, searchableColumns, joinColumns, userDetails);
    }

    public PaginatedArtifactList<Enumeration> getEnumerationVersions(String enumerationId, Integer offset, Integer limit, UserDetails userDetails) throws LottabyteException {
        if (enumerationId != null)
            getEnumerationById(enumerationId, userDetails);

        PaginatedArtifactList<Enumeration> res = enumerationRepository.getEnumerationVersions(enumerationId, offset, limit, userDetails);
        List<Tag> tagList = tagService.getArtifactTags(enumerationId, userDetails);
        res.getResources().forEach(d -> d.getMetadata().setTags(tagList));
        return res;
    }

    public SearchableEnumeration getSearchableArtifact(Enumeration enumeration, UserDetails userDetails) {
        SearchableEnumeration searchableEnumeration = new SearchableEnumeration();
        searchableEnumeration.setId(enumeration.getMetadata().getId());
        searchableEnumeration.setVersionId(enumeration.getMetadata().getVersionId());
        searchableEnumeration.setName(enumeration.getMetadata().getName());
        searchableEnumeration.setDescription(enumeration.getEntity().getDescription());
        searchableEnumeration.setModifiedBy(enumeration.getMetadata().getModifiedBy());
        searchableEnumeration.setModifiedAt(enumeration.getMetadata().getModifiedAt());
        searchableEnumeration.setArtifactType(enumeration.getMetadata().getArtifactType());
        searchableEnumeration.setEffectiveStartDate(enumeration.getMetadata().getEffectiveStartDate());
        searchableEnumeration.setEffectiveEndDate(enumeration.getMetadata().getEffectiveEndDate());
        searchableEnumeration.setTags(Helper.getEmptyListIfNull(enumeration.getMetadata().getTags()).stream()
                .map(x -> x.getName()).collect(Collectors.toList()));

        searchableEnumeration.setVariants(enumeration.getEntity().getVariants());
        return searchableEnumeration;
    }
}
