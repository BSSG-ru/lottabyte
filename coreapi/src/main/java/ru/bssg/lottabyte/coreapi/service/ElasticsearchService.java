package ru.bssg.lottabyte.coreapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ElasticsearchService {
    private final RestHighLevelClient esClient;

    private final ObjectMapper mapper = new ObjectMapper()
                .findAndRegisterModules()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private Map<String, Object> convertSearchableArtifactToMap(SearchableArtifact searchableArtifact) throws JsonProcessingException {
        String json = mapper.writeValueAsString(searchableArtifact);
        return mapper.readValue(json, Map.class);
    }
    public void insertElasticSearchEntity(List<SearchableArtifact> searchableArtifactList, UserDetails userDetail) {
        BulkRequest bulkRequest = Requests.bulkRequest();
        searchableArtifactList.forEach(searchableArtifact -> {
            try{
                IndexRequest indexRequest = Requests
                        .indexRequest("da_" + userDetail.getTenant()).id(searchableArtifact.getId())
                        .source(convertSearchableArtifactToMap(searchableArtifact), XContentType.JSON);
                bulkRequest.add(indexRequest);
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        });

        RequestOptions options = RequestOptions.DEFAULT;
        try {
            esClient.bulk(bulkRequest, options);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void deleteElasticSearchEntityById(List<String> idList, UserDetails userDetail) {
        BulkRequest bulkRequest = Requests.bulkRequest();
        idList.forEach(id -> bulkRequest.add(new DeleteRequest("da_" + userDetail.getTenant(), id)));

        RequestOptions options = RequestOptions.DEFAULT;
        try {
            esClient.bulk(bulkRequest, options);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void deleteAllDocumentFromIndexElasticSearch(String indexName, UserDetails userDetail) throws IOException {
        DeleteByQueryRequest request = new DeleteByQueryRequest(indexName);
        request.setQuery(QueryBuilders.matchAllQuery());
        esClient.deleteByQuery(request, RequestOptions.DEFAULT);
    }

    public void updateElasticSearchEntity(List<SearchableArtifact> searchableArtifactList, UserDetails userDetail) {
        BulkRequest bulkRequest = Requests.bulkRequest();
        searchableArtifactList.forEach(searchableArtifact -> {
            UpdateRequest updateRequest = null;
            try {
                updateRequest = new UpdateRequest("da_" + userDetail.getTenant(), searchableArtifact.getId())
                        .doc(
                                convertSearchableArtifactToMap(searchableArtifact), XContentType.JSON
                        )
                        .upsert(
                                convertSearchableArtifactToMap(searchableArtifact), XContentType.JSON
                        );
                bulkRequest.add(updateRequest);
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        });

        RequestOptions options = RequestOptions.DEFAULT;
        try {
            esClient.bulk(bulkRequest, options);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public List<HashMap<String, Object>> searchPost(String requestValue, UserDetails userDetail) throws IOException {
        HashMap<String, Object> entity = new HashMap<>();
        List<HashMap<String, Object>> objectList = new ArrayList<>();
        Request request = new Request("POST","/da_" + userDetail.getTenant() + "/_search");
        requestValue = requestValue.substring(0, requestValue.length() - 1);
        String requestForFindField = ",\n" +
                "  \"highlight\": {\n" +
                "    \"fields\": {\n" +
                "      \"*\": {}\n" +
                "    }\n" +
                "  }" +
                "}";
        requestValue = requestValue + requestForFindField;
        request.setJsonEntity(requestValue);

        Response response = esClient.getLowLevelClient().performRequest(request);

        TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};

        HashMap<String, Object> hits = (HashMap<String, Object>) mapper.readValue(EntityUtils.toString(response.getEntity()), typeRef).get("hits");
        Integer size = (Integer) ((HashMap<String, Object>) hits.get("total")).get("value");
        entity.put("size", size);
        objectList.add(entity);
        for(HashMap<String, Object> hit : (ArrayList<HashMap<String, Object>>) hits.get("hits")){
            entity = new HashMap<>();
            entity.put("_source", hit.get("_source"));
            entity.put("_score", hit.get("_score"));
            entity.put("highlight", hit.get("highlight"));
            objectList.add(entity);
        }

        return objectList;
    }

    public String searchGet(String requestValue, UserDetails userDetail) throws IOException {
        Request request = new Request("GET","/da_" + userDetail.getTenant() + "/_search?" + requestValue);

        Response response = esClient.getLowLevelClient().performRequest(request);

        return EntityUtils.toString(response.getEntity());
    }

    public void createIndex(String indexName, String indexSetting) throws LottabyteException {
        try {
            CreateIndexRequest cir = new CreateIndexRequest(indexName);
            cir.source(indexSetting, XContentType.JSON);
            esClient.indices().create(cir, RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    public void deleteIndex(String indexName) throws LottabyteException {
        try {
            var deleteRequest = new DeleteIndexRequest(indexName);
            esClient.indices().delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
}
