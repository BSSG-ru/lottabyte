package ru.bssg.lottabyte.coreapi.util;

import java.sql.ResultSet;
import java.sql.SQLException;

public class JDBCUtil {

    public static Integer getInt(ResultSet rs, String fieldName) throws SQLException {
        return rs.getObject(fieldName) != null ? rs.getInt(fieldName) : null;
    }

}
