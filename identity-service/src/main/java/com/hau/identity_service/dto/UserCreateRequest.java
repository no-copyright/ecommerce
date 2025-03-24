package com.hau.identity_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {
    @NotBlank(message = "Username không được để trống")
    private String username;
    @NotBlank(message = "Password không được để trống")
    private String password;
    @Email(message = "Email không đúng định dạng")
    @NotBlank(message = "Email không được để trống")
    private String email;
    @Pattern(regexp = "^[0-9]{10}$", message = "Số điện thoại phải có 10 chữ số")
    private String phone;
    private String address;
    private String profileImage;
    private Integer gender;
}
