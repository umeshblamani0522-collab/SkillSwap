package com.skillexchange.repository;

import com.skillexchange.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("select u from User u where u.enabled = true and u.suspended = false and u.emailVerified = true " +
        "and (lower(u.username) like lower(concat('%', :q, '%')) " +
        "or lower(coalesce(u.fullName, '')) like lower(concat('%', :q, '%')) " +
        "or lower(coalesce(u.location, '')) like lower(concat('%', :q, '%'))) ")
    Page<User> search(String q, Pageable pageable);

    @Query("""
        select count(u)
        from User u
        where u.enabled = true
        and u.suspended = false
        and u.emailVerified = true
    """)
    long countActiveUsers();

    @Query("""
        select count(u)
        from User u
        where u.createdAt >= :since
    """)
    long countCreatedSince(@Param("since") LocalDateTime since);
    }
