
package com.skillexchange.controller;

import com.skillexchange.dto.PageResponse;
import com.skillexchange.dto.admin.AdminUserDto;
import com.skillexchange.dto.admin.DashboardStatsDto;
import com.skillexchange.dto.skill.SkillCategoryDto;
import com.skillexchange.dto.skill.SkillDto;
import com.skillexchange.security.CurrentUser;
import com.skillexchange.service.AccountDeletionService;
import com.skillexchange.service.AdminService;
import com.skillexchange.service.CreditService;
import com.skillexchange.service.SkillService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * All endpoints require the ADMIN role.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final SkillService skillService;
    private final CreditService creditService;
    private final AccountDeletionService accountDeletionService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDto> dashboard() {
        return ResponseEntity.ok(
                adminService.getDashboardStats());
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponse<AdminUserDto>> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                PageResponse.of(
                        adminService.listUsers(
                                PageRequest.of(page, size))));
    }

    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<AdminUserDto> suspend(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {

        String reason = body != null
                ? body.getOrDefault("reason", "No reason provided")
                : "No reason provided";

        return ResponseEntity.ok(
                adminService.setSuspended(
                        CurrentUser.id(), id, true, reason));
    }

    @PostMapping("/users/{id}/unsuspend")
    public ResponseEntity<AdminUserDto> unsuspend(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {

        String reason = body != null
                ? body.getOrDefault("reason", "No reason provided")
                : "No reason provided";

        return ResponseEntity.ok(
                adminService.setSuspended(
                        CurrentUser.id(), id, false, reason));
    }

    // Permanently delete a user without blocking their email.
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id) {

        accountDeletionService.deleteAccount(
                id, CurrentUser.id());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/skills")
    public ResponseEntity<SkillDto> createSkill(
            @RequestBody Map<String, Object> body) {

        String name = (String) body.get("name");

        Long categoryId = body.get("categoryId") == null
                ? null
                : Long.valueOf(body.get("categoryId").toString());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(skillService.createSkill(name, categoryId));
    }

    @PostMapping("/skills/{id}/deactivate")
    public ResponseEntity<Void> deactivateSkill(
            @PathVariable Long id) {

        skillService.setSkillActive(id, false);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/skills/{id}/activate")
    public ResponseEntity<Void> activateSkill(
            @PathVariable Long id) {

        skillService.setSkillActive(id, true);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/skill-categories")
    public ResponseEntity<SkillCategoryDto> createCategory(
            @RequestBody Map<String, String> body) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        skillService.createCategory(
                                body.get("name"),
                                body.get("description")));
    }

    @PostMapping("/credits/{userId}/adjust")
    public ResponseEntity<Void> adjustCredits(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {

        BigDecimal amount = new BigDecimal(body.get("amount").toString());

        String reason = (String) body.getOrDefault(
                "reason", "Admin adjustment");

        creditService.adminAdjust(userId, amount, reason);

        return ResponseEntity.noContent().build();
    }
}
