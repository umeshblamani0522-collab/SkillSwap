package com.skillexchange.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.skillexchange.entity.Rating;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findBySessionIdAndRaterId(Long sessionId, Long raterId);

    Page<Rating> findByRatedUserIdOrderByCreatedAtDesc(
            Long ratedUserId,
            Pageable pageable);

    @Query("select avg(r.stars) from Rating r where r.ratedUser.id = :userId")
    Double findAverageRatingForUser(Long userId);

    long countByRatedUserId(Long userId);

    void deleteByRaterIdOrRatedUserId(
            Long raterId, Long ratedUserId);

    void deleteBySessionIdIn(List<Long> sessionIds);
}