package com.skillexchange.controller;

import com.skillexchange.dto.PageResponse;
import com.skillexchange.dto.UserSkillDto;
import com.skillexchange.dto.skill.AddUserSkillRequest;
import com.skillexchange.dto.skill.SkillCategoryDto;
import com.skillexchange.dto.skill.SkillDto;
import com.skillexchange.security.CurrentUser;
import com.skillexchange.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping("/api/skill-categories")
    public ResponseEntity<List<SkillCategoryDto>> categories() {
        return ResponseEntity.ok(skillService.listCategories());
    }

    @GetMapping("/api/skills")
    public ResponseEntity<PageResponse<SkillDto>> skills(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(PageResponse.of(skillService.searchSkills(q, PageRequest.of(page, size))));
    }

    @PostMapping("/api/users/me/skills")
    public ResponseEntity<UserSkillDto> addSkill(@Valid @RequestBody AddUserSkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(skillService.addUserSkill(CurrentUser.id(), request));
    }

    @PutMapping("/api/users/me/skills/{id}")
    public ResponseEntity<UserSkillDto> updateSkill(@PathVariable Long id, @RequestBody AddUserSkillRequest request) {
        return ResponseEntity.ok(skillService.updateUserSkill(CurrentUser.id(), id, request));
    }

    @DeleteMapping("/api/users/me/skills/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        skillService.deleteUserSkill(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }
}
