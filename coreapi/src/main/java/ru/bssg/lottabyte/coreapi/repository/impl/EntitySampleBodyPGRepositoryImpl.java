package ru.bssg.lottabyte.coreapi.repository.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.IEntitySampleBodyRepository;

import java.sql.*;
import java.util.*;

@Repository
@Slf4j
public class EntitySampleBodyPGRepositoryImpl implements IEntitySampleBodyRepository {
    private final JdbcTemplate jdbcTemplate;
    public EntitySampleBodyPGRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    @Override
    public String getEntitySampleBodyById(String entityId, UserDetails userDetails) {
        final String[] entitySampleBody = new String[1];

        String query = "SELECT entity_sample_id, sample_body  " +
                "FROM da_" + userDetails.getTenant() + ".entity_sample_body " +
                "where entity_sample_id = ? ";

        jdbcTemplate.query(
                query,
                new RowCallbackHandler() {
                    @SneakyThrows
                    @Override
                    public void processRow(ResultSet rs) {
                        entitySampleBody[0] = rs.getString("sample_body");
                    }
                },
                UUID.fromString(entityId)
        );
        return entitySampleBody[0];
    }

    @Override
    public void createEntitySampleBody(String customAttributeDefElementId, String sampleBody, UserDetails userDetails) throws LottabyteException {}

    @Override
    public void deleteEntitySampleBodyById(String sampleBodyId, UserDetails userDetails) throws LottabyteException {}
}
