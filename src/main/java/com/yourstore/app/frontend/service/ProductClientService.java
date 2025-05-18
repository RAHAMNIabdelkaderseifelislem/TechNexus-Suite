package com.yourstore.app.frontend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yourstore.app.backend.model.dto.ProductDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ProductClientService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${server.port:8080}") // Default to 8080 if not set
    private String serverPort;

    private String getBaseUrl() {
        return "http://localhost:" + serverPort + "/api/v1/products";
    }

    public ProductClientService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // Important for LocalDateTime
    }

    public CompletableFuture<List<ProductDto>> getAllProducts() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl()))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(httpResponse -> {
                if (httpResponse.statusCode() == 200) {
                    try {
                        return objectMapper.readValue(httpResponse.body(), new TypeReference<List<ProductDto>>() {});
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse product list from response", e);
                    }
                } else {
                    // Consider more specific error handling based on status code
                    throw new RuntimeException("Failed to fetch products: " + httpResponse.statusCode() + " " + httpResponse.body());
                }
            });
    }

    public CompletableFuture<ProductDto> createProduct(ProductDto productDto) {
        try {
            String requestBody = objectMapper.writeValueAsString(productDto);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(httpResponse -> {
                    if (httpResponse.statusCode() == 201) { // CREATED
                        try {
                            return objectMapper.readValue(httpResponse.body(), ProductDto.class);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to parse created product from response", e);
                        }
                    } else {
                        throw new RuntimeException("Failed to create product: " + httpResponse.statusCode() + " " + httpResponse.body());
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize product DTO for creation", e);
        }
    }

    public CompletableFuture<ProductDto> updateProduct(Long id, ProductDto productDto) {
        try {
            String requestBody = objectMapper.writeValueAsString(productDto);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/" + id)) // Ensure ID is part of the URL
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(httpResponse -> {
                    if (httpResponse.statusCode() == 200) { // OK
                        try {
                            return objectMapper.readValue(httpResponse.body(), ProductDto.class);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to parse updated product from response", e);
                        }
                    } else {
                        // Handle other status codes e.g. 404 if product not found by backend for update
                        throw new RuntimeException("Failed to update product: " + httpResponse.statusCode() + " " + httpResponse.body());
                    }
                });
        } catch (IOException e) {
            // This exception is for failure to serialize the productDto to JSON
            throw new RuntimeException("Failed to serialize product DTO for update", e);
        }
    }


    public CompletableFuture<Void> deleteProduct(Long id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/" + id))
                .DELETE()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(httpResponse -> { // Using thenAccept for processing response
                if (httpResponse.statusCode() != 204) { // No Content is the expected success status
                    // You could try to parse an error message from httpResponse.body() if backend sends one
                    throw new RuntimeException("Failed to delete product: " + httpResponse.statusCode() + " " + httpResponse.body());
                }
                // For a 204, there's no body to parse, and thenAccept's Consumer<HttpResponse> is fine.
                // The CompletableFuture<Void> is completed normally.
            });
            // If an exception occurs in thenAccept, it will complete the returned future exceptionally.
    }

     public CompletableFuture<ProductDto> getProductById(Long id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/" + id))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(httpResponse -> {
                if (httpResponse.statusCode() == 200) {
                    try {
                        return objectMapper.readValue(httpResponse.body(), ProductDto.class);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse product from response", e);
                    }
                } else if (httpResponse.statusCode() == 404) {
                    return null; // Or throw a specific "NotFoundInClientException"
                }
                else {
                    throw new RuntimeException("Failed to fetch product by ID: " + httpResponse.statusCode() + " " + httpResponse.body());
                }
            });
    }
}