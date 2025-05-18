package com.yourstore.app.backend.config;

import com.yourstore.app.backend.model.entity.User;
import com.yourstore.app.backend.model.enums.UserRole;
import com.yourstore.app.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy; // Import @Lazy
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Keep this final

    @Autowired
    public DataInitializer(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder) { // Add @Lazy here
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = new User(
                    "admin",
                    passwordEncoder.encode("adminpass"), // passwordEncoder will be resolved when needed
                    Set.of(UserRole.ROLE_ADMIN, UserRole.ROLE_USER)
            );
            userRepository.save(adminUser);
            System.out.println("Created default admin user (admin/adminpass)");
        }

        if (!userRepository.existsByUsername("user")) {
            User regularUser = new User(
                    "user",
                    passwordEncoder.encode("userpass"),
                    Set.of(UserRole.ROLE_USER)
            );
            userRepository.save(regularUser);
            System.out.println("Created default regular user (user/userpass)");
        }
    }
}