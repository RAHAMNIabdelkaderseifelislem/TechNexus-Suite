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
                // Spring Security's formLogin usually redirects on success (e.g., to defaultSuccessUrl)
                // A 200 OK on the /perform_login itself, or a redirect (302) to the success URL indicates success.
                // A 401 or redirect to failureUrl indicates failure.
                // We check if a JSESSIONID cookie is set as a primary indicator of successful session creation.
                Optional<HttpCookie> sessionCookie = httpClient.cookieHandler().map(ch -> {
                    try {
                        Map<String, List<String>> headers = new HashMap<>();
                        // The cookie handler might not expose cookies directly this way
                        // This is a simplification. Actual cookie check might need to inspect response headers
                        // or trust that the CookieManager handled it.
                        // For now, we rely on the response status code and defaultSuccessUrl behavior.
                        // If the defaultSuccessUrl is hit and returns 200, that's good.
                        if (response.statusCode() == 200 && response.uri().getPath().equals("/api/v1/users/me")) {
                            return new HttpCookie("JSESSIONID", "dummy"); // Placeholder
                        }
                        List<String> cookieHeaders = response.headers().map().get("Set-Cookie");
                        if (cookieHeaders != null) {
                            return cookieHeaders.stream()
                                .map(HttpCookie::parse)
                                .flatMap(List::stream)
                                .filter(c -> "JSESSIONID".equalsIgnoreCase(c.getName()))
                                .findFirst().orElse(null);
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    return null;
                }).orElse(null);

                // Successful login if redirected to defaultSuccessUrl and it returns 200
                // OR if perform_login directly returns 200 (less common for default Spring Security)
                // AND a session cookie is established (managed by CookieManager)
                if (response.statusCode() == 200 && response.uri().getPath().equals("/api/v1/users/me")) {
                     System.out.println("Login successful. Session cookie should be managed.");
                     return true;
                } else if (response.statusCode() == 401) { // Unauthorized
                    System.err.println("Login failed: Unauthorized (401)");
                    return false;
                } else if (response.uri().getPath().contains("login") && response.uri().getQuery() != null && response.uri().getQuery().contains("error=true")) {
                    System.err.println("Login failed: Redirected to login?error=true");
                    return false;
                }
                // Any other case, assume failure for now, or inspect further
                System.err.println("Login failed: Status " + response.statusCode() + ", URI: " + response.uri());
                System.err.println("Response body: " + response.body());
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