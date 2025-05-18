package com.yourstore.app.frontend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourstore.app.frontend.util.StageManager; // Assuming StageManager for alerts
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class AdminClientService {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final StageManager stageManager; // For showing alerts via initOwner

    @Value("${server.port:8080}")
    private String serverPort;

    private String getBaseUrl() {
        return "http://localhost:" + serverPort + "/api/v1/admin";
    }

    @Autowired
    public AdminClientService(HttpClient httpClient, ObjectMapper objectMapper, StageManager stageManager) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.stageManager = stageManager;
    }

    public CompletableFuture<Map<String, String>> backupDatabase(String backupDirectoryPath) {
        try {
            Map<String, String> payload = Map.of("backupDirectory", backupDirectoryPath);
            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/db/backup"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(httpResponse -> {
                    try {
                        Map<String, String> responseMap = objectMapper.readValue(httpResponse.body(), Map.class);
                        if (httpResponse.statusCode() == 200) {
                            return responseMap;
                        } else {
                            String errorMsg = responseMap.getOrDefault("message", "Unknown error during backup.");
                            throw new RuntimeException("Backup failed on server: " + errorMsg + " (Status: " + httpResponse.statusCode() + ")");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse backup response: " + e.getMessage(), e);
                    }
                });
        } catch (IOException e) { // Catch JSON serialization error for the request
            return CompletableFuture.failedFuture(new RuntimeException("Failed to prepare backup request: " + e.getMessage(), e));
        }
    }

    // Helper method for showing error alerts, can be moved to a shared utility
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