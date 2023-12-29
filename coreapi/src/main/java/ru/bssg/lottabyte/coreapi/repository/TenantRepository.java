package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.tenant.Tenant;
import ru.bssg.lottabyte.core.model.tenant.TenantEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class TenantRepository extends GenericArtifactRepository<Tenant> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = {};

    public TenantRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.tenant.name(), extFields);
        super.setMapper(new TenantRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    static class TenantRowMapper implements RowMapper<Tenant> {

        @Override
        public Tenant mapRow(ResultSet rs, int rowNum) throws SQLException {
            Tenant tenant = null;

            TenantEntity tenantEntity = new TenantEntity();
            tenantEntity.setName(rs.getString("name"));
            tenantEntity.setDefaultTenant(rs.getBoolean("default_tenant"));
            tenantEntity.setDomain(rs.getString("domain"));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));
            md.setName(rs.getString("name"));
            md.setArtifactType(tenantEntity.getArtifactType().toString());

            try {
                tenant = new Tenant(tenantEntity, md);
            } catch (LottabyteException e) {
                log.error(e.getMessage(), e);
            }
            return tenant;
        }
    }

    public Integer createTenant(String tenantName, String domainName, UserDetails userDetails) {
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        String query = "INSERT INTO da.tenant " +
                "(id, \"name\", \"domain\", default_tenant, created, creator, modified, modifier) " +
                "VALUES((SELECT MAX( id ) + 1 FROM da.tenant), ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        return jdbcTemplate.queryForObject(query, Integer.class, tenantName, domainName, false,
                ts, userDetails.getUid(), ts, userDetails.getUid());

    }
    public void deleteTenant(Integer id) {
        String query = "DELETE FROM da.tenant " +
                "WHERE id = ?";
        jdbcTemplate.update(query, id);
    }

    public Tenant getTenantByName(String name) {
        List<Tenant> tenantList = jdbcTemplate.query("SELECT * FROM da.tenant WHERE name=?",
                new TenantRowMapper(), name);
        return tenantList.stream().findFirst().orElse(null);
    }
    public Tenant getTenantById(Integer id) {
        List<Tenant> tenantList = jdbcTemplate.query("SELECT * FROM da.tenant WHERE id=?",
                new TenantRowMapper(), id);
        return tenantList.stream().findFirst().orElse(null);
    }
    public Tenant getTenantByDomain(String domain) {
        List<Tenant> tenantList = jdbcTemplate.query("SELECT * FROM da.tenant WHERE domain=?",
                new TenantRowMapper(), domain);
        return tenantList.stream().findFirst().orElse(null);
    }
}
