package com.yourstore.app.frontend.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert; // Import Alert
import javafx.scene.control.ButtonType; // Import ButtonType
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional; // Import Optional

@Component
public class StageManager {

    private Stage primaryStage;
    private final ConfigurableApplicationContext springContext;

    @Autowired
    public StageManager(ConfigurableApplicationContext springContext) {
        this.springContext = springContext;
    }

    public void initialize(Stage stage) {
        this.primaryStage = stage;
    }

    public void showLoginView() {
        showView("/fxml/LoginView.fxml", "Login - Computer Store");
    }

    public void showMainView() {
        showView("/fxml/MainView.fxml", springContext.getEnvironment().getProperty("javafx.application.title", "Computer Store Management"));
    }

    public void showView(String fxmlPath, String title) {
        if (primaryStage == null) {
            System.err.println("PrimaryStage not initialized in StageManager. Cannot show view: " + fxmlPath);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Scene sceneToSet = primaryStage.getScene();
            if (sceneToSet == null) {
                sceneToSet = new Scene(root);
                primaryStage.setScene(sceneToSet);
            } else {
                sceneToSet.setRoot(root); // Efficiently replace root of existing scene
            }
            
            try {
                String css = Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm();
                if (!sceneToSet.getStylesheets().contains(css)) { // Add if not already present
                    sceneToSet.getStylesheets().add(css);
                }
            } catch (NullPointerException e) {
                System.err.println("Stylesheet /css/styles.css not found.");
            }
            
            primaryStage.setTitle(title);
            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load view: " + fxmlPath + ". Error: " + e.getMessage());
            showErrorAlert("Navigation Error", "Could not load: " + fxmlPath); // Use own method
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    // --- Centralized Alert Utilities ---
    public void showErrorAlert(String title, String content) {
        Platform.runLater(() -> { // Ensure UI updates are on JavaFX thread
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            if (primaryStage != null && primaryStage.isShowing()) {
                alert.initOwner(primaryStage);
            }
            alert.showAndWait();
        });
    }

    public void showInfoAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            if (primaryStage != null && primaryStage.isShowing()) {
                alert.initOwner(primaryStage);
            }
            alert.showAndWait();
        });
    }

    public Optional<ButtonType> showConfirmationAlert(String title, String header, String content) {
        // This needs to be called and waited for, so can't be Platform.runLater directly if result is needed immediately.
        // However, if called from JavaFX thread, it's fine.
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        if (primaryStage != null && primaryStage.isShowing()) {
            alert.initOwner(primaryStage);
        }
        return alert.showAndWait();
    }
}