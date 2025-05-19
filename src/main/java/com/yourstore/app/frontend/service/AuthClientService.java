package com.yourstore.app.frontend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourstore.app.backend.model.dto.UserBasicDto;
import com.yourstore.app.frontend.util.StageManager;

import javafx.application.Platform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AuthClientService {

    private final HttpClient httpClient; // Shared HttpClient with CookieManager
    private final ObjectMapper objectMapper;
    private final StageManager stageManager;

    @Value("${server.port:8080}")
    private String serverPort;

    private String getBaseAppUrl() {
        return "http://localhost:" + serverPort;
    }
    @Autowired
    public AuthClientService(HttpClient httpClient, ObjectMapper objectMapper, StageManager stageManager) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.stageManager = stageManager;
    }

        // In AuthClientService.java
    public CompletableFuture<Boolean> login(String username, String password) {
        // Prepare form data
        Map<Object, Object> formData = new HashMap<>();
        formData.put("username", username);
        formData.put("password", password);

        String form = formData.entrySet()
                .stream()
                .map(e -> URLEncoder.encode(e.getKey().toString(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue().toString(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseAppUrl() + "/perform_login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                // Successful login if Spring Security redirects to defaultSuccessUrl ('/api/v1/users/me')
                // and that subsequent request (automatically followed by HttpClient) returns 200 OK.
                // The CookieManager in the shared HttpClient will have handled the JSESSIONID cookie.
                if (response.statusCode() == 200 && response.uri().getPath().equals("/api/v1/users/me")) {
                     System.out.println("Login successful. URI: " + response.uri());
                     // You can optionally try to get user details here to be absolutely sure
                     // but for now, this check is a strong indicator.
                     return true;
                } else if (response.statusCode() == 401) {
                    System.err.println("Login failed: Unauthorized (401) from " + response.uri());
                    // You might want to read the response body for more details if backend provides them
                    return false;
                } else {
                    // This case covers redirects to /login?error=true or other unexpected responses
                    System.err.println("Login failed or unexpected response: Status " + response.statusCode() + ", Final URI: " + response.uri());
                    // System.err.println("Response body: " + response.body()); // For debugging
                    return false;
                }
            }).exceptionally(ex -> {
                // Handle network errors or other exceptions during the HTTP request
                System.err.println("Exception during login request: " + ex.getMessage());
                ex.printStackTrace();
                return false;
            });
    }

    public CompletableFuture<Void> logout() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseAppUrl() + "/perform_logout"))
                .POST(HttpRequest.BodyPublishers.noBody()) // Adjust if your logout expects form data or is GET
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .thenAccept(response -> {
                if (response.statusCode() != 200 && response.statusCode() != 302 ) { // OK or Redirect to login page
                    // Log or handle unexpected status
                    System.err.println("Logout request failed or had unexpected status: " + response.statusCode());
                }
                 System.out.println("Logout request sent. Session cookie should be invalidated by server.");
                // CookieManager should automatically clear session cookies if server invalidates them
            });
    }

    public CompletableFuture<Map<String, Object>> getCurrentUserDetails() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseAppUrl() + "/api/v1/users/me"))
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 200) {
                    try {
                        // Assuming the response is a JSON map like {"username": "admin", "roles": ["ROLE_ADMIN"]}
                        return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
                    } catch (IOException e) {
                        System.err.println("Failed to parse user details: " + e.getMessage());
                        throw new RuntimeException("Failed to parse user details", e);
                    }
                } else if (response.statusCode() == 401) {
                    System.err.println("Not authenticated to get user details (401).");
                    return null; // Or throw an AuthenticationException
                }
                System.err.println("Failed to get user details, status: " + response.statusCode());
                return null; // Or throw a generic exception
            }).exceptionally(ex -> {
                System.err.println("Exception while fetching user details: " + ex.getMessage());
                return null; // Or rethrow wrapped exception
            });
    }

    public CompletableFuture<List<UserBasicDto>> getAssignableUsers() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseAppUrl() + "/api/v1/users/assignable")) // Path as defined in UserController
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> { // This lambda MUST return List<UserBasicDto> or throw
                if (response.statusCode() == 200) {
                    try {
                        return objectMapper.readValue(response.body(), new TypeReference<List<UserBasicDto>>() {});
                    } catch (IOException e) {
                        // This exception will be caught by the .exceptionally() block below
                        throw new RuntimeException("Failed to parse assignable users list: " + e.getMessage(), e);
                    }
                } else {
                    // Let an error handler (if you have a generic one like handleHttpError) show UI feedback,
                    // then throw to be caught by .exceptionally().
                    // For now, just throw a specific error for this case.
                    String errorMsg = "Failed to fetch assignable users, status: " + response.statusCode();
                    System.err.println(errorMsg + " Body: " + response.body());
                    // Platform.runLater(() -> stageManager.showErrorAlert("Fetch Error", errorMsg)); // Or handle error display in exceptionally
                    throw new RuntimeException(errorMsg);
                }
            }).exceptionally(ex -> { // Catches exceptions from sendAsync or thenApply
                 System.err.println("Original exception in getAssignableUsers: " + ex.getMessage());
                 ex.printStackTrace(); // Good for debugging

                 Platform.runLater(() -> {
                    // Check if an alert was already effectively shown by a specific RuntimeException message from thenApply
                    if (!(ex instanceof RuntimeException && ex.getMessage().startsWith("Failed to fetch assignable users, status:"))) {
                         stageManager.showErrorAlert("Operation Failed", "Could not fetch assignable users: " + ex.getMessage());
                    }
                 });
                 return Collections.<UserBasicDto>emptyList(); // Return an empty list of the correct type
            });
    }
}