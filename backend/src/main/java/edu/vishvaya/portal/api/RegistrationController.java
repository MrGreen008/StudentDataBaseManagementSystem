package edu.vishvaya.portal.api;

import edu.vishvaya.portal.security.RegistrationCaptcha;
import edu.vishvaya.portal.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {
    public record Recovery(@NotBlank @Size(max=254) String identifier,
        @NotBlank @Pattern(regexp="STUDENT|FACULTY|ADMIN") String role) {}
    public record Reset(@NotBlank @Size(max=254) String identifier,
        @NotBlank @Pattern(regexp="STUDENT|FACULTY|ADMIN") String role,
        @NotBlank @Pattern(regexp="[0-9]{6}") String code,
        @NotBlank @Size(max=72) String password) {
        @Override public String toString() { return "Reset[redacted]"; }
    }
    public record Contact(@NotBlank @Email @Size(max=254) String email,@NotBlank String currentPassword) {
        @Override public String toString() {return "Contact[redacted]";}
    }
    @GetMapping("/profile")
    public Map<String,Object> profile(@org.springframework.security.core.annotation.AuthenticationPrincipal
            edu.vishvaya.portal.security.AccountPrincipal principal) {
        return registration.profile(principal.id());
    }
    private final RegistrationService registration;
    private final RegistrationCaptcha captcha;
    public RegistrationController(RegistrationService registration,RegistrationCaptcha captcha) {
        this.registration=registration;this.captcha=captcha;
    }
    @GetMapping("/captcha")
    public Map<String,String> captcha(HttpServletRequest request) {
        return Map.of("image",captcha.create(request.getSession()));
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegistrationRequest body,HttpServletRequest request) {
        if(!captcha.consume(request.getSession(),body.captcha()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"CAPTCHA expired or incorrect. Try the new image.");
        registration.register(body);
    }
    @PostMapping("/forgot-password")
    public Map<String,String> recover(@Valid @RequestBody Recovery body) {
        registration.recover(body.identifier(),body.role());
        return Map.of("message","If a matching account with an email exists, a reset code has been sent.");
    }
    @PostMapping("/reset-password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody Reset body) {
        if(!registration.reset(body.identifier(),body.role(),body.code(),body.password()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid or expired reset code.");
    }
    @PostMapping("/contact") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void contact(@Valid @RequestBody Contact body,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            edu.vishvaya.portal.security.AccountPrincipal principal) {
        registration.contact(principal.id(),body.email(),body.currentPassword());
    }
}
