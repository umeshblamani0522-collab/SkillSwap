package com.skillexchange.controller;

import com.skillexchange.dto.availability.AvailabilityDto;
import com.skillexchange.dto.availability.AvailabilityRequest;
import com.skillexchange.security.CurrentUser;
import com.skillexchange.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/availability")
@RequiredArgsConstructor
public class AvailabilityController {
    private final AvailabilityService availabilityService;

    @GetMapping
    public ResponseEntity<List<AvailabilityDto>> mine() { return ResponseEntity.ok(availabilityService.getMine(CurrentUser.id())); }
    @PostMapping
    public ResponseEntity<AvailabilityDto> add(@Valid @RequestBody AvailabilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityService.add(CurrentUser.id(), request));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        availabilityService.delete(CurrentUser.id(), id); return ResponseEntity.noContent().build();
    }
}
