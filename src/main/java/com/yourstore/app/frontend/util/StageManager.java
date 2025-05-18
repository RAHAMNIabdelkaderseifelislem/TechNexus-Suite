package com.yourstore.app.frontend.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

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

    private void showView(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle(title);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle error (e.g., show an error dialog)
            System.err.println("Failed to load view: " + fxmlPath);
            Platform.exit(); // Or some other fallback
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}