package com.aimentor.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Validates the required fields for user registration.
 */
public record SignupRequest(
        @NotBlank(message = "Name is required.")
        @Size(max = 50, message = "Name must be 50 characters or fewer.")
        String name,

        @NotBlank(message = "Email is required.")
        @Email(message = "Email format is invalid.")
        String email,

        @NotBlank(message = "Phone number is required.")
        @Size(max = 20, message = "Phone number must be 20 characters or fewer.")
        @Pattern(
                regexp = "^[0-9-]+$",
                message = "Phone number can contain only digits and hyphens."
        )
        String phone,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Password must include at least one letter and one number."
        )
        String password
) {
}
