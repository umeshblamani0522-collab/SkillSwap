package com.skillexchange.controller;

import com.skillexchange.dto.PageResponse;
import com.skillexchange.dto.rating.CreateRatingRequest;
import com.skillexchange.dto.rating.RatingDto;
import com.skillexchange.security.CurrentUser;
import com.skillexchange.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<RatingDto> rate(@Valid @RequestBody CreateRatingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ratingService.rate(CurrentUser.id(), request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponse<RatingDto>> forUser(@PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(ratingService.getRatingsForUser(userId, PageRequest.of(page, size))));
    }

    @GetMapping("/user/{userId}/average")
    public ResponseEntity<Double> average(@PathVariable Long userId) {
        Double avg = ratingService.getAverageRating(userId);
        return ResponseEntity.ok(avg == null ? 0.0 : avg);
    }
}
