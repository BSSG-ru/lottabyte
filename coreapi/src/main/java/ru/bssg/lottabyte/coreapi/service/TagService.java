package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.tag.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.TagCategoryRepository;
import ru.bssg.lottabyte.coreapi.repository.TagRepository;
import ru.bssg.lottabyte.coreapi.util.Helper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    private final TagCategoryRepository tagCategoryRepository;
    private final ElasticsearchService elasticsearchService;
    private final String defaultTagCategoryId = "d7cf3076-4cb4-4b74-8d69-938a70018db3";

    public Tag getTagById(String tagId, UserDetails userDetails) throws LottabyteException {
        Tag tag = tagRepository.getById(tagId, userDetails);
        if (tag == null)
            throw new LottabyteException(Message.LBE01001, userDetails.getLanguage(), tagId);
        return tag;
    }

    public void deleteAllTagsByArtifactId(String artifactId, UserDetails userDetails) {
        tagRepository.deleteAllTagsByArtifactId(artifactId, userDetails);
    }

    public void deleteTagById(String tagId, UserDetails userDetails) throws LottabyteException {
        if (!tagRepository.existsById(tagId, userDetails))
            throw new LottabyteException(Message.LBE01001, userDetails.getLanguage(), tagId);
        tagRepository.deleteById(tagId, userDetails);
        elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(tagId), userDetails);
    }

    public Tag createTag(TagEntity tagEntity, UserDetails userDetails) throws LottabyteException {
        if (tagEntity.getTagCategoryId() == null || tagEntity.getTagCategoryId().isEmpty())
            throw new LottabyteException(Message.LBE01002, userDetails.getLanguage());
        if (tagEntity.getName() == null || tagEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE01003, userDetails.getLanguage());
        if (!tagCategoryRepository.existsById(tagEntity.getTagCategoryId(), userDetails))
            throw new LottabyteException(Message.LBE01004, userDetails.getLanguage(), tagEntity.getTagCategoryId());
        if (tagRepository.tagNameExists(tagEntity.getName(), tagEntity.getTagCategoryId(), userDetails))
            throw new LottabyteException(Message.LBE01005, userDetails.getLanguage(),tagEntity.getName());

        Tag tag = tagRepository.createTag(tagEntity, userDetails);
        elasticsearchService.insertElasticSearchEntity(Collections.singletonList(getSearchableArtifact(tag, userDetails)), userDetails);
        return getTagById(tag.getId(), userDetails);
    }

    public Tag updateTag(String tagId, UpdatableTagEntity tagEntity, UserDetails userDetails) throws LottabyteException {
        if (!tagRepository.existsById(tagId, userDetails))
            throw new LottabyteException(Message.LBE01001, userDetails.getLanguage(), tagId);
        if (tagEntity.getTagCategoryId() != null && !tagCategoryRepository.existsById(tagEntity.getTagCategoryId(), userDetails))
            throw new LottabyteException(Message.LBE01004, userDetails.getLanguage(), tagEntity.getTagCategoryId());

        String catId = tagEntity.getTagCategoryId();
        if (catId == null) {
            Tag t = getTagById(tagId, userDetails);
            if (t != null)
                catId = t.getEntity().getTagCategoryId();
        }

        if (catId != null && tagEntity.getName() != null && tagRepository.tagNameExists(tagEntity.getName(), catId, tagId, userDetails))
            throw new LottabyteException(Message.LBE01005, userDetails.getLanguage(), tagEntity.getName());

        Tag tag = tagRepository.updateTag(tagId, tagEntity, userDetails);
        elasticsearchService.updateElasticSearchEntity(Collections.singletonList(getSearchableArtifact(tag, userDetails)), userDetails);
        return getTagById(tag.getId(), userDetails);
    }

    public PaginatedArtifactList<Tag> getTagsPaginated(Integer offset, Integer limit, UserDetails userDetails) {
        return tagRepository.getAllPaginated(offset, limit, "/v1/tags/", userDetails);
    }

    public PaginatedArtifactList<Tag> getArtifactTagsPaginated(String artifactId, Integer offset, Integer limit, UserDetails userDetails) {
        return tagRepository.getArtifactTagsPaginated(artifactId, offset, limit, userDetails);
    }

    public List<Tag> getArtifactTags(String artifactId, UserDetails userDetails) {
        return tagRepository.getArtifactTags(artifactId, userDetails);
    }

    public List<Tag> getArtifactTags(String artifactId, UserDetails userDetails, LocalDateTime dateFrom, LocalDateTime dateTo) {
        return tagRepository.getArtifactTags(artifactId, userDetails, dateFrom, dateTo);
    }

    public void linkToArtifact(String artifactId, String artifactType, TagEntity tagEntity, UserDetails userDetails) throws LottabyteException {

        if (tagEntity == null)
            throw new LottabyteException(Message.LBE01009, userDetails.getLanguage());
        if (StringUtils.isEmpty(tagEntity.getName()))
            throw new LottabyteException(Message.LBE01003, userDetails.getLanguage());
        Tag tag = tagRepository.getTagByName(tagEntity.getName(), defaultTagCategoryId, userDetails);
        if (tag == null) {
            TagEntity newTagEntity = new TagEntity();
            newTagEntity.setTagCategoryId(defaultTagCategoryId);
            newTagEntity.setName(tagEntity.getName());
            tag = tagRepository.createTag(newTagEntity, userDetails);
        }
        if (tag != null) {
            // TODO fix error code, invalid
            if (tagRepository.tagIsLinkedToArtifact(tag.getId(), artifactId, userDetails))
                throw new LottabyteException(Message.LBE01401, userDetails.getLanguage(), tag.getName(), artifactId);
            tagRepository.linkTagToArtifact(tag.getId(), artifactId, artifactType, userDetails);
        }
    }

    public void unlinkFromArtifact(String artifactId, String artifactType, TagEntity tagEntity, UserDetails userDetails) throws LottabyteException {
        if (tagEntity == null)
            throw new LottabyteException(Message.LBE01009, userDetails.getLanguage());
        if (StringUtils.isEmpty(tagEntity.getName()))
            throw new LottabyteException(Message.LBE01003, userDetails.getLanguage());
        Tag tag = tagRepository.getTagByName(tagEntity.getName(), defaultTagCategoryId, userDetails);
        if (tag == null) {
            throw new LottabyteException(Message.LBE01010, userDetails.getLanguage(), tagEntity.getName());
        }
        if (!tagRepository.tagIsLinkedToArtifact(tag.getId(), artifactId, userDetails))
            throw new LottabyteException(Message.LBE01302, userDetails.getLanguage(), tag.getName(), artifactId);
        tagRepository.unlinkTagFromArtifact(tag.getId(), artifactId, artifactType, userDetails);
    }

    public void mergeTags(String fromId, ArtifactType fromType, String toId, ArtifactType toType, UserDetails userDetails) {
        List<Tag> fromTags = tagRepository.getArtifactTags(fromId, userDetails);
        List<Tag> toTags = tagRepository.getArtifactTags(toId, userDetails);
        fromTags.stream().filter(x -> toTags.stream().noneMatch(y -> y.getId().equals(x.getId())))
                .forEach(z -> tagRepository.linkTagToArtifact(z.getId(), toId, toType.getText(), userDetails));
        toTags.stream().filter(x -> fromTags.stream().noneMatch(y -> y.getId().equals(x.getId())))
                .forEach(z -> tagRepository.unlinkTagFromArtifact(z.getId(), toId, toType.getText(), userDetails));
    }

    public List<FlatTag> searchTags(String query, Integer offset, Integer limit, UserDetails userDetails) {
        return tagRepository.searchTags(query, offset, limit, userDetails);
    }

    public SearchableTag getSearchableArtifact(Tag tag, UserDetails userDetails) {
        SearchableTag sa = SearchableTag.builder()
            .id(tag.getMetadata().getId())
            .versionId(tag.getMetadata().getVersionId())
            .name(tag.getMetadata().getName())
            .description(tag.getEntity().getDescription())
            .modifiedBy(tag.getMetadata().getModifiedBy())
            .modifiedAt(tag.getMetadata().getModifiedAt())
            .artifactType(tag.getMetadata().getArtifactType())
            .effectiveStartDate(tag.getMetadata().getEffectiveStartDate())
            .effectiveEndDate(tag.getMetadata().getEffectiveEndDate())
            .tags(Helper.getEmptyListIfNull(tag.getMetadata().getTags()).stream()
                    .map(x -> x.getName()).collect(Collectors.toList()))

            .tagCategoryId(tag.getEntity().getTagCategoryId()).build();
        return sa;
    }
}
