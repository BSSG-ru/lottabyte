package ru.bssg.lottabyte.usermanagement.repository.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.WorkflowableMetadata;
import ru.bssg.lottabyte.core.model.businessEntity.BusinessEntity;
import ru.bssg.lottabyte.core.model.businessEntity.BusinessEntityEntity;
import ru.bssg.lottabyte.core.model.externalGroup.ExternalGroup;
import ru.bssg.lottabyte.core.model.externalGroup.ExternalGroupEntity;
import ru.bssg.lottabyte.core.model.externalGroup.FlatExternalGroup;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.*;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.usermanagement.repository.UserRepository;

@Slf4j
@Repository
public class UserRepositoryImpl implements UserRepository {

    private JdbcTemplate jdbcTemplate;
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Lazy
    public UserRepositoryImpl(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    class UserDetailsWithUserDetailsGroupRowMapper implements RowMapper<UserDetails> {
        @Override
        public UserDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserDetails userDetails = new UserDetails();
            Map<String, UserDetailsGroup> userDetailsGroupMap = new HashMap<>();
            try {
                UserDetailsGroup userDetailsGroup = new UserDetailsGroup();
                userDetailsGroup.setDescription(rs.getString("description"));
                userDetailsGroup.setName(rs.getString("display_name"));
                userDetailsGroupMap.put(rs.getString("uid"), userDetailsGroupMap.getOrDefault(rs.getString("uid"), userDetailsGroup));
                userDetails.setGroups(new ArrayList<>(userDetailsGroupMap.values()));

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
                List<String> permissions = new ArrayList<>();
                userDetails.setPermissions(permissions);
                if(rs.getString("permissions") != null){
                    for(String permission : rs.getString("permissions").split(",")){
                        if(!permissions.contains(permission)){
                            permissions.add(permission);
                        }
                    }
                    for(String permission : rs.getString("permissions_user_roles").split(",")){
                        if(!permissions.contains(permission)){
                            permissions.add(permission);
                        }
                    }
                }
                if(rs.getString("user_roles") != null){
                    userDetails.setUserRoles(new ArrayList<>(Arrays.asList(rs.getString("user_roles").split(","))));
                }
            } catch (SQLException e) {
                log.error(e.getMessage());
            }
            return userDetails;
        }
    }

    class PermissionRowMapper implements RowMapper<Permission> {
        @Override
        public Permission mapRow(ResultSet rs, int rowNum) throws SQLException {
            Permission p = new Permission();
            p.setId(rs.getString("id"));
            p.setName(rs.getString("name"));
            p.setDescription(rs.getString("description"));
            return p;
        }
    }

