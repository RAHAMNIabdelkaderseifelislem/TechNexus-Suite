package com.yourstore.app.frontend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourstore.app.backend.model.dto.PurchaseDto;
import com.yourstore.app.frontend.util.StageManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class PurchaseClientService {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final StageManager stageManager;

    @Value("${server.port:8080}")
    private String serverPort;

    private String getBaseUrl() {
        return "http://localhost:" + serverPort + "/api/v1/purchases";
    }

    @Autowired
    public PurchaseClientService(HttpClient httpClient, ObjectMapper objectMapper, StageManager stageManager) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.stageManager = stageManager;
    }

    public CompletableFuture<List<PurchaseDto>> getAllPurchases() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl()))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(httpResponse -> {
                if (httpResponse.statusCode() == 200) {
                    try {
                        return objectMapper.readValue(httpResponse.body(), new TypeReference<List<PurchaseDto>>() {});
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse purchases list: " + e.getMessage(), e);
                    }
                } else {
                    handleHttpError(httpResponse, "fetch purchases");
                    throw new RuntimeException("HTTP error handled, but flow continued unexpectedly.");
                }
            }).exceptionally(ex -> {
                System.err.println("Original exception in getAllPurchases: " + ex.getMessage());
                Platform.runLater(() -> {
                     if (!(ex instanceof RuntimeException && ex.getMessage().startsWith("Failed to fetch purchases. Status:"))) {
                        showErrorAlert("Operation Failed", "Could not fetch purchases: " + ex.getMessage());
                     }
                });
                return Collections.emptyList();
            });
    }

    public CompletableFuture<PurchaseDto> createPurchase(PurchaseDto purchaseDto) {
        try {
            String requestBody = objectMapper.writeValueAsString(purchaseDto);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(httpResponse -> {
                    if (httpResponse.statusCode() == 201) {
                        try {
                            return objectMapper.readValue(httpResponse.body(), PurchaseDto.class);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to parse created purchase response: " + e.getMessage(), e);
                        }
                    } else {
                        handleHttpError(httpResponse, "create purchase");
                        throw new RuntimeException("Purchase creation failed with status: " + httpResponse.statusCode());
                    }
                }).exceptionally(ex -> {
                    System.err.println("Exception during createPurchase request: " + ex.getMessage());
                     if (!(ex.getCause() instanceof RuntimeException && ex.getCause().getMessage().startsWith("Failed to create purchase. Status:"))) {
                        Platform.runLater(() -> showErrorAlert("Purchase Creation Error", ex.getMessage()));
                    }
                    throw new RuntimeException("Could not complete purchase creation.", ex);
                });
        } catch (IOException e) {
             Platform.runLater(() -> showErrorAlert("Client Error", "Failed to prepare purchase data: " + e.getMessage()));
             return CompletableFuture.failedFuture(new RuntimeException("Failed to serialize purchase DTO", e));
        }
    }

    private void handleHttpError(HttpResponse<?> response, String action) {
        String errorMessage = "Failed to " + action + ". Status: " + response.statusCode();
        String responseBody = response.body() instanceof String ? (String) response.body() : "No detailed body.";
        System.err.println(errorMessage + "\nBody: " + responseBody);

        Platform.runLater(() -> {
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Authentication Issue");
                alert.setHeaderText("Your session may have expired or you don't have permission.");
                alert.setContentText("Please try logging in again.");
                if (stageManager != null && stageManager.getPrimaryStage() != null) alert.initOwner(stageManager.getPrimaryStage());
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    stageManager.showLoginView();
                }
            } else {
                String detailedError = errorMessage;
                try {
                    // Now Map and List should be recognized
                    Map<String, Object> errorMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                    if (errorMap.containsKey("messages") && errorMap.get("messages") instanceof List) {
                        @SuppressWarnings("unchecked") // Safe cast after instanceof check
                        List<String> messages = (List<String>) errorMap.get("messages");
                        detailedError = "Validation failed: " + String.join(", ", messages);
                    } else if (errorMap.containsKey("message")) {
                        detailedError = (String) errorMap.get("message");
                    }
                } catch (Exception ignored) {
                    // If parsing the errorMap fails, stick to the original errorMessage
                }
                showErrorAlert("API Error", detailedError);
            }
        });
        throw new RuntimeException(errorMessage); // Ensure this is intended behavior for this method
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        if (stageManager != null && stageManager.getPrimaryStage() != null && stageManager.getPrimaryStage().isShowing()) {
            alert.initOwner(stageManager.getPrimaryStage());
        }
        alert.showAndWait();
    }
}