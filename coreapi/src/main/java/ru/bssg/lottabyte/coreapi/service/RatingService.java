package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.rating.Rating;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.RatingRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final RecentViewService recentViewService;

    public Rating getArtifactRatingById(String artifactId, UserDetails userDetails) {
        if (artifactId == null || !ratingRepository.existsRatingByArtifactId(artifactId, userDetails)) {
            Rating zeroRate = new Rating();
            zeroRate.setTotalRates(0);
            zeroRate.setRating(0.0);
            zeroRate.setArtifactType("-");
            zeroRate.setArtifactId(artifactId);
            zeroRate.setArtifactName("-");
            return zeroRate;
        }
        return ratingRepository.getRatingByArtifactId(artifactId, userDetails);
    }

    public Rating rateArtifact(String artifactType, String artifactId, Integer rating, UserDetails userDetails) throws LottabyteException {
        ArtifactType artType = ratingRepository.getDataArtifactType(artifactType);
        if (artifactType == null || artType == null) {
            throw new LottabyteException(Message.LBE01703, userDetails.getLanguage(), artifactType);
        }
        if (rating > 5 || rating < 1) {
            throw new LottabyteException(Message.LBE02301, userDetails.getLanguage(), rating);
        }
        if (!ratingRepository.existsObjectByIdAndArtifactType(artifactId, artifactType, userDetails)) {
            throw new LottabyteException(Message.LBE00037, userDetails.getLanguage(), artifactId, artifactType);
        }
        if(!recentViewService.getArtifactState(artifactType, artifactId, userDetails).equals(ArtifactState.PUBLISHED.toString())){
            throw new LottabyteException(Message.LBE00069, userDetails.getLanguage(), artifactId, artifactType);
        }
        if (ratingRepository.existsRatingByArtifactIdAndUserId(artifactId, userDetails)) {
            ratingRepository.patchRating(artifactId, rating, userDetails);
        } else {
            ratingRepository.createRating(artifactType, artifactId, rating, userDetails);
        }
        return getArtifactRatingById(artifactId, userDetails);
    }

    public Rating removeArtifactRate(String artifactId, UserDetails userDetails) throws LottabyteException {
        /*if (artifactId == null || !ratingRepository.existsRatingByArtifactIdAndUserId(artifactId, userDetails)) {
            throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.format(Message.LBE00038.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), artifactId));
        }*/
        ratingRepository.deleteRating(artifactId, userDetails);
        return getArtifactRatingById(artifactId, userDetails);
    }

    public Integer getOwnArtifactRate(String artifactId, UserDetails userDetails) {
        if (artifactId == null || !ratingRepository.existsRatingByArtifactIdAndUserId(artifactId, userDetails)) {
            return 0;
        }

        return ratingRepository.getOwnRatingByArtifactId(artifactId, userDetails);
    }
}
