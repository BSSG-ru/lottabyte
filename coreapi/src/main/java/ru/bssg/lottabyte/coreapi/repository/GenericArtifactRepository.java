package ru.bssg.lottabyte.coreapi.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.Entity;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchRequest;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchSQLParts;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;

import java.util.*;
import java.util.stream.Collectors;

public abstract class GenericArtifactRepository<T extends ModeledObject<? extends Entity>> {

    protected JdbcTemplate jdbcTemplate;
    protected RowMapper<T> mapper;
    protected String tableName;
    protected String[] extFields;
    protected String[] defaultFields = {"id","name","description",
            "created","creator","modified","modifier"};
    protected String[] historyFields = {"history_id","history_start","history_end"};

    public GenericArtifactRepository(JdbcTemplate jdbcTemplate, String tableName, String[] extFields) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        this.extFields = extFields.clone();
    }

    public void setMapper(RowMapper<T> mapper) {
        this.mapper = mapper;
    }

    private String getFullTableName(UserDetails userDetails) {
        return (userDetails == null ? "da." + tableName : "da_" + userDetails.getTenant() + "." + tableName);
    }

    public T getById(String id, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM " + getFullTableName(userDetails) + " WHERE id=?",
                mapper, UUID.fromString(id)).stream().findFirst().orElse(null);
    }

    public T getByIdAndState(String id, String state, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM " + getFullTableName(userDetails) + " WHERE id=? AND state=?",
                mapper, UUID.fromString(id), state).stream().findFirst().orElse(null);
    }

    public List<T> getBy(String field, Object val, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM " + getFullTableName(userDetails) + " WHERE " + field + "=?",
                mapper, val);
    }

    public Boolean existsById(String id, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT id FROM " + getFullTableName(userDetails) + " WHERE id=?) AS exists", Boolean.class, UUID.fromString(id));
    }

    public Boolean existsByIdAndPublished(String id, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT EXISTS(SELECT id FROM " + getFullTableName(userDetails) + " WHERE id=? AND state=?) AS exists", Boolean.class, UUID.fromString(id), ArtifactState.PUBLISHED.name());
    }

    public T getByIdAndStation(String id, ArtifactState artifactState, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM " + getFullTableName(userDetails) + " WHERE id = ? and state = ? ",
                mapper, UUID.fromString(id), artifactState.toString()).stream().findFirst().orElse(null);
    }
    public void deleteById(String id, UserDetails userDetails) {
        String query = "DELETE FROM " + getFullTableName(userDetails) +
                " WHERE id=?;";
        jdbcTemplate.update(query, UUID.fromString(id));
    }

    public PaginatedArtifactList<T> getAllPaginated(Integer offset, Integer limit, String url, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM " + getFullTableName(userDetails), Integer.class);
        String query = "SELECT * FROM " + getFullTableName(userDetails) + " offset ? limit ? ";
        List<T> res = jdbcTemplate.query(query, mapper, offset, limit);

        return new PaginatedArtifactList<>(res, offset, limit, total, url);
    }

    public PaginatedArtifactList<T> getAllPaginated(Integer offset, Integer limit, String url, ArtifactState artifactState, UserDetails userDetails) {
        int total = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM " + getFullTableName(userDetails)
                + " WHERE STATE = ?", Integer.class, artifactState.toString());
        String query = "SELECT * FROM " + getFullTableName(userDetails)
                + " WHERE state = ? "
                + " offset ? limit ? ";
        List<T> res = jdbcTemplate.query(query, mapper, artifactState.toString(), offset, limit);

        return new PaginatedArtifactList<>(res, offset, limit, total, url);
    }


}
