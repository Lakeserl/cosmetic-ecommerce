package com.cosmetics.server.DTO.request;

import com.cosmetics.server.validation.EmailOrPhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @EmailOrPhone(message = "Username must be a valid email or phone number")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 charaters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]",
            message = "Password must contain uppercase, lowercase, digit, and special character")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @Pattern(regexp = "^[\\p{L}\\p{M}\\s'-]+$", message = "First name can only contain letters, spaces, hyphens, and apostrophes")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Pattern(regexp = "^[\\p{L}\\p{M}\\s'-]+$", message = "Last name can only contain letters, spaces, hyphens, and apostrophes")
    private String lastName;

    public boolean isEmail() {
        return username != null && username.contains("@");
    }

    public boolean isPhone() {
        return username != null && !username.contains("@");
    }

    public String getNormalizedUsername() {
        if (username == null) return "";

        String trimmed = username.trim();
        if (isEmail()) {
            return trimmed.toLowerCase();
        } else {
            // Phone normalization
            String cleaned = trimmed.replaceAll("[^+\\d]", "");
            // Convert Vietnamese phone format
            if (cleaned.startsWith("0") && !cleaned.startsWith("+")) {
                cleaned = "+84" + cleaned.substring(1);
            }
            return cleaned;
        }
    }
}
