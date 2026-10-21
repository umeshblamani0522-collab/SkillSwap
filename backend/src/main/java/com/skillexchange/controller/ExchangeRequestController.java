package com.skillexchange.controller;

import com.skillexchange.dto.PageResponse;
import com.skillexchange.dto.exchange.CreateExchangeRequestDto;
import com.skillexchange.dto.exchange.ExchangeRequestDto;
import com.skillexchange.security.CurrentUser;
import com.skillexchange.service.ExchangeRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exchange-requests")
@RequiredArgsConstructor
public class ExchangeRequestController {

    private final ExchangeRequestService exchangeRequestService;

    @PostMapping
    public ResponseEntity<ExchangeRequestDto> create(@Valid @RequestBody CreateExchangeRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exchangeRequestService.create(CurrentUser.id(), request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ExchangeRequestDto>> myRequests(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(exchangeRequestService.getMyRequests(CurrentUser.id(), PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExchangeRequestDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(exchangeRequestService.getById(CurrentUser.id(), id));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ExchangeRequestDto> accept(@PathVariable Long id) {
        return ResponseEntity.ok(exchangeRequestService.respond(CurrentUser.id(), id, true));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ExchangeRequestDto> reject(@PathVariable Long id) {
        return ResponseEntity.ok(exchangeRequestService.respond(CurrentUser.id(), id, false));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ExchangeRequestDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(exchangeRequestService.cancel(CurrentUser.id(), id));
    }
}
