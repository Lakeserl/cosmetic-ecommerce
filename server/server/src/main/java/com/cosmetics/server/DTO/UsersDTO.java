package com.cosmetics.server.DTO;

import com.cosmetics.server.entity.ENUM.Role;
import com.cosmetics.server.entity.ENUM.STATUS;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsersDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private STATUS status;
    private Set<Role> roles;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
