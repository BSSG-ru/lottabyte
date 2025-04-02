package ru.bssg.lottabyte.coreapi.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.FlatArtifact;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.FlatWFItemObject;
import ru.bssg.lottabyte.core.model.domain.FlatDomain;
import ru.bssg.lottabyte.core.ui.model.SearchRequest;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.ui.model.UploadedFilesListData;
import ru.bssg.lottabyte.core.ui.model.dashboard.DashboardEntity;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelData;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelNodeData;
import ru.bssg.lottabyte.core.ui.model.gojs.UpdatableGojsModelData;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.config.ApplicationConfig;
import ru.bssg.lottabyte.coreapi.repository.ArtifactRepository;
import ru.bssg.lottabyte.coreapi.service.APILogService;
import ru.bssg.lottabyte.coreapi.service.ArtifactService;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@RestController
@Slf4j
@RequestMapping("v1/artifacts")
@RequiredArgsConstructor
public class ArtifactController {
    private final ArtifactService artifactService;
    private final APILogService apiLogService;
    private final ApplicationConfig applicationConfig;
    private final JwtHelper jwtHelper;
    private final String[] artifactTypes = { ArtifactType.domain.getText(), ArtifactType.system.getText(), ArtifactType.entity.getText(),
            ArtifactType.entity_query.getText(), ArtifactType.entity_sample.getText(), ArtifactType.data_asset.getText(), ArtifactType.task.getText(),
            ArtifactType.business_entity.getText(), ArtifactType.indicator.getText(), ArtifactType.product.getText(), ArtifactType.dq_rule.getText(),
            "draft", ArtifactType.meta_database.getText(), ArtifactType.etl.getText() };

