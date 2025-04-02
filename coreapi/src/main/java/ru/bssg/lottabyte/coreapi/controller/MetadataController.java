package ru.bssg.lottabyte.coreapi.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.domain.UpdatableDomainEntity;
import ru.bssg.lottabyte.core.model.metaColumn.FlatMetaColumn;
import ru.bssg.lottabyte.core.model.metaDatabase.FlatMetaDatabase;
import ru.bssg.lottabyte.core.model.metaObject.FlatMetaObject;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.APILogService;
import ru.bssg.lottabyte.coreapi.service.MetadataService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "MetaData", description = "APIs for MetaData.")
@RestController
@Slf4j
@RequestMapping("v1/metadata")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class MetadataController {
    private final JwtHelper jwtHelper;
    private final MetadataService metadataService;
    private final APILogService apiLogService;

    @Hidden
    @RequestMapping(value = "/db/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"metadata_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatMetaDatabase>> searchMetaDatabases(
            @RequestBody SearchRequestWithJoin sr,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, sr);
        return new ResponseEntity<>(metadataService.searchMetaDatabases(sr, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/db/{id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"metadata_r"}, level = ANY_ROLE)
    public ResponseEntity<FlatMetaDatabase> getDatabase(
            @PathVariable("id") UUID id,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, id);
        return new ResponseEntity<>(metadataService.getMetaDatabaseById(id, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/db/{id}/version/{version_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"metadata_r"}, level = ANY_ROLE)
    public ResponseEntity<FlatMetaDatabase> getDatabaseVersion(
            @PathVariable("id") UUID id,
            @PathVariable("version_id") Integer versionId,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, id, versionId);
        return new ResponseEntity<>(metadataService.getMetaDatabaseVersionById(id, versionId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/object/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"metadata_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatMetaObject>> searchMetaObjects(
            @RequestBody SearchRequestWithJoin sr,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, sr);
        return new ResponseEntity<>(metadataService.searchMetaObjects(sr, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/column/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"metadata_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatMetaColumn>> searchMetaColumns(
            @RequestBody SearchRequestWithJoin sr,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, sr);
        return new ResponseEntity<>(metadataService.searchMetaColumns(sr, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/db/{id}/versions", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"metadata_r"}, level = ANY_ROLE)
    public ResponseEntity<List<FlatMetaDatabase>> getDatabaseVersions(
            @PathVariable("id") UUID id,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, id);
        return new ResponseEntity<>(metadataService.getMetaDatabaseVersions(id.toString(), jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/db/{id}/update", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"metadata_r", "metadata_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<FlatMetaDatabase> patchMetaDatabase(
            @PathVariable("id") UUID id,
            @RequestBody FlatMetaDatabase fmb,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, id, fmb);
        FlatMetaDatabase res = metadataService.updateMetaDatabase(fmb, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}
