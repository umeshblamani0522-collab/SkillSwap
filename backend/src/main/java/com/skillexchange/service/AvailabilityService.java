package com.skillexchange.service;

import com.skillexchange.dto.availability.AvailabilityDto;
import com.skillexchange.dto.availability.AvailabilityRequest;
import com.skillexchange.entity.Availability;
import com.skillexchange.entity.User;
import com.skillexchange.exception.AccessDeniedCustomException;
import com.skillexchange.exception.BusinessRuleException;
import com.skillexchange.exception.ResourceNotFoundException;
import com.skillexchange.repository.AvailabilityRepository;
import com.skillexchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {
    private final AvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AvailabilityDto> getMine(Long userId) {
        return availabilityRepository.findByUserIdOrderByDayOfWeekAscStartTimeAsc(userId).stream().map(this::toDto).toList();
    }

    @Transactional
    public AvailabilityDto add(Long userId, AvailabilityRequest request) {
        validateTime(request.getStartTime(), request.getEndTime());
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Availability> sameDay = availabilityRepository.findByUserAndDayOfWeek(user, request.getDayOfWeek());
        boolean overlap = sameDay.stream().anyMatch(a -> a.getStartTime().isBefore(request.getEndTime()) && request.getStartTime().isBefore(a.getEndTime()));
        if (overlap) throw new BusinessRuleException("This availability overlaps an existing time slot");
        Availability a = Availability.builder().user(user).dayOfWeek(request.getDayOfWeek()).startTime(request.getStartTime())
                .endTime(request.getEndTime()).timezone(normalizeTimezone(request.getTimezone())).build();
        return toDto(availabilityRepository.save(a));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Availability a = availabilityRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Availability not found"));
        if (!a.getUser().getId().equals(userId)) throw new AccessDeniedCustomException("You can only delete your own availability");
        availabilityRepository.delete(a);
    }

    private void validateTime(LocalTime start, LocalTime end) {
        if (!start.isBefore(end)) throw new BusinessRuleException("Availability start time must be before end time");
        if (java.time.Duration.between(start, end).toMinutes() < 30) throw new BusinessRuleException("Availability must be at least 30 minutes");
    }

    private String normalizeTimezone(String timezone) { return timezone == null || timezone.isBlank() ? "UTC" : timezone.trim(); }
    private AvailabilityDto toDto(Availability a) { return AvailabilityDto.builder().id(a.getId()).dayOfWeek(a.getDayOfWeek())
            .startTime(a.getStartTime()).endTime(a.getEndTime()).timezone(a.getTimezone()).build(); }
}
