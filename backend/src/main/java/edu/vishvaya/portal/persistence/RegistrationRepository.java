package edu.vishvaya.portal.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class RegistrationRepository {
    private final JdbcTemplate jdbc;
    public RegistrationRepository(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public boolean duplicate(String login,String email,String name,String mobile) {
        return jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM portal_account
              WHERE login=? OR lower(email)=? OR (full_name_key=? AND mobile_number=?)
                 OR lower(email)=? OR login=?)
            """,Boolean.class,login,email,name,mobile,login,email);
    }
    public void contact(UUID id,String email,String name,String mobile) {
        jdbc.update("UPDATE portal_account SET email=?,full_name_key=?,mobile_number=? WHERE id=?",
                email,name,mobile,id);
    }
    public String email(UUID id) {
        return jdbc.queryForObject("SELECT email FROM portal_account WHERE id=?",String.class,id);
    }
    public Map<String,Object> profile(UUID id) {
        return jdbc.queryForMap("SELECT login,display_name,email,mobile_number,role FROM portal_account WHERE id=?",id);
    }
    public void updateEmail(UUID id,String email) {
        jdbc.update("UPDATE portal_account SET email=?,updated_by=? WHERE id=?",email,id.toString(),id);
        jdbc.update("DELETE FROM password_recovery WHERE account_id=?",id);
    }
    public Map<String,Object> recovery(UUID account) {
        return jdbc.queryForList("SELECT * FROM password_recovery WHERE account_id=? FOR UPDATE",account)
                .stream().findFirst().orElse(Map.of());
    }
    public void issue(UUID id,String hash,long version) {
        jdbc.update("""
            INSERT INTO password_recovery(account_id,code_hash,security_version,expires_at)
            VALUES(?,?,?,CURRENT_TIMESTAMP+INTERVAL '10 minutes')
            ON CONFLICT(account_id) DO UPDATE SET code_hash=EXCLUDED.code_hash,
                security_version=EXCLUDED.security_version,expires_at=EXCLUDED.expires_at,
                sent_at=CURRENT_TIMESTAMP,attempts=0
            """,id,hash,version);
    }
    public void failedAttempt(UUID id) {
        jdbc.update("UPDATE password_recovery SET attempts=attempts+1 WHERE account_id=?",id);
    }
    public void finish(UUID id,String hash) {
        jdbc.update("""
            UPDATE portal_account SET password_hash=?,password_change_required=FALSE,
                security_version=security_version+1,updated_by=? WHERE id=?
            """,hash,id.toString(),id);
        jdbc.update("DELETE FROM password_recovery WHERE account_id=?",id);
    }
}
