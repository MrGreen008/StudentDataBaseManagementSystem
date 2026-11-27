package edu.vishvaya.portal.security;

import edu.vishvaya.portal.persistence.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class SessionAccountFilter extends OncePerRequestFilter {
    private final AccountRepository accounts;
    public SessionAccountFilter(AccountRepository accounts) { this.accounts = accounts; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            var current = accounts.byId(principal.id());
            if (current.isEmpty() || !current.get().enabled()
                    || current.get().securityVersion() != principal.version()) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                response.sendError(401);
                return;
            }
            if (principal.passwordChangeRequired()
                    && request.getRequestURI().startsWith("/api/")
                    && !request.getRequestURI().startsWith("/api/auth/")
                    && !request.getRequestURI().startsWith("/api/public/")) {
                response.sendError(403);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
