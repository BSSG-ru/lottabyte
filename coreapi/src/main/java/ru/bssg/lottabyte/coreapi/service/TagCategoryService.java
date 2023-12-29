package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.tag.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.TagCategoryRepository;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class TagCategoryService {
    private final TagCategoryRepository tagCategoryRepository;
    private final ElasticsearchService elasticsearchService;

    public TagCategory getTagCategoryById(String tagCategoryId, UserDetails userDetails) {
        return tagCategoryRepository.getTagCategoryById(tagCategoryId, userDetails);
    }

    public void deleteTagCategoryById(String tagCategoryId, UserDetails userDetails) throws LottabyteException {
        if (!tagCategoryRepository.existsById(tagCategoryId, userDetails))
            throw new LottabyteException(Message.LBE01004, userDetails.getLanguage(), tagCategoryId);
        if (tagCategoryRepository.categoryHasTags(tagCategoryId, userDetails))
            throw new LottabyteException(Message.LBE01006, userDetails.getLanguage(), tagCategoryId);
        tagCategoryRepository.deleteById(tagCategoryId, userDetails);
        elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(tagCategoryId), userDetails);
    }

    public TagCategory createTagCategory(TagCategoryEntity tagCategoryEntity, UserDetails userDetails) throws LottabyteException {
        if (tagCategoryEntity.getName() == null || tagCategoryEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE01007, userDetails.getLanguage());
        if (tagCategoryRepository.categoryNameExists(tagCategoryEntity.getName(), userDetails))
            throw new LottabyteException(Message.LBE01008, userDetails.getLanguage(), tagCategoryEntity.getName());
        TagCategory tagCategory = tagCategoryRepository.createTagCategory(tagCategoryEntity, userDetails);
        //elasticsearchService.insertElasticSearchEntity(Collections.singletonList(tagCategory.getSearchableArtifact()), userDetails);
        return tagCategoryRepository.getTagCategoryById(tagCategory.getId(), userDetails);
    }

    public TagCategory updateTagCategory(String tagCategoryId, UpdatableTagCategoryEntity tagCategoryEntity, UserDetails userDetails) throws LottabyteException {
        if (!tagCategoryRepository.existsById(tagCategoryId, userDetails))
            throw new LottabyteException(Message.LBE01004, userDetails.getLanguage(), tagCategoryId);
        if (tagCategoryEntity.getName() != null && tagCategoryRepository.categoryNameExists(tagCategoryEntity.getName(), tagCategoryId, userDetails))
            throw new LottabyteException(Message.LBE01008, userDetails.getLanguage(), tagCategoryEntity.getName());
        TagCategory tagCategory = tagCategoryRepository.updateTagCategory(tagCategoryId, tagCategoryEntity, userDetails);
        //elasticsearchService.updateElasticSearchEntity(Collections.singletonList(tagCategory.getSearchableArtifact()), userDetails);
        return tagCategoryRepository.getTagCategoryById(tagCategory.getId(), userDetails);
    }

    public PaginatedArtifactList<TagCategory> getTagCategoriesPaginated(Integer offset, Integer limit, UserDetails userDetails) {
        return tagCategoryRepository.getAllPaginated(offset, limit, "/v1/tags/category", userDetails);
    }
}
