package edu.vishvaya.portal.service.impl;

import edu.vishvaya.portal.api.RegistrationRequest;
import edu.vishvaya.portal.persistence.*;
import edu.vishvaya.portal.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
public class RegistrationServiceImpl implements RegistrationService {
    private final AccountRepository accounts;
    private final RegistrationRepository registration;
    private final PasswordEncoder passwords;
    private final PortalMailService mail;
    private final SecureRandom random=new SecureRandom();
    @Override public Map<String,Object> profile(UUID account) { return registration.profile(account); }
    @Override @Transactional
    public void contact(UUID id,String email,String password) {
        accounts.lockIdentity();
        var account=accounts.byId(id).orElseThrow();
        if(password.getBytes(StandardCharsets.UTF_8).length>72 || !passwords.matches(password,account.passwordHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Current password is incorrect.");
        String normalized=email.trim().toLowerCase(Locale.ROOT);
        var existing=accounts.byLogin(normalized);
        if(existing.isPresent() && !existing.get().id().equals(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Email is already in use.");
        registration.updateEmail(id,normalized);
    }
    public RegistrationServiceImpl(AccountRepository accounts,RegistrationRepository registration,
            PasswordEncoder passwords,PortalMailService mail) {
        this.accounts=accounts;this.registration=registration;this.passwords=passwords;this.mail=mail;
    }
    @Override @Transactional
    public void register(RegistrationRequest request) {
        if(!Set.of("STUDENT","FACULTY").contains(request.role()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose the Student or Faculty portal.");
        mail.requireConfigured();
        accounts.lockIdentity();
        String login=request.username().toLowerCase(Locale.ROOT);
        String email=request.email().trim().toLowerCase(Locale.ROOT);
        String name=request.fullName().trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
        if (registration.duplicate(login,email,name,request.mobileNumber()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Username, email or name and mobile combination is already registered.");
        byte[] bytes=new byte[18];random.nextBytes(bytes);
        String temporary=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        UUID id=UUID.randomUUID();
        accounts.insert(id,login,request.fullName().trim(),passwords.encode(temporary),request.role(),"PUBLIC");
        registration.contact(id,email,name,request.mobileNumber());
        mail.send(email,"Vishvaya demo registration",
            "Your demo registration was successful.\nUsername: "+login+"\nPortal: "+request.role()
            +"\nTemporary password: "+temporary+"\nSign in and change your temporary password before using the portal."
            +"\nThis is a fictional college demonstration. Faculty publishing requires administrator approval.");
    }
    @Override @Transactional
    public void recover(String identifier,String role) {
        mail.requireConfigured();
        accounts.lockIdentity();
        var account=accounts.byLogin(identifier.trim().toLowerCase(Locale.ROOT));
        if(account.isEmpty() || !account.get().enabled() || !account.get().role().equals(role)) return;
        var user=account.get();
        String email=registration.email(user.id());
        if(email==null) return;
        var existing=registration.recovery(user.id());
        if(!existing.isEmpty() && ((java.sql.Timestamp)existing.get("sent_at")).toInstant()
                .isAfter(Instant.now().minusSeconds(60))) return;
        String code=String.format("%06d",random.nextInt(1_000_000));
        registration.issue(user.id(),passwords.encode(code),user.securityVersion());
        mail.send(email,"Vishvaya password reset code",
            "Your password reset code is "+code+". It expires in ten minutes and can be used once."
            +"\nIf you did not request this, you can ignore this email.");
    }
    @Override @Transactional
    public boolean reset(String identifier,String role,String code,String password) {
        if(password.isBlank() || password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use at least 12 characters and at most 72 UTF-8 bytes.");
        accounts.lockIdentity();
        var account=accounts.byLogin(identifier.trim().toLowerCase(Locale.ROOT));
        if(account.isEmpty() || !account.get().enabled() || !account.get().role().equals(role)) return false;
        var user=account.get(); var record=registration.recovery(user.id());
        if(record.isEmpty() || ((Number)record.get("attempts")).intValue()>=5
            || ((java.sql.Timestamp)record.get("expires_at")).toInstant().isBefore(Instant.now())
            || ((Number)record.get("security_version")).longValue()!=user.securityVersion()) return false;
        if(!passwords.matches(code,(String)record.get("code_hash"))) {
            registration.failedAttempt(user.id());return false;
        }
        registration.finish(user.id(),passwords.encode(password));return true;
    }
}
