package com.yourstore.app.frontend;

import com.yourstore.app.backend.BackendApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.Objects;

public class FrontendApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() throws Exception {
        // Option 1: Initialize Spring context here if not already done by a separate launcher
        // springContext = new SpringApplicationBuilder(BackendApplication.class).headless(false).run();

        // Option 2: Retrieve context if started by an external launcher (our AppLauncher)
        springContext = BackendApplication.getSpringContext();
        if (springContext == null) {
            // Fallback if AppLauncher didn't start it (e.g., running JavaFX directly, though not intended)
            System.out.println("Spring context not initialized by launcher, initializing now...");
            springContext = BackendApplication.startSpring(new String[]{});
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/MainView.fxml")));

        // If your controller needs Spring DI, you can set the controller factory
        // This allows Spring to manage your JavaFX controllers
        if (springContext != null) {
            fxmlLoader.setControllerFactory(springContext::getBean);
        } else {
            System.err.println("Spring context is null. Controllers will not be Spring-managed.");
        }

        Parent root = fxmlLoader.load();

        primaryStage.setTitle(springContext != null ?
                              springContext.getEnvironment().getProperty("javafx.application.title", "Computer Store Management") :
                              "Computer Store Management");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0); // Ensures the application fully terminates
        });
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        if (springContext != null && springContext.isActive()) {
            springContext.close();
        }
        Platform.exit(); // Ensures JavaFX toolkit is shut down
        System.exit(0);  // Ensures JVM terminates if stop is called directly
    }

    // This main method is for standalone JavaFX launch (not recommended for this project structure)
    // We will use AppLauncher.main()
    // public static void main(String[] args) {
    //     launch(args);
    // }
}