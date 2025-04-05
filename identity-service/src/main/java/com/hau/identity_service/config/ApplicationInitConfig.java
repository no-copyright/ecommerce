package com.hau.identity_service.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hau.identity_service.entity.Role;
import com.hau.identity_service.entity.User;
import com.hau.identity_service.repository.RoleRepository;
import com.hau.identity_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Configuration
@Slf4j
public class ApplicationInitConfig {

    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.role}")
    private String adminRole;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.findByName(adminRole).isEmpty()) {
                Role role = Role.builder()
                        .name(adminRole)
                        .description("Quyền quản trị viên")
                        .build();
                roleRepository.save(role);
                log.info("Role ADMIN đã được tạo.");
            }

            if (roleRepository.findByName("USER").isEmpty()) {
                Role userRole = Role.builder()
                        .name("USER")
                        .description("Quyền người dùng thông thường")
                        .build();
                roleRepository.save(userRole);
                log.info("Role USER đã được tạo.");
            }

            if (userRepository.findByUsername(adminUsername).isEmpty()) {

                User user = User.builder()
                        .username(adminUsername)
                        .password(passwordEncoder.encode(adminPassword))
                        .email("datdnk3@gmail.com")
                        .phone("0986964761")
                        .address("Hà Nội")
                        .gender(0)
                        .build();
                var roles = roleRepository.findAllById(Set.of(adminRole));
                if (roles.isEmpty()) {
                    log.error("Không tìm thấy role ADMIN");
                    return;
                }
                user.setRoles(new HashSet<>(roles));
                userRepository.save(user);
                log.warn(
                        "Người dùng admin đã được tạo với mật khẩu là admin, hãy thay đổi mật khẩu ngay sau khi đăng nhập lần đầu tiên");
            }
        };
    }
}
