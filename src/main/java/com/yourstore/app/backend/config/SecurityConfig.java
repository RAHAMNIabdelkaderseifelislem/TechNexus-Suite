package com.yourstore.app.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // For CSRF
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // Enables Spring Security's web security support
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF protection, common for stateless REST APIs
            // if you are not using browser-based form submissions directly to Spring backend
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                    // Permit all GET, POST, PUT, DELETE requests to product endpoints
                    .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/products/**").permitAll()
                    .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").permitAll()
                    // You might want to permit access to actuator health if you use it
                    // .requestMatchers("/actuator/health").permitAll()
                    // For any other request, require authentication (default behavior if not specified)
                    .anyRequest().authenticated()
            )
            // Spring Security by default enables HTTP Basic and form login.
            // If you want to allow unauthenticated access to specific paths (like above)
            // and keep others secured, this is fine.
            // If you want NO security for now (not recommended beyond initial testing),
            // you could permitAll() for anyRequest(), but the above is better.
            .httpBasic(basic -> {}); // Enable HTTP Basic auth for other secured endpoints if any
                                      // or customize as needed, e.g. .formLogin(form -> {});

        return http.build();
    }
}