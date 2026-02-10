package com.demo.cloud.repository;

import com.demo.cloud.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByToken(String token);
}