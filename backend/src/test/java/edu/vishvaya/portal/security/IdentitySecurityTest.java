package edu.vishvaya.portal.security;

import edu.vishvaya.portal.persistence.PortalAccount;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class IdentitySecurityTest {
    @Test
    void temporaryPasswordWithholdsAuthoritiesAndCredentialErasureRemovesHash() {
        var principal = new AccountPrincipal(new PortalAccount(UUID.randomUUID(), "synthetic.admin",
                "Synthetic administrator", "synthetic-hash", "ADMIN", true, true, true, 0));
        assertTrue(principal.getAuthorities().isEmpty());
        principal.eraseCredentials();
        assertNull(principal.getPassword());
        assertFalse(principal.toString().contains("synthetic-hash"));
    }

    @Test
    void loginThrottleBoundsAttemptsAndSeparatesClientAddresses() throws Exception {
        var filter = new LoginThrottleFilter();
        for (int attempt = 0; attempt < 10; attempt++) {
            var response = new MockHttpServletResponse();
            filter.doFilter(login("127.0.0.1"), response, (request, result) -> result.setContentType("passed"));
            assertEquals("passed", response.getContentType());
        }
        var blocked = new MockHttpServletResponse();
        filter.doFilter(login("127.0.0.1"), blocked, (request, response) -> fail("Throttled request reached login."));
        assertEquals(429, blocked.getStatus());
        assertEquals("60", blocked.getHeader("Retry-After"));
        var other = new MockHttpServletResponse();
        filter.doFilter(login("127.0.0.2"), other, (request, response) -> response.setContentType("passed"));
        assertEquals("passed", other.getContentType());
    }

    private MockHttpServletRequest login(String address) {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(address);
        return request;
    }
}
