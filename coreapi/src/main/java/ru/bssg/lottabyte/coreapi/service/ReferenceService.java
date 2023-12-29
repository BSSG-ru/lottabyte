package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.reference.Reference;
import ru.bssg.lottabyte.core.model.reference.ReferenceType;
import ru.bssg.lottabyte.core.model.reference.UpdatableReferenceEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.ArtifactRepository;
import ru.bssg.lottabyte.coreapi.repository.ReferenceRepository;

import java.sql.Ref;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReferenceService {

    private final ReferenceRepository referenceRepository;
    private final ArtifactRepository artifactRepository;

    public List<Reference> getAllByArtifactId(String artifactId, UserDetails userDetails) {
        return referenceRepository.getAllByArtifactId(artifactId, userDetails);
    }

    public Reference getReferenceById(String id, UserDetails userDetails) throws LottabyteException {
        Reference reference = referenceRepository.getById(id, userDetails);
        if (reference == null)
            throw new LottabyteException(Message.LBE02808, userDetails.getLanguage(), id);
        return reference;
    }
    public Reference getReferenceBySourceIdAndTargetId(String sourceId, String targetId, UserDetails userDetails) throws LottabyteException {
        Reference reference = referenceRepository.getReferenceBySourceIdAndTargetId(sourceId, targetId, userDetails);
        if (reference == null)
            throw new LottabyteException(Message.LBE02812, userDetails.getLanguage(), sourceId, targetId);
        return reference;
    }

    public List<Reference> getAllReferenceBySourceIdAndTargetType(String artifactId, String type, UserDetails userDetails) {
        return referenceRepository.getAllReferenceBySourceIdAndTargetType(artifactId, type, userDetails);
    }
    public List<Reference> getAllReferenceByPublishedIdAndTypeAndVersionId(String publishedId, Integer versionId, String type, UserDetails userDetails) {
        return referenceRepository.getAllReferenceByPublishedIdAndTypeAndVersionId(publishedId, versionId, type, userDetails);
    }
    public List<Reference> getAllReferencesByObjectId(String objectId, UserDetails userDetails) {
        return referenceRepository.getAllReferencesByObjectId(objectId, userDetails);
    }
    public List<Reference> getAllReferencesByTargetIdAndRefType(String targetId, ReferenceType referenceType, UserDetails userDetails) {
        return referenceRepository.getAllReferencesByTargetIdAndRefType(targetId, referenceType, userDetails);
    }
    public Integer getLastVersionByPublishedId(String publishedId, UserDetails userDetails) {
        return referenceRepository.getLastVersionByPublishedId(publishedId, userDetails);
    }

    public Reference createReference(UpdatableReferenceEntity newReferenceEntity, UserDetails userDetails) throws LottabyteException {
        if (newReferenceEntity.getSourceId() == null || newReferenceEntity.getSourceId().isEmpty())
            throw new LottabyteException(Message.LBE02801, userDetails.getLanguage());
        if (newReferenceEntity.getSourceType() == null)
            throw new LottabyteException(Message.LBE02802, userDetails.getLanguage());
        if (newReferenceEntity.getTargetId() == null)
            throw new LottabyteException(Message.LBE02803, userDetails.getLanguage());
        if (newReferenceEntity.getTargetId().isEmpty())
            return null;
        if (newReferenceEntity.getTargetType() == null)
            throw new LottabyteException(Message.LBE02804, userDetails.getLanguage());
        if (newReferenceEntity.getReferenceType() == null)
            throw new LottabyteException(Message.LBE02805, userDetails.getLanguage());
        if (newReferenceEntity.getSourceId().equals(newReferenceEntity.getTargetId()))
            throw new LottabyteException(Message.LBE02806, userDetails.getLanguage());
        if (!artifactRepository.existsArtifact(newReferenceEntity.getSourceId(), newReferenceEntity.getSourceType(), userDetails))
            throw new LottabyteException(Message.LBE02809, userDetails.getLanguage(), newReferenceEntity.getSourceId(), newReferenceEntity.getSourceType().getText());
        if (!artifactRepository.existsArtifact(newReferenceEntity.getTargetId(), newReferenceEntity.getTargetType(), userDetails))
            throw new LottabyteException(Message.LBE02810, userDetails.getLanguage(), newReferenceEntity.getSourceId(), newReferenceEntity.getSourceType().getText());
        if (referenceRepository.existsReference(newReferenceEntity.getSourceId(), newReferenceEntity.getTargetId(),
                newReferenceEntity.getReferenceType(), userDetails))
            throw new LottabyteException(Message.LBE02807, userDetails.getLanguage());
        String newId = referenceRepository.createReference(newReferenceEntity, userDetails);
        return getReferenceById(newId, userDetails);
    }

    public Reference patchReferenceBySourceIdAndTargetId(UpdatableReferenceEntity newReferenceEntity, UserDetails userDetails) throws LottabyteException {
        if (newReferenceEntity.getPublishedId() == null || newReferenceEntity.getPublishedId().isEmpty())
            throw new LottabyteException(Message.LBE02801, userDetails.getLanguage());
        if (getReferenceBySourceIdAndTargetId(newReferenceEntity.getSourceId(), newReferenceEntity.getTargetId(), userDetails) == null)
            throw new LottabyteException(Message.LBE02812, userDetails.getLanguage(), newReferenceEntity.getSourceId(), newReferenceEntity.getTargetId());
        referenceRepository.patchReferenceBySourceIdAndTargetId(newReferenceEntity, userDetails);
        return getReferenceBySourceIdAndTargetId(newReferenceEntity.getSourceId(), newReferenceEntity.getTargetId(), userDetails);
    }

    public void deleteReferenceById(String id, UserDetails userDetails) throws LottabyteException {
        if (referenceRepository.getById(id, userDetails) == null)
            throw new LottabyteException(Message.LBE02808, userDetails.getLanguage(), id);
        referenceRepository.deleteById(id, userDetails);
    }

    public void deleteReferenceBySourceId(String id, UserDetails userDetails) {
        if (getReferenceBySourceId(id, userDetails) != null)
            referenceRepository.deleteReferenceBySourceId(id, userDetails);
    }

    public void deleteAllByArtifactId(String artifactId, UserDetails userDetails) {
        referenceRepository.deleteAllByArtifactId(artifactId, userDetails);
    }
    public Reference getReferenceBySourceId(String id, UserDetails userDetails) {
        return referenceRepository.getReferenceBySourceId(id, userDetails);
    }
    public void deleteByReferenceSourceIdAndTargetId(String sourceId, String targetId, UserDetails userDetails) {
        referenceRepository.deleteByReferenceSourceIdAndTargetId(sourceId, targetId, userDetails);
    }
}
