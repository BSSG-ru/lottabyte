package ru.bssg.lottabyte.usermanagement.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.model.UserRole;
import ru.bssg.lottabyte.core.usermanagement.model.group.ExternalGroup;
import ru.bssg.lottabyte.core.usermanagement.model.group.ExternalGroupEntity;
import ru.bssg.lottabyte.core.usermanagement.model.group.UpdatableExternalGroupEntity;
import ru.bssg.lottabyte.usermanagement.repository.ExternalGroupRepository;
import ru.bssg.lottabyte.usermanagement.repository.UserRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@Repository
public class ExternalGroupRepositoryImpl implements ExternalGroupRepository {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ExternalGroupRepositoryImpl(UserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class ExternalGroupRowMapper implements RowMapper<ExternalGroup> {
        @Override
        public ExternalGroup mapRow(ResultSet rs, int rowNum) throws SQLException {
            ExternalGroupEntity externalGroupEntity = new ExternalGroupEntity();
            externalGroupEntity.setName(rs.getString("name"));
            externalGroupEntity.setAttributes(rs.getString("attributes"));
            externalGroupEntity.setTenant(rs.getString("tenant"));
            externalGroupEntity.setDescription(rs.getString("description"));
            if (rs.getArray("permissions") != null) {
                String[] array = (String[])rs.getArray("permissions").getArray();
                externalGroupEntity.setPermissions(new ArrayList<>(Arrays.asList(array)));
            }
            if (rs.getArray("user_roles") != null) {
                String[] array = (String[])rs.getArray("user_roles").getArray();
                externalGroupEntity.setUserRoles(new ArrayList<>(Arrays.asList(array)));
            }

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setName(rs.getString("name"));
            return new ExternalGroup(externalGroupEntity, md);
        }
    }

    @Override
    public ExternalGroup getExternalGroupById(String groupId, UserDetails userDetails) {
        ExternalGroup externalGroup =  jdbcTemplate.query("SELECT * " +
                        "FROM usermgmt.external_groups " +
                        "WHERE id = ? and tenant = ?",
                new ExternalGroupRowMapper(), Integer.parseInt(groupId), userDetails.getTenant()).stream().findFirst().orElse(null);
//        if(externalGroup != null) {
//            List<String> roleNameList = new ArrayList<>();
//            if(externalGroup.getEntity().getUserRoles() != null && !externalGroup.getEntity().getUserRoles().isEmpty()){
//                for(String rolesId : externalGroup.getEntity().getUserRoles()){
//                    UserRole userRole = userRepository.getRoleById(rolesId, userDetails);
//                    if(userRole != null)
//                        roleNameList.add(userRole.getName());
//                }
//                externalGroup.getEntity().setUserRoles(roleNameList);
//            }
//        }
        return externalGroup;
    }
    @Override
    public void deleteExternalGroup(String groupId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM usermgmt.external_groups where id = ? and tenant = ?",
                Integer.parseInt(groupId), userDetails.getTenant());
    }
    @Override
    public void updateExternalGroup(String groupId, UpdatableExternalGroupEntity updatableExternalGroupEntity) {
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (updatableExternalGroupEntity.getName() != null) {
            sets.add("name=?");
            args.add(updatableExternalGroupEntity.getName());
        }
        if (updatableExternalGroupEntity.getDescription() != null) {
            sets.add("description=?");
            args.add(updatableExternalGroupEntity.getDescription());
        }
        if (updatableExternalGroupEntity.getAttributes() != null) {
            sets.add("attributes=?");
            args.add(updatableExternalGroupEntity.getAttributes());
        }
        if (updatableExternalGroupEntity.getPermissions() != null) {
            sets.add("permissions=?");
            args.add(createStringSqlArray(updatableExternalGroupEntity.getPermissions()));
        }
        if (updatableExternalGroupEntity.getUserRoles() != null) {
            sets.add("user_roles=?");
            args.add(createStringSqlArray(updatableExternalGroupEntity.getUserRoles()));
        }
        if (sets.size() > 0) {
            sets.add("modified=?");
            args.add(ts);
            args.add(Integer.parseInt(groupId));
            jdbcTemplate.update("UPDATE usermgmt.external_groups SET " + StringUtils.join(sets, ", ")
                    + " WHERE id=?", args.toArray());
        }
    }

    @Override
    public Integer createExternalGroup(UpdatableExternalGroupEntity updatableExternalGroupEntity, UserDetails userDetails) {
        Timestamp ts = new Timestamp(new java.util.Date().getTime());

        String query = "INSERT INTO usermgmt.external_groups " +
                "(\"name\", description, created, modified, permissions, user_roles, \"attributes\", tenant) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        return jdbcTemplate.queryForObject(query, Integer.class, updatableExternalGroupEntity.getName(),
                updatableExternalGroupEntity.getDescription(),
                ts, ts,
                createStringSqlArray(updatableExternalGroupEntity.getPermissions()),
                createStringSqlArray(updatableExternalGroupEntity.getUserRoles()),
                updatableExternalGroupEntity.getAttributes(), userDetails.getTenant());
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
}
