package com.yourstore.app.backend.config;

import com.yourstore.app.backend.model.enums.UserRole;
import com.yourstore.app.backend.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    @Autowired
    public SecurityConfig(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, @Lazy PasswordEncoder passwordEncoder) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeRequests(authorizeRequests ->
                authorizeRequests
                    .antMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/icons/**").permitAll() // Allow icons
                    .antMatchers("/actuator/health").permitAll()
                    .antMatchers("/api/v1/products/**").authenticated() // General access for authenticated users
                    .antMatchers(HttpMethod.GET, "/api/v1/sales/export/csv").hasAnyAuthority(UserRole.ROLE_ADMIN.name(), UserRole.ROLE_MANAGER.name())
                    .antMatchers(HttpMethod.GET, "/api/v1/sales/**").hasAnyAuthority(UserRole.ROLE_ADMIN.name(), UserRole.ROLE_MANAGER.name(), UserRole.ROLE_STAFF.name())
                    .antMatchers(HttpMethod.POST, "/api/v1/sales/**").hasAnyAuthority(UserRole.ROLE_ADMIN.name(), UserRole.ROLE_MANAGER.name(), UserRole.ROLE_STAFF.name())
                    .antMatchers("/api/v1/purchases/**").hasAnyAuthority(UserRole.ROLE_ADMIN.name(), UserRole.ROLE_MANAGER.name()) // Purchases for admin/manager
                    .antMatchers("/api/v1/repairs/**").hasAnyAuthority(UserRole.ROLE_ADMIN.name(), UserRole.ROLE_MANAGER.name(), UserRole.ROLE_STAFF.name()) // Staff can manage repairs
                    .antMatchers("/api/v1/dashboard/**").authenticated()
                    .antMatchers("/api/v1/reports/**").hasAnyAuthority(UserRole.ROLE_ADMIN.name(), UserRole.ROLE_MANAGER.name())
                    .antMatchers("/api/v1/admin/**").hasAuthority(UserRole.ROLE_ADMIN.name())
                    .anyRequest().authenticated()
            )
            .formLogin(formLogin ->
                formLogin
                    .loginPage("/login") // Not directly used by JavaFX if client handles form
                    .loginProcessingUrl("/perform_login")
                    .defaultSuccessUrl("/api/v1/users/me", true)
                    .failureUrl("/login?error=true") // Client interprets failure
                    .permitAll()
            )
            .logout(logout ->
                logout
                    .logoutRequestMatcher(new AntPathRequestMatcher("/perform_logout", "POST"))
                    .logoutSuccessUrl("/login?logout=true") // Client interprets this
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
            );
        return http.build();
    }
}