package com.skillexchange.mapper;

import com.skillexchange.dto.UserProfileDto;
import com.skillexchange.dto.UserSkillDto;
import com.skillexchange.dto.chat.MessageDto;
import com.skillexchange.dto.credit.CreditAccountDto;
import com.skillexchange.dto.credit.CreditTransactionDto;
import com.skillexchange.dto.exchange.ExchangeRequestDto;
import com.skillexchange.dto.notification.NotificationDto;
import com.skillexchange.dto.rating.RatingDto;
import com.skillexchange.dto.report.ReportDto;
import com.skillexchange.dto.session.SessionDto;
import com.skillexchange.dto.skill.SkillCategoryDto;
import com.skillexchange.dto.skill.SkillDto;
import com.skillexchange.entity.*;
import org.springframework.stereotype.Component;

/**
 * Manual (non-reflective) mapping between JPA entities and DTOs.
 * Entities are never returned directly from controllers.
 */
@Component
public class EntityMapper {

    public SkillCategoryDto toDto(SkillCategory c) {
        if (c == null) return null;
        return SkillCategoryDto.builder()
                .id(c.getId()).name(c.getName()).description(c.getDescription()).build();
    }

    public SkillDto toDto(Skill s) {
        if (s == null) return null;
        return SkillDto.builder()
                .id(s.getId()).name(s.getName())
                .categoryId(s.getCategory() != null ? s.getCategory().getId() : null)
                .categoryName(s.getCategory() != null ? s.getCategory().getName() : null)
                .active(s.isActive())
                .build();
    }

    public UserSkillDto toDto(UserSkill us) {
        if (us == null) return null;
        return UserSkillDto.builder()
                .id(us.getId())
                .skillId(us.getSkill().getId())
                .skillName(us.getSkill().getName())
                .categoryName(us.getSkill().getCategory() != null ? us.getSkill().getCategory().getName() : null)
                .skillType(us.getSkillType())
                .level(us.getLevel())
                .experienceDescription(us.getExperienceDescription())
                .build();
    }

    public ExchangeRequestDto toDto(ExchangeRequest er) {
        if (er == null) return null;
        return ExchangeRequestDto.builder()
                .id(er.getId())
                .senderId(er.getSender().getId())
                .senderUsername(er.getSender().getUsername())
                .receiverId(er.getReceiver().getId())
                .receiverUsername(er.getReceiver().getUsername())
                .offeredSkillId(er.getOfferedSkill().getId())
                .offeredSkillName(er.getOfferedSkill().getName())
                .requestedSkillId(er.getRequestedSkill().getId())
                .requestedSkillName(er.getRequestedSkill().getName())
                .durationHours(er.getDurationHours())
                .message(er.getMessage())
                .status(er.getStatus())
                .matchPercentage(er.getMatchPercentage())
                .createdAt(er.getCreatedAt())
                .build();
    }

    public SessionDto toDto(Session s) {
        if (s == null) return null;
        return SessionDto.builder()
                .id(s.getId())
                .exchangeRequestId(s.getExchangeRequest().getId())
                .teacherId(s.getTeacher().getId())
                .teacherUsername(s.getTeacher().getUsername())
                .learnerId(s.getLearner().getId())
                .learnerUsername(s.getLearner().getUsername())
                .skillId(s.getSkill().getId())
                .skillName(s.getSkill().getName())
                .sessionDate(s.getSessionDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .status(s.getStatus())
                .meetingInfo(s.getMeetingInfo())
                .notes(s.getNotes())
                .creditsTransferred(s.isCreditsTransferred())
                .createdAt(s.getCreatedAt())
                .build();
    }

    public MessageDto toDto(Message m) {
        if (m == null) return null;
        return MessageDto.builder()
                .id(m.getId())
                .senderId(m.getSender().getId())
                .senderUsername(m.getSender().getUsername())
                .receiverId(m.getReceiver().getId())
                .receiverUsername(m.getReceiver().getUsername())
                .content(m.getContent())
                .read(m.isRead())
                .createdAt(m.getCreatedAt())
                .build();
    }

    public NotificationDto toDto(Notification n) {
        if (n == null) return null;
        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .content(n.getContent())
                .linkUrl(n.getLinkUrl())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    public CreditAccountDto toDto(CreditAccount ca) {
        if (ca == null) return null;
        return CreditAccountDto.builder()
                .balance(ca.getBalance())
                .totalEarned(ca.getTotalEarned())
                .totalSpent(ca.getTotalSpent())
                .build();
    }

    public CreditTransactionDto toDto(CreditTransaction t) {
        if (t == null) return null;
        return CreditTransactionDto.builder()
                .id(t.getId())
                .type(t.getType())
                .amount(t.getAmount())
                .balanceAfter(t.getBalanceAfter())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }

    public RatingDto toDto(Rating r) {
        if (r == null) return null;
        return RatingDto.builder()
                .id(r.getId())
                .sessionId(r.getSession().getId())
                .raterId(r.getRater().getId())
                .raterUsername(r.getRater().getUsername())
                .ratedUserId(r.getRatedUser().getId())
                .stars(r.getStars())
                .review(r.getReview())
                .createdAt(r.getCreatedAt())
                .build();
    }

    public ReportDto toDto(Report r) {
        if (r == null) return null;
        return ReportDto.builder()
                .id(r.getId())
                .reporterId(r.getReporter().getId())
                .reporterUsername(r.getReporter().getUsername())
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .reason(r.getReason())
                .status(r.getStatus())
                .resolutionNotes(r.getResolutionNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
