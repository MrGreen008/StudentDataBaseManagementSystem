package edu.vishvaya.portal.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AccountRepository {
    private final JdbcTemplate jdbc;
    private static final RowMapper<PortalAccount> MAPPER = (rs, row) -> new PortalAccount(
            rs.getObject("id", UUID.class), rs.getString("login"), rs.getString("display_name"),
            rs.getString("password_hash"), rs.getString("role"), rs.getBoolean("enabled"),
            rs.getBoolean("password_change_required"), rs.getBoolean("can_publish"),
            rs.getLong("security_version"));

    public AccountRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<PortalAccount> byLogin(String login) {
        return jdbc.query("SELECT * FROM portal_account WHERE login = ?", MAPPER, login).stream().findFirst();
    }

    public Optional<PortalAccount> byId(UUID id) {
        return jdbc.query("SELECT * FROM portal_account WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public List<PortalAccount> list() {
        return jdbc.query("SELECT * FROM portal_account ORDER BY login", MAPPER);
    }

    public void lockIdentity() {
        jdbc.queryForObject("SELECT id FROM identity_lock WHERE id = 1 FOR UPDATE", Integer.class);
    }

    public long count() {
        return jdbc.queryForObject("SELECT count(*) FROM portal_account", Long.class);
    }

    public long activeAdministrators() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM portal_account WHERE role = 'ADMIN' AND enabled", Long.class);
    }

    public void insert(UUID id, String login, String name, String hash, String role, String actor) {
        jdbc.update("""
                INSERT INTO portal_account
                    (id, login, display_name, password_hash, role, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, login, name, hash, role, actor, actor);
    }

    public void permissions(UUID id, String role, boolean enabled, boolean publish, String actor) {
        jdbc.update("""
                UPDATE portal_account SET role = ?, enabled = ?, can_publish = ?,
                    security_version = security_version + 1, updated_by = ? WHERE id = ?
                """, role, enabled, publish, actor, id);
    }

    public void resetPassword(UUID id, String hash, String actor) {
        jdbc.update("""
                UPDATE portal_account SET password_hash = ?, password_change_required = TRUE,
                    security_version = security_version + 1, updated_by = ? WHERE id = ?
                """, hash, actor, id);
    }

    public boolean changePassword(UUID id, long version, String hash) {
        return jdbc.update("""
                UPDATE portal_account SET password_hash = ?, password_change_required = FALSE,
                    security_version = security_version + 1, updated_by = ?
                WHERE id = ? AND security_version = ? AND enabled
                """, hash, id.toString(), id, version) == 1;
    }
}
