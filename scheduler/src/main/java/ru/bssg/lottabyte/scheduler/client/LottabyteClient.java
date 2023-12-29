package ru.bssg.lottabyte.scheduler.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.bssg.lottabyte.scheduler.config.ApplicationConfig;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Slf4j
@Service
public class LottabyteClient {
    private static final String PREAUTH = "/v1/preauth/validateAuth";
    private static final String TASKRUN = "/v1/tasks/run/";
    private String bearerToken = null;
    private String baseURL = null;
    private RestTemplate restTemplate;
    private ObjectMapper mapper;

    private final ApplicationConfig applicationConfig;

    @Autowired
    public LottabyteClient(ApplicationConfig applicationConfig) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        this.applicationConfig = applicationConfig;
        this.baseURL = applicationConfig.getLottabyteApiUrl();

        TrustStrategy acceptingTrustStrategy = (x509Certificates, s) -> true;
        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        this.restTemplate = new RestTemplate(requestFactory);
        //this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper();
    }

    public JsonNode postRunTask(String taskId) {
        String url = baseURL + TASKRUN + taskId;
        return _makeRequest( url, HttpMethod.POST, (JsonNode)null);
    }

    public String preauth() {
        JsonNode res = _makeRequestInternal(baseURL + PREAUTH, HttpMethod.POST, (JsonNode)null, null);
        log.info("res token " + res.get("accessToken"));
        if (res.get("accessToken")!=null && !res.get("accessToken").asText().isEmpty()) {
            this.bearerToken = res.get("accessToken").asText();
            log.info("Got bearer token from user management");
        }
        return this.bearerToken;
    }

    protected JsonNode _makeRequest(String endpoint, HttpMethod method, JsonNode payload) {
        try {
            return _makeRequestInternal(endpoint, method, payload, null);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.FORBIDDEN ||
                    ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                try {
                    log.error("Redoing preauth");
                    preauth();
                } catch (Exception e) {
                    log.error("Catch status code when redoing preauth: " + ex.getStatusCode());
                    log.error(ExceptionUtils.getStackTrace(e));
                }
            }
        }
        return _makeRequestInternal(endpoint, method, payload, null);
    }

    protected JsonNode _makeRequestInternal(String endpoint, HttpMethod method, JsonNode payload, Map<String, String> headerOpts)
    {
        HttpEntity<String> toSend = new HttpEntity(this.getHttpHeaders(headerOpts));
        if (payload != null) {
            toSend = new HttpEntity(payload.toString(), this.getHttpHeaders(headerOpts));
        }
        ResponseEntity<String> response = this.restTemplate.exchange(endpoint, method, toSend, String.class, new Object[0]);
        JsonNode jsonNode = null;
        if (response.hasBody()) {
            try {
                jsonNode = this.mapper.readTree((String)response.getBody());
            } catch (IOException e) {
                log.error(e.getMessage());
                log.error(ExceptionUtils.getStackTrace(e));
            }
        }
        return jsonNode;
    }

    protected HttpHeaders getHttpHeaders(Map<String, String> headerOpts) {
        HttpHeaders headers = new HttpHeaders();
        if (this.bearerToken != null) {
            if (headerOpts != null) {
                for (String s : headerOpts.keySet()) {
                    headers.add(s, headerOpts.get(s));
                }
            }
            headers.add("Cache-Control", "no-cache");
            headers.add("Content-Type", "application/json");
            String auth = "Bearer " + this.bearerToken;
            headers.add("Authorization",auth);
        } else {
            String auth = "Basic " + encodeBasicAuth(applicationConfig.getLottabyteApiUsername(), applicationConfig.getLottabyteApiPassword(), applicationConfig.getLottabyteApiLanguage());
            headers.add("Authorization", auth);
            headers.add("Content-Type", "application/json");
        }
        return headers;
    }

    protected static String encodeBasicAuth(String username, String password, String language) {
        return Base64Utils.encodeToString((username + ":" + password + ":" + language).getBytes(StandardCharsets.UTF_8));
    }
}
