package ru.bssg.lottabyte.usermanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.indicator.Indicator;
import ru.bssg.lottabyte.core.model.product.Product;
import ru.bssg.lottabyte.core.model.reference.Reference;
import ru.bssg.lottabyte.core.model.tenant.Tenant;
import ru.bssg.lottabyte.core.model.token.BlackListToken;
import ru.bssg.lottabyte.core.model.token.Token;
import ru.bssg.lottabyte.core.model.token.UpdatableBlackListTokenEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UpdatableUserDetails;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.usermanagement.entity.TenantLdapConfig;
import ru.bssg.lottabyte.usermanagement.repository.TenantRepostiory;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TenantService {

    private final TenantRepostiory tenantRepostiory;

    @Autowired
    public TenantService(TenantRepostiory tenantRepostiory) {
        this.tenantRepostiory = tenantRepostiory;
    }

    public TenantLdapConfig getTenantLdapConfig(String tenantId) {
        return tenantRepostiory.getTenantLdapConfig(tenantId);
    }

    public String getTenant(String username) {
        if (username.contains("@")) {
            return tenantRepostiory.getTenant(username.split("@")[1]);
        } else {
            return tenantRepostiory.getDefaultTenant();
        }
    }

    public PaginatedArtifactList<Token> getAllTokensPaginatedByTenant(String tenant, Integer offset, Integer limit, UserDetails userDetails) {
        return tenantRepostiory.getAllTokensPaginatedByTenant(tenant, offset, limit, userDetails);
    }
    public PaginatedArtifactList<BlackListToken> getAllBlackListTokensPaginated(Integer offset, Integer limit, UserDetails userDetails) {
        return tenantRepostiory.getAllBlackListTokensPaginated(offset, limit, userDetails);
    }

    public BlackListToken getBlackListValueByTokenId(String tokenId, UserDetails userDetails) throws LottabyteException {
        BlackListToken blackListToken = tenantRepostiory.getBlackListValueByTokenId(tokenId);
        if (blackListToken == null)
            throw new LottabyteException(Message.LBE03201, userDetails.getLanguage(), tokenId);
        return blackListToken;
    }
    public BlackListToken getBlackListValueById(String blackListTokenId, UserDetails userDetails) throws LottabyteException {
        BlackListToken blackListToken = tenantRepostiory.getBlackListValueById(blackListTokenId);
        if (blackListToken == null)
            throw new LottabyteException(Message.LBE03201, userDetails.getLanguage(), blackListTokenId);
        return blackListToken;
    }
    public Token getTokenById(String tokenId, UserDetails userDetails) throws LottabyteException {
        Token token = tenantRepostiory.getTokenById(tokenId);
        if (token == null)
            throw new LottabyteException(Message.LBE03202, userDetails.getLanguage(), tokenId);
        return token;
    }

    public BlackListToken createBlackListToken(UpdatableBlackListTokenEntity updatableBlackListTokenEntity, UserDetails userDetails) throws LottabyteException {
        if (updatableBlackListTokenEntity.getApiTokenId() != null){
            getTokenById(updatableBlackListTokenEntity.getApiTokenId(), userDetails);
        }else{
            throw new LottabyteException(Message.LBE00048, userDetails.getLanguage());
        }

        Integer id = tenantRepostiory.createBlackListToken(updatableBlackListTokenEntity, userDetails);
        return getBlackListValueById(id.toString(), userDetails);
    }
    public void deleteBlackListTokenById(String id, UserDetails userDetails) throws LottabyteException {
        if(id != null)
            getBlackListValueById(id, userDetails);

        tenantRepostiory.deleteBlackListTokenById(id);
    }
}
