package ru.bssg.lottabyte.coreapi.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.FlatWFItemObject;
import ru.bssg.lottabyte.core.model.domain.FlatDomain;
import ru.bssg.lottabyte.core.ui.model.SearchRequest;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.ui.model.dashboard.DashboardEntity;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelData;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelNodeData;
import ru.bssg.lottabyte.core.ui.model.gojs.UpdatableGojsModelData;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.config.ApplicationConfig;
import ru.bssg.lottabyte.coreapi.service.ArtifactService;

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
    private final ApplicationConfig applicationConfig;
    private final JwtHelper jwtHelper;
    private final String[] artifactTypes = { ArtifactType.domain.getText(), ArtifactType.system.getText(), ArtifactType.entity.getText(),
            ArtifactType.entity_query.getText(), ArtifactType.entity_sample.getText(), ArtifactType.data_asset.getText(), ArtifactType.task.getText(),
            ArtifactType.business_entity.getText(), ArtifactType.indicator.getText(), ArtifactType.product.getText(), ArtifactType.dq_rule.getText(), "draft" };

    @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get Artifacts Count.", description = "This method can be used to get Artifacts Count.", operationId = "get_artifact_count")
    @RequestMapping(value = "/count/{limit_steward}", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = { "artifacts_r" }, level = ANY_ROLE)
    public ResponseEntity<Map<String, Integer>> getArtifactsCount(
            @Parameter(description = "Workflow task action", example = "publish") @PathVariable("limit_steward") Boolean limitSteward,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return new ResponseEntity<>(artifactService.getArtifactsCount(Arrays.asList(artifactTypes), limitSteward,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get Artifact Drafts.", description = "This method can be used to get Artifact Drafts.", operationId = "get_drafts")
    @RequestMapping(value = "/drafts", method = RequestMethod.POST, produces = { "application/json" })
    @Secured(roles = { "artifacts_r" }, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatWFItemObject>> getDrafts(
            @RequestBody SearchRequest request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return new ResponseEntity<>(artifactService.searchDrafts(request,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/model/{artifact_type}", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"lo_mdl_r"}, level = ANY_ROLE)
    public ResponseEntity<String> getModel(@RequestHeader HttpHeaders headers, @PathVariable("artifact_type") String artifactType) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        //GojsModelData res = artifactService.getModel(artifactType, userDetails);

        String res = applicationConfig.getModelJson();
        //String res = "{\"nodes\":[{\"id\":\"a913195b-5ada-41b7-8ffe-cc684813ee2c\",\"name\":\"ДОМЕН: Финансы\",\"type\":\"defaultNodeType\",\"artifactType\":\"domain\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ДОМЕН: Финансы\",\"zorder\":1},{\"id\":\"22dc2f1e-73fe-4f97-919a-1a2f533bbb9e\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"a913195b-5ada-41b7-8ffe-cc684813ee2c\",\"group\":\"a913195b-5ada-41b7-8ffe-cc684813ee2c\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"b1f7f97d-e7cd-4868-81d5-6df44ec51f69\",\"name\":\"ДОМЕН: Кадры\",\"type\":\"defaultNodeType\",\"artifactType\":\"domain\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ДОМЕН: Кадры\",\"zorder\":1},{\"id\":\"0aec248d-28f5-465c-b711-612d761a4f6f\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"b1f7f97d-e7cd-4868-81d5-6df44ec51f69\",\"group\":\"b1f7f97d-e7cd-4868-81d5-6df44ec51f69\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"d9420536-bc15-4670-94ba-003d3f3578b4\",\"name\":\"ПРОДУКТ: Кадры дашборд BI\",\"type\":\"defaultNodeType\",\"artifactType\":\"product\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ПРОДУКТ: Кадры дашборд BI\",\"zorder\":1},{\"id\":\"0d105295-b8f7-4ee0-8a52-74fbb6167b3c\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"d9420536-bc15-4670-94ba-003d3f3578b4\",\"group\":\"d9420536-bc15-4670-94ba-003d3f3578b4\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"ed5a856b-1794-4333-9ebc-92273585b927\",\"name\":\"ПОКАЗАТЕЛЬ: Производительность труда\",\"type\":\"defaultNodeType\",\"artifactType\":\"indicator\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ПОКАЗАТЕЛЬ: Производительность труда\",\"zorder\":1},{\"id\":\"bcee8b78-120b-4b59-b5ab-1774e47e9462\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"ed5a856b-1794-4333-9ebc-92273585b927\",\"group\":\"ed5a856b-1794-4333-9ebc-92273585b927\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"a71e6211-9411-4beb-bc65-3aee2da86429\",\"name\":\"ПОКАЗАТЕЛЬ: Выручка\",\"type\":\"defaultNodeType\",\"artifactType\":\"indicator\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ПОКАЗАТЕЛЬ: Выручка\",\"zorder\":1},{\"id\":\"1a2130a0-d2a9-4292-a3bf-030a38998175\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"a71e6211-9411-4beb-bc65-3aee2da86429\",\"group\":\"a71e6211-9411-4beb-bc65-3aee2da86429\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"c88b37b4-e334-4e1f-a099-2d3533a638d7\",\"name\":\"ПОКАЗАТЕЛЬ: Среднесписочное количество работников\",\"type\":\"defaultNodeType\",\"artifactType\":\"indicator\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ПОКАЗАТЕЛЬ: Среднесписочное количество работников\",\"zorder\":1},{\"id\":\"bb1f3c3d-1096-4f29-a4b9-52d34042ffe4\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"c88b37b4-e334-4e1f-a099-2d3533a638d7\",\"group\":\"c88b37b4-e334-4e1f-a099-2d3533a638d7\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"834d6263-4f60-4f0e-851a-7d0fb9914151\",\"name\":\"АКТИВ: Таблица начислений зарплаты P2B2_ZES\",\"type\":\"defaultNodeType\",\"artifactType\":\"data_asset\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"АКТИВ: Таблица начислений зарплаты P2B2_ZES\",\"zorder\":1},{\"id\":\"d94f0d12-9b02-43be-9be7-0934631bec77\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"834d6263-4f60-4f0e-851a-7d0fb9914151\",\"group\":\"834d6263-4f60-4f0e-851a-7d0fb9914151\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"27828581-713f-4665-9cf7-7fb5f0633a5a\",\"name\":\"АКТИВ: Таблица платежей ACDOCA\",\"type\":\"defaultNodeType\",\"artifactType\":\"data_asset\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"АКТИВ: Таблица платежей ACDOCA\",\"zorder\":1},{\"id\":\"608789da-8928-4fc7-a2ca-7d11a1871653\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"27828581-713f-4665-9cf7-7fb5f0633a5a\",\"group\":\"27828581-713f-4665-9cf7-7fb5f0633a5a\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"7fdf1864-3061-4565-991d-b40094dd5e8c\",\"name\":\"ЛОГИЧЕСКИЙ ОБЪЕКТ: Начисления зарплаты\",\"type\":\"defaultNodeType\",\"artifactType\":\"entity\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ЛОГИЧЕСКИЙ ОБЪЕКТ: Начисления зарплаты\",\"zorder\":1},{\"id\":\"63a11481-96d4-4248-86c9-3c9cb7cafa83\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"7fdf1864-3061-4565-991d-b40094dd5e8c\",\"group\":\"7fdf1864-3061-4565-991d-b40094dd5e8c\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"672535f8-2b5a-4a5e-b3ac-ee8fcb555887\",\"name\":\"ЛОГИЧЕСКИЙ ОБЪЕКТ: Платежи\",\"type\":\"defaultNodeType\",\"artifactType\":\"entity\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ЛОГИЧЕСКИЙ ОБЪЕКТ: Платежи\",\"zorder\":1},{\"id\":\"379388c5-543a-4938-86f6-c9ef322b71c4\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"672535f8-2b5a-4a5e-b3ac-ee8fcb555887\",\"group\":\"672535f8-2b5a-4a5e-b3ac-ee8fcb555887\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"120f8d97-2ff3-4589-ac91-b53a6b77d476\",\"name\":\"СИСТЕМА: Yandex Cloud\",\"type\":\"defaultNodeType\",\"artifactType\":\"system\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"СИСТЕМА: Yandex Cloud\",\"zorder\":1},{\"id\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"120f8d97-2ff3-4589-ac91-b53a6b77d476\",\"group\":\"120f8d97-2ff3-4589-ac91-b53a6b77d476\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"c33b697e-b0da-4b8e-8a66-35cf11cb11db\",\"name\":\"ЗАПРОС: ACDOCA\",\"type\":\"defaultNodeType\",\"artifactType\":\"entity_query\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ЗАПРОС: ACDOCA\",\"zorder\":1},{\"id\":\"31686cf8-0ef9-4803-999c-7601b58d7f36\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"c33b697e-b0da-4b8e-8a66-35cf11cb11db\",\"group\":\"c33b697e-b0da-4b8e-8a66-35cf11cb11db\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"7c7bcbd9-6b00-4fa4-acf9-6a7b019b0e9b\",\"name\":\"ЗАПРОС: P2B2_ZES\",\"type\":\"defaultNodeType\",\"artifactType\":\"entity_query\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ЗАПРОС: P2B2_ZES\",\"zorder\":1},{\"id\":\"57a17794-e815-4eb5-889d-53e90618eaa2\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"7c7bcbd9-6b00-4fa4-acf9-6a7b019b0e9b\",\"group\":\"7c7bcbd9-6b00-4fa4-acf9-6a7b019b0e9b\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"2d2c293c-2bbc-4708-a55e-66c9d4db6431\",\"name\":\"СЭМПЛ: ACDOCA\",\"type\":\"defaultNodeType\",\"artifactType\":\"entity_sample\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"СЭМПЛ: ACDOCA\",\"zorder\":1},{\"id\":\"5a254917-654f-4e33-8579-fa85b4a19dae\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"2d2c293c-2bbc-4708-a55e-66c9d4db6431\",\"group\":\"2d2c293c-2bbc-4708-a55e-66c9d4db6431\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"86898292-b564-4248-9aca-178324e73f4f\",\"name\":\"СЭМПЛ: P2B2_ZES\",\"type\":\"defaultNodeType\",\"artifactType\":\"entity_sample\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"СЭМПЛ: P2B2_ZES\",\"zorder\":1},{\"id\":\"8836ae11-0e7c-4198-a27d-89faf8b35463\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"86898292-b564-4248-9aca-178324e73f4f\",\"group\":\"86898292-b564-4248-9aca-178324e73f4f\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1}],\"links\":[{\"id\":\"391e6052-cad9-4c53-bc82-f23dd857e8e9\",\"from\":\"0d105295-b8f7-4ee0-8a52-74fbb6167b3c\",\"to\":\"bcee8b78-120b-4b59-b5ab-1774e47e9462\",\"points\":\"\",\"zorder\":1},{\"id\":\"cf5ae0af-6cb4-467c-b879-6177f3e7eb7b\",\"from\":\"0d105295-b8f7-4ee0-8a52-74fbb6167b3c\",\"to\":\"1a2130a0-d2a9-4292-a3bf-030a38998175\",\"points\":\"\",\"zorder\":1},{\"id\":\"922970b1-eb63-45a9-ba77-f8ea8e55b01a\",\"from\":\"0d105295-b8f7-4ee0-8a52-74fbb6167b3c\",\"to\":\"bb1f3c3d-1096-4f29-a4b9-52d34042ffe4\",\"points\":\"\",\"zorder\":1},{\"id\":\"25ec60be-0f99-49d6-8483-f1ff60d46ab6\",\"from\":\"0d105295-b8f7-4ee0-8a52-74fbb6167b3c\",\"to\":\"0aec248d-28f5-465c-b711-612d761a4f6f\",\"points\":\"\",\"zorder\":1},{\"id\":\"0d542ad0-2862-4d79-873c-fd14d99d7ca8\",\"from\":\"0d105295-b8f7-4ee0-8a52-74fbb6167b3c\",\"to\":\"d94f0d12-9b02-43be-9be7-0934631bec77\",\"points\":\"\",\"zorder\":1},{\"id\":\"720606e2-0416-450f-94aa-f613ed6c67ea\",\"from\":\"0d105295-b8f7-4ee0-8a52-74fbb6167b3c\",\"to\":\"608789da-8928-4fc7-a2ca-7d11a1871653\",\"points\":\"\",\"zorder\":1},{\"id\":\"c7cf8da9-aaed-4fff-aa75-ed54f7308be8\",\"from\":\"1a2130a0-d2a9-4292-a3bf-030a38998175\",\"to\":\"22dc2f1e-73fe-4f97-919a-1a2f533bbb9e\",\"points\":\"\",\"zorder\":1},{\"id\":\"365674d5-c59f-477b-9f62-c366824a60a1\",\"from\":\"1a2130a0-d2a9-4292-a3bf-030a38998175\",\"to\":\"608789da-8928-4fc7-a2ca-7d11a1871653\",\"points\":\"\",\"zorder\":1},{\"id\":\"e1ad29d0-4e3b-4891-baf9-ec6a3e79b23b\",\"from\":\"bcee8b78-120b-4b59-b5ab-1774e47e9462\",\"to\":\"0aec248d-28f5-465c-b711-612d761a4f6f\",\"points\":\"\",\"zorder\":1},{\"id\":\"14824443-e211-4eab-83f2-41b8e16bab47\",\"from\":\"bcee8b78-120b-4b59-b5ab-1774e47e9462\",\"to\":\"bb1f3c3d-1096-4f29-a4b9-52d34042ffe4\",\"points\":\"\",\"zorder\":1},{\"id\":\"f6d4fe77-79b4-4015-8be5-7b1d8b915788\",\"from\":\"bb1f3c3d-1096-4f29-a4b9-52d34042ffe4\",\"to\":\"0aec248d-28f5-465c-b711-612d761a4f6f\",\"points\":\"\",\"zorder\":1},{\"id\":\"c439f541-4e54-4577-a9bc-1fe19fd714ca\",\"from\":\"bb1f3c3d-1096-4f29-a4b9-52d34042ffe4\",\"to\":\"d94f0d12-9b02-43be-9be7-0934631bec77\",\"points\":\"\",\"zorder\":1},{\"id\":\"9b1fd1e8-77ed-4b0f-86ae-7fe38f151e8d\",\"from\":\"bb1f3c3d-1096-4f29-a4b9-52d34042ffe4\",\"to\":\"608789da-8928-4fc7-a2ca-7d11a1871653\",\"points\":\"\",\"zorder\":1},{\"id\":\"fc739688-5353-49b4-b2ee-29efd93cc1e9\",\"from\":\"d94f0d12-9b02-43be-9be7-0934631bec77\",\"to\":\"22dc2f1e-73fe-4f97-919a-1a2f533bbb9e\",\"points\":\"\",\"zorder\":1},{\"id\":\"7da11431-2d8c-49fe-b950-7b9eee9cd692\",\"from\":\"d94f0d12-9b02-43be-9be7-0934631bec77\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"029dbfba-7019-4e7b-b9a2-a509a6b6bc02\",\"from\":\"d94f0d12-9b02-43be-9be7-0934631bec77\",\"to\":\"63a11481-96d4-4248-86c9-3c9cb7cafa83\",\"points\":\"\",\"zorder\":1},{\"id\":\"0e7b725c-a704-4738-a82e-ad7b116b82d5\",\"from\":\"608789da-8928-4fc7-a2ca-7d11a1871653\",\"to\":\"22dc2f1e-73fe-4f97-919a-1a2f533bbb9e\",\"points\":\"\",\"zorder\":1},{\"id\":\"d058f2a2-2e4b-4254-a3bc-99e1f29539cf\",\"from\":\"608789da-8928-4fc7-a2ca-7d11a1871653\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"08eb226e-0ca3-4179-819e-519c96765793\",\"from\":\"608789da-8928-4fc7-a2ca-7d11a1871653\",\"to\":\"379388c5-543a-4938-86f6-c9ef322b71c4\",\"points\":\"\",\"zorder\":1},{\"id\":\"6bead51b-36cd-4dfd-9ef5-13b6fc2d59a4\",\"from\":\"5a254917-654f-4e33-8579-fa85b4a19dae\",\"to\":\"379388c5-543a-4938-86f6-c9ef322b71c4\",\"points\":\"\",\"zorder\":1},{\"id\":\"5869ab8f-2bdf-4e92-a146-fd6614c0450b\",\"from\":\"5a254917-654f-4e33-8579-fa85b4a19dae\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"7dc76be7-e5b3-4ea4-afdd-224249d126bd\",\"from\":\"5a254917-654f-4e33-8579-fa85b4a19dae\",\"to\":\"31686cf8-0ef9-4803-999c-7601b58d7f36\",\"points\":\"\",\"zorder\":1},{\"id\":\"f9aa8212-4b9a-423a-8a2f-bc35790d626f\",\"from\":\"57a17794-e815-4eb5-889d-53e90618eaa2\",\"to\":\"63a11481-96d4-4248-86c9-3c9cb7cafa83\",\"points\":\"\",\"zorder\":1},{\"id\":\"9fd7bc07-84b8-43ca-8f0c-6a21e787da16\",\"from\":\"57a17794-e815-4eb5-889d-53e90618eaa2\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"87f415fa-c984-4b2a-838a-0e7d8a828e2c\",\"from\":\"57a17794-e815-4eb5-889d-53e90618eaa2\",\"to\":\"57a17794-e815-4eb5-889d-53e90618eaa2\",\"points\":\"\",\"zorder\":1},{\"id\":\"6f047abe-2e81-4427-950a-6a4f01077d9c\",\"from\":\"31686cf8-0ef9-4803-999c-7601b58d7f36\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"5b22fc2c-bdc9-4f0b-af03-864a39b66eec\",\"from\":\"31686cf8-0ef9-4803-999c-7601b58d7f36\",\"to\":\"379388c5-543a-4938-86f6-c9ef322b71c4\",\"points\":\"\",\"zorder\":1},{\"id\":\"f4405cce-5f31-485e-baf2-60d2f0afdeda\",\"from\":\"57a17794-e815-4eb5-889d-53e90618eaa2\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"4acd6b0b-527d-4753-9960-bc9da19f2660\",\"from\":\"57a17794-e815-4eb5-889d-53e90618eaa2\",\"to\":\"63a11481-96d4-4248-86c9-3c9cb7cafa83\",\"points\":\"\",\"zorder\":1},{\"id\":\"cd2e14c4-cfb4-4c87-b4d1-e3a97e747026\",\"from\":\"63a11481-96d4-4248-86c9-3c9cb7cafa83\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"b8d059db-baab-4faf-8c04-88ccf183e790\",\"from\":\"379388c5-543a-4938-86f6-c9ef322b71c4\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"26722c6f-8131-44e8-8c0d-13ae1329f76a\",\"from\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"to\":\"22dc2f1e-73fe-4f97-919a-1a2f533bbb9e\",\"points\":\"\",\"zorder\":1},{\"id\":\"f429d85c-0414-4d65-b680-931867a6de33\",\"from\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"to\":\"0aec248d-28f5-465c-b711-612d761a4f6f\",\"points\":\"\",\"zorder\":1}]}";

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/model/{artifact_type}/{artifact_id}", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"lo_mdl_r"}, level = ANY_ROLE)
    public ResponseEntity<GojsModelData> getArtifactModel(@RequestHeader HttpHeaders headers, @PathVariable("artifact_type") String artifactType,
                                                   @PathVariable("artifact_id") String artifactId) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        GojsModelData res = artifactService.getArtifactModel(artifactId, artifactType, userDetails);

        //String res = applicationConfig.getModelJson();
        //String res = "{\"nodes\":[{\"id\":\"a913195b-5ada-41b7-8ffe-cc684813ee2c\",\"name\":\"ДОМЕН: Финансы\",\"type\":\"defaultNodeType\",\"artifactType\":\"domain\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ДОМЕН: Финансы\",\"zorder\":1},{\"id\":\"22dc2f1e-73fe-4f97-919a-1a2f533bbb9e\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"a913195b-5ada-41b7-8ffe-cc684813ee2c\",\"group\":\"a913195b-5ada-41b7-8ffe-cc684813ee2c\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"b1f7f97d-e7cd-4868-81d5-6df44ec51f69\",\"name\":\"ДОМЕН: Кадры\",\"type\":\"defaultNodeType\",\"artifactType\":\"domain\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ДОМЕН: Кадры\",\"zorder\":1},{\"id\":\"0aec248d-28f5-465c-b711-612d761a4f6f\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"b1f7f97d-e7cd-4868-81d5-6df44ec51f69\",\"group\":\"b1f7f97d-e7cd-4868-81d5-6df44ec51f69\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"d9420536-bc15-4670-94ba-003d3f3578b4\",\"name\":\"ПРОДУКТ: Кадры дашборд BI\",\"type\":\"defaultNodeType\",\"artifactType\":\"product\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ПРОДУКТ: Кадры дашборд BI\",\"zorder\":1},{\"id\":\"0d105295-b8f7-4ee0-8a52-74fbb6167b3c\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"d9420536-bc15-4670-94ba-003d3f3578b4\",\"group\":\"d9420536-bc15-4670-94ba-003d3f3578b4\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"ed5a856b-1794-4333-9ebc-92273585b927\",\"name\":\"ПОКАЗАТЕЛЬ: Производительность труда\",\"type\":\"defaultNodeType\",\"artifactType\":\"indicator\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ПОКАЗАТЕЛЬ: Производительность труда\",\"zorder\":1},{\"id\":\"bcee8b78-120b-4b59-b5ab-1774e47e9462\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"ed5a856b-1794-4333-9ebc-92273585b927\",\"group\":\"ed5a856b-1794-4333-9ebc-92273585b927\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"a71e6211-9411-4beb-bc65-3aee2da86429\",\"name\":\"ПОКАЗАТЕЛЬ: Выручка\",\"type\":\"defaultNodeType\",\"artifactType\":\"indicator\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ПОКАЗАТЕЛЬ: Выручка\",\"zorder\":1},{\"id\":\"1a2130a0-d2a9-4292-a3bf-030a38998175\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"a71e6211-9411-4beb-bc65-3aee2da86429\",\"group\":\"a71e6211-9411-4beb-bc65-3aee2da86429\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"c88b37b4-e334-4e1f-a099-2d3533a638d7\",\"name\":\"ПОКАЗАТЕЛЬ: Среднесписочное количество работников\",\"type\":\"defaultNodeType\",\"artifactType\":\"indicator\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ПОКАЗАТЕЛЬ: Среднесписочное количество работников\",\"zorder\":1},{\"id\":\"bb1f3c3d-1096-4f29-a4b9-52d34042ffe4\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"c88b37b4-e334-4e1f-a099-2d3533a638d7\",\"group\":\"c88b37b4-e334-4e1f-a099-2d3533a638d7\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"834d6263-4f60-4f0e-851a-7d0fb9914151\",\"name\":\"АКТИВ: Таблица начислений зарплаты P2B2_ZES\",\"type\":\"defaultNodeType\",\"artifactType\":\"data_asset\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"АКТИВ: Таблица начислений зарплаты P2B2_ZES\",\"zorder\":1},{\"id\":\"d94f0d12-9b02-43be-9be7-0934631bec77\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"834d6263-4f60-4f0e-851a-7d0fb9914151\",\"group\":\"834d6263-4f60-4f0e-851a-7d0fb9914151\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"27828581-713f-4665-9cf7-7fb5f0633a5a\",\"name\":\"АКТИВ: Таблица платежей ACDOCA\",\"type\":\"defaultNodeType\",\"artifactType\":\"data_asset\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"АКТИВ: Таблица платежей ACDOCA\",\"zorder\":1},{\"id\":\"608789da-8928-4fc7-a2ca-7d11a1871653\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"27828581-713f-4665-9cf7-7fb5f0633a5a\",\"group\":\"27828581-713f-4665-9cf7-7fb5f0633a5a\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"7fdf1864-3061-4565-991d-b40094dd5e8c\",\"name\":\"ЛОГИЧЕСКИЙ ОБЪЕКТ: Начисления зарплаты\",\"type\":\"defaultNodeType\",\"artifactType\":\"entity\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ЛОГИЧЕСКИЙ ОБЪЕКТ: Начисления зарплаты\",\"zorder\":1},{\"id\":\"63a11481-96d4-4248-86c9-3c9cb7cafa83\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"7fdf1864-3061-4565-991d-b40094dd5e8c\",\"group\":\"7fdf1864-3061-4565-991d-b40094dd5e8c\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"672535f8-2b5a-4a5e-b3ac-ee8fcb555887\",\"name\":\"ЛОГИЧЕСКИЙ ОБЪЕКТ: Платежи\",\"type\":\"defaultNodeType\",\"artifactType\":\"entity\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ЛОГИЧЕСКИЙ ОБЪЕКТ: Платежи\",\"zorder\":1},{\"id\":\"379388c5-543a-4938-86f6-c9ef322b71c4\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"672535f8-2b5a-4a5e-b3ac-ee8fcb555887\",\"group\":\"672535f8-2b5a-4a5e-b3ac-ee8fcb555887\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"120f8d97-2ff3-4589-ac91-b53a6b77d476\",\"name\":\"СИСТЕМА: Yandex Cloud\",\"type\":\"defaultNodeType\",\"artifactType\":\"system\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"СИСТЕМА: Yandex Cloud\",\"zorder\":1},{\"id\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"120f8d97-2ff3-4589-ac91-b53a6b77d476\",\"group\":\"120f8d97-2ff3-4589-ac91-b53a6b77d476\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"c33b697e-b0da-4b8e-8a66-35cf11cb11db\",\"name\":\"ЗАПРОС: ACDOCA\",\"type\":\"defaultNodeType\",\"artifactType\":\"entity_query\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ЗАПРОС: ACDOCA\",\"zorder\":1},{\"id\":\"31686cf8-0ef9-4803-999c-7601b58d7f36\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"c33b697e-b0da-4b8e-8a66-35cf11cb11db\",\"group\":\"c33b697e-b0da-4b8e-8a66-35cf11cb11db\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"7c7bcbd9-6b00-4fa4-acf9-6a7b019b0e9b\",\"name\":\"ЗАПРОС: P2B2_ZES\",\"type\":\"defaultNodeType\",\"artifactType\":\"entity_query\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"ЗАПРОС: P2B2_ZES\",\"zorder\":1},{\"id\":\"57a17794-e815-4eb5-889d-53e90618eaa2\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"7c7bcbd9-6b00-4fa4-acf9-6a7b019b0e9b\",\"group\":\"7c7bcbd9-6b00-4fa4-acf9-6a7b019b0e9b\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"2d2c293c-2bbc-4708-a55e-66c9d4db6431\",\"name\":\"СЭМПЛ: ACDOCA\",\"type\":\"defaultNodeType\",\"artifactType\":\"entity_sample\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"СЭМПЛ: ACDOCA\",\"zorder\":1},{\"id\":\"5a254917-654f-4e33-8579-fa85b4a19dae\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"2d2c293c-2bbc-4708-a55e-66c9d4db6431\",\"group\":\"2d2c293c-2bbc-4708-a55e-66c9d4db6431\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1},{\"id\":\"86898292-b564-4248-9aca-178324e73f4f\",\"name\":\"СЭМПЛ: P2B2_ZES\",\"type\":\"defaultNodeType\",\"artifactType\":\"entity_sample\",\"loc\":\"\",\"isGroup\":true,\"parentId\":\"\",\"group\":\"\",\"text\":\"СЭМПЛ: P2B2_ZES\",\"zorder\":1},{\"id\":\"8836ae11-0e7c-4198-a27d-89faf8b35463\",\"name\":\"\",\"type\":\"defaultNodeType\",\"artifactType\":\"\",\"loc\":\"\",\"isGroup\":false,\"parentId\":\"86898292-b564-4248-9aca-178324e73f4f\",\"group\":\"86898292-b564-4248-9aca-178324e73f4f\",\"text\":\"\",\"order\":1,\"datatype\":\"TEXT\",\"zorder\":1}],\"links\":[{\"id\":\"391e6052-cad9-4c53-bc82-f23dd857e8e9\",\"from\":\"0d105295-b8f7-4ee0-8a52-74fbb6167b3c\",\"to\":\"bcee8b78-120b-4b59-b5ab-1774e47e9462\",\"points\":\"\",\"zorder\":1},{\"id\":\"cf5ae0af-6cb4-467c-b879-6177f3e7eb7b\",\"from\":\"0d105295-b8f7-4ee0-8a52-74fbb6167b3c\",\"to\":\"1a2130a0-d2a9-4292-a3bf-030a38998175\",\"points\":\"\",\"zorder\":1},{\"id\":\"922970b1-eb63-45a9-ba77-f8ea8e55b01a\",\"from\":\"0d105295-b8f7-4ee0-8a52-74fbb6167b3c\",\"to\":\"bb1f3c3d-1096-4f29-a4b9-52d34042ffe4\",\"points\":\"\",\"zorder\":1},{\"id\":\"25ec60be-0f99-49d6-8483-f1ff60d46ab6\",\"from\":\"0d105295-b8f7-4ee0-8a52-74fbb6167b3c\",\"to\":\"0aec248d-28f5-465c-b711-612d761a4f6f\",\"points\":\"\",\"zorder\":1},{\"id\":\"0d542ad0-2862-4d79-873c-fd14d99d7ca8\",\"from\":\"0d105295-b8f7-4ee0-8a52-74fbb6167b3c\",\"to\":\"d94f0d12-9b02-43be-9be7-0934631bec77\",\"points\":\"\",\"zorder\":1},{\"id\":\"720606e2-0416-450f-94aa-f613ed6c67ea\",\"from\":\"0d105295-b8f7-4ee0-8a52-74fbb6167b3c\",\"to\":\"608789da-8928-4fc7-a2ca-7d11a1871653\",\"points\":\"\",\"zorder\":1},{\"id\":\"c7cf8da9-aaed-4fff-aa75-ed54f7308be8\",\"from\":\"1a2130a0-d2a9-4292-a3bf-030a38998175\",\"to\":\"22dc2f1e-73fe-4f97-919a-1a2f533bbb9e\",\"points\":\"\",\"zorder\":1},{\"id\":\"365674d5-c59f-477b-9f62-c366824a60a1\",\"from\":\"1a2130a0-d2a9-4292-a3bf-030a38998175\",\"to\":\"608789da-8928-4fc7-a2ca-7d11a1871653\",\"points\":\"\",\"zorder\":1},{\"id\":\"e1ad29d0-4e3b-4891-baf9-ec6a3e79b23b\",\"from\":\"bcee8b78-120b-4b59-b5ab-1774e47e9462\",\"to\":\"0aec248d-28f5-465c-b711-612d761a4f6f\",\"points\":\"\",\"zorder\":1},{\"id\":\"14824443-e211-4eab-83f2-41b8e16bab47\",\"from\":\"bcee8b78-120b-4b59-b5ab-1774e47e9462\",\"to\":\"bb1f3c3d-1096-4f29-a4b9-52d34042ffe4\",\"points\":\"\",\"zorder\":1},{\"id\":\"f6d4fe77-79b4-4015-8be5-7b1d8b915788\",\"from\":\"bb1f3c3d-1096-4f29-a4b9-52d34042ffe4\",\"to\":\"0aec248d-28f5-465c-b711-612d761a4f6f\",\"points\":\"\",\"zorder\":1},{\"id\":\"c439f541-4e54-4577-a9bc-1fe19fd714ca\",\"from\":\"bb1f3c3d-1096-4f29-a4b9-52d34042ffe4\",\"to\":\"d94f0d12-9b02-43be-9be7-0934631bec77\",\"points\":\"\",\"zorder\":1},{\"id\":\"9b1fd1e8-77ed-4b0f-86ae-7fe38f151e8d\",\"from\":\"bb1f3c3d-1096-4f29-a4b9-52d34042ffe4\",\"to\":\"608789da-8928-4fc7-a2ca-7d11a1871653\",\"points\":\"\",\"zorder\":1},{\"id\":\"fc739688-5353-49b4-b2ee-29efd93cc1e9\",\"from\":\"d94f0d12-9b02-43be-9be7-0934631bec77\",\"to\":\"22dc2f1e-73fe-4f97-919a-1a2f533bbb9e\",\"points\":\"\",\"zorder\":1},{\"id\":\"7da11431-2d8c-49fe-b950-7b9eee9cd692\",\"from\":\"d94f0d12-9b02-43be-9be7-0934631bec77\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"029dbfba-7019-4e7b-b9a2-a509a6b6bc02\",\"from\":\"d94f0d12-9b02-43be-9be7-0934631bec77\",\"to\":\"63a11481-96d4-4248-86c9-3c9cb7cafa83\",\"points\":\"\",\"zorder\":1},{\"id\":\"0e7b725c-a704-4738-a82e-ad7b116b82d5\",\"from\":\"608789da-8928-4fc7-a2ca-7d11a1871653\",\"to\":\"22dc2f1e-73fe-4f97-919a-1a2f533bbb9e\",\"points\":\"\",\"zorder\":1},{\"id\":\"d058f2a2-2e4b-4254-a3bc-99e1f29539cf\",\"from\":\"608789da-8928-4fc7-a2ca-7d11a1871653\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"08eb226e-0ca3-4179-819e-519c96765793\",\"from\":\"608789da-8928-4fc7-a2ca-7d11a1871653\",\"to\":\"379388c5-543a-4938-86f6-c9ef322b71c4\",\"points\":\"\",\"zorder\":1},{\"id\":\"6bead51b-36cd-4dfd-9ef5-13b6fc2d59a4\",\"from\":\"5a254917-654f-4e33-8579-fa85b4a19dae\",\"to\":\"379388c5-543a-4938-86f6-c9ef322b71c4\",\"points\":\"\",\"zorder\":1},{\"id\":\"5869ab8f-2bdf-4e92-a146-fd6614c0450b\",\"from\":\"5a254917-654f-4e33-8579-fa85b4a19dae\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"7dc76be7-e5b3-4ea4-afdd-224249d126bd\",\"from\":\"5a254917-654f-4e33-8579-fa85b4a19dae\",\"to\":\"31686cf8-0ef9-4803-999c-7601b58d7f36\",\"points\":\"\",\"zorder\":1},{\"id\":\"f9aa8212-4b9a-423a-8a2f-bc35790d626f\",\"from\":\"57a17794-e815-4eb5-889d-53e90618eaa2\",\"to\":\"63a11481-96d4-4248-86c9-3c9cb7cafa83\",\"points\":\"\",\"zorder\":1},{\"id\":\"9fd7bc07-84b8-43ca-8f0c-6a21e787da16\",\"from\":\"57a17794-e815-4eb5-889d-53e90618eaa2\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"87f415fa-c984-4b2a-838a-0e7d8a828e2c\",\"from\":\"57a17794-e815-4eb5-889d-53e90618eaa2\",\"to\":\"57a17794-e815-4eb5-889d-53e90618eaa2\",\"points\":\"\",\"zorder\":1},{\"id\":\"6f047abe-2e81-4427-950a-6a4f01077d9c\",\"from\":\"31686cf8-0ef9-4803-999c-7601b58d7f36\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"5b22fc2c-bdc9-4f0b-af03-864a39b66eec\",\"from\":\"31686cf8-0ef9-4803-999c-7601b58d7f36\",\"to\":\"379388c5-543a-4938-86f6-c9ef322b71c4\",\"points\":\"\",\"zorder\":1},{\"id\":\"f4405cce-5f31-485e-baf2-60d2f0afdeda\",\"from\":\"57a17794-e815-4eb5-889d-53e90618eaa2\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"4acd6b0b-527d-4753-9960-bc9da19f2660\",\"from\":\"57a17794-e815-4eb5-889d-53e90618eaa2\",\"to\":\"63a11481-96d4-4248-86c9-3c9cb7cafa83\",\"points\":\"\",\"zorder\":1},{\"id\":\"cd2e14c4-cfb4-4c87-b4d1-e3a97e747026\",\"from\":\"63a11481-96d4-4248-86c9-3c9cb7cafa83\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"b8d059db-baab-4faf-8c04-88ccf183e790\",\"from\":\"379388c5-543a-4938-86f6-c9ef322b71c4\",\"to\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"points\":\"\",\"zorder\":1},{\"id\":\"26722c6f-8131-44e8-8c0d-13ae1329f76a\",\"from\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"to\":\"22dc2f1e-73fe-4f97-919a-1a2f533bbb9e\",\"points\":\"\",\"zorder\":1},{\"id\":\"f429d85c-0414-4d65-b680-931867a6de33\",\"from\":\"919b5817-daf5-4ae3-af41-b529a8cbdb02\",\"to\":\"0aec248d-28f5-465c-b711-612d761a4f6f\",\"points\":\"\",\"zorder\":1}]}";

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/model/{artifact_type}/{artifact_id}", method = RequestMethod.PATCH, produces = { "application/json" })
    @Secured(roles = {"lo_mdl_u"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<List<GojsModelNodeData>> updateArtifactModel(@RequestBody UpdatableGojsModelData updatableGojsModelData, @PathVariable("artifact_type") String artifactType,
                                                                       @PathVariable("artifact_id") String artifactId, @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        List<GojsModelNodeData> res = artifactService.updateArtifactModel(updatableGojsModelData, artifactType, artifactId, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/model", method = RequestMethod.PATCH, produces = { "application/json" })
    @Secured(roles = {"lo_mdl_u"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<List<GojsModelNodeData>> updateModel(@RequestBody UpdatableGojsModelData updatableGojsModelData, @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        List<GojsModelNodeData> res = artifactService.updateModel(updatableGojsModelData, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/clearModels", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"lo_mdl_r", "lo_mdl_u"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<Boolean> clearModels(@RequestBody UpdatableGojsModelData updatableGojsModelData, @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        artifactService.clearModels(userDetails);

        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/dashboard", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<List<DashboardEntity>> getDashboard(@RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        List<DashboardEntity> res = artifactService.getDashboard(userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/artifact_types", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<Map<String, String>> getArtifactTypes(@RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(artifactService.getArtifactTypes(false, userDetails), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/workflowable_artifact_types", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<Map<String, String>> getWorkflowableArtifactTypes(@RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(artifactService.getArtifactTypes(true, userDetails), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/artifact_type/{code}", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<String> getArtifactType(@PathVariable("code") String code, @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(artifactService.getArtifactType(code, userDetails), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/artifact_actions", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"artifacts_r"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<List<String>> getArtifactActions(@RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(artifactService.getArtifactActions(userDetails), HttpStatus.OK);
    }
}
