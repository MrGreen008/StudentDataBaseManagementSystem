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

    private synchronized int retryAfter(String address, int limit) {
        long now = System.currentTimeMillis();
        windows.values().removeIf(window -> now - window.start() >= 60_000);
        Window current = windows.getOrDefault(address, new Window(now, 0));
        if (current.attempts() >= limit) {
            return (int)Math.max(1, (60_000 - (now - current.start()) + 999) / 1000);
        }
        if (!windows.containsKey(address) && windows.size() >= 10_000) {
            return 60;
        }
        windows.put(address, new Window(current.start(), current.attempts() + 1));
        return 0;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path=request.getServletPath().isEmpty()?request.getRequestURI():request.getServletPath();
        boolean limited=request.getMethod().equals("POST") && java.util.Set.of("/api/auth/login",
                "/api/auth/register","/api/auth/forgot-password","/api/auth/reset-password").contains(path)
                || request.getMethod().equals("GET") && path.equals("/api/auth/captcha");
        if (limited) {
            int wait=retryAfter(request.getRemoteAddr()+":"+path,path.equals("/api/auth/captcha")?30:10);
            if(wait>0) {
                response.setHeader("Retry-After", String.valueOf(wait));
                response.sendError(429);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
