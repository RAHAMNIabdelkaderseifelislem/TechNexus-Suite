package com.yourstore.app.frontend.controller;

import com.yourstore.app.frontend.service.AuthClientService;
import com.yourstore.app.frontend.util.StageManager; // We'll create this utility
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoginViewController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorMessageLabel;

    private final AuthClientService authClientService;
    private final StageManager stageManager;


    @Autowired
    public LoginViewController(AuthClientService authClientService, StageManager stageManager) {
        this.authClientService = authClientService;
        this.stageManager = stageManager;
    }

    @FXML
    public void initialize() {
        errorMessageLabel.setText("");
        usernameField.setOnAction(event -> handleLogin()); // Allow login on Enter from username field
        passwordField.setOnAction(event -> handleLogin()); // Allow login on Enter from password field
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorMessageLabel.setText("Username and password cannot be empty.");
            return;
        }

        loginButton.setDisable(true);
        errorMessageLabel.setText("Attempting login...");

        authClientService.login(username, password)
            .thenAcceptAsync(loginSuccess -> Platform.runLater(() -> {
                if (loginSuccess) {
                    errorMessageLabel.setText("Login successful!");
                    // Transition to the main application view
                    stageManager.showMainView();
                } else {
                    errorMessageLabel.setText("Login failed. Invalid credentials or server error.");
                    loginButton.setDisable(false);
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    errorMessageLabel.setText("Login error: " + ex.getMessage());
                    System.err.println("Login exception: " + ex.getMessage());
                    ex.printStackTrace();
                    loginButton.setDisable(false);
                });
                return null;
            });
    }
}