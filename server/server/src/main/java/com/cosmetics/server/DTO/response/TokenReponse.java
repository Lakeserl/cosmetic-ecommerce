package com.cosmetics.server.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenReponse {

    private String access_token;
    private String refresh_token;
    private String token_type;
    private long expires_in;
}
