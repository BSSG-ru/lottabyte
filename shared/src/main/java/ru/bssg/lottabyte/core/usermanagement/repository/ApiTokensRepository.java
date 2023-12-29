package ru.bssg.lottabyte.core.usermanagement.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.dataasset.UpdatableDataAssetEntity;
import ru.bssg.lottabyte.core.usermanagement.model.ApiDataForLongPeriodToken;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class ApiTokensRepository {
    public final JdbcTemplate jdbcTemplate;

    public ApiTokensRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Boolean longPeriodTokenInBlacklistExist(String apiTokenId) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT 1 FROM usermgmt.api_tokens_blacklist WHERE api_token_id=?)", Boolean.class, UUID.fromString(apiTokenId));
    }

    public Integer createApiToken(UserDetails apiDataForLongPeriodToken, UserDetails userDetails) {
        Timestamp currentTimestamp = new Timestamp(new java.util.Date().getTime());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 1);
        Timestamp timestamp = new Timestamp(cal.getTimeInMillis());
        String query = "INSERT INTO usermgmt.api_tokens " +
                "(name, description, valid_till, created, creator, modified, modifier, permissions, user_roles, tenant) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        return jdbcTemplate.queryForObject(query, Integer.class, apiDataForLongPeriodToken.getSystemName(), apiDataForLongPeriodToken.getDescription(), timestamp,
                currentTimestamp, userDetails.getUid(), currentTimestamp, userDetails.getUid(),
                createStringSqlArray(apiDataForLongPeriodToken.getPermissions()),
                createStringSqlArray(apiDataForLongPeriodToken.getUserRoles()), userDetails.getTenant());
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
