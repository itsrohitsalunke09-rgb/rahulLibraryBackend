package org.ngolibrary.dto;

import org.ngolibrary.domain.Role;

public class AuthResponse {
    private String token;
    private Long id;
    private String username;
    private String fullName;
    private Role role;

    public AuthResponse(String token, Long id, String username, String fullName, Role role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }
}
