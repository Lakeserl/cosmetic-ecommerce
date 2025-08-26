package com.cosmetics.server.controller;


import com.cosmetics.server.DTO.response.ApiResponse;
import com.cosmetics.server.DTO.response.AuthResponse;
import com.cosmetics.server.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final AuthService authService;

    @GetMapping("/callback/{provider}")
    public ResponseEntity<ApiResponse<AuthResponse>> oauth2Callback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state,
            HttpServletRequest request) {

        AuthResponse response = authService.handleOAuth2Callback(
                provider, code, state, getClientInfo(request));

        return ResponseEntity.ok(ApiResponse.success("OAuth2 login successful", response));
    }

    @PostMapping("/google/idtoken")
    public ResponseEntity<ApiResponse<AuthResponse>> googleIdToken(
            @Valid @RequestBody GoogleIdTokenRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.handleGoogleIdToken(
                request, getClientInfo(httpRequest));

        return ResponseEntity.ok(ApiResponse.success("Google login successful", response));
    }

    @GetMapping("/authorization-url/{provider}")
    public ResponseEntity<ApiResponse<AuthorizationUrlResponse>> getAuthorizationUrl(
            @PathVariable String provider,
            @RequestParam(required = false) String redirectUri) {

        AuthorizationUrlResponse response = authService.getOAuth2AuthorizationUrl(provider, redirectUri);

        return ResponseEntity.ok(ApiResponse.success("Authorization URL generated", response));
    }

    private ClientInfo getClientInfo(HttpServletRequest request) {
        return ClientInfo.builder()
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .build();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
