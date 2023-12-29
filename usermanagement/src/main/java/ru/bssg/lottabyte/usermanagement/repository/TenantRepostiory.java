package ru.bssg.lottabyte.usermanagement.repository;

import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.token.BlackListToken;
import ru.bssg.lottabyte.core.model.token.Token;
import ru.bssg.lottabyte.core.model.token.UpdatableBlackListTokenEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.usermanagement.entity.TenantLdapConfig;

public interface TenantRepostiory {

    String getDefaultTenant();
    TenantLdapConfig getTenantLdapConfig(String tenant);
    PaginatedArtifactList<Token> getAllTokensPaginatedByTenant(String tenant, Integer offset, Integer limit, UserDetails userDetails);
    PaginatedArtifactList<BlackListToken> getAllBlackListTokensPaginated(Integer offset, Integer limit, UserDetails userDetails);
    String getTenant(String username);
    BlackListToken getBlackListValueByTokenId(String tokenId);
    BlackListToken getBlackListValueById(String blackListTokenId);
    Integer createBlackListToken(UpdatableBlackListTokenEntity updatableBlackListTokenEntity, UserDetails userDetails);
    Token getTokenById(String tokenId);
    void deleteBlackListTokenById(String id);

}
