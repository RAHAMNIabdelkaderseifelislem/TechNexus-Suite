package com.yourstore.app.frontend;

import com.yourstore.app.backend.BackendApplication;
import com.yourstore.app.frontend.util.StageManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.Objects;

public class FrontendApplication extends Application {

    private ConfigurableApplicationContext springContext;
    private StageManager stageManager;

    @Override
    public void init() throws Exception {
        // This method is called on the JavaFX launcher thread.
        // It's a good place to initialize Spring context if not already done.
        System.out.println("FrontendApplication: init() called.");

        // Get or start Spring context
        // Assuming AppLauncher has already started Spring, BackendApplication.getSpringContext() should return it.
        springContext = BackendApplication.getSpringContext();

        if (springContext == null) {
            System.err.println("FrontendApplication: Spring context is null in init(). This might be an issue if AppLauncher didn't start it.");
            // As a fallback, if not started by AppLauncher (e.g., direct JavaFX launch for testing - not recommended for this setup)
            // springContext = BackendApplication.startSpring(new String[]{});
            // However, for the intended flow, AppLauncher should ensure Spring is up.
            // If it's null here, there's a problem in the launch sequence or how context is shared.
            // For now, we'll proceed assuming it *should* be available from AppLauncher.
        }

        if (springContext != null) {
            // Get StageManager bean from Spring context
            // This must happen after Spring context is fully initialized.
            try {
                this.stageManager = springContext.getBean(StageManager.class);
                System.out.println("FrontendApplication: StageManager bean retrieved successfully.");
            } catch (Exception e) {
                System.err.println("FrontendApplication: Failed to get StageManager bean from Spring context.");
                e.printStackTrace();
                // This is a critical failure for the UI.
                Platform.exit(); // Exit if StageManager can't be loaded.
                System.exit(1);
            }
        } else {
            System.err.println("FrontendApplication: Spring context was not available. UI cannot proceed.");
            Platform.exit();
            System.exit(1);
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        // This method is called on the JavaFX Application Thread.
        System.out.println("FrontendApplication: start() called.");

        if (this.stageManager == null) {
            System.err.println("FrontendApplication: StageManager is null in start(). Cannot show views.");
            // This indicates a failure in init() to retrieve the StageManager.
            // An alert could be shown here before exiting.
            Platform.runLater(() -> {
                javafx.scene.control.Alert errorAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                errorAlert.setTitle("Application Startup Error");
                errorAlert.setHeaderText("Critical UI Component Missing");
                errorAlert.setContentText("The StageManager could not be initialized. The application cannot start.\nPlease check the logs for more details (Spring Context or Bean issues).");
                errorAlert.showAndWait();
                Platform.exit();
                System.exit(1);
            });
            return;
        }

        // Initialize StageManager with the primary stage provided by JavaFX
        stageManager.initialize(primaryStage);

        // Set a global close request handler for the primary stage
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("FrontendApplication: Primary stage close request. Shutting down.");
            // Optional: Add confirmation dialog before closing
            // Alert confirmExit = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to exit?", ButtonType.YES, ButtonType.NO);
            // Optional<ButtonType> result = confirmExit.showAndWait();
            // if (result.isPresent() && result.get() == ButtonType.NO) {
            //     event.consume(); // Don't close
            //     return;
            // }

            Platform.exit(); // Properly shuts down JavaFX toolkit
            System.exit(0);  // Ensures JVM termination
        });

        // Show the initial view (Login View)
        // The StageManager's showView method will also apply the stylesheet.
        stageManager.showLoginView();
        System.out.println("FrontendApplication: LoginView requested via StageManager.");

        // Note: primaryStage.show() is typically called by the StageManager's showView method itself.
        // If StageManager doesn't call show(), you might need it here, but the current StageManager does.
    }

    @Override
    public void stop() throws Exception {
        // This method is called when the application is shutting down.
        // (e.g., after Platform.exit() or last window closed if implicitExit is true)
        System.out.println("FrontendApplication: stop() called. Closing Spring context.");
        if (springContext != null && springContext.isActive()) {
            springContext.close();
            System.out.println("FrontendApplication: Spring context closed.");
        }
        // The System.exit(0) in setOnCloseRequest usually handles JVM termination.
        // No need to call Platform.exit() or System.exit(0) here again if already handled.
    }

    // Main method is usually not needed here if AppLauncher is the entry point.
    // If you wanted to run JavaFX directly (without Spring Boot for some tests),
    // you might have a main here, but it wouldn't initialize Spring correctly for this app.
    // public static void main(String[] args) {
    //     launch(args);
    // }
}