    class UserRoleRowMapper implements RowMapper<UserRole> {
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
            userDetails.setStewardId(rs.getString("steward_id"));
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
            try {
                if (rs.getString("user_domains") != null) {
                    UUID[] array = (UUID[]) rs.getArray("user_domains").getArray();
                    userDetails.setUserDomains(new ArrayList<>(Arrays.asList(array)));
                }
            } catch (Exception e) {}
            userDetails.setPermissions(permissions.stream().collect(Collectors.toList()));
            return userDetails;
        }
    }

    @Override
    public UserDetails getPlatformUserByUsername(String username, String tenant) {
        return jdbcTemplate.query("SELECT pu.uid, pu.username, pu.display_name, pu.description, pu.email, pu.salt, pu.password_hash, pu.apikey_hash, pu.apikey_salt, " +
                        "pu.approval_status, pu.permissions, pu.user_roles, pu.current_account_status, pu.internal_user, pu.deletable, pu.authenticator, pu.created, pu.modified, pu.tenant, " +
                        "s.id as steward_id " +
                        ", array(SELECT domain_id FROM da_" + tenant + ".user_to_domain WHERE user_id=pu.uid) AS user_domains " +
                        "FROM usermgmt.platform_users pu left join da_" + tenant + ".steward s on s.user_id = pu.uid " +
                        "WHERE pu.username = ? AND pu.tenant = ?",
                new UserDetailsRowMapper(), username, tenant).stream().findFirst().orElse(null);
    }

    @Override
    public UserDetails getPlatformUserById(String userId, String tenant) {
        return jdbcTemplate.query("SELECT pu.uid, pu.username, pu.display_name, pu.description, pu.email, pu.salt, pu.password_hash, pu.apikey_hash, pu.apikey_salt, " +
                        "pu.approval_status, pu.permissions, pu.user_roles, pu.current_account_status, pu.internal_user, pu.deletable, pu.authenticator, pu.created, pu.modified, pu.tenant, " +
                        "s.id as steward_id " +
                        ", array(SELECT domain_id FROM da_" + tenant + ".user_to_domain WHERE user_id=pu.uid) AS user_domains " +
                        "FROM usermgmt.platform_users pu left join da_" + tenant + ".steward s on s.user_id = pu.uid " +
                        "WHERE pu.uid = ? AND pu.tenant = ?",
                new UserDetailsRowMapper(), Long.parseLong(userId), tenant).stream().findFirst().orElse(null);
    }

    public String getDefaultTenant() {
        return jdbcTemplate.queryForObject("SELECT id " +
                        "FROM da.tenant " +
                        "WHERE default_tenant IS TRUE LIMIT 1",
                String.class);
    }

    @Override
    public UserDetails getFullUserByUsername(String username) {
        return jdbcTemplate.query("select pu.uid, pu.username, pu.display_name, pu.description, pu.email, pu.salt, pu.password_hash, pu.apikey_hash, pu.apikey_salt, pu.approval_status, array_to_string(pu.permissions, ',', 'empty_role') as permissions, array_to_string(array_agg(ur.permissions), ',', 'empty_role') as permissions_user_roles, array_to_string(user_roles, ',', 'empty_role') as user_roles, pu.current_account_status, pu.internal_user, pu.deletable, pu.authenticator, pu.created, pu.modified, pu.tenant " +
                        "from usermgmt.platform_users pu " +
                        "   left join usermgmt.user_roles ur on ur.id = ANY(array_remove(pu.user_roles, '')::uuid[]) " +
                        "where username = ? " +
                        "group by pu.uid",
                new UserDetailsWithUserDetailsGroupRowMapper(), username).stream().findFirst().orElse(null);
    }

    @Override
    public void insertUser(String username, String tenantId) {
        jdbcTemplate.update(
                "INSERT INTO usermgmt.platform_users " +
                        "(uid, username, display_name, approval_status, current_account_status, internal_user, deletable, authenticator, created, modified, tenant) " +
                        "VALUES((SELECT MAX(uid) FROM usermgmt.platform_users) + 1, ?, ?, 'approved', 'enabled', false, false, 'external', ?, ?, ?)",
                username, username, LocalDateTime.now(), LocalDateTime.now(), tenantId
        );
    }

    @Override
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
            }, roleIds.stream().map(x -> UUID.fromString(x)).collect(Collectors.toList()).toArray());
        return res;
    }

    @Override
    public Set<String> getRolesByGroups(List<Integer> groupIds) {
        Set<String> roleIds = new HashSet<>();
        String inSql = String.join(",", Collections.nCopies(groupIds.size(), "?"));
        String query = String.format("SELECT user_roles FROM usermgmt.external_groups WHERE ID in (%s)", inSql);
        jdbcTemplate.query(query, new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    if (rs.getArray("user_roles") != null) {
                        String[] array = (String[])rs.getArray("user_roles").getArray();
                        roleIds.addAll(Arrays.asList(array));
                    }
                }
            }, groupIds.toArray()
        );
        return roleIds;
    }

    @Override
    public Set<String> getPermissionsByGroups(List<Integer> groupIdList) {
        Set<String> permissionSet = new HashSet<>();
        String inSql = String.join(",", Collections.nCopies(groupIdList.size(), "?"));

        jdbcTemplate.query(
                String.format("SELECT id, \"name\", description, created, modified, array_to_string(permissions, ',', 'empty_role') as permissions, user_roles, \"attributes\", tenant " +
                        "FROM usermgmt.external_groups " +
                        "WHERE id in (%s)", inSql),
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) {
                        try {
                            permissionSet.addAll(Arrays.asList(rs.getString("permissions").replace("\"", "").split(",")));
                        } catch (SQLException e) {
                            log.error(e.getMessage());
                        }
                    }
                },
                groupIdList.toArray()
        );
        return permissionSet;
    }

    @Override
    public Map<String, Integer> getLdapGroups() {
        Map<String, Integer> ldapGroups = new HashMap<>();

        jdbcTemplate.query(
                "SELECT id, \"name\", description, created, modified, permissions, user_roles, \"attributes\", tenant " +
                        "FROM usermgmt.external_groups",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) {
                        try {
                            ldapGroups.put(rs.getString("attributes").replace("\"", ""), rs.getInt("id"));
                        } catch (SQLException e) {
                            log.error(e.getMessage());
                        }
                    }
                }
        );
        return ldapGroups;
    }

    @Override
    public SearchResponse<FlatUserDetails> searchUsers(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {
        String orderby = "name";
        if (!StringUtils.isEmpty(searchRequest.getSort()))
            orderby = searchRequest.getSort().replaceAll("[\\-\\+]", "") + ((searchRequest.getSort().contains("-")) ? " DESC" : " ASC");
        Map<String, List<Object>> wheresMap = ServiceUtils.buildWhereForSearchRequestWithJoin(searchRequest, searchableColumns);
        String where = "";
        List<Object> vals = new ArrayList<>();
        for (String key : wheresMap.keySet()) {
            where = key;
            vals = wheresMap.get(key);
        }
        String join = "";
        if (!searchRequest.getFiltersForJoin().isEmpty()) {
            join = "left join da_" + userDetails.getTenant() + "." + searchRequest.getFiltersForJoin().get(0).getTable() + " "
                    + "tbl2 on tbl1." + searchRequest.getFiltersForJoin().get(0).getOnColumn() + "=tbl2." + searchRequest.getFiltersForJoin().get(0).getEqualColumn() +" ";
            for (SearchColumnForJoin searchColumnForJoin : searchableColumnsForJoin) {
                if (searchRequest.getFiltersForJoin().get(0).getTable().equalsIgnoreCase(searchColumnForJoin.getTable()) &&
                        searchRequest.getFiltersForJoin().get(0).getColumn().equalsIgnoreCase(searchColumnForJoin.getColumn())) {
                    if (searchColumnForJoin.getColumnType().equals(SearchColumn.ColumnType.Text)) {
                        vals.add("%" + searchRequest.getFiltersForJoin().get(0).getValue() + "%");
                    } else {
                        vals.add(searchRequest.getFiltersForJoin().get(0).getValue());
                    }
                }
            }
            where = ServiceUtils.appendWhereForSearchRequestWithJoin(where, searchRequest, searchableColumnsForJoin);
        }

        String subQuery = "select u.*, s.id as steward_id from usermgmt.platform_users u left join da_" + userDetails.getTenant()
                + ".steward s on u.uid = s.user_id where u.tenant = '" + userDetails.getTenant() + "' ";
        String queryForItems = "SELECT tbl1.* FROM (" + subQuery + ") as tbl1 " + join
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<UserDetails> items = jdbcTemplate.query(queryForItems, new UserDetailsRowMapper(), vals.toArray());

        String queryForTotal = "SELECT COUNT(tbl1.uid) FROM (select * from usermgmt.platform_users where tenant='" + userDetails.getTenant() + "') tbl1 " + join
                /*+ " LEFT JOIN usermgmt.user_roles user_roles ON user_roles.id::text = any(tbl1.user_roles) "*/
                + where;
        Long total = jdbcTemplate.queryForObject(queryForTotal, Long.class, vals.toArray());

        SearchResponse<FlatUserDetails> res = new SearchResponse<>();
        res.setCount(total.intValue());
        res.setLimit(searchRequest.getLimit());
        res.setOffset(searchRequest.getOffset());
        List<FlatUserDetails> flatItems = items.stream().map(FlatUserDetails::new).collect(Collectors.toList());

        flatItems.forEach(x -> x.setUserRoles(getRolesNames(x.getUserRoles())));

        int num = searchRequest.getOffset() + 1;
        for (FlatUserDetails fs : flatItems)
            fs.setNum(num++);

        res.setItems(flatItems);

        return res;
    }

    @Override
    public SearchResponse<FlatPermission> searchPermissions(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails){
        String orderby = "name";
        if (!StringUtils.isEmpty(searchRequest.getSort()))
            orderby = searchRequest.getSort().replaceAll("[\\-\\+]", "") + ((searchRequest.getSort().contains("-")) ? " DESC" : " ASC");
        Map<String, List<Object>> wheresMap = ServiceUtils.buildWhereForSearchRequestWithJoin(searchRequest, searchableColumns);
        String where = "";
        List<Object> vals = new ArrayList<>();
        for (String key : wheresMap.keySet()) {
            where = key;
            vals = wheresMap.get(key);
        }
        String join = "";
        if (!searchRequest.getFiltersForJoin().isEmpty()) {
            join = "left join da_" + userDetails.getTenant() + "." + searchRequest.getFiltersForJoin().get(0).getTable() + " "
                    + "tbl2 on tbl1." + searchRequest.getFiltersForJoin().get(0).getOnColumn() + "=tbl2." + searchRequest.getFiltersForJoin().get(0).getEqualColumn() +" ";
            for (SearchColumnForJoin searchColumnForJoin : searchableColumnsForJoin) {
                if (searchRequest.getFiltersForJoin().get(0).getTable().equalsIgnoreCase(searchColumnForJoin.getTable()) &&
                        searchRequest.getFiltersForJoin().get(0).getColumn().equalsIgnoreCase(searchColumnForJoin.getColumn())) {
                    if (searchColumnForJoin.getColumnType().equals(SearchColumn.ColumnType.Text)) {
                        vals.add("%" + searchRequest.getFiltersForJoin().get(0).getValue() + "%");
                    } else {
                        vals.add(searchRequest.getFiltersForJoin().get(0).getValue());
                    }
                }
            }
            where = ServiceUtils.appendWhereForSearchRequestWithJoin(where, searchRequest, searchableColumnsForJoin);
        }

        String subQuery = "SELECT r.* FROM usermgmt.permissions r ";
        String queryForItems = "SELECT tbl1.* FROM (" + subQuery + ") tbl1 " + join
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<Permission> items = jdbcTemplate.query(queryForItems, new PermissionRowMapper(), vals.toArray());

        String queryForTotal = "SELECT COUNT(tbl1.id) FROM (" + subQuery + ") tbl1 " + join
                + where;
        final int[] count = {0};
        jdbcTemplate.query(
                queryForTotal,
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        count[0] = rs.getInt("count");
                    }
                },
                vals.toArray()
        );

        int total = count[0];

        SearchResponse<FlatPermission> res = new SearchResponse<>();
        res.setCount(total);
        res.setLimit(searchRequest.getLimit());
        res.setOffset(searchRequest.getOffset());
        List<FlatPermission> flatItems = items.stream().map(FlatPermission::new).collect(Collectors.toList());

        int num = searchRequest.getOffset() + 1;
        for (FlatPermission fs : flatItems)
            fs.setNum(num++);

        res.setItems(flatItems);

        return res;
    }
    @Override
    public SearchResponse<FlatExternalGroup> searchExternalGroup(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails){
        String orderby = "name";
        if (!StringUtils.isEmpty(searchRequest.getSort()))
            orderby = searchRequest.getSort().replaceAll("[\\-\\+]", "") + ((searchRequest.getSort().contains("-")) ? " DESC" : " ASC");
        Map<String, List<Object>> wheresMap = ServiceUtils.buildWhereForSearchRequestWithJoin(searchRequest, searchableColumns);
        String where = "";
        List<Object> vals = new ArrayList<>();
        for (String key : wheresMap.keySet()) {
            where = key;
            vals = wheresMap.get(key);
        }
        String join = "";
        if (!searchRequest.getFiltersForJoin().isEmpty()) {
            join = "left join da_" + userDetails.getTenant() + "." + searchRequest.getFiltersForJoin().get(0).getTable() + " "
                    + "tbl2 on tbl1." + searchRequest.getFiltersForJoin().get(0).getOnColumn() + "=tbl2." + searchRequest.getFiltersForJoin().get(0).getEqualColumn() +" ";
            for (SearchColumnForJoin searchColumnForJoin : searchableColumnsForJoin) {
                if (searchRequest.getFiltersForJoin().get(0).getTable().equalsIgnoreCase(searchColumnForJoin.getTable()) &&
                        searchRequest.getFiltersForJoin().get(0).getColumn().equalsIgnoreCase(searchColumnForJoin.getColumn())) {
                    if (searchColumnForJoin.getColumnType().equals(SearchColumn.ColumnType.Text)) {
                        vals.add("%" + searchRequest.getFiltersForJoin().get(0).getValue() + "%");
                    } else {
                        vals.add(searchRequest.getFiltersForJoin().get(0).getValue());
                    }
                }
            }
            where = ServiceUtils.appendWhereForSearchRequestWithJoin(where, searchRequest, searchableColumnsForJoin);
        }

        String subQuery = "SELECT r.* FROM usermgmt.external_groups r where tenant = '" + userDetails.getTenant() + "'";
        String queryForItems = "SELECT tbl1.* FROM (" + subQuery + ") tbl1 " + join
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<ExternalGroup> items = jdbcTemplate.query(queryForItems, new ExternalGroupRowMapper(), vals.toArray());

        String queryForTotal = "SELECT COUNT(tbl1.id) FROM (" + subQuery + ") tbl1 " + join
                + where;
        final int[] count = {0};
        jdbcTemplate.query(
                queryForTotal,
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        count[0] = rs.getInt("count");
                    }
                },
                vals.toArray()
        );

        int total = count[0];

        SearchResponse<FlatExternalGroup> res = new SearchResponse<>();
        res.setCount(total);
        res.setLimit(searchRequest.getLimit());
        res.setOffset(searchRequest.getOffset());
        List<FlatExternalGroup> flatItems = items.stream().map(FlatExternalGroup::new).collect(Collectors.toList());

