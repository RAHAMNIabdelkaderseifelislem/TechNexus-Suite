// src/main/java/com/yourstore/app/frontend/service/ReportClientService.java
package com.yourstore.app.frontend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
// Use the DTO from the backend package
import com.yourstore.app.backend.model.dto.SaleDto;
import com.yourstore.app.backend.model.dto.reports.ProfitLossReportDto; // Import from backend
import com.yourstore.app.backend.model.dto.reports.StockReportItemDto;  // Import from backend
import com.yourstore.app.backend.model.dto.reports.SalesByProductDto; // Import from backend
import com.yourstore.app.frontend.util.StageManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger; // <<< ADDED IMPORT
import org.slf4j.LoggerFactory; // <<< ADDED IMPORT
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ReportClientService {
    private static final Logger logger = LoggerFactory.getLogger(ReportClientService.class); // <<< CORRECTED: Logger initialized
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final StageManager stageManager;

    @Value("${server.port:8080}")
    private String serverPort;
    private String getBaseUrl() { return "http://localhost:" + serverPort + "/api/v1/reports"; }
    private final DateTimeFormatter isoDateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;

    @Autowired
    public ReportClientService(HttpClient httpClient, ObjectMapper objectMapper, StageManager stageManager) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.stageManager = stageManager;
    }

    public CompletableFuture<List<SaleDto>> getDetailedSalesReport(LocalDateTime startDate, LocalDateTime endDate) {
        String startDateStr = URLEncoder.encode(startDate.format(isoDateTimeFormatter), StandardCharsets.UTF_8);
        String endDateStr = URLEncoder.encode(endDate.format(isoDateTimeFormatter), StandardCharsets.UTF_8);
        String uri = getBaseUrl() + "/detailed-sales?startDate=" + startDateStr + "&endDate=" + endDateStr;

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                .header("Accept", "application/json")
                .GET().build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(httpResponse -> {
                if (httpResponse.statusCode() == 200) {
                    try {
                        return objectMapper.readValue(httpResponse.body(), new TypeReference<List<SaleDto>>() {});
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse detailed sales report: " + e.getMessage(), e);
                    }
                } else {
                    handleHttpError(httpResponse, "fetch detailed sales report");
                    throw new RuntimeException("HTTP error handled, but flow continued unexpectedly.");
                }
            }).exceptionally(ex -> {
                logger.error("Exception in getDetailedSalesReport: {}", ex.getMessage(), ex);
                 Platform.runLater(() -> {
                    if (!(ex.getCause() instanceof RuntimeException && ex.getCause().getMessage().startsWith("Failed to fetch detailed sales report. Status:"))) {
                        stageManager.showErrorAlert("Report Error", "Could not fetch detailed sales report: " + ex.getMessage());
                    }
                 });
                return Collections.emptyList();
            });
    }

    public CompletableFuture<ProfitLossReportDto> getProfitLossReport(LocalDateTime startDate, LocalDateTime endDate) {
        String startDateStr = URLEncoder.encode(startDate.format(isoDateTimeFormatter), StandardCharsets.UTF_8);
        String endDateStr = URLEncoder.encode(endDate.format(isoDateTimeFormatter), StandardCharsets.UTF_8);
        String uri = getBaseUrl() + "/profit-loss?startDate=" + startDateStr + "&endDate=" + endDateStr;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                .header("Accept", "application/json")
                .GET().build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 200) {
                    try {
                        return objectMapper.readValue(response.body(), ProfitLossReportDto.class); // Uses backend DTO
                    } catch (Exception e) { throw new RuntimeException("Failed to parse profit/loss report", e); }
                } else {
                    handleHttpError(response, "fetch profit/loss report");
                    throw new RuntimeException("HTTP error handled, but flow continued unexpectedly.");
                }
            })
            .exceptionally(ex -> {
                logger.error("Exception in getProfitLossReport: {}", ex.getMessage(), ex);
                Platform.runLater(() -> {
                    if (!(ex.getCause() instanceof RuntimeException && ex.getCause().getMessage().startsWith("Failed to fetch profit/loss report. Status:"))) {
                        stageManager.showErrorAlert("Report Error", "Could not fetch profit/loss report: " + ex.getMessage());
                    }
                });
                // Return null or throw a more specific exception if preferred,
                // but for CompletableFuture it's often null or completes exceptionally.
                // Returning a new DTO might imply success with empty data, which may not be desired.
                return null; // Or rethrow: throw new CompletionException(ex);
            });
    }

    public CompletableFuture<List<StockReportItemDto>> getCurrentStockReport() {
        String uri = getBaseUrl() + "/current-stock";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                .header("Accept", "application/json")
                .GET().build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(httpResponse -> {
                if (httpResponse.statusCode() == 200) {
                    try {
                        return objectMapper.readValue(httpResponse.body(), new TypeReference<List<StockReportItemDto>>() {});
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse stock report: " + e.getMessage(), e);
                    }
                } else {
                    handleHttpError(httpResponse, "fetch stock report");
                     throw new RuntimeException("HTTP error handled, but flow continued unexpectedly.");
                }
            }).exceptionally(ex -> {
                logger.error("Exception in getCurrentStockReport: {}", ex.getMessage(), ex);
                Platform.runLater(() -> {
                    if (!(ex.getCause() instanceof RuntimeException && ex.getCause().getMessage().startsWith("Failed to fetch stock report. Status:"))) {
                        stageManager.showErrorAlert("Report Error", "Could not fetch stock report: " + ex.getMessage());
                    }
                 });
                return Collections.emptyList();
            });
    }


    private void handleHttpError(HttpResponse<?> response, String action) {
        String errorMessage = "Failed to " + action + ". Status: " + response.statusCode();
        String responseBody = response.body() instanceof String ? (String) response.body() : "No detailed body.";
        logger.error(errorMessage + "\nBody: " + responseBody);

        Platform.runLater(() -> {
            String displayError = errorMessage;
            try {
                // Attempt to parse a more specific error message from backend if available
                Map<String, Object> errorMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                if (errorMap.containsKey("message")) {
                    displayError = (String) errorMap.get("message");
                } else if (errorMap.containsKey("error")) { // Common for Spring Boot error responses
                    displayError = (String) errorMap.get("error");
                }
            } catch (Exception ignored) {
                // If parsing errorMap fails, just use the generic errorMessage
            }
            stageManager.showErrorAlert("API Error (" + response.statusCode() + ")", displayError);
        });
        throw new RuntimeException(errorMessage); // Critical to ensure CompletableFuture completes exceptionally
    }
}