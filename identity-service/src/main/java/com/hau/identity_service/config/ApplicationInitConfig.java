package com.hau.identity_service.config;

import com.hau.identity_service.entity.Role;
import com.hau.identity_service.entity.User;
import com.hau.identity_service.repository.RoleRepository;
import com.hau.identity_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Configuration
@Slf4j
public class ApplicationInitConfig {

    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.findByName("ADMIN").isEmpty()) {
                Role adminRole = Role.builder()
                        .name("ADMIN")
                        .description("Quyền quản trị viên")
                        .build();
                roleRepository.save(adminRole);
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


            if (userRepository.findByUsername("admin").isEmpty()) {

                User user = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin"))
                        .email("datdnk3@gmail.com")
                        .phone("0986964761")
                        .address("Hà Nội")
                        .gender(0)
                        .build();
                var roles = roleRepository.findAllById(Set.of("ADMIN"));
                if (roles.isEmpty()) {
                    log.error("Không tìm thấy role ADMIN");
                    return;
                }
                user.setRoles(new HashSet<>(roles));
                userRepository.save(user);
                log.warn("Người dùng admin đã được tạo với mật khẩu là admin, hãy thay đổi mật khẩu ngay sau khi đăng nhập lần đầu tiên");
            }
        };
    }

}
