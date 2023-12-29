package ru.bssg.lottabyte.core.util;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static ru.bssg.lottabyte.core.i18n.Message.LBE03001;

public class ServiceUtils {

    public static void validateSearchRequestState(SearchRequest searchRequest, UserDetails userDetails) throws LottabyteException {
        if (searchRequest.getState() != null && !EnumUtils.isValidEnum(ArtifactState.class, searchRequest.getState()))
            throw new LottabyteException(LBE03001, userDetails.getLanguage(), searchRequest.getState());
    }

    public static void validateSearchRequestWithJoinState(SearchRequestWithJoin searchRequest, UserDetails userDetails) throws LottabyteException {
        if (searchRequest.getState() != null && !EnumUtils.isValidEnum(ArtifactState.class, searchRequest.getState()))
            throw new LottabyteException(LBE03001, userDetails.getLanguage(), searchRequest.getState());
    }

    public static void validateSearchRequest(SearchRequest request, SearchColumn[] searchableColumns,
            UserDetails userDetails) throws LottabyteException {
        String sortField = StringUtils.isEmpty(request.getSort()) ? null : request.getSort().replaceAll("[\\+\\-]", "");

        if ( sortField != null && Arrays.stream(searchableColumns).noneMatch(x -> x.getColumn().equals(sortField)))
            throw new LottabyteException(Message.LBE00002, userDetails.getLanguage(), sortField);

        if (request.getFilters() != null) {
            for (SearchRequestFilter srf : request.getFilters()) {
                if (Arrays.stream(searchableColumns).noneMatch(x -> x.getColumn().equals(srf.getColumn())))
                    throw new LottabyteException(Message.LBE00002, userDetails.getLanguage(), srf.getColumn());
            }
        }
    }

    public static void validateSearchRequestWithJoin(SearchRequestWithJoin request, SearchColumn[] searchableColumns,
            SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) throws LottabyteException {
        String sortField = StringUtils.isEmpty(request.getSort()) ? null : request.getSort().replaceAll("[\\+\\-]", "");

        if ( sortField != null && Arrays.stream(searchableColumns).noneMatch(x -> x.getColumn().equals(sortField)))
            throw new LottabyteException(Message.LBE00002, userDetails.getLanguage(), sortField);

        if (request.getFilters() != null) {
            for (SearchRequestFilter srf : request.getFilters()) {
                if (Arrays.stream(searchableColumns).noneMatch(x -> x.getColumn().equals(srf.getColumn())))
                    throw new LottabyteException(Message.LBE00002, userDetails.getLanguage(), srf.getColumn());
            }
        }

        if (request.getFiltersForJoin() != null) {
            for (SearchRequestFilterForJoin srf : request.getFiltersForJoin()) {
                if (Arrays.stream(searchableColumnsForJoin).noneMatch(x -> x.getColumn().equals(srf.getColumn()))) {
                    throw new LottabyteException(Message.LBE00013, userDetails.getLanguage(), srf.getColumn());
                }
                if (Arrays.stream(searchableColumnsForJoin).noneMatch(x -> x.getTable().equals(srf.getTable()))) {
                    throw new LottabyteException(Message.LBE00014, userDetails.getLanguage(), srf.getTable());
                }
                if (Arrays.stream(searchableColumnsForJoin).noneMatch(x -> x.getOnColumn().equals(srf.getOnColumn()))) {
                    throw new LottabyteException(Message.LBE00015, userDetails.getLanguage(), srf.getOnColumn());
                }
                if (Arrays.stream(searchableColumnsForJoin).noneMatch(x -> x.getEqualColumn().equals(srf.getEqualColumn()))) {
                    throw new LottabyteException(Message.LBE00016, userDetails.getLanguage(), srf.getEqualColumn());
                }
            }
        }
    }

    public static String escForLike(String s) {
        return s.replaceAll("_", "\\\\_")
                .replaceAll("%", "\\\\%")
                .replaceAll("'", "''");
    }

