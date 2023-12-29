package ru.bssg.lottabyte.coreapi.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.externalGroup.ExternalGroup;
import ru.bssg.lottabyte.core.model.externalGroup.ExternalGroupEntity;
import ru.bssg.lottabyte.core.model.ldapProperty.LdapProperty;
import ru.bssg.lottabyte.core.model.ldapProperty.LdapPropertyEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UpdatableUserDetails;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.model.UserRole;
import ru.bssg.lottabyte.coreapi.util.JDBCUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Repository
@Slf4j
@RequiredArgsConstructor
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    class UserDetailsRowMapper implements RowMapper<UserDetails> {
        @Override
        public UserDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserDetails userDetails = new UserDetails();
            userDetails.setUid(rs.getString("uid"));
            userDetails.setUsername(rs.getString("username"));
            userDetails.setApprovalStatus(rs.getString("approval_status"));
            userDetails.setAuthenticator(rs.getString("authenticator"));
            userDetails.setCurrentAccountStatus(rs.getString("current_account_status"));
            userDetails.setDeletable(rs.getBoolean("deletable"));
            userDetails.setDisplayName(rs.getString("display_name"));
            userDetails.setEmail(rs.getString("email"));
            userDetails.setInternalUser(rs.getString("password_hash") != null);
            userDetails.setPassword(rs.getString("password_hash"));
            userDetails.setTenant(rs.getString("tenant"));
            Set<String> permissions = new HashSet<>();
            if (rs.getArray("permissions") != null) {
                String[] array = (String[])rs.getArray("permissions").getArray();
                permissions.addAll(Arrays.asList(array));
            }
            if (rs.getString("user_roles") != null) {
                String[] array = (String[])rs.getArray("user_roles").getArray();
                userDetails.setUserRoles(new ArrayList<>(Arrays.asList(array)));
                permissions.addAll(getPermissionsByRoles(userDetails.getUserRoles()));
            }
            if (rs.getString("user_domains") != null) {
                UUID[] array = (UUID[])rs.getArray("user_domains").getArray();
                userDetails.setUserDomains(new ArrayList<UUID>(Arrays.asList(array)));

            }
            userDetails.setPermissions(new ArrayList<>(permissions));
            return userDetails;
        }
    }
    static class UserRolesRowMapper implements RowMapper<UserRole> {
        @Override
        public UserRole mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserRole ur = new UserRole();
            ur.setId(rs.getString("id"));
            ur.setName(rs.getString("name"));
            ur.setDescription(rs.getString("description"));
            if (rs.getArray("permissions") != null) {
                String[] array = (String[])rs.getArray("permissions").getArray();
                ur.setPermissions(new ArrayList<>(Arrays.asList(array)));
            }
            return ur;
        }
    }
    static class ExternalGroupRowMapper implements RowMapper<ExternalGroup> {
        @Override
        public ExternalGroup mapRow(ResultSet rs, int rowNum) throws SQLException {
            ExternalGroupEntity externalGroupEntity = new ExternalGroupEntity();
            externalGroupEntity.setName(rs.getString("name"));
            externalGroupEntity.setDescription(rs.getString("description"));
            externalGroupEntity.setTenant(rs.getString("tenant"));
            externalGroupEntity.setAttributes(rs.getString("attributes"));
            if (rs.getArray("user_roles") != null) {
                String[] array = (String[])rs.getArray("user_roles").getArray();
                externalGroupEntity.setUserRoles(new ArrayList<>(Arrays.asList(array)));
            }
            if (rs.getArray("permissions") != null) {
                String[] array = (String[])rs.getArray("permissions").getArray();
                externalGroupEntity.setPermissions(new ArrayList<>(Arrays.asList(array)));
            }

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setArtifactType(externalGroupEntity.getArtifactType().toString());
            md.setName(rs.getString("name"));

            return new ExternalGroup(externalGroupEntity, md);
        }
    }
    static class LdapPropertyRowMapper implements RowMapper<LdapProperty> {
        @Override
        public LdapProperty mapRow(ResultSet rs, int rowNum) throws SQLException {
            LdapPropertyEntity ldapPropertyEntity = new LdapPropertyEntity();
            ldapPropertyEntity.setBase_dn(rs.getString("base_dn"));
            ldapPropertyEntity.setPrincipal(rs.getString("principal"));
            ldapPropertyEntity.setCredentials(rs.getString("credentials"));
            ldapPropertyEntity.setUser_query(rs.getString("user_query"));
            ldapPropertyEntity.setProviderUrl(rs.getString("provider_url"));
            ldapPropertyEntity.setTenantId(JDBCUtil.getInt(rs,"tenant_id"));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setArtifactType(ldapPropertyEntity.getArtifactType().toString());

            return new LdapProperty(ldapPropertyEntity, md);
        }
    }

    public UserDetails getPlatformUserById(String userId, String tenant) {
        return jdbcTemplate.query("SELECT uid, username, display_name, description, email, salt, password_hash, apikey_hash, apikey_salt, " +
                        "approval_status, permissions, user_roles, current_account_status, internal_user, deletable, authenticator, created, modified, tenant " +
                        ", array(SELECT domain_id FROM da_" + tenant + ".user_to_domain WHERE user_id=?) AS user_domains " +
                        "FROM usermgmt.platform_users " +
                        "WHERE uid = ? AND tenant = ?",
                new UserDetailsRowMapper(), Long.parseLong(userId), Long.parseLong(userId), tenant).stream().findFirst().orElse(null);
    }
    public List<UserDetails> getPlatformUserByTenantId(String tenantId) {
        return jdbcTemplate.query("SELECT uid, username, display_name, description, email, salt, password_hash, apikey_hash, apikey_salt, " +
                        "approval_status, permissions, user_roles, current_account_status, internal_user, deletable, authenticator, created, modified, tenant " +
                        ", array(SELECT domain_id FROM da_" + tenantId + ".user_to_domain WHERE user_id=?) AS user_domains " +
                        "FROM usermgmt.platform_users " +
                        "WHERE tenant = ?",
                new UserDetailsRowMapper(), tenantId);
    }

    public List<UserDetails> getUsersByRoleName(String roleName, String tenant) {
        return jdbcTemplate.query("SELECT uid, username, display_name, description, email, salt, password_hash, apikey_hash, apikey_salt, " +
                "approval_status, permissions, user_roles, current_account_status, internal_user, deletable, authenticator, created, modified, tenant " +
                ", array(SELECT domain_id FROM da_" + tenant + ".user_to_domain WHERE user_id=uid) AS user_domains " +
                "FROM usermgmt.platform_users WHERE tenant = ? AND CAST((SELECT id FROM usermgmt.user_roles WHERE name=?) AS TEXT)=ANY(user_roles)",
                new UserDetailsRowMapper(), tenant, roleName);
    }

    public List<UserRole> getUserRolesByTenantId(String tenantId) {
        return jdbcTemplate.query("SELECT id, name, description, permissions from usermgmt.user_roles where tenant = ?",
                new UserRolesRowMapper(), tenantId);
    }

    public UserRole getUserRoleById(String roleId, String tenantId) {
        return jdbcTemplate.query("SELECT id, name, description, permissions from usermgmt.user_roles where id = ? and tenant = ?",
                new UserRolesRowMapper(), UUID.fromString(roleId), tenantId)
                .stream().findFirst().orElse(null);
    }

    public List<ExternalGroup> getExternalGroupsByTenantId(String tenantId) {
        return jdbcTemplate.query("SELECT * from usermgmt.external_groups where tenant = ?",
                new ExternalGroupRowMapper(), tenantId);
    }
    public List<LdapProperty> getLdapPropertiesByTenantId(Integer tenantId) {
        return jdbcTemplate.query("SELECT * from da.ldap_properties where tenant_id = ?",
                new LdapPropertyRowMapper(), tenantId);
    }
    public Set<String> getPermissionsByRoles(List<String> roleIds) {
        Set<String> res = new HashSet<>();
        if (roleIds == null || roleIds.isEmpty())
            return res;
        String inSql = String.join(",", Collections.nCopies(roleIds.size(), "?"));
        String query = String.format("SELECT permissions FROM usermgmt.user_roles WHERE ID in (%s)", inSql);
        jdbcTemplate.query(query, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                if (rs.getArray("permissions") != null) {
                    String[] array = (String[])rs.getArray("permissions").getArray();
                    res.addAll(Arrays.asList(array));
                }
            }
        }, roleIds.stream().map(UUID::fromString).toArray());
        return res;
    }

    public Integer createUser(UpdatableUserDetails newUserDetails, String tenant) {
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        String sql = "INSERT INTO usermgmt.platform_users (username, display_name, description, email, password_hash, approval_status, " +
                "permissions, user_roles, current_account_status, internal_user, deletable, authenticator, created, modified, tenant) " +
                "values (?,?,?,?,?,?," +
                "?,?,?,?,?,?,?,?,?) RETURNING uid";
        return jdbcTemplate.queryForObject(sql, Integer.class,
                newUserDetails.getUsername(), newUserDetails.getDisplayName(),
                newUserDetails.getDescription(), newUserDetails.getEmail(), passwordEncoder.encode(newUserDetails.getPassword()),
                "approved",
                createStringSqlArray(newUserDetails.getPermissions()),
                createStringSqlArray(newUserDetails.getUserRolesIds()),
                "enabled", true, true, "default", ts, ts, tenant);
    }

    public java.sql.Array createStringSqlArray(List<String> list){
        java.sql.Array stringArray = null;
        List<String> lst = list;
        if (lst == null) lst = new ArrayList<>();
        try {
            stringArray = jdbcTemplate.getDataSource().getConnection().createArrayOf("VARCHAR",
                    lst.toArray());
        } catch (SQLException ignore) {
        }
        return stringArray;
    }

    public void deleteUserByTenant(String tenantId) {
        jdbcTemplate.update("DELETE FROM usermgmt.platform_users where tenant = ?", tenantId);
    }
    public void deleteUserRolesByTenantId(String tenantId) {
        jdbcTemplate.update("DELETE FROM usermgmt.user_roles where tenant = ?", tenantId);
    }
    public void deleteExternalGroupsByTenantId(String tenantId) {
        jdbcTemplate.update("DELETE FROM usermgmt.external_groups where tenant = ?", tenantId);
    }
    public void deleteLdapPropertiesByTenantId(Integer tenantId) {
        jdbcTemplate.update("DELETE FROM da.ldap_properties where tenant_id = ?", tenantId);
    }

    public boolean existsUserWithDomain(String domainId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT ID FROM da_" + userDetails.getTenant() + ".user_to_domain " +
                        "WHERE domain_id is not null and domain_id = ?) AS EXISTS",
                Boolean.class, UUID.fromString(domainId));
    }

}
