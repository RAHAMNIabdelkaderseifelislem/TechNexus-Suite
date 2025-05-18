package com.yourstore.app.backend.config;

import com.yourstore.app.backend.model.enums.UserRole;
import com.yourstore.app.backend.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy; // Import @Lazy
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

    // Inject UserDetailsService via constructor
    private final UserDetailsServiceImpl userDetailsService;

    // We will inject PasswordEncoder lazily into the constructor FOR configureGlobal
    // but the @Bean method itself is what provides it.
    @Autowired
    public SecurityConfig(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Use @Autowired on the method and inject PasswordEncoder directly here.
    // Spring will ensure passwordEncoder() bean is created before this method is called.
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, @Lazy PasswordEncoder passwordEncoder) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeRequests(authorizeRequests -> // Renamed from authorizeHttpRequests to authorizeRequests for Spring Boot 2.7
            authorizeRequests
                .antMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .antMatchers("/actuator/health").permitAll()
                .antMatchers("/api/v1/products/**").authenticated()
                .antMatchers("/api/v1/sales/**").authenticated()
                .antMatchers("/api/v1/admin/**").hasAuthority(UserRole.ROLE_ADMIN.name()) // Only ADMIN can access admin functions
                .anyRequest().authenticated() // <<< This is the first anyRequest()
        )
            .formLogin(formLogin ->
                formLogin
                    .loginPage("/login")
                    .loginProcessingUrl("/perform_login")
                    .defaultSuccessUrl("/api/v1/users/me", true)
                    .failureUrl("/login?error=true")
                    .permitAll()
            )
            .logout(logout ->
                logout
                    .logoutRequestMatcher(new AntPathRequestMatcher("/perform_logout", "POST"))
                    .logoutSuccessUrl("/login?logout=true")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
            );
        return http.build();
    }
}