package com.yourstore.app.frontend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourstore.app.backend.model.dto.RepairJobDto; // Ensure this is the correct DTO path
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
public class RepairClientService {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final StageManager stageManager;

    @Value("${server.port:8080}")
    private String serverPort;

    private String getBaseUrl() {
        return "http://localhost:" + serverPort + "/api/v1/repairs";
    }

    @Autowired
    public RepairClientService(HttpClient httpClient, ObjectMapper objectMapper, StageManager stageManager) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.stageManager = stageManager;
    }

    public CompletableFuture<List<RepairJobDto>> getAllRepairJobs() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl()))
                .header("Accept", "application/json")
                .GET()
                .build();
        return sendRequestAndParseList(request, "fetch repair jobs", new TypeReference<List<RepairJobDto>>() {});
    }

    public CompletableFuture<RepairJobDto> getRepairJobById(Long id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/" + id))
                .header("Accept", "application/json")
                .GET()
                .build();
        return sendRequestAndParseSingle(request, "fetch repair job by ID", new TypeReference<RepairJobDto>() {});
    }

    public CompletableFuture<RepairJobDto> createRepairJob(RepairJobDto repairJobDto) {
        try {
            String requestBody = objectMapper.writeValueAsString(repairJobDto);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            return sendRequestAndParseSingle(request, "create repair job", new TypeReference<RepairJobDto>() {}, 201);
        } catch (IOException e) {
            Platform.runLater(() -> showErrorAlert("Client Error", "Failed to prepare repair job data: " + e.getMessage()));
            return CompletableFuture.failedFuture(new RuntimeException("Failed to serialize repair job DTO", e));
        }
    }

    public CompletableFuture<RepairJobDto> updateRepairJob(Long id, RepairJobDto repairJobDto) {
        try {
            String requestBody = objectMapper.writeValueAsString(repairJobDto);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/" + id))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            return sendRequestAndParseSingle(request, "update repair job", new TypeReference<RepairJobDto>() {});
        } catch (IOException e) {
            Platform.runLater(() -> showErrorAlert("Client Error", "Failed to prepare repair job data for update: " + e.getMessage()));
            return CompletableFuture.failedFuture(new RuntimeException("Failed to serialize repair job DTO for update", e));
        }
    }

    // --- Generic Helper Methods for Sending Requests ---
    private <T> CompletableFuture<T> sendRequestAndParseSingle(HttpRequest request, String action, TypeReference<T> typeReference, int expectedStatus) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(httpResponse -> {
                if (httpResponse.statusCode() == expectedStatus) {
                    try {
                        if (httpResponse.body() == null || httpResponse.body().isEmpty()) return null; // For 204 No Content etc.
                        return objectMapper.readValue(httpResponse.body(), typeReference);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse response for " + action + ": " + e.getMessage(), e);
                    }
                } else {
                    handleHttpError(httpResponse, action);
                    throw new RuntimeException(action + " failed with status: " + httpResponse.statusCode());
                }
            }).exceptionally(ex -> {
                System.err.println("Exception during " + action + " request: " + ex.getMessage());
                if (! (ex.getCause() instanceof RuntimeException && ex.getCause().getMessage().startsWith("Failed to " + action))) {
                     Platform.runLater(() -> showErrorAlert(action + " Error", ex.getMessage()));
                }
                throw new RuntimeException("Could not complete " + action + ".", ex);
            });
    }
    private <T> CompletableFuture<T> sendRequestAndParseSingle(HttpRequest request, String action, TypeReference<T> typeReference) {
        return sendRequestAndParseSingle(request, action, typeReference, 200);
    }


    private <T> CompletableFuture<List<T>> sendRequestAndParseList(HttpRequest request, String action, TypeReference<List<T>> listTypeReference) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(httpResponse -> {
                if (httpResponse.statusCode() == 200) {
                    try {
                        return objectMapper.readValue(httpResponse.body(), listTypeReference);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse list for " + action + ": " + e.getMessage(), e);
                    }
                } else {
                    handleHttpError(httpResponse, action);
                     throw new RuntimeException(action + " failed with status: " + httpResponse.statusCode());
                }
            }).exceptionally(ex -> {
                System.err.println("Original exception in " + action + ": " + ex.getMessage());
                if (! (ex.getCause() instanceof RuntimeException && ex.getCause().getMessage().startsWith("Failed to " + action))) {
                    Platform.runLater(() -> showErrorAlert("Operation Failed", "Could not " + action + ": " + ex.getMessage()));
                }
                return Collections.emptyList();
            });
    }


    // --- Error Handling (copied from other client services, ideally in a shared utility or base class) ---
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
                    Map<String, Object> errorMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                    if (errorMap.containsKey("messages") && errorMap.get("messages") instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> messages = (List<String>) errorMap.get("messages");
                        detailedError = "Validation failed: " + String.join(", ", messages);
                    } else if (errorMap.containsKey("message")) {
                        detailedError = (String) errorMap.get("message");
                    }
                } catch (Exception ignored) {}
                showErrorAlert("API Error", detailedError);
            }
        });
        // This throw ensures the CompletableFuture completes exceptionally
        throw new RuntimeException(errorMessage);
    }

    private void showErrorAlert(String title, String content) {
        // This method is now available in StageManager, so ideally this would call stageManager.showErrorAlert
        // For now, keeping it local to avoid further refactoring in this step if StageManager not passed to this method.
        // If StageManager is available (it is via constructor):
        stageManager.showErrorAlert(title, content);
    }
}