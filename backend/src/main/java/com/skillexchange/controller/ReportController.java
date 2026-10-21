package com.skillexchange.controller;

import com.skillexchange.dto.PageResponse;
import com.skillexchange.dto.report.CreateReportRequest;
import com.skillexchange.dto.report.ReportDto;
import com.skillexchange.dto.report.ResolveReportRequest;
import com.skillexchange.entity.enums.ReportStatus;
import com.skillexchange.security.CurrentUser;
import com.skillexchange.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // Any authenticated user can file a report (users, messages, sessions, content).
    @PostMapping
    public ResponseEntity<ReportDto> create(@Valid @RequestBody CreateReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.create(CurrentUser.id(), request));
    }

    // Reviewing the moderation queue is an admin-only action.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ReportDto>> byStatus(
            @RequestParam(defaultValue = "OPEN") ReportStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(reportService.getByStatus(status, PageRequest.of(page, size))));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportDto> resolve(@PathVariable Long id, @Valid @RequestBody ResolveReportRequest request) {
        return ResponseEntity.ok(reportService.resolve(id, request));
    }
}