    public static String getFilterSqlQuery(SearchRequestFilter searchRequestFilter, SearchColumn[] searchableColumns) {
        if (!StringUtils.isEmpty(searchRequestFilter.getValue())) {
            Optional<SearchColumn> sc = Arrays.stream(searchableColumns)
                    .filter(x -> x.getColumn().equals(searchRequestFilter.getColumn())).findFirst();
            boolean needValueQuotes = (sc.isPresent() && (sc.get().getColumnType().equals(SearchColumn.ColumnType.Text)
                    || sc.get().getColumnType().equals(SearchColumn.ColumnType.UUID)));

            String columnVariable;
            if (sc.isPresent() && sc.get().getColumnType().equals(SearchColumn.ColumnType.Timestamp))
                columnVariable = "to_char(" + searchRequestFilter.getColumn() + ", 'DD.MM.YYYY')";
            else
                columnVariable = searchRequestFilter.getColumn();

            switch (searchRequestFilter.getOperator()) {
                case LIKE:
                    return "LOWER(" + columnVariable + ") LIKE '%"
                            + escForLike(searchRequestFilter.getValue().toLowerCase()) + "%'";
                case ENDS_WITH:
                    return "LOWER(" + columnVariable + ") LIKE '%"
                            + escForLike(searchRequestFilter.getValue().toLowerCase()) + "'";
                case STARTS_WITH:
                    return "LOWER(" + columnVariable + ") LIKE '"
                            + escForLike(searchRequestFilter.getValue().toLowerCase()) + "%'";
                case EQUAL:
                    return columnVariable + " = "
                            + (needValueQuotes ? "'" + searchRequestFilter.getValue().replaceAll("'", "''") + "'"
                                    : searchRequestFilter.getValue());
                case NOT_EQUAL:
                    return columnVariable + " <> "
                            + (needValueQuotes ? "'" + searchRequestFilter.getValue().replaceAll("'", "''") + "'"
                                    : searchRequestFilter.getValue());
                case LESS:
                    return columnVariable + " < "
                            + (needValueQuotes ? "'" + searchRequestFilter.getValue().replaceAll("'", "''") + "'"
                                    : searchRequestFilter.getValue());
                case MORE:
                    return columnVariable + " > "
                            + (needValueQuotes ? "'" + searchRequestFilter.getValue().replaceAll("'", "''") + "'"
                                    : searchRequestFilter.getValue());
                case LESS_OR_EQUAL:
                    return columnVariable + " <= "
                            + (needValueQuotes ? "'" + searchRequestFilter.getValue().replaceAll("'", "''") + "'"
                                    : searchRequestFilter.getValue());
                case MORE_OR_EQUAL:
                    return columnVariable + " >= "
                            + (needValueQuotes ? "'" + searchRequestFilter.getValue().replaceAll("'", "''") + "'"
                                    : searchRequestFilter.getValue());
                case ALL:
                    return columnVariable + " <@ string_to_array('"
                            + searchRequestFilter.getValue().replaceAll("'", "''") + "',',')";
                case ANY:
                    return columnVariable + " && string_to_array('"
                            + searchRequestFilter.getValue().replaceAll("'", "''") + "',',')";
                case ALL_AVAILABLE:
                    return columnVariable + " @> string_to_array('"
                            + searchRequestFilter.getValue().replaceAll("'", "''") + "',',')";
            }
        }
        return null;
    }

    public static String getSQLOperatorByRequestOperator(SearchRequestFilterOperator searchRequestFilterOperator) {
        switch (searchRequestFilterOperator) {
            case LIKE:
            case ENDS_WITH:
            case STARTS_WITH:
                return " LIKE ? ESCAPE '!'";
            case EQUAL:
                return " = ?";
            case NOT_EQUAL:
                return " <> ?";
            case LESS:
                return " < ?";
            case MORE:
                return " > ?";
            case LESS_OR_EQUAL:
                return " <= ?";
            case MORE_OR_EQUAL:
                return " >= ?";
            case ALL:
                return " <@ string_to_array(?,',')";
            case ANY:
                return " && string_to_array(?,',')";
            case ALL_AVAILABLE:
                return " @> string_to_array(?,',')";
        }
        return "=";
    }