//        flatItems.forEach(x -> x.setPermissions(getRolesNames(x.getPermissions())));

        int num = searchRequest.getOffset() + 1;
        for (FlatExternalGroup fs : flatItems)
            fs.setNum(num++);

        res.setItems(flatItems);

        return res;
    }
    @Override
    public SearchResponse<FlatUserRole> searchRoles(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails){
        String orderby = "name";
        if (!StringUtils.isEmpty(searchRequest.getSort()))
            orderby = searchRequest.getSort().replaceAll("[\\-\\+]", "") + ((searchRequest.getSort().contains("-")) ? " DESC" : " ASC");
        Map<String, List<Object>> wheresMap = ServiceUtils.buildWhereForSearchRequestWithJoin(searchRequest, searchableColumns);
        String where = "";
        List<Object> vals = new ArrayList<>();
        for (String key : wheresMap.keySet()) {
            where = key;
            vals = wheresMap.get(key);
        }
        String join = "";
        if (!searchRequest.getFiltersForJoin().isEmpty()) {
            join = "left join da_" + userDetails.getTenant() + "." + searchRequest.getFiltersForJoin().get(0).getTable() + " "
                    + "tbl2 on tbl1." + searchRequest.getFiltersForJoin().get(0).getOnColumn() + "=tbl2." + searchRequest.getFiltersForJoin().get(0).getEqualColumn() +" ";
            for (SearchColumnForJoin searchColumnForJoin : searchableColumnsForJoin) {
                if (searchRequest.getFiltersForJoin().get(0).getTable().equalsIgnoreCase(searchColumnForJoin.getTable()) &&
                        searchRequest.getFiltersForJoin().get(0).getColumn().equalsIgnoreCase(searchColumnForJoin.getColumn())) {
                    if (searchColumnForJoin.getColumnType().equals(SearchColumn.ColumnType.Text)) {
                        vals.add("%" + searchRequest.getFiltersForJoin().get(0).getValue() + "%");
                    } else {
                        vals.add(searchRequest.getFiltersForJoin().get(0).getValue());
                    }
                }
            }
            where = ServiceUtils.appendWhereForSearchRequestWithJoin(where, searchRequest, searchableColumnsForJoin);
        }

        String subQuery = "SELECT r.* FROM usermgmt.user_roles r where tenant = '" + userDetails.getTenant() + "'";
        String queryForItems = "SELECT tbl1.* FROM (" + subQuery + ") tbl1 " + join
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<UserRole> items = jdbcTemplate.query(queryForItems, new UserRoleRowMapper(), vals.toArray());

        String queryForTotal = "SELECT COUNT(tbl1.id) FROM (" + subQuery + ") tbl1 " + join
                + where;
        final int[] count = {0};
        jdbcTemplate.query(
                queryForTotal,
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        count[0] = rs.getInt("count");
                    }
                },
                vals.toArray()
        );

        int total = count[0];

        SearchResponse<FlatUserRole> res = new SearchResponse<>();
        res.setCount(total);
        res.setLimit(searchRequest.getLimit());
        res.setOffset(searchRequest.getOffset());
        List<FlatUserRole> flatItems = items.stream().map(FlatUserRole::new).collect(Collectors.toList());

