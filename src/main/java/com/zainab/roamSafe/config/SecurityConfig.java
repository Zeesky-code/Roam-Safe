package com.zainab.roamSafe.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * Turns a stale-CSRF rejection into a retry instead of a dead end.
         *
         * A token belongs to the session that issued it, so a login page left
         * open across a server restart posts a token the new session has never
         * seen. That is correct to reject, but the default response is a
         * Whitelabel 403 with no explanation and no way forward.
         *
         * Only CSRF failures on browser page requests are redirected. API calls
         * and genuine authorization failures still get a plain 403, so this
         * can't mask a real permission problem.
         */
        private static org.springframework.security.web.access.AccessDeniedHandler accessDeniedHandler() {
                return (request, response, ex) -> {
                        boolean csrfProblem = ex instanceof org.springframework.security.web.csrf.CsrfException;
                        boolean apiCall = request.getRequestURI().startsWith("/api/");
                        if (csrfProblem && !apiCall) {
                                response.sendRedirect(request.getContextPath() + "/login?expired=true");
                                return;
                        }
                        response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                };
        }

        /**
         * Resolves the CSRF token eagerly instead of on demand.
         *
         * Setting the request-attribute name to null opts out of Spring Security
         * 6's deferred token loading, which is what stopped Thymeleaf rendering
         * the token into forms. With the token available as a plain request
         * attribute, th:action forms get a hidden field injected automatically
         * and templates can read it directly.
         */
        private static org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler csrfTokenRequestHandler() {
                var handler = new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler();
                handler.setCsrfRequestAttributeName(null);
                return handler;
        }

        @Autowired
        private ApiKeyAuthFilter apiKeyAuthFilter;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(authz -> authz
                                                // Public pages
                                                .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/robots.txt",
                                                                "/sitemap.xml")
                                                .permitAll()
                                                .requestMatchers("/api/admin/seed/**").permitAll()
                                                .requestMatchers("/scams", "/submit", "/waitlist", "/register",
                                                                "/login", "/dashboard/**", "/pricing", "/subscribe",
                                                                "/map")
                                                .permitAll()

                                                // Public API (Protected by API Key via Filter)
                                                .requestMatchers("/api/v1/**").authenticated()

                                                // Admin pages
                                                .requestMatchers("/admin/**").hasRole("ADMIN")

                                                // All other requests
                                                .anyRequest().permitAll())
                                .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler()))
                                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .formLogin(form -> form.disable())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/")
                                                .permitAll())
                                .csrf(csrf -> csrf
                                                // Spring Security 6 defers resolving the CSRF token
                                                // until something asks for it, and the deferred
                                                // lookup is what broke token rendering in Thymeleaf
                                                // and truncated every form. Opting out of the
                                                // deferred behaviour (a null request-attribute name)
                                                // resolves the token eagerly, so templates can
                                                // render it. Browser form POSTs are protected again.
                                                .csrfTokenRequestHandler(csrfTokenRequestHandler())
                                                // Only /api/** stays exempt: it is stateless and
                                                // authenticated by API key rather than a session
                                                // cookie, so it isn't open to cross-site form
                                                // submission, and the Bachs payment webhook is a
                                                // server-to-server call that can't carry a token.
                                                .ignoringRequestMatchers("/api/**"));

                return http.build();
        }
}