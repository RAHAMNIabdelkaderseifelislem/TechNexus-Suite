package com.yourstore.app.launcher;

import com.yourstore.app.backend.BackendApplication;
import com.yourstore.app.frontend.FrontendApplication;
import javafx.application.Application;
import org.springframework.context.ConfigurableApplicationContext;

public class AppLauncher {

    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        System.out.println("Launcher: Starting Spring Boot backend...");

        // Start Spring Boot in a separate thread or ensure it starts before JavaFX
        // Using the static method we created in BackendApplication
        try {
            // Pass command line args to Spring if any are relevant
            springContext = BackendApplication.startSpring(args);
            if (springContext != null && springContext.isActive()) {
                System.out.println("Launcher: Spring Boot backend started successfully.");
            } else {
                System.err.println("Launcher: Spring Boot backend FAILED to start. Exiting.");
                System.exit(1); // Exit if backend fails to start
            }
        } catch (Exception e) {
            System.err.println("Launcher: Exception while starting Spring Boot backend. Exiting.");
            e.printStackTrace();
            System.exit(1);
        }


        System.out.println("Launcher: Launching JavaFX frontend...");
        // Launch JavaFX application
        // This will call FrontendApplication.init(), start(), etc.
        Application.launch(FrontendApplication.class, args);
    }
}