//        flatItems.forEach(x -> x.setPermissions(getRolesNames(x.getPermissions())));

        int num = searchRequest.getOffset() + 1;
        for (FlatUserRole fs : flatItems)
            fs.setNum(num++);

        res.setItems(flatItems);

        return res;
    }


    private List<String> getRolesNames(List<String> rolesIds) {
        List<String> res = new ArrayList<>();
        for (String roleId : rolesIds) {
            res.addAll(jdbcTemplate.queryForList("SELECT name FROM usermgmt.user_roles WHERE id = ?", String.class, UUID.fromString(roleId)));
        }
        return res;
    }

    @Override
    public List<UserRole> getAllRoles(String tenantId) {
        return jdbcTemplate.query("SELECT id, name, description, permissions from usermgmt.user_roles where tenant = ?",
                new UserRoleRowMapper(), tenantId);
    }

    @Override
    public List<Permission> getAllPermissions() {
        return jdbcTemplate.query("SELECT id, name, description from usermgmt.permissions  where level='tenant'",
                new PermissionRowMapper());
    }
    @Override
    public Permission getPermissionById(String permissionId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT id, name, description from usermgmt.permissions where id=?", new PermissionRowMapper(), UUID.fromString(permissionId)).stream().findFirst().orElse(null);
    }

    @Override
    public Integer createUser(UpdatableUserDetails newUserDetails, UserDetails userDetails) {
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        String sql = "INSERT INTO usermgmt.platform_users (username, display_name, description, email, password_hash, approval_status, " +
                "permissions, user_roles, current_account_status, internal_user, deletable, authenticator, created, modified, tenant) " +
                "values (?,?,?,?,?,?," +
                "?,?,?,?,?,?,?,?,?) RETURNING uid";
        Integer id = jdbcTemplate.queryForObject(sql, Integer.class,
                newUserDetails.getUsername(), newUserDetails.getDisplayName(),
                newUserDetails.getDescription(), newUserDetails.getEmail(), passwordEncoder.encode(newUserDetails.getPassword()),
                "approved",
                createStringSqlArray(newUserDetails.getPermissions()),
                createStringSqlArray(newUserDetails.getUserRolesIds()),
                "enabled", true, true, "default", ts, ts, userDetails.getTenant());
        return id;
    }

    @Override
    public void updateUser(String userId, UpdatableUserDetails updatableUserDetails, UserDetails userDetails) {
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (updatableUserDetails.getUsername() != null) {
            sets.add("username=?");
            args.add(updatableUserDetails.getUsername());
        }
        if (updatableUserDetails.getDisplayName() != null) {
            sets.add("display_name=?");
            args.add(updatableUserDetails.getDisplayName());
        }
        if (updatableUserDetails.getDescription() != null) {
            sets.add("description=?");
            args.add(updatableUserDetails.getDescription());
        }
        if (updatableUserDetails.getEmail() != null) {
            sets.add("email=?");
            args.add(updatableUserDetails.getEmail());
        }
        if (updatableUserDetails.getPassword() != null) {
            sets.add("password_hash=?");
            args.add(passwordEncoder.encode(updatableUserDetails.getPassword()));
        }
        if (updatableUserDetails.getPermissions() != null) {
            sets.add("permissions=?");
            args.add(createStringSqlArray(updatableUserDetails.getPermissions()));
        }
        if (updatableUserDetails.getUserRolesIds() != null) {
            sets.add("user_roles=?");
            args.add(createStringSqlArray(updatableUserDetails.getUserRolesIds()));
        }

        if (sets.size() > 0) {
            sets.add("modified=?");
            args.add(ts);
            args.add(Integer.parseInt(userId));
            jdbcTemplate.update("UPDATE usermgmt.platform_users SET " + StringUtils.join(sets, ", ")
                    + " WHERE uid=?", args.toArray());
        }
        if (updatableUserDetails.getUserDomains() != null) {
            //Timestamp ts = new Timestamp(new java.util.Date().getTime());
            jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".user_to_domain WHERE user_id=?", Integer.parseInt(userId));
            for (String did : updatableUserDetails.getUserDomains()) {
                jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".user_to_domain (id, user_id, domain_id, created, creator, modified, modifier) VALUES (?,?,?,?,?,?,?)",
                        UUID.randomUUID(), Integer.parseInt(userId), UUID.fromString(did), ts, userDetails.getUid(), ts, userDetails.getUid());
            }
        }
    }

    private java.sql.Array createStringSqlArray(List<String> list){
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

    @Override
    public Boolean existsRole(String roleId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT ID FROM usermgmt.user_roles WHERE id = ? AND tenant = ?) AS EXISTS",
                Boolean.class, UUID.fromString(roleId), userDetails.getTenant());
    }

    @Override
    public Boolean existsRoleByName(String roleName, String currentId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT ID FROM usermgmt.user_roles WHERE name = ? and tenant = ? "
                        + (currentId != null ? " and id <> ? " : "")
                        + ") AS EXISTS",
                Boolean.class,
                currentId != null ? new Object[]{roleName, userDetails.getTenant(), UUID.fromString(currentId)}
                        : new Object[]{roleName, userDetails.getTenant()});
    }

    @Override
    public Boolean existsUserWithRole(String roleId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT UID FROM usermgmt.platform_users WHERE " +
                "? = ANY(user_roles)) AS EXISTS", Boolean.class, roleId);
    }

    @Override
    public Boolean existsExternalGroupWithRole(String roleId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT id FROM usermgmt.external_groups WHERE ? = ANY(user_roles)) AS EXISTS",
                Boolean.class, roleId);
    }

    @Override
    public Boolean existsPermission(String permission) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT ID FROM usermgmt.permissions where name=? and level='tenant') as exists",
                Boolean.class, permission);
    }

    @Override
    public UserRole getRoleById(String roleId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT id, name, description, permissions from usermgmt.user_roles " +
                "where id = ? and tenant = ?",
                new UserRoleRowMapper(), UUID.fromString(roleId), userDetails.getTenant())
                .stream().findFirst().orElse(null);
    }

    @Override
    public String createRole(UpdatableUserRole newUserRole, UserDetails userDetails) {
        UUID newId = UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        jdbcTemplate.update("INSERT INTO usermgmt.user_roles (id, name, description, permissions, created, modified, tenant) " +
                "values (?, ?, ?, ?, ?, ?, ?)", newId,
                newUserRole.getName(),
                newUserRole.getDescription(),
                createStringSqlArray(newUserRole.getPermissions()),
                ts, ts, userDetails.getTenant());
        return newId.toString();
    }

    @Override
    public void updateRole(String roleId, UpdatableUserRole userRole, UserDetails userDetails) {
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (userRole.getName() != null) {
            sets.add("name=?");
            args.add(userRole.getName());
        }
        if (userRole.getDescription() != null) {
            sets.add("description=?");
            args.add(userRole.getDescription());
        }
        if (userRole.getPermissions() != null) {
            sets.add("permissions = ?");
            args.add(createStringSqlArray(userRole.getPermissions()));
        }
        if (sets.size() > 0) {
            sets.add("modified=?");
            args.add(ts);
            args.add(UUID.fromString(roleId));
            jdbcTemplate.update("UPDATE usermgmt.user_roles SET " + StringUtils.join(sets, ", ")
                    + " WHERE id=?", args.toArray());
        }
    }

    @Override
    public Boolean existsUserByUsername(String username, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS (SELECT UID FROM usermgmt.platform_users WHERE username=? AND tenant=?) AS EXISTS",
                Boolean.class, username, userDetails.getTenant());
    }

    @Override
    public Boolean existsUserById(String uid, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS (SELECT UID FROM usermgmt.platform_users WHERE uid = ? and tenant = ?) AS EXISTS",
                Boolean.class, Integer.parseInt(uid), userDetails.getTenant());
    }

    @Override
    public void deleteUser(String userId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM usermgmt.platform_users where uid = ? and tenant = ?",
                Integer.parseInt(userId), userDetails.getTenant());
    }

    @Override
    public void deleteRole(String roleId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM usermgmt.user_roles where id = ? and tenant = ?",
                UUID.fromString(roleId), userDetails.getTenant());
    }

    @Override
    public boolean updateUserPassword(UpdatableUserPassword updatableUserPassword, UserDetails userDetails) {

        String password_hash = jdbcTemplate.queryForObject("SELECT password_hash FROM usermgmt.platform_users WHERE uid=?",
                String.class, Integer.parseInt(userDetails.getUid()));

        if (!passwordEncoder.matches(updatableUserPassword.getOldPassword(), password_hash))
            return false;

        return jdbcTemplate.update("UPDATE usermgmt.platform_users SET password_hash=? WHERE uid=?",
                passwordEncoder.encode(updatableUserPassword.getNewPassword()), Integer.parseInt(userDetails.getUid())) > 0;
    }
}