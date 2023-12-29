package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.WorkflowableMetadata;
import ru.bssg.lottabyte.core.model.datatype.DataType;
import ru.bssg.lottabyte.core.model.datatype.DataTypeEntity;
import ru.bssg.lottabyte.core.model.datatype.FlatDataType;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
@Slf4j
public class DataTypeRepository extends GenericArtifactRepository<DataType> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = { };

    public DataTypeRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.datatype.name(), extFields);
        super.setMapper(new DataTypeRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class DataTypeRowMapper implements RowMapper<DataType> {
        @Override
        public DataType mapRow(ResultSet rs, int rowNum) throws SQLException {
            DataTypeEntity e = new DataTypeEntity();
            e.setName(rs.getString("name"));

            return new DataType(e,
                    new WorkflowableMetadata(rs, e.getArtifactType()));
        }
    }

    public static class FlatDataTypeRowMapper implements RowMapper<FlatDataType> {
        @Override
        public FlatDataType mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatDataType fi = new FlatDataType();
            fi.setId(rs.getString("id"));
            fi.setName(rs.getString("name"));
            return fi;
        }
    }


    public SearchResponse<FlatDataType> searchDataType(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns,
                                                       SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> vals = searchSQLParts.getWhereValues();

        String subQuery = "select * from da_" + userDetails.getTenant() + ".datatype ";
        if (userDetails.getStewardId() != null && searchRequest.getLimitSteward() != null
                && searchRequest.getLimitSteward()) {
            subQuery = subQuery + QueryHelper.getWhereIdInQuery(ArtifactType.datatype, userDetails);
        }

        subQuery = "SELECT sq.* FROM (" + subQuery + ") as sq  ";

        String queryForItems = "SELECT distinct tbl1.* FROM (" + subQuery + ") tbl1 " + join
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatDataType> flatItems = jdbcTemplate.query(queryForItems, new FlatDataTypeRowMapper(),
                vals.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") tbl1 " + join
                + where;
        Long total = jdbcTemplate.queryForObject(queryForTotal, Long.class, vals.toArray());

        SearchResponse<FlatDataType> res = new SearchResponse<>(total.intValue(), searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }
}