    public static Map<String, Object> getFilterSqlQueryWithJoin(SearchRequestFilter searchRequestFilter,
            SearchColumn[] searchableColumns) {
        if (!StringUtils.isEmpty(searchRequestFilter.getValue())) {
            Optional<SearchColumn> sc = Arrays.stream(searchableColumns)
                    .filter(x -> x.getColumn().equals(searchRequestFilter.getColumn())).findFirst();
            Map<String, Object> resMap = new HashMap<>();
            String res = "";

            String columnName = searchRequestFilter.getColumn().contains(".") ? searchRequestFilter.getColumn()
                    : "tbl1." + searchRequestFilter.getColumn();

            String columnVariable;
            if (sc.isPresent() && sc.get().getColumnType().equals(SearchColumn.ColumnType.Timestamp))
                columnVariable = "to_char(" + columnName + ", 'DD.MM.YYYY')";
            else
                columnVariable = columnName;

            Object statementParameter = sc.isPresent() && sc.get().getColumnType().equals(SearchColumn.ColumnType.UUID)
                    ? UUID.fromString(searchRequestFilter.getValue())
                    : searchRequestFilter.getValue();
            String valForLike = searchRequestFilter.getValue().toLowerCase()
                    .replace("!", "!!")
                    .replace("%", "!%")
                    .replace("_", "!_")
                    .replace("[", "![");
            switch (searchRequestFilter.getOperator()) {
                case LIKE: {
                    statementParameter = "%" + valForLike + "%";
                    if (sc.isPresent() && sc.get().getColumnType().equals(SearchColumn.ColumnType.Array))
                        res = "LOWER(ARRAY_TO_STRING(" + columnVariable + ",'|')) LIKE ? ESCAPE '!'";
                    else
                        res = "LOWER(" + columnVariable + ") LIKE ? ESCAPE '!'";
                    break;
                }
                case ENDS_WITH: {
                    statementParameter = "%" + valForLike;
                    res = "LOWER(" + columnVariable + ") LIKE ? ESCAPE '!'";
                    break;
                }
                case STARTS_WITH: {
                    statementParameter = valForLike + "%";
                    res = "LOWER(" + columnVariable + ") LIKE ? ESCAPE '!'";
                    break;
                }
                case EQUAL:
                    res = columnVariable + " = ?";
                    break;
                case NOT_EQUAL:
                    res = columnVariable + " <> ?";
                    break;
                case LESS:
                    res = columnVariable + " < ?";
                    break;
                case MORE:
                    res = columnVariable + " > ?";
                    break;
                case LESS_OR_EQUAL:
                    res = columnVariable + " <= ?";
                    break;
                case MORE_OR_EQUAL:
                    res = columnVariable + " >= ?";
                    break;
                case ALL:
                    res = columnVariable + " <@ string_to_array(?,',')";
                    break;
                case ANY:
                    res = columnVariable + " && string_to_array(?,',')";
                    break;
                case ALL_AVAILABLE:
                    res = columnVariable + " @> string_to_array(?,',')";
                    break;
            }
            resMap.put(res, statementParameter);
            return resMap;
        }
        return null;
    }

    public static String buildWhereForSearchRequest(SearchRequest searchRequest, SearchColumn[] searchableColumns,
            String prefix) {
        List<String> wheres = new ArrayList<>();

        if (!StringUtils.isEmpty(searchRequest.getGlobalQuery())) {
            List<String> parts = new ArrayList<>();
            for (SearchColumn sc : searchableColumns) {
                if (sc.getColumnType().equals(SearchColumn.ColumnType.Text))
                    parts.add("LOWER(" + (prefix == null ? "" : prefix) + sc.getColumn() + ") LIKE '%"
                            + escForLike(searchRequest.getGlobalQuery().toLowerCase()) + "%'");
            }
            wheres.add("(" + StringUtils.join(parts, " OR ") + ")");
        }

        if (searchRequest.getFilters() != null) {
            for (SearchRequestFilter searchRequestFilter : searchRequest.getFilters()) {
                String filterSqlQuery = ServiceUtils.getFilterSqlQuery(searchRequestFilter, searchableColumns);
                if (filterSqlQuery != null && !filterSqlQuery.isEmpty())
                    wheres.add(filterSqlQuery);

            }
        }

        return wheres.isEmpty() ? "" : "WHERE " + StringUtils.join(wheres, " AND ");
    }

    public static String buildWhereForSearchRequest(SearchRequest searchRequest, SearchColumn[] searchableColumns) {
        return buildWhereForSearchRequest(searchRequest, searchableColumns, null);
    }

