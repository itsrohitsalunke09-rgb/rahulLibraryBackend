package org.ngolibrary.dto;

import java.time.LocalDate;
import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.UserAccount;

public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private boolean active;
    private LocalDate dateOfBirth;
    private String address;

    public static UserResponse from(UserAccount user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.username = user.getUsername();
        response.fullName = user.getFullName();
        response.email = user.getEmail();
        response.phone = user.getPhone();
        response.role = user.getRole();
        response.active = user.isActive();
        response.dateOfBirth = user.getDateOfBirth();
        response.address = user.getAddress();
        return response;
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

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAddress() {
        return address;
    }
}
