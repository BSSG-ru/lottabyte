package ru.bssg.lottabyte.coreapi.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.domain.SearchableDomain;
import ru.bssg.lottabyte.core.model.metaColumn.FlatMetaColumn;
import ru.bssg.lottabyte.core.model.metaColumn.SearchableMetaColumn;
import ru.bssg.lottabyte.core.model.metaDatabase.FlatMetaDatabase;
import ru.bssg.lottabyte.core.model.metaDatabase.SearchableMetaDatabase;
import ru.bssg.lottabyte.core.model.metaObject.FlatMetaObject;
import ru.bssg.lottabyte.core.model.metaObject.SearchableMetaObject;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchColumnForJoin;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.MetaColumnRepository;
import ru.bssg.lottabyte.coreapi.repository.MetaDatabaseRepository;
import ru.bssg.lottabyte.coreapi.repository.MetaObjectRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class MetadataService {
    private final MetaDatabaseRepository metaDatabaseRepository;
    private final MetaObjectRepository metaObjectRepository;
    private final MetaColumnRepository metaColumnRepository;
    private final ElasticsearchService elasticsearchService;
    private final TagService tagService;
    private final UserFavService userFavService;

    public FlatMetaDatabase createMetaDatabase(FlatMetaDatabase fmb, UserDetails userDetails) {
        FlatMetaDatabase res = metaDatabaseRepository.createMetaDatabase(fmb, userDetails);
        elasticsearchService.insertElasticSearchEntity(Collections.singletonList(getSearchableArtifact(res, userDetails)), userDetails);
        return res;
    }

    public FlatMetaObject createMetaObject(FlatMetaObject fmo, UserDetails userDetails) {
        if (fmo.getId() == null)
            fmo.setId(UUID.randomUUID().toString());

        FlatMetaObject res = metaObjectRepository.createMetaObject(fmo, userDetails);
        elasticsearchService.insertElasticSearchEntity(Collections.singletonList(getSearchableArtifact(res, userDetails)), userDetails);

        return res;
    }

    public FlatMetaColumn createMetaColumn(FlatMetaColumn fmc, UserDetails userDetails) {
        if (fmc.getId() == null)
            fmc.setId(UUID.randomUUID().toString());

        FlatMetaColumn res = metaColumnRepository.createMetaColumn(fmc, userDetails);
        elasticsearchService.insertElasticSearchEntity(Collections.singletonList(getSearchableArtifact(res, userDetails)), userDetails);

        return res;
    }

    public FlatMetaDatabase findMetaDatabaseByUrl(String jdbc_url, UserDetails userDetails) {
        return metaDatabaseRepository.findMetaDatabaseByUrl(jdbc_url, userDetails);
    }

    public FlatMetaDatabase updateMetaDatabase(FlatMetaDatabase fmb, UserDetails userDetails) {
        FlatMetaDatabase fmd = metaDatabaseRepository.updateMetaDatabase(fmb, userDetails);
        elasticsearchService.updateElasticSearchEntity(Collections.singletonList(getSearchableArtifact(fmd, userDetails)), userDetails);
        return fmd;
    }

    public boolean metaObjectChanged(FlatMetaObject fmo, UserDetails userDetails) {
        FlatMetaObject dbFMO = metaObjectRepository.findMetaObject(fmo, userDetails);
        return  (dbFMO == null);
    }

    public boolean metaColumnChanged(FlatMetaColumn fmc, UserDetails userDetails) {
        FlatMetaColumn dbFMC = metaColumnRepository.findMetaColumn(fmc, userDetails);
        return (dbFMC == null || !dbFMC.getIsKey().equals(fmc.getIsKey()) || !dbFMC.getColumnType().equals(fmc.getColumnType()));
    }

    public List<FlatMetaObject> listMetaObjects(FlatMetaDatabase fmb, UserDetails userDetails) {
        return metaObjectRepository.listMetaObjects(fmb, userDetails);
    }

    public List<FlatMetaColumn> listMetaColumns(FlatMetaDatabase fmb, UserDetails userDetails) {
        return metaColumnRepository.listMetaColumns(fmb, userDetails);
    }

    public SearchableMetaDatabase getSearchableArtifact(FlatMetaDatabase fmd, UserDetails userDetails) {
        SearchableMetaDatabase smb = SearchableMetaDatabase.builder()
                .id(fmd.getId())
                .versionId(fmd.getVersionId())
                .name(fmd.getName())
                .description(fmd.getDescription())
                .modifiedAt(fmd.getModified())
                .modifiedBy(userDetails.getUid())
                .effectiveStartDate(null)
                .effectiveEndDate(null)
                .relatedArtifacts(new ArrayList<>())
                .build();

        if (fmd.getSystemId() != null)
            smb.addRelatedArtifact("system", fmd.getSystemId().toString());

        return smb;
    }

    public SearchableMetaObject getSearchableArtifact(FlatMetaObject fmo, UserDetails userDetails) {
        SearchableMetaObject smo = SearchableMetaObject.builder()
                .id(fmo.getId())
                .versionId(fmo.getVersionId())
                .name(fmo.getName())
                .description(fmo.getDescription())
                .modifiedAt(fmo.getModified())
                .modifiedBy(userDetails.getUid())
                .effectiveStartDate(null)
                .effectiveEndDate(null)
                .parentId(fmo.getParentId() == null ? null : fmo.getParentId().toString())
                .metaDatabaseId(fmo.getMetaDatabaseId().toString())
                .metaObjectType(fmo.getMetaObjectType())
                .relatedArtifacts(new ArrayList<>())
                .build();

        smo.addRelatedArtifact("meta_database", fmo.getMetaDatabaseId().toString());
        smo.addRelatedArtifact("meta_object", fmo.getParentId().toString());

        return smo;
    }

    public SearchableMetaColumn getSearchableArtifact(FlatMetaColumn fmc, UserDetails userDetails) {
        SearchableMetaColumn smc = SearchableMetaColumn.builder()
                .id(fmc.getId())
                .name(fmc.getName())
                .description(fmc.getDescription())
                .modifiedAt(fmc.getModified())
                .modifiedBy(userDetails.getUid())
                .effectiveStartDate(null)
                .effectiveEndDate(null)
                .metaObjectId(fmc.getMetaObjectId().toString())
                .metaDatabaseId(fmc.getMetaDatabaseId().toString())
                .relatedArtifacts(new ArrayList<>())
                .build();

        smc.addRelatedArtifact("meta_object", fmc.getMetaObjectId().toString());
        smc.addRelatedArtifact("meta_database", fmc.getMetaDatabaseId().toString());
        smc.addRelatedArtifact("entity", fmc.getEntityId().toString());

        return smc;
    }

    public SearchResponse<FlatMetaDatabase> searchMetaDatabases(SearchRequestWithJoin request, UserDetails userDetails)
            throws LottabyteException {
        final SearchColumn[] searchableColumns = {
                new SearchColumn("name", SearchColumn.ColumnType.Text),
                new SearchColumn("description", SearchColumn.ColumnType.Text),
                new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
                new SearchColumn("jdbc_url", SearchColumn.ColumnType.Text),
                new SearchColumn("driver_class_name", SearchColumn.ColumnType.Text),
        };

        final SearchColumnForJoin[] joinColumns = {};

        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        ServiceUtils.validateSearchRequestWithJoinState(request, userDetails);
        SearchResponse<FlatMetaDatabase> res = metaDatabaseRepository.searchMetaDatabases(request, searchableColumns, userDetails);
        /*res.getItems().stream().forEach(
                x -> x.setTags(tagService.getArtifactTags(x.getId(), userDetails)
                        .stream().map(y -> y.getName()).collect(Collectors.toList())));*/
        res.getItems().forEach(d -> d.setIsInFav(userFavService.isInFav(d.getId(), userDetails)));
        return res;
    }

    public FlatMetaDatabase getMetaDatabaseById(UUID id, UserDetails userDetails) {
        FlatMetaDatabase fmb = metaDatabaseRepository.getById(id, userDetails);
        fmb.setTags(tagService.getArtifactTags(id.toString(), userDetails).stream().map(ModeledObject::getName).collect(Collectors.toList()));
        fmb.setTasks(metaDatabaseRepository.getMetaDatabaseTasks(id, userDetails));
        return fmb;
    }

    public FlatMetaObject getMetaObjectById(UUID id, UserDetails userDetails) {
        FlatMetaObject fmo = metaObjectRepository.getById(id, userDetails);
        fmo.setTags(tagService.getArtifactTags(id.toString(), userDetails).stream().map(ModeledObject::getName).collect(Collectors.toList()));
        return fmo;
    }

    public FlatMetaDatabase getMetaDatabaseVersionById(UUID id, Integer versionId, UserDetails userDetails) {
        FlatMetaDatabase fmb = metaDatabaseRepository.getVersionById(id, versionId, userDetails);
        fmb.setTags(tagService.getArtifactTags(id.toString(), userDetails).stream().map(ModeledObject::getName).collect(Collectors.toList()));
        fmb.setTasks(metaDatabaseRepository.getMetaDatabaseTasks(id, userDetails));
        return fmb;
    }

    public SearchResponse<FlatMetaObject> searchMetaObjects(SearchRequestWithJoin request,
                                                            UserDetails userDetails) throws LottabyteException {
        final SearchColumn[] searchableColumns = {
                new SearchColumn("id", SearchColumn.ColumnType.UUID),
                new SearchColumn("name", SearchColumn.ColumnType.Text),
                new SearchColumn("description", SearchColumn.ColumnType.Text),
                new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
                new SearchColumn("tags", SearchColumn.ColumnType.Text),
                new SearchColumn("meta_object_type", SearchColumn.ColumnType.Text),
                new SearchColumn("parent_name", SearchColumn.ColumnType.Text),
                new SearchColumn("parent_id", SearchColumn.ColumnType.UUID),
                new SearchColumn("version_id", SearchColumn.ColumnType.Number),
                new SearchColumn("meta_database_id", SearchColumn.ColumnType.UUID)
        };

        final SearchColumnForJoin[] joinColumns = {};

        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        SearchResponse<FlatMetaObject> res = metaObjectRepository.searchMetaObjects(request,
                searchableColumns, joinColumns, userDetails);
        /*res.getItems().stream().forEach(
                x -> x.setTags(tagService.getArtifactTags(x.getId(), userDetails)
                        .stream().map(y -> y.getName()).collect(Collectors.toList())));*/
        return res;
    }

    public SearchResponse<FlatMetaColumn> searchMetaColumns(SearchRequestWithJoin request,
                                                            UserDetails userDetails) throws LottabyteException {
        final SearchColumn[] searchableColumns = {
                new SearchColumn("id", SearchColumn.ColumnType.UUID),
                new SearchColumn("name", SearchColumn.ColumnType.Text),
                new SearchColumn("description", SearchColumn.ColumnType.Text),
                new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
                new SearchColumn("tags", SearchColumn.ColumnType.Text),
                new SearchColumn("schema_name", SearchColumn.ColumnType.Text),
                new SearchColumn("meta_object_name", SearchColumn.ColumnType.Text),
                new SearchColumn("meta_object_id", SearchColumn.ColumnType.UUID),
                new SearchColumn("version_id", SearchColumn.ColumnType.Number),
                new SearchColumn("meta_database_id", SearchColumn.ColumnType.UUID)
        };

        final SearchColumnForJoin[] joinColumns = {};

        //ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        request.setSort(request.getSort().replace("entity_id", "name").replace("entity_attribute_name", "name"));
        SearchResponse<FlatMetaColumn> res = metaColumnRepository.searchMetaColumns(request,
                searchableColumns, joinColumns, userDetails);
        return res;
    }

    public List<FlatMetaDatabase> getMetaDatabaseVersions(String id, UserDetails userDetails) {
        return metaDatabaseRepository.getVersionsById(id, "/v1/metadata/db/" + id + "/versions", userDetails);
    }
}
