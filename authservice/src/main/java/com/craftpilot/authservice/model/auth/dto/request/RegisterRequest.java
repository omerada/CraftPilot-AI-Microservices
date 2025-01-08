package com.craftpilot.authservice.model.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Represents a request named {@link RegisterRequest} for user registration.
 * This class contains the necessary details required to register a new user.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @Email(message = "Please enter valid e-mail address")
    @Size(min = 7, message = "Minimum e-mail length is 7 characters.")
    private String email;

    @Size(min = 8)
    private String password;

    private String name;

    @NotBlank(message = "Membership can't be blank.")
    private String membership;

    @NotBlank(message = "Job can't be blank.")
    private String job;

    @NotBlank(message = "Language can't be blank.")
    private String language;

    @NotBlank(message = "Role can't be blank.")
    private String role;

}
