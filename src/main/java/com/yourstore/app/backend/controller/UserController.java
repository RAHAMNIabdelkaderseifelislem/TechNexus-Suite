package com.yourstore.app.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yourstore.app.backend.model.dto.UserBasicDto;
import com.yourstore.app.backend.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", userDetails.getUsername());
        userMap.put("roles", userDetails.getAuthorities().stream()
                                    .map(auth -> auth.getAuthority())
                                    .collect(Collectors.toList()));
        // Add other details if needed
        return ResponseEntity.ok(userMap);
    }
    @GetMapping("/assignable") // Or a more specific path
    public ResponseEntity<List<UserBasicDto>> getAssignableUsers() {
        // For now, return all users. Later, filter by role (e.g., ROLE_STAFF, ROLE_TECHNICIAN)
        List<UserBasicDto> users = userRepository.findAll().stream()
                                .map(user -> new UserBasicDto(user.getId(), user.getUsername()))
                                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}