    public static String appendWhereForSearchRequestWithJoin(String where, SearchRequestWithJoin searchRequest,
            SearchColumnForJoin[] joinColumns) {
        StringBuilder res = new StringBuilder();
        res.append(where);
        if (where.isEmpty()) {
            res.append(" WHERE ");
        } else {
            res.append(" AND ");
        }
        String column = "tbl2." + searchRequest.getFiltersForJoin().get(0).getColumn();
        if (searchRequest.getFiltersForJoin().get(0).getOperator().equals(SearchColumn.ColumnType.Text)) {
            column = "LOWER(" + column + ")";
        }
        res.append(column);
        res.append(getSQLOperatorByRequestOperator(searchRequest.getFiltersForJoin().get(0).getOperator()));

        return res.toString();
    }

    public static Map<String, List<Object>> buildWhereForSearchRequestWithJoin(SearchRequestWithJoin searchRequest,
            SearchColumn[] searchableColumns) {
        List<String> wheres = new ArrayList<>();
        List<Object> vals = new ArrayList<>();

        if (!StringUtils.isEmpty(searchRequest.getGlobalQuery())) {
            List<String> parts = new ArrayList<>();
            for (SearchColumn sc : searchableColumns) {
                if (sc.getColumnType().equals(SearchColumn.ColumnType.Text)) {

                    if (!sc.getColumn().contains(".")) {
                        vals.add("%" + searchRequest.getGlobalQuery().replaceAll("'", "''").toLowerCase() + "%");

                        parts.add("LOWER(tbl1." + sc.getColumn() + ") LIKE ? ESCAPE '!'");
                    }
                }
            }
            wheres.add("(" + StringUtils.join(parts, " OR ") + ")");
        }

        if (searchRequest.getFilters() != null) {
            for (SearchRequestFilter searchRequestFilter : searchRequest.getFilters()) {
                Map<String, Object> wheresMap = ServiceUtils.getFilterSqlQueryWithJoin(searchRequestFilter,
                        searchableColumns);
                if (wheresMap != null) {
                    for (String key : wheresMap.keySet()) {
                        wheres.add(key);
                        vals.add(wheresMap.get(key));
                    }
                }
            }
        }
        Map<String, List<Object>> resMap = new HashMap<>();
        resMap.put(wheres.isEmpty() ? "" : "WHERE " + StringUtils.join(wheres, " AND "), vals);
        return resMap;
    }

    public static String buildJoinForSearchRequestWithJoin(SearchRequestWithJoin searchRequest,
            UserDetails userDetails) {
        String join = "";
        if (!searchRequest.getFiltersForJoin().isEmpty()) {
            join = "left join da_" + userDetails.getTenant() + "." + searchRequest.getFiltersForJoin().get(0).getTable()
                    + " "
                    + "tbl2 on tbl1." + searchRequest.getFiltersForJoin().get(0).getOnColumn() + "=tbl2."
                    + searchRequest.getFiltersForJoin().get(0).getEqualColumn() + " ";
        }
        return join;
    }

    public static String updateWhereForSearchRequestWithJoin(SearchRequestWithJoin searchRequest, String where) {
        if (!searchRequest.getFiltersForJoin().isEmpty()) {
            if (where.isEmpty()) {
                where = " WHERE tbl2." + searchRequest.getFiltersForJoin().get(0).getColumn() + " = "
                        + searchRequest.getFiltersForJoin().get(0).getValue();
            } else {
                where = where + " AND tbl2." + searchRequest.getFiltersForJoin().get(0).getColumn() + " = "
                        + searchRequest.getFiltersForJoin().get(0).getValue();
            }
        }
        return where;
    }

    public static int getTotalForSearchRequestWithJoinAndSubQuery(JdbcTemplate jdbcTemplate, UserDetails userDetails,
            String subQuery, String join, String where, List<Object> vals) {
        String queryForTotal = "SELECT COUNT(tbl1.id) FROM (" + subQuery + ") as tbl1 " + join + where;
        final int[] count = { 0 };
        jdbcTemplate.query(
                queryForTotal,
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        count[0] = rs.getInt("count");
                    }
                },
                vals.toArray());
        return count[0];
    }

    public static int getTotalForSearchRequestWithJoin(JdbcTemplate jdbcTemplate, UserDetails userDetails,
            String table, String join, String where, List<Object> vals) {
        return getTotalForSearchRequestWithJoinAndSubQuery(jdbcTemplate, userDetails,
                "SELECT da_" + userDetails.getTenant() + "." + table, join, where, vals);
    }
}
