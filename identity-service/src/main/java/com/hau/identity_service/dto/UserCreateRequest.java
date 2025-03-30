package com.hau.identity_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {
    @NotBlank(message = "Username không được để trống")
    private String username;
    @Size(min = 6, message = "Password phải có ít nhất 6 ký tự")
    private String password;
    @Email(message = "Email không đúng định dạng")
    @NotBlank(message = "Email không được để trống")
    private String email;
    @Pattern(regexp = "^[0-9]{10}$", message = "Số điện thoại phải có 10 chữ số")
    private String phone;
    private String address;
    private String profileImage;
    private Integer gender;
    private Set<String> roles;
}