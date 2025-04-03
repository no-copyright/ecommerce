package com.hau.identity_service.repository;

import com.hau.identity_service.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {
    List<InvalidatedToken> findByExpiryDateLessThanEqual(Date expiryDate);
}