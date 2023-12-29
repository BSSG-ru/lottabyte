package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.steward.FlatSteward;
import ru.bssg.lottabyte.core.model.steward.SearchableSteward;
import ru.bssg.lottabyte.core.model.steward.Steward;
import ru.bssg.lottabyte.core.model.steward.UpdatableStewardEntity;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.DomainRepository;
import ru.bssg.lottabyte.coreapi.repository.StewardRepository;
import ru.bssg.lottabyte.coreapi.util.Helper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StewardService {
    private final StewardRepository stewardRepository;
    private final DomainRepository domainRepository;
    private final ElasticsearchService elasticsearchService;

    private final SearchColumn[] searchableColumns = {
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("user_id", SearchColumn.ColumnType.Number),
            new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
            new SearchColumn("domains", SearchColumn.ColumnType.Text)
    };

    private final SearchColumnForJoin[] joinColumns = {};

    public boolean stewardExists(String stewardId, UserDetails userDetails) {
        return stewardRepository.existsById(stewardId, userDetails);
    }

    public Steward getStewardById(String stewardId, UserDetails userDetails) throws LottabyteException {
        if (!stewardRepository.existsById(stewardId, userDetails))
            throw new LottabyteException(Message.LBE00201, userDetails.getLanguage(), stewardId);
        return stewardRepository.getById(stewardId, userDetails);
    }

    public void addStewardToDomain(String stewardId, String domainId, Boolean validate, UserDetails userDetails) throws LottabyteException {
        if (validate) {

        }
        stewardRepository.addStewardToDomain(stewardId, domainId, userDetails);
    }

    public void removeStewardFromDomain(String stewardId, String domainId, Boolean validate, UserDetails userDetails)  throws LottabyteException {
        if (validate) {

        }
        stewardRepository.removeStewardFromDomain(stewardId, domainId, userDetails);
    }

    public void deleteStewardById(String stewardId, UserDetails userDetails) throws LottabyteException {
        if (!stewardRepository.existsById(stewardId, userDetails))
            throw new LottabyteException(Message.LBE00201, userDetails.getLanguage(), stewardId);
        stewardRepository.deleteById(stewardId, userDetails);
        elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(stewardId), userDetails);
    }

    public Steward createSteward(UpdatableStewardEntity stewardEntity, UserDetails userDetails) throws LottabyteException {
        if (stewardEntity.getUserId() == null)
            throw new LottabyteException(Message.LBE00202, userDetails.getLanguage());
        if (stewardRepository.userIdExists(stewardEntity.getUserId(), userDetails))
            throw new LottabyteException(Message.LBE00203, userDetails.getLanguage(), stewardEntity.getUserId());
        if (!domainRepository.allDomainsExist(stewardEntity.getDomains(), userDetails))
            throw new LottabyteException(Message.LBE00109, userDetails.getLanguage(), StringUtils.join(stewardEntity.getDomains(), ", "));
        Steward steward = stewardRepository.createSteward(stewardEntity, userDetails);
        elasticsearchService.insertElasticSearchEntity(Collections.singletonList(getSearchableArtifact(steward, userDetails)), userDetails);
        return getStewardById(steward.getId(), userDetails);
    }

    public Steward updateSteward(String stewardId, UpdatableStewardEntity stewardEntity, UserDetails userDetails) throws LottabyteException {
        if (!stewardRepository.existsById(stewardId, userDetails))
            throw new LottabyteException(Message.LBE00201, userDetails.getLanguage(), stewardId);
        if (stewardEntity.getUserId() != null && stewardRepository.userIdExists(stewardEntity.getUserId(), stewardId, userDetails))
            throw new LottabyteException(Message.LBE00203, userDetails.getLanguage(), stewardEntity.getUserId());
        if (stewardEntity.getDomains() != null && !stewardEntity.getDomains().isEmpty() && !domainRepository.allDomainsExist(stewardEntity.getDomains(), userDetails))
            throw new LottabyteException(Message.LBE00109, userDetails.getLanguage(), StringUtils.join(stewardEntity.getDomains(), ", "));

        Steward steward = stewardRepository.updateSteward(stewardId, stewardEntity, userDetails);
        elasticsearchService.updateElasticSearchEntity(Collections.singletonList(getSearchableArtifact(steward, userDetails)), userDetails);
        return getStewardById(stewardId, userDetails);
    }

    public PaginatedArtifactList<Steward> getStewardsPaginated(Integer offset, Integer limit, UserDetails userDetails) {
        return stewardRepository.getAllPaginated(offset, limit, userDetails);
    }

    public List<Steward> getStewardsByDomainId(String domainId, UserDetails userDetails) {
        return stewardRepository.getStewardsByDomainId(domainId, userDetails);
    }

    public SearchResponse<FlatSteward> searchStewards(SearchRequestWithJoin request, UserDetails userDetails) throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        return stewardRepository.searchStewards(request, searchableColumns, userDetails);
    }

    public SearchableSteward getSearchableArtifact(Steward steward, UserDetails userDetails) {
        SearchableSteward sa = SearchableSteward.builder()
            .id(steward.getMetadata().getId())
            .versionId(steward.getMetadata().getVersionId())
            .name(steward.getMetadata().getName())
            .description(steward.getEntity().getDescription())
            .modifiedBy(steward.getMetadata().getModifiedBy())
            .modifiedAt(steward.getMetadata().getModifiedAt())
            .artifactType(steward.getMetadata().getArtifactType())
            .effectiveStartDate(steward.getMetadata().getEffectiveStartDate())
            .effectiveEndDate(steward.getMetadata().getEffectiveEndDate())
            .tags(Helper.getEmptyListIfNull(steward.getMetadata().getTags()).stream()
                    .map(x -> x.getName()).collect(Collectors.toList()))

            .domains(steward.getEntity().getDomains())
            .userId(steward.getEntity().getUserId()).build();
        return sa;
    }
}
