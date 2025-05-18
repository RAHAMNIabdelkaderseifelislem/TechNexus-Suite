package com.yourstore.app.frontend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AuthClientService {

    private final HttpClient httpClient; // Shared HttpClient with CookieManager
    private final ObjectMapper objectMapper;

    @Value("${server.port:8080}")
    private String serverPort;

    private String getBaseAppUrl() {
        return "http://localhost:" + serverPort;
    }

    @Autowired
    public AuthClientService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
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
                        return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse user details", e);
                    }
                }
                return null; // Or throw exception for non-200
            });
    }
}