package com.cosmetics.server.DTO.request;

import com.cosmetics.server.entity.ENUM.OtpPurposes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendOtpRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotNull(message = "Purpose is required")
    private OtpPurposes purpose;
}
