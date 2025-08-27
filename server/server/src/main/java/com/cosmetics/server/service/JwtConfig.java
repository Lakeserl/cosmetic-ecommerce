package com.cosmetics.server.service;

import com.cosmetics.server.entity.auth.Users;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtConfig {

    @Value("{app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private long accessTokenExpiration;

    @Value("{app.jwtRefreshExpirationMs}")
    private long refreshTokenExpiration;

    private final RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    protected void init() {
        jwtSecret = Base64.getEncoder().encodeToString(jwtSecret.getBytes());
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public void blackListToken(String token) {
        try {
            long expiration = extractExpiration(token).getTime();
            long ttl = expiration - System.currentTimeMillis();

            if(ttl > 0) {
                redisTemplate.opsForValue().set(
                        "blackList: " + token,
                        "revoked",
                        Duration.ofMillis(expiration)
                );
            }
        } catch (Exception e) {
            log.warn("Failed to blacklist token", e);
        }
    }

    public String generateAccessToken(Users user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid",  user.getId());
        claims.put("roles",  user.getRoles());
        claims.put("provider", user.getAuthProvider());
        claims.put("verified", user.isEmailVerified() || user.isPhoneVerified());

        return generateToken(claims, user.getUsername(), accessTokenExpiration);
    }

    public String generateRefreshToken(Users user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", user.getId());
        claims.put("type", "refresh");

        return generateToken(claims, user.getUsername(), refreshTokenExpiration);
    }

    private String generateToken(Map<String, Object> claims, String username, long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("uid", Long.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    private boolean isTokenBlacklisted(String token) {
        try {
            return redisTemplate.hasKey("blackList:" + token);
        } catch (Exception e) {
            log.warn("Failed to checked token blacklist", e);
            return false;
        }
    }

    public Boolean !isTokenBlacklisted(String token, Users user) {
        final String username = extractUserName(token);
        return (username.equals(user.getUsername()) &&
                !isTokenExpired(token)
                && isTokenBlacklisted(token));
    }

}
