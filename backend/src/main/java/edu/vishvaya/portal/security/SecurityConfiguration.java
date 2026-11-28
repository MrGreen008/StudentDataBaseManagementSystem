package edu.vishvaya.portal.security;

import edu.vishvaya.portal.persistence.AccountRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
public class SecurityConfiguration {
    @Bean
    PortalAuthenticationProvider portalAuthenticationProvider(AccountUserDetailsService users, PasswordEncoder passwords) {
        return new PortalAuthenticationProvider(users, passwords);
    }
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccountRepository accounts) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/app.js", "/styles.css",
                        "/images/**", "/portal.html", "/portal.js", "/portal-workspace.js", "/portal-access.js",
                        "/portal-academic.js", "/portal-services.js", "/portal.css",
                        "/api/auth/csrf", "/api/auth/captcha", "/api/public/announcements", "/api/public/overview",
                        "/api/public/departments", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**",
                        "/v3/api-docs", "/v3/api-docs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/public/enquiries").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/forgot-password",
                        "/api/auth/reset-password").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/portal/notices").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/portal/notices/*/attachment").authenticated()
                .requestMatchers("/api/staff/portal-notices/**").hasAnyRole("ADMIN","FACULTY")
                .requestMatchers("/api/staff/announcements/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/faculty/**").hasRole("FACULTY")
                .requestMatchers("/api/student/**").hasRole("STUDENT")
                .requestMatchers("/api/staff/**").hasAuthority("PUBLISH")
                .anyRequest().denyAll())
            .csrf(csrf -> csrf.ignoringRequestMatchers(request ->
                    request.getMethod().equals("POST")
                            && request.getServletPath().equals("/api/public/enquiries")))
            .formLogin(form -> form.loginPage("/portal.html")
                    .authenticationDetailsSource(PortalLoginDetails::new)
                    .loginProcessingUrl("/api/auth/login")
                    .successHandler((request, response, authentication) -> response.setStatus(204))
                    .failureHandler((request, response, exception) -> response.sendError(401, "Sign-in failed.")))
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
            .requestCache(cache -> cache.disable())
            .exceptionHandling(errors -> errors
                    .authenticationEntryPoint((request, response, exception) -> response.sendError(401))
                    .accessDeniedHandler((request, response, exception) -> response.sendError(403)))
            .addFilterBefore(new LoginThrottleFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(new SessionAccountFilter(accounts), SecurityContextHolderFilter.class);
        return http.build();
    }
}
