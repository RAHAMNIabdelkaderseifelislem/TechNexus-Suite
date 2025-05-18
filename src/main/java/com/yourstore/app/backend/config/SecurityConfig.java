package com.yourstore.app.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// For Spring Boot 2.7.x, these imports are more common for CSRF disabling:
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
// Or directly: import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Correct way to disable CSRF in this version
            .authorizeRequests(authorizeRequests ->
                authorizeRequests
                    .antMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                    .antMatchers(HttpMethod.POST, "/api/v1/products/**").permitAll()
                    .antMatchers(HttpMethod.PUT, "/api/v1/products/**").permitAll()
                    .antMatchers(HttpMethod.DELETE, "/api/v1/products/**").permitAll()
                    // Example for actuator health endpoint if you use it
                    // .antMatchers("/actuator/health").permitAll()
                    .anyRequest().authenticated()
            )
            .httpBasic(basic -> {}); // Enable HTTP Basic for other secured endpoints

        return http.build();
    }
}