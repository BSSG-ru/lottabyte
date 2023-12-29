package ru.bssg.lottabyte.coreapi.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Connection;

@Repository
@Slf4j
@RequiredArgsConstructor
public class AdminRepository {
    public final JdbcTemplate jdbcTemplate;
    public final Connection connection;

    public void request(String ddl) {
        jdbcTemplate.execute(ddl);
    }
}
