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
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccountRepository accounts) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/app.js", "/styles.css",
                        "/images/**", "/portal.html", "/portal.js", "/portal.css",
                        "/api/auth/csrf", "/api/public/announcements", "/api/public/overview",
                        "/api/public/departments", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**",
                        "/v3/api-docs", "/v3/api-docs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/public/enquiries").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/faculty/**").hasRole("FACULTY")
                .requestMatchers("/api/student/**").hasRole("STUDENT")
                .requestMatchers("/api/staff/**").hasAuthority("PUBLISH")
                .anyRequest().denyAll())
            .csrf(csrf -> csrf.ignoringRequestMatchers(request ->
                    request.getMethod().equals("POST")
                            && request.getServletPath().equals("/api/public/enquiries")))
            .formLogin(form -> form.loginPage("/portal.html")
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
