package com.skillexchange.repository;

import com.skillexchange.entity.Availability;
import com.skillexchange.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findByUserOrderByDayOfWeekAscStartTimeAsc(User user);

    List<Availability> findByUser(User user);

    List<Availability> findByUserIdOrderByDayOfWeekAscStartTimeAsc(Long userId);

    List<Availability> findByUserAndDayOfWeek(User user, DayOfWeek dayOfWeek);

    void deleteByUserId(Long userId);
}
