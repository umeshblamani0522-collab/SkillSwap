package com.skillexchange.service;

import com.skillexchange.dto.rating.CreateRatingRequest;
import com.skillexchange.dto.rating.RatingDto;
import com.skillexchange.entity.Rating;
import com.skillexchange.entity.Session;
import com.skillexchange.entity.User;
import com.skillexchange.entity.enums.NotificationType;
import com.skillexchange.entity.enums.SessionStatus;
import com.skillexchange.exception.AccessDeniedCustomException;
import com.skillexchange.exception.BusinessRuleException;
import com.skillexchange.exception.ResourceNotFoundException;
import com.skillexchange.mapper.EntityMapper;
import com.skillexchange.repository.RatingRepository;
import com.skillexchange.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final SessionRepository sessionRepository;
    private final NotificationService notificationService;
    private final EntityMapper mapper;

    @Transactional
    public RatingDto rate(Long raterId, CreateRatingRequest request) {
        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (!session.getTeacher().getId().equals(raterId) && !session.getLearner().getId().equals(raterId)) {
            throw new AccessDeniedCustomException("You are not a participant in this session");
        }
        // Business rule: only completed sessions can be rated
        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new BusinessRuleException("Only completed sessions can be rated");
        }
        // Business rule: a session cannot be rated twice by the same rater
        ratingRepository.findBySessionIdAndRaterId(session.getId(), raterId).ifPresent(r -> {
            throw new BusinessRuleException("You have already rated this session");
        });

        User ratedUser = session.getTeacher().getId().equals(raterId) ? session.getLearner() : session.getTeacher();
        User rater = session.getTeacher().getId().equals(raterId) ? session.getTeacher() : session.getLearner();

        Rating rating = Rating.builder()
                .session(session).rater(rater).ratedUser(ratedUser)
                .stars(request.getStars()).review(request.getReview())
                .build();
        rating = ratingRepository.save(rating);

        notificationService.notify(ratedUser.getId(), NotificationType.RATING_RECEIVED,
                rater.getUsername() + " rated you " + request.getStars() + " star(s)", "/ratings");

        return mapper.toDto(rating);
    }

    public Page<RatingDto> getRatingsForUser(Long userId, Pageable pageable) {
        return ratingRepository.findByRatedUserIdOrderByCreatedAtDesc(userId, pageable).map(mapper::toDto);
    }

    public Double getAverageRating(Long userId) {
        return ratingRepository.findAverageRatingForUser(userId);
    }
}
