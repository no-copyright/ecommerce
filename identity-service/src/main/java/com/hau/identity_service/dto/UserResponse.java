package com.hau.identity_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String address;
    private String profileImage;
    private Integer gender;
    private String createdAt;
    private String updatedAt;
}
