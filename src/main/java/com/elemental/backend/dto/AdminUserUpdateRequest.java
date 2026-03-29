package com.elemental.backend.dto;

public class AdminUserUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String role; // "ROLE_USER" o "ROLE_ADMIN"

    // Getters y setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}