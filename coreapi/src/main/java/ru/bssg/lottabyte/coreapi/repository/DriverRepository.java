package ru.bssg.lottabyte.coreapi.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.dataasset.UpdatableDataAssetEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.Timestamp;
import java.util.UUID;

@Repository
@Slf4j
@RequiredArgsConstructor
public class DriverRepository {
    private final JdbcTemplate jdbcTemplate;

    public String insertDriver(String fileName, UserDetails userDetails) {
        UUID newId = java.util.UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        String query = "INSERT INTO da_" + userDetails.getTenant() + ".driver " +
                "(id, file_name, version_id, created, creator, modified, modifier) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(query, newId, fileName, 0,
                ts, userDetails.getUid(), ts, userDetails.getUid());
        return newId.toString();
    }
}
