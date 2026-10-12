package com.example.translator.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Holds the data typed into the registration form and defines the validation rules.
 */
public class RegistrationForm {

    @NotBlank(message = "VIT ID is required.")
    @Pattern(regexp = "^[A-Za-z0-9]{4,20}$",
             message = "VIT ID must be 4 to 20 letters or digits (no spaces).")
    private String vitId;

    @NotBlank(message = "Full name is required.")
    @Size(max = 100, message = "Name can have at most 100 characters.")
    private String name;

    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    @Size(max = 150, message = "Email can have at most 150 characters.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters long.")
    private String password;

    @NotBlank(message = "Please confirm your password.")
    private String confirmPassword;

    public String getVitId() {
        return vitId;
    }

    public void setVitId(String vitId) {
        this.vitId = vitId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
