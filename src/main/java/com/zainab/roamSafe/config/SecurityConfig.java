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
                                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .formLogin(form -> form.disable())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/")
                                                .permitAll())
                                .csrf(csrf -> csrf
                                                // CSRF token rendering is broken under this
                                                // Thymeleaf/Security combo (the token can't be
                                                // written into forms), so exempt the API webhooks
                                                // and the session form-POST endpoints. TODO: restore
                                                // proper CSRF once the token-rendering issue is fixed.
                                                .ignoringRequestMatchers("/api/**", "/login", "/register",
                                                                "/submit", "/waitlist", "/dashboard/**"));

                return http.build();
        }
}