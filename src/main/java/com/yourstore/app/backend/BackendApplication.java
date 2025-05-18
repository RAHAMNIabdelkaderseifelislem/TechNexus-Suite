package com.yourstore.app.backend;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// Scan components in the base package and its sub-packages
@SpringBootApplication(scanBasePackages = "com.yourstore.app")
@EnableJpaAuditing // Enable JPA Auditing
public class BackendApplication {

    // Keep a reference to the Spring context if needed by JavaFX later,
    // though direct coupling is often avoided.
    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // This main method is primarily for Spring Boot.
        // The combined launcher will handle starting both.
        springContext = new SpringApplicationBuilder(BackendApplication.class)
                            .headless(false) // Important for JavaFX if Spring manages it
                            .run(args);
        System.out.println("Spring Boot BackendApplication started!");
    }

    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }

    // This method will be called by the AppLauncher
    public static ConfigurableApplicationContext startSpring(String[] args) {
        if (springContext == null || !springContext.isActive()) {
            springContext = new SpringApplicationBuilder(BackendApplication.class)
                                .headless(false)
                                .run(args);
            System.out.println("Spring Boot BackendApplication started via startSpring!");
        }
        return springContext;
    }
}