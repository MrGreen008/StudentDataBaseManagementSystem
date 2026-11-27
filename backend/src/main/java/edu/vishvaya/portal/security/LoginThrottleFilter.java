package edu.vishvaya.portal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoginThrottleFilter extends OncePerRequestFilter {
    private record Window(long start, int attempts) {}
    private final Map<String, Window> windows = new HashMap<>();

    private synchronized boolean allow(String address) {
        long now = System.currentTimeMillis();
        windows.values().removeIf(window -> now - window.start() >= 60_000);
        Window current = windows.getOrDefault(address, new Window(now, 0));
        if (current.attempts() >= 10 || (!windows.containsKey(address) && windows.size() >= 10_000)) {
            return false;
        }
        windows.put(address, new Window(current.start(), current.attempts() + 1));
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getMethod().equals("POST") && request.getRequestURI().equals("/api/auth/login")
                && !allow(request.getRemoteAddr())) {
            response.setHeader("Retry-After", "60");
            response.sendError(429);
            return;
        }
        chain.doFilter(request, response);
    }
}
