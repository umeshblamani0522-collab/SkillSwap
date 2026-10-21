package com.skillexchange.repository;

import com.skillexchange.entity.BlockedEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlockedEmailRepository
        extends JpaRepository<BlockedEmail, Long> {

    boolean existsByEmail(String email);

    Optional<BlockedEmail> findByEmail(String email);

    void deleteByEmail(String email);
}