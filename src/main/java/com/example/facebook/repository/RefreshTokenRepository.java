package com.example.facebook.repository;

import com.example.facebook.entity.RefreshToken;
import com.example.facebook.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUser(User user);
    void deleteByUser(User user);
}
