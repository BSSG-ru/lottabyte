package ru.bssg.lottabyte.core.usermanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.usermanagement.model.ApiDataForLongPeriodToken;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.repository.ApiTokensRepository;

@Service
@Slf4j
public class ApiTokensService {
    private final ApiTokensRepository apiTokensRepository;

    public ApiTokensService(ApiTokensRepository apiTokensRepository) {
        this.apiTokensRepository = apiTokensRepository;
    }

    public Boolean longPeriodTokenInBlacklistExist(String apiTokenId){
        return apiTokensRepository.longPeriodTokenInBlacklistExist(apiTokenId);
    }

    public Integer createApiToken(UserDetails ApiDataForLongPeriodToken, UserDetails userDetails){
        return apiTokensRepository.createApiToken(ApiDataForLongPeriodToken, userDetails);
    }
}
