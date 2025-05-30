package com.yourstore.app.frontend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourstore.app.backend.model.dto.SaleDto; // Assuming this DTO path
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
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@Service
public class SaleClientService {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final StageManager stageManager;

    @Value("${server.port:8080}")
    private String serverPort;

    private String getBaseUrl() {
        return "http://localhost:" + serverPort + "/api/v1/sales";
    }

    @Autowired
    public SaleClientService(HttpClient httpClient, ObjectMapper objectMapper, StageManager stageManager) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.stageManager = stageManager;
    }

    public CompletableFuture<List<SaleDto>> getAllSales() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl()))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(httpResponse -> { // This lambda MUST return List<SaleDto> or throw
                if (httpResponse.statusCode() == 200) {
                    try {
                        return objectMapper.readValue(httpResponse.body(), new TypeReference<List<SaleDto>>() {});
                    } catch (IOException e) {
                        // This exception will be caught by the .exceptionally() block below
                        throw new RuntimeException("Failed to parse sales list: " + e.getMessage(), e);
                    }
                } else {
                    // Let handleHttpError show UI feedback, then throw to be caught by .exceptionally()
                    handleHttpError(httpResponse, "fetch sales");
                    // The line above throws, so this part is effectively unreachable.
                    // If handleHttpError could *not* throw, we'd need:
                    // return Collections.emptyList(); // Explicitly typed empty list
                    throw new RuntimeException("HTTP error handled, but flow continued unexpectedly."); // Should not happen if handleHttpError throws
                }
            }).exceptionally(ex -> { // Catches exceptions from sendAsync or thenApply
                 // Log the original exception for debugging before showing a general UI message
                 System.err.println("Original exception in getAllSales: " + ex.getMessage());
                 // ex.printStackTrace(); // Optionally print stack trace

                 // Show UI error (if not already shown by handleHttpError for HTTP issues)
                 // If the exception came from handleHttpError, it might have already shown an alert.
                 // If it's a parsing or network error, show an alert here.
                 // To avoid double alerts, handleHttpError should perhaps not throw if it shows an alert
                 // and returns a specific signal, or this exceptionally block becomes more nuanced.

                 // For simplicity now: ensure UI is updated about the failure
                 // and return a valid type for CompletableFuture<List<SaleDto>>.
                 Platform.runLater(() -> {
                    // Check if the alert was already shown by handleHttpError based on ex.getCause() or type
                    if (!(ex instanceof RuntimeException && ex.getMessage().startsWith("Failed to fetch sales. Status:"))) {
                         showErrorAlert("Operation Failed", "Could not fetch sales: " + ex.getMessage());
                    }
                 });
                 return Collections.emptyList(); // Return an empty list of SaleDto on failure
            });
    }


    private void handleHttpError(HttpResponse<?> response, String action) {
        // (Same as in ProductClientService, or move to a shared utility)
        String errorMessage = "Failed to " + action + ". Status: " + response.statusCode();
        System.err.println(errorMessage + "\nBody: " + (response.body() instanceof String ? response.body() : "N/A"));

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
                showErrorAlert("API Error", "An error occurred (" + response.statusCode() + ") while trying to " + action + ".");
            }
        });
        throw new RuntimeException(errorMessage);
    }

    private void showErrorAlert(String title, String content) {
        // (Same as in ProductClientService, or move to a shared utility)
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        if (stageManager != null && stageManager.getPrimaryStage() != null && stageManager.getPrimaryStage().isShowing()) {
            alert.initOwner(stageManager.getPrimaryStage());
        }
        alert.showAndWait();
    }

    public CompletableFuture<SaleDto> createSale(SaleDto saleDto) {
        try {
            String requestBody = objectMapper.writeValueAsString(saleDto);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl())) // POST to /api/v1/sales
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(httpResponse -> {
                    if (httpResponse.statusCode() == 201) { // CREATED
                        try {
                            return objectMapper.readValue(httpResponse.body(), SaleDto.class);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to parse created sale response: " + e.getMessage(), e);
                        }
                    } else {
                        handleHttpError(httpResponse, "create sale"); // Uses the existing error handler
                        // handleHttpError throws, so this part won't be reached.
                        // If it didn't throw, you'd need to return null or throw here.
                        throw new RuntimeException("Sale creation failed with status: " + httpResponse.statusCode());
                    }
                }).exceptionally(ex -> {
                    System.err.println("Exception during createSale request: " + ex.getMessage());
                    // The alert might be shown by handleHttpError if it's an HTTP error that gets converted.
                    // If it's a network/parsing error before handleHttpError, show it here.
                    if (!(ex.getCause() instanceof RuntimeException && ex.getCause().getMessage().startsWith("Failed to create sale. Status:"))) {
                        Platform.runLater(() -> showErrorAlert("Sale Creation Error", ex.getMessage()));
                    }
                    throw new RuntimeException("Could not complete sale creation.", ex); // Rethrow to be caught by controller
                });
        } catch (IOException e) { // Catch JSON serialization error for the request
            Platform.runLater(() -> showErrorAlert("Client Error", "Failed to prepare sale data for sending: " + e.getMessage()));
            return CompletableFuture.failedFuture(new RuntimeException("Failed to serialize sale DTO for creation", e));
        }
    }

    public CompletableFuture<Path> exportAllSalesToCsv(Path targetFile) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/export/csv"))
                .GET() // Server will set Content-Disposition
                .build();

        // The response body will be the CSV content. We stream it to the target file.
        return httpClient.sendAsync(request, BodyHandlers.ofFile(targetFile))
            .thenApply(httpResponse -> {
                if (httpResponse.statusCode() == 200) {
                    return httpResponse.body(); // Returns the Path to the downloaded file
                } else {
                    // Try to delete the partially downloaded/empty file on error
                    try { Files.deleteIfExists(targetFile); } catch (IOException ignored) {}
                    // Error already handled by server or generic client error
                    // This specific error means the download itself failed post-request
                    handleHttpError(httpResponse, "export sales to CSV");
                    throw new RuntimeException("Failed to download sales CSV, status: " + httpResponse.statusCode());
                }
            }).exceptionally(ex -> {
                try { Files.deleteIfExists(targetFile); } catch (IOException ignored) {}
                Platform.runLater(() -> showErrorAlert("Export Error", "Could not complete sales CSV export: " + ex.getMessage()));
                throw new RuntimeException("Error during sales CSV export request", ex);
            });
    }
}