package com.skillexchange.controller;

import com.skillexchange.dto.match.MatchResultDto;
import com.skillexchange.security.CurrentUser;
import com.skillexchange.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchingService matchingService;

    @GetMapping
    public ResponseEntity<List<MatchResultDto>> myMatches(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(matchingService.findMatchesForUser(CurrentUser.id(), limit));
    }

    @GetMapping("/{candidateId}")
    public ResponseEntity<MatchResultDto> matchWith(@PathVariable Long candidateId) {
        return ResponseEntity.ok(matchingService.calculateMatchBetween(CurrentUser.id(), candidateId));
    }
}
