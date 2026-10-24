package org.ngolibrary.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_role_active", columnList = "role,active"),
        @Index(name = "idx_users_fullname", columnList = "fullName")
})
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    private String email;

    private String phone;

    private LocalDate dateOfBirth;

    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    /** Forgot password: hash of the emailed code, when it stops working, and wrong tries so far. */
    private String resetCodeHash;

    private LocalDateTime resetCodeExpiresAt;

    private Integer resetCodeAttempts;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getResetCodeHash() {
        return resetCodeHash;
    }

    public void setResetCodeHash(String resetCodeHash) {
        this.resetCodeHash = resetCodeHash;
    }

    public LocalDateTime getResetCodeExpiresAt() {
        return resetCodeExpiresAt;
    }

    public void setResetCodeExpiresAt(LocalDateTime resetCodeExpiresAt) {
        this.resetCodeExpiresAt = resetCodeExpiresAt;
    }

    public int getResetCodeAttempts() {
        return resetCodeAttempts == null ? 0 : resetCodeAttempts;
    }

    public void setResetCodeAttempts(Integer resetCodeAttempts) {
        this.resetCodeAttempts = resetCodeAttempts;
    }
}
