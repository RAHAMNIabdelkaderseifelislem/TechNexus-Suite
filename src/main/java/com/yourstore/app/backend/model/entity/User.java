package com.yourstore.app.backend.model.entity;

import com.yourstore.app.backend.model.entity.base.Auditable; // If you want audit fields
import com.yourstore.app.backend.model.enums.UserRole;

import javax.persistence.*; // Using javax.persistence for Spring Boot 2.x
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users") // "user" is often a reserved keyword in SQL
public class User extends Auditable { // Optional: extends Auditable

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100) // Increased length for encoded passwords
    private String password;

    @Column(nullable = false)
    private boolean enabled = true; // User account status

    @ElementCollection(targetClass = UserRole.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<UserRole> roles = new HashSet<>();

    // Constructors
    public User() {}

    public User(String username, String password, Set<UserRole> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
        this.enabled = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<UserRole> getRoles() { return roles; }
    public void setRoles(Set<UserRole> roles) { this.roles = roles; }
}