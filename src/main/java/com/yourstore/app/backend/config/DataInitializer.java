package com.yourstore.app.backend.config;

import com.yourstore.app.backend.model.entity.Product;
import com.yourstore.app.backend.model.entity.User;
import com.yourstore.app.backend.model.enums.ProductCategory;
import com.yourstore.app.backend.model.enums.UserRole;
import com.yourstore.app.backend.repository.UserRepository;
import com.yourstore.app.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy; // Import @Lazy
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Keep this final
    private final ProductRepository productRepository;

    @Autowired
    public DataInitializer(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder, ProductRepository productRepository) { // Add @Lazy here
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.productRepository = productRepository;
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
        if (productRepository.count() == 0) { // Check if products exist
            Product laptop = new Product();
            laptop.setName("Demo Laptop");
            laptop.setCategory(ProductCategory.LAPTOP);
            laptop.setDescription("A good demo laptop");
            laptop.setPurchasePrice(new BigDecimal("700.00"));
            laptop.setSellingPrice(new BigDecimal("999.99"));
            laptop.setQuantityInStock(10);
            productRepository.save(laptop);

            Product mouse = new Product();
            mouse.setName("Demo Mouse");
            mouse.setCategory(ProductCategory.MOUSE);
            mouse.setDescription("A basic demo mouse");
            mouse.setPurchasePrice(new BigDecimal("10.00"));
            mouse.setSellingPrice(new BigDecimal("25.00"));
            mouse.setQuantityInStock(50);
            productRepository.save(mouse);
            System.out.println("Created default products.");
        }
    }
}