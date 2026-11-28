package edu.vishvaya.portal.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

public class PortalLoginDetails extends WebAuthenticationDetails {
    private final String role;
    public PortalLoginDetails(HttpServletRequest request) {
        super(request);
        role = request.getParameter("portalRole");
    }
    public String role() { return role; }
}
