package com.yourstore.app.frontend;

import com.yourstore.app.backend.BackendApplication;
import com.yourstore.app.frontend.util.StageManager;

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
    private StageManager stageManager; // Add StageManager

    @Override
    public void init() throws Exception {
        springContext = BackendApplication.getSpringContext();
        if (springContext == null) {
            System.out.println("Spring context not initialized by launcher, initializing now...");
            springContext = BackendApplication.startSpring(new String[]{});
        }
        // Get StageManager bean from Spring context
        stageManager = springContext.getBean(StageManager.class);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        stageManager.initialize(primaryStage); // Initialize StageManager with primary stage
        stageManager.showLoginView();         // Show login view first

        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });
        // primaryStage.show() is handled by stageManager.showView()
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