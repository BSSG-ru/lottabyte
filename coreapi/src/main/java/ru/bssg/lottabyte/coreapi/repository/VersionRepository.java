package ru.bssg.lottabyte.coreapi.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.Timestamp;

@Repository
@Slf4j
@RequiredArgsConstructor
public class VersionRepository {
    private final JdbcTemplate jdbcTemplate;

    public String getDDLByVersion(Integer version) {
        return jdbcTemplate.queryForObject("SELECT ddl FROM da.pg_versions WHERE version = ?", String.class, version);
    }

    public String getSchemaByVersion(Integer version) {
        return jdbcTemplate.queryForObject("SELECT schema FROM da.elastic_versions WHERE version = ?", String.class, version);
    }

    public Integer getLastVersionByType(String type) {
        return jdbcTemplate.queryForObject("SELECT version FROM da.versions WHERE type = ? ORDER BY version DESC LIMIT 1", Integer.class, type);
    }
}