    @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get Artifacts Count.", description = "This method can be used to get Artifacts Count.", operationId = "get_artifact_count")
    @RequestMapping(value = "/count/{limit_steward}", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = { "artifacts_r" }, level = ANY_ROLE)
    public ResponseEntity<Map<String, Integer>> getArtifactsCount(
            @Parameter(description = "Workflow task action", example = "publish") @PathVariable("limit_steward") Boolean limitSteward,
            @RequestHeader HttpHeaders headers, HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, limitSteward);
        return new ResponseEntity<>(artifactService.getArtifactsCount(Arrays.asList(artifactTypes), limitSteward,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get Settings Count.", description = "This method can be used to get Settings Count.", operationId = "get_settings_count")
    @RequestMapping(value = "/settings/count/{type}", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = { "artifacts_r" }, level = ANY_ROLE)
    public ResponseEntity<Integer> getSettingsCount(
            @PathVariable("type") String type,
            @RequestHeader HttpHeaders headers, HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, type);
        return new ResponseEntity<>(artifactService.getSettingsCount(type, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get Artifact Drafts.", description = "This method can be used to get Artifact Drafts.", operationId = "get_drafts")
    @RequestMapping(value = "/drafts", method = RequestMethod.POST, produces = { "application/json" })
    @Secured(roles = { "task_r" }, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatWFItemObject>> getDrafts(
            @RequestBody SearchRequest sr,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, sr);
        return new ResponseEntity<>(artifactService.searchDrafts(sr,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/model/{artifact_type}/{artifact_id}", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"lo_mdl_r"}, level = ANY_ROLE)
    public ResponseEntity<GojsModelData> getArtifactModel(@RequestHeader HttpHeaders headers, @PathVariable("artifact_type") String artifactType,
                   @PathVariable("artifact_id") String artifactId, HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, artifactType, artifactId);
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        GojsModelData res = artifactService.getArtifactModel(artifactId, artifactType, userDetails);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/model/{artifact_type}/{artifact_id}", method = RequestMethod.PATCH, produces = { "application/json" })
    @Secured(roles = {"lo_mdl_u"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<List<GojsModelNodeData>> updateArtifactModel(@RequestBody UpdatableGojsModelData updatableGojsModelData, @PathVariable("artifact_type") String artifactType,
                                                                       @PathVariable("artifact_id") String artifactId, @RequestHeader HttpHeaders headers,
                                                                       HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, updatableGojsModelData, artifactType, artifactId);
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        List<GojsModelNodeData> res = artifactService.updateArtifactModel(updatableGojsModelData, artifactType, artifactId, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/model", method = RequestMethod.PATCH, produces = { "application/json" })
    @Secured(roles = {"lo_mdl_u"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<List<GojsModelNodeData>> updateModel(@RequestBody UpdatableGojsModelData updatableGojsModelData,
                                                               @RequestHeader HttpHeaders headers,
                                                               HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, updatableGojsModelData);
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        List<GojsModelNodeData> res = artifactService.updateModel(updatableGojsModelData, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/clearModels", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"lo_mdl_r", "lo_mdl_u"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<Boolean> clearModels(@RequestBody UpdatableGojsModelData updatableGojsModelData,
                                               @RequestHeader HttpHeaders headers,
                                               HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, updatableGojsModelData);
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        artifactService.clearModels(userDetails);

        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/dashboard", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<List<DashboardEntity>> getDashboard(@RequestHeader HttpHeaders headers,
                                                              HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request);
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        List<DashboardEntity> res = artifactService.getDashboard(userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/dashboard/recommended", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<List<DashboardEntity>> getDashboardRecommended(@RequestHeader HttpHeaders headers,
                                                                         HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request);
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        List<DashboardEntity> res = artifactService.getRecommended(userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/dashboard/popular", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<List<DashboardEntity>> getDashboardPopular(@RequestHeader HttpHeaders headers,
                                                                     HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request);
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        List<DashboardEntity> res = artifactService.getPopular(userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/dashboard/favorites", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<List<DashboardEntity>> getDashboardFavorites(@RequestHeader HttpHeaders headers,
                                                                       HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request);
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        List<DashboardEntity> res = artifactService.getFavorites(userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/artifact_types", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<Map<String, String>> getArtifactTypes(@RequestHeader HttpHeaders headers,
                                                                HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request);
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(artifactService.getArtifactTypes(false, userDetails), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/workflowable_artifact_types", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<Map<String, String>> getWorkflowableArtifactTypes(@RequestHeader HttpHeaders headers,
                                                                            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request);
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(artifactService.getArtifactTypes(true, userDetails), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/artifact_type/{code}", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<String> getArtifactType(@PathVariable("code") String code, @RequestHeader HttpHeaders headers,
                                                  HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, code);
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(artifactService.getArtifactType(code, userDetails), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/artifact_actions", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<List<String>> getArtifactActions(@RequestHeader HttpHeaders headers,
                                                           HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request);
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(artifactService.getArtifactActions(userDetails), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/related_artifact_types/{artifact_type}", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<List<String>> getRelatedArtifactTypes(@PathVariable("artifact_type") String artifactType,
                                                                @RequestHeader HttpHeaders headers,
                                                                HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, artifactType);
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(artifactService.getRelatedArtifactTypes(artifactType, userDetails), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search_related_artifacts/{src_artifact_type}/{src_artifact_id}/{tgt_artifact_type}", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<? extends FlatModeledObject>> searchDomains(
            @PathVariable("src_artifact_type") String srcArtifactType,
            @PathVariable("src_artifact_id") String srcArtifactId,
            @PathVariable("tgt_artifact_type") String tgtArtifactType,
            @RequestBody SearchRequestWithJoin sr,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, srcArtifactType, srcArtifactId, tgtArtifactType, sr);
        return ResponseEntity.ok(artifactService.searchRelatedArtifacts(srcArtifactType, srcArtifactId, tgtArtifactType, sr, jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
    }

    @Operation(summary = "Upload image", description = "This method can be used to upload image", operationId = "upload_image")
    @RequestMapping(value = "/upload_image", method = RequestMethod.POST, produces = { "application/json" })
    public ResponseEntity<UploadedFilesListData> uploadImage(
            @RequestParam("file-0") MultipartFile file,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, file.getName());
        return new ResponseEntity<>(artifactService.uploadImage(new MultipartFile[] { file }, "images"), HttpStatus.OK);
    }

    @Operation(summary = "Serve image", description = "This method can be used to serve image", operationId = "get_image")
    @RequestMapping(value = "/image/{filename}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> image(@PathVariable("filename") String filename, HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, filename);
        ArtifactRepository.ServeFileData filedata = artifactService.getImage(filename, "images");
        return ResponseEntity.ok().contentType(MediaType.valueOf(filedata.getContentType())).body(filedata.getContents());
    }

    @Hidden
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatArtifact>> searchArtifacts(
            @RequestBody SearchRequestWithJoin sr,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, sr);
        return ResponseEntity.ok(artifactService.searchArtifacts(sr, jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
    }

    @Hidden
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    public ResponseEntity<FlatModeledObject> getArtifact(
            @PathVariable("id") String id,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, id);
        return ResponseEntity.ok(artifactService.getArtifact(id, jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
    }
}
