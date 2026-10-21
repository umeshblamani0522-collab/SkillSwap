package com.skillexchange.controller;

import com.skillexchange.dto.PageResponse;
import com.skillexchange.dto.credit.CreditAccountDto;
import com.skillexchange.dto.credit.CreditTransactionDto;
import com.skillexchange.security.CurrentUser;
import com.skillexchange.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    @GetMapping("/wallet")
    public ResponseEntity<CreditAccountDto> wallet() {
        return ResponseEntity.ok(creditService.getAccount(CurrentUser.id()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<CreditTransactionDto>> transactions(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(creditService.getHistory(CurrentUser.id(), PageRequest.of(page, size))));
    }
}
