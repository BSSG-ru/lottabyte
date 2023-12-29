package ru.bssg.lottabyte.coreapi.service.connector;

import lombok.extern.slf4j.Slf4j;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.connector.IConnectorService;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.connector.Connector;
import ru.bssg.lottabyte.core.model.connector.ConnectorParam;
import ru.bssg.lottabyte.core.model.dataentity.DataEntity;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQueryResult;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleType;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.model.system.SystemConnection;
import ru.bssg.lottabyte.core.model.system.SystemConnectionParam;
import ru.bssg.lottabyte.core.model.util.RestApiAuthMethod;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
public class GenericRESTAPIConnectorServiceImpl implements IConnectorService {
    public EntityQueryResult querySystem(Connector connector,
                                         List<ConnectorParam> connectorParams,
                                         System system,
                                         DataEntity entity,
                                         EntityQuery entityQuery,
                                         SystemConnection systemConnection,
                                         List<SystemConnectionParam> systemConnectionParams,
                                         UserDetails userDetails)
            throws LottabyteException {
        EntityQueryResult res = new EntityQueryResult();

        if (entityQuery.getEntity() == null || entityQuery.getEntity().getQueryText() == null || entityQuery.getEntity().getQueryText().isEmpty())
            throw new LottabyteException(Message.LBE00005, userDetails.getLanguage(), entityQuery.getId());

        Map<String, String> paramValues = new HashMap<>();

        for (SystemConnectionParam scp : systemConnectionParams) {
            if (scp.getEntity() == null)
                throw new LottabyteException(Message.LBE00006, userDetails.getLanguage(), scp.getId());

            Optional<ConnectorParam> cp = connectorParams.stream().filter(
                    x -> x.getId().equals(scp.getEntity().getConnectorParamId())
            ).findFirst();
            if (cp.isEmpty())
                throw new LottabyteException(Message.LBE00007, userDetails.getLanguage(), scp.getEntity().getConnectorParamId());
            paramValues.put(cp.get().getEntity().getName(), scp.getEntity().getParamValue());
        }

        if (!paramValues.containsKey("restapi_base_url"))
            throw new LottabyteException(Message.LBE00008, userDetails.getLanguage(), "restapi_base_url");
        if (!paramValues.containsKey("restapi_result_type"))
            throw new LottabyteException(Message.LBE00008, userDetails.getLanguage(), "restapi_result_type");
        if (!paramValues.containsKey("restapi_auth_method"))
            throw new LottabyteException(Message.LBE00008, userDetails.getLanguage(), "restapi_auth_method");

        String[] queryParam = entityQuery.getEntity().getQueryText().split("\\s+");
        String httpMethod = queryParam[0];
        String path = queryParam[1];
        try {
            if(!httpMethod.equals("POST") && !httpMethod.equals("GET"))
                throw new LottabyteException(Message.LBE00045, userDetails.getLanguage(), httpMethod);

            HttpClient client = HttpClient.newHttpClient();
            String body = null;
            if(queryParam.length > 2){
                body = entityQuery.getEntity().getQueryText().split(path)[1].substring(1);
            }
            var requestConstructor = HttpRequest
                    .newBuilder()
                    .uri(URI.create(paramValues.get("restapi_base_url") + "/" + path))
                    .version(HttpClient.Version.HTTP_2)
                    .timeout(Duration.ofMinutes(1))
                    .header("Content-Type", "application/json");

            String authMethod = paramValues.get("restapi_auth_method");

            if(authMethod.equals(RestApiAuthMethod.basic.getVal())){
                if (!paramValues.containsKey("restapi_basicauth_username"))
                    throw new LottabyteException(Message.LBE00008, userDetails.getLanguage(), "restapi_basicauth_username");
                if (!paramValues.containsKey("restapi_basicauth_password"))
                    throw new LottabyteException(Message.LBE00008, userDetails.getLanguage(), "restapi_basicauth_password");

                String basicAuthData = paramValues.get("restapi_basicauth_username") + ":" + paramValues.get("restapi_basicauth_password");
                String basicAuthPayload = "Basic " + Base64.getEncoder().encodeToString(basicAuthData.getBytes());
                requestConstructor.header("Authorization", basicAuthPayload);
            }else if(authMethod.equals(RestApiAuthMethod.bearer_token.getVal())){
                if (!paramValues.containsKey("restapi_bearer_token"))
                    throw new LottabyteException(Message.LBE00008, userDetails.getLanguage(), "restapi_bearer_token");

                String token = paramValues.get("restapi_bearer_token");
                if(token != null && !token.isEmpty()){
                    requestConstructor.header("Authorization", "Bearer " + token);
                }
            }

            if(httpMethod.equals("POST")){
                if (body != null) {
                    requestConstructor = requestConstructor.POST(HttpRequest.BodyPublishers.ofString(body));
                }else{
                    requestConstructor = requestConstructor.POST(HttpRequest.BodyPublishers.noBody());
                }
            }else if(httpMethod.equals("GET")){
                requestConstructor = requestConstructor.GET();
            }

            HttpRequest request = requestConstructor.build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() >= 300){
                throw new LottabyteException(response.body());
            }

            List<String> contentType = response.headers().allValues("content-type");
            res.setSampleType(EntitySampleType.table);
            if(contentType != null && !contentType.isEmpty()){
                if(contentType.stream().findFirst().get().equals("application/json")){
                    res.setSampleType(EntitySampleType.json);
                }else if(contentType.stream().findFirst().get().equals("text/xml")){
                    res.setSampleType(EntitySampleType.xml);
                }else{
                    res.setSampleType(EntitySampleType.unknown);
                }
            }

            res.setTextSampleBody(response.body());
            res.setBinarySampleBody(response.body().getBytes());

            return res;
        } catch (IOException | InterruptedException e) {
            throw new LottabyteException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
