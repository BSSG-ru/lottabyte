package ru.bssg.lottabyte.usermanagement.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.WorkflowableMetadata;
import ru.bssg.lottabyte.core.model.product.Product;
import ru.bssg.lottabyte.core.model.product.UpdatableProductEntity;
import ru.bssg.lottabyte.core.model.token.*;
import ru.bssg.lottabyte.core.usermanagement.model.UpdatableUserDetails;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.usermanagement.entity.TenantLdapConfig;
import ru.bssg.lottabyte.usermanagement.repository.TenantRepostiory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Repository
public class TenantRepositoryImpl implements TenantRepostiory {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TenantRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    class TenantLdapConfigMapper implements RowMapper<TenantLdapConfig> {
        @Override
        public TenantLdapConfig mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new TenantLdapConfig(
                    rs.getString("provider_url"),
                    rs.getString("principal"),
                    rs.getString("credentials"),
                    rs.getString("base_dn"),
                    rs.getString("user_query"));
        }
    }
    static class TokenMapper implements RowMapper<Token> {
        public static Token mapTokenRow(ResultSet rs) throws SQLException, LottabyteException {
            TokenEntity tokenEntity = new TokenEntity();
            tokenEntity.setName(rs.getString("name"));
            tokenEntity.setDescription(rs.getString("description"));
            tokenEntity.setTenant(rs.getString("tenant"));
            tokenEntity.setValidTill(rs.getTimestamp("valid_till").toLocalDateTime());

            Metadata metadata = new Metadata();
            metadata.setArtifactType(tokenEntity.getArtifactType().getText());
            metadata.setId(rs.getString("id"));
            metadata.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            metadata.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            metadata.setCreatedBy(rs.getString("creator"));
            metadata.setModifiedBy(rs.getString("modifier"));

            return new Token(tokenEntity, metadata);
        }

        @Override
        public Token mapRow(ResultSet rs, int rowNum) throws SQLException {
            try {
                return mapTokenRow(rs);
            } catch (LottabyteException e) {
                return null;
            }
        }
    }
    static class BlackListTokenMapper implements RowMapper<BlackListToken> {
        public static BlackListToken mapBlackListTokenRow(ResultSet rs) throws SQLException, LottabyteException {
            BlackListTokenEntity blackListTokenEntity = new BlackListTokenEntity();
            blackListTokenEntity.setApiTokenId(rs.getString("api_token_id"));
            blackListTokenEntity.setCause(rs.getString("cause"));

            Metadata metadata = new Metadata();
            metadata.setId(rs.getString("id"));
            metadata.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            metadata.setCreatedBy(rs.getString("creator"));
            metadata.setArtifactType(blackListTokenEntity.getArtifactType().getText());

            return new BlackListToken(blackListTokenEntity, metadata);
        }

        @Override
        public BlackListToken mapRow(ResultSet rs, int rowNum) throws SQLException {
            try {
                return mapBlackListTokenRow(rs);
            } catch (LottabyteException e) {
                return null;
            }
        }
    }

    public String getDefaultTenant() {
        return jdbcTemplate.queryForObject("SELECT id " +
                        "FROM da.tenant " +
                        "WHERE default_tenant IS TRUE LIMIT 1",
                String.class);
    }

    public TenantLdapConfig getTenantLdapConfig(String tenantId) {
        List<TenantLdapConfig> res = jdbcTemplate.query(
                "SELECT * FROM da.ldap_properties WHERE tenant_id = ?",
                new TenantLdapConfigMapper(), Integer.parseInt(tenantId));
        return res.stream().findFirst().orElse(null);
    }
    public PaginatedArtifactList<Token> getAllTokensPaginatedByTenant(String tenant, Integer offset, Integer limit, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM usermgmt.api_tokens WHERE tenant = ?",
                Integer.class, tenant);
        String query = "SELECT * FROM usermgmt.api_tokens WHERE tenant = ? offset ? limit ? ";
        List<Token> productList = jdbcTemplate.query(query, new TokenMapper(), tenant, offset, limit);

        PaginatedArtifactList<Token> res = new PaginatedArtifactList<>(
                productList, offset, limit, total, "/v1/preauth/token/" + tenant);
        return res;
    }
    public PaginatedArtifactList<BlackListToken> getAllBlackListTokensPaginated(Integer offset, Integer limit, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM usermgmt.api_tokens_blacklist",
                Integer.class);
        String query = "SELECT * FROM usermgmt.api_tokens_blacklist offset ? limit ? ";
        List<BlackListToken> productList = jdbcTemplate.query(query, new BlackListTokenMapper(), offset, limit);

        PaginatedArtifactList<BlackListToken> res = new PaginatedArtifactList<>(
                productList, offset, limit, total, "/v1/preauth/black_list_token");
        return res;
    }

    @Override
    public String getTenant(String domain) {
        return jdbcTemplate.queryForObject("SELECT id " +
                        "FROM da.tenant " +
                        "WHERE \"domain\" = ?",
                String.class, domain);
    }

    @Override
    public BlackListToken getBlackListValueByTokenId(String tokenId) {
        return jdbcTemplate.query("SELECT * FROM usermgmt.api_tokens_blacklist WHERE api_token_id=?",
                new BlackListTokenMapper(), Integer.parseInt(tokenId)).stream().findFirst().orElse(null);
    }
    @Override
    public BlackListToken getBlackListValueById(String blackListTokenId) {
        return jdbcTemplate.query("SELECT * FROM usermgmt.api_tokens_blacklist WHERE id=?",
                new BlackListTokenMapper(), Integer.parseInt(blackListTokenId)).stream().findFirst().orElse(null);
    }
    @Override
    public Token getTokenById(String tokenId) {
        return jdbcTemplate.query("SELECT * FROM usermgmt.api_tokens WHERE id=?",
                new TokenMapper(), Integer.parseInt(tokenId)).stream().findFirst().orElse(null);
    }

    @Override
    public Integer createBlackListToken(UpdatableBlackListTokenEntity updatableBlackListTokenEntity, UserDetails userDetails) {
        Timestamp ts = new Timestamp(new Date().getTime());

        return jdbcTemplate.queryForObject("INSERT INTO usermgmt.api_tokens_blacklist (api_token_id, cause, created, creator) " +
                        "VALUES (?,?,?,?) RETURNING id",
                Integer.class, Integer.parseInt(updatableBlackListTokenEntity.getApiTokenId()), updatableBlackListTokenEntity.getCause(), ts, userDetails.getUid());
    }

    @Override
    public void deleteBlackListTokenById(String id) {
        String query = "DELETE FROM usermgmt.api_tokens_blacklist " +
                " WHERE id=?;";
        jdbcTemplate.update(query, Integer.parseInt(id));
    }
}
