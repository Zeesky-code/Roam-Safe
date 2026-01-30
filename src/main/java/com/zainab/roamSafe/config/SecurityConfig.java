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
                                                .requestMatchers("/api/admin/seed/**").permitAll() // Seeding remains
                                                                                                   // open for demo
                                                                                                   // convenience
                                                .requestMatchers("/scams", "/submit", "/waitlist", "/register",
                                                                "/login", "/dashboard/**")
                                                .permitAll()

                                                // Public API (Protected by API Key via Filter, but Spring Security sees
                                                // it as authenticated)
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
                                                // Disable CSRF for APIs
                                                .ignoringRequestMatchers("/api/**"));

                return http.build();
        }
}