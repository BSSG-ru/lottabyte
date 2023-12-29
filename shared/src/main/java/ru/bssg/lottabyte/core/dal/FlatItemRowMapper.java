package ru.bssg.lottabyte.core.dal;

import org.springframework.jdbc.core.RowMapper;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

public class FlatItemRowMapper<T extends FlatModeledObject> implements RowMapper<T> {
    private Supplier<T> supplier;

    public FlatItemRowMapper(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        T t = supplier.get();
        t.setId(rs.getString("id"));
        t.setName(rs.getString("name"));
        t.setDescription(rs.getString("description"));
        try{
            rs.findColumn("version_id");
            if (rs.getObject("version_id") != null)
                t.setVersionId(rs.getInt("version_id"));
        }catch(SQLException ignored){}
        t.setModified(rs.getTimestamp("modified").toLocalDateTime());
        try{
            rs.findColumn("has_access");
            if (rs.getObject("has_access") != null)
                t.setHasAccess(rs.getBoolean("has_access"));
        }catch(SQLException ignored){}
        return t;
    }

}
