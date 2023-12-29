package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.recentView.RecentView;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.ArtifactRepository;
import ru.bssg.lottabyte.coreapi.repository.RecentViewRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecentViewService {
    private final RecentViewRepository recentViewRepository;
    private final ArtifactRepository artifactRepository;
    private final WorkflowService workflowService;
    private final int maxRecentByType = 5;

    public RecentView changeRecentView(String artifactId, String artifactType, UserDetails userDetails) throws LottabyteException {
        if (!EnumUtils.isValidEnum(ArtifactType.class, artifactType))
            throw new LottabyteException(Message.LBE00044, userDetails.getLanguage(), artifactType);
        if (!recentViewRepository.existsObjectByIdAndArtifactType(artifactId, artifactType, userDetails)) {
            throw new LottabyteException(Message.LBE01704, userDetails.getLanguage(), artifactId, artifactType);
        }
        RecentView currentRecentView = recentViewRepository.getRecentViewByArtifactIdAndType(artifactId, artifactType, userDetails);
        if (currentRecentView != null) {
            recentViewRepository.patchRecentView(artifactId, artifactType, userDetails);
        } else {
            Integer recentCountByType = recentViewRepository.getRecentCountByType(artifactType, userDetails);
            if (recentCountByType >= maxRecentByType) {
                recentViewRepository.deleteOldestRecentViewByType(artifactType, maxRecentByType - 1, userDetails);
            }
            recentViewRepository.createRecentView(artifactId, artifactType, userDetails);
        }
        return recentViewRepository.getRecentViewByArtifactIdAndType(artifactId, artifactType, userDetails);
    }

    public List<RecentView> getRecentViews(String artifactType, UserDetails userDetails) throws LottabyteException {
        if (artifactType != null && !EnumUtils.isValidEnum(ArtifactType.class, artifactType))
            throw new LottabyteException(Message.LBE00044, userDetails.getLanguage(), artifactType);
        List<RecentView> res = recentViewRepository.getRecentViews(artifactType, maxRecentByType, userDetails);
        res.stream().forEach(x -> x.setName(artifactRepository.getArtifactName(ArtifactType.fromString(x.getArtifactType()), x.getArtifactId(), userDetails)));
        res = res.stream().filter(x -> x.getName() != null).collect(Collectors.toList());
        return res;
    }
    public String getArtifactState(String artifactType, String artifactId, UserDetails userDetails) {
        if (workflowService.isWorkflowEnabled(ArtifactType.valueOf(artifactType)))
            return artifactRepository.getArtifactState(artifactType, artifactId, userDetails);
        return "PUBLISHED";
    }
}
