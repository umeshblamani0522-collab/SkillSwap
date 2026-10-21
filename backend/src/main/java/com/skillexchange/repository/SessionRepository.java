
package com.skillexchange.repository;

import com.skillexchange.entity.Session;
import com.skillexchange.entity.enums.SessionStatus;
import com.skillexchange.dto.video.JaasParticipantDto;
import com.skillexchange.dto.video.VideoSessionAccessDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

       List<Session> findByTeacherIdOrLearnerIdOrderByCreatedAtDesc(
                     Long teacherId,
                     Long learnerId);

       // Find sessions belonging to exchange requests being deleted.
       List<Session> findByExchangeRequestIdIn(
                     List<Long> exchangeRequestIds);

       @Query("select new com.skillexchange.dto.video.VideoSessionAccessDto(" +
                     "s.id, s.status, s.meetingInfo, " +
                     "case when t.id = :userId then t.id else l.id end, " +
                     "case when t.id = :userId then t.username else l.username end, " +
                     "case when t.id = :userId then t.fullName else l.fullName end, " +
                     "case when t.id = :userId then t.email else l.email end, " +
                     "case when t.id = :userId then true else false end) " +
                     "from Session s join s.teacher t join s.learner l " +
                     "where s.id = :sessionId and (t.id = :userId or l.id = :userId)")
       Optional<VideoSessionAccessDto> findVideoSessionAccess(
                     @Param("sessionId") Long sessionId,
                     @Param("userId") Long userId);

       @Query("select new com.skillexchange.dto.video.JaasParticipantDto(" +
                     "case when t.id = :userId then t.id else l.id end, " +
                     "case when t.id = :userId then t.username else l.username end, " +
                     "case when t.id = :userId then t.fullName else l.fullName end, " +
                     "case when t.id = :userId then t.email else l.email end, " +
                     "case when t.id = :userId then true else false end) " +
                     "from Session s join s.teacher t join s.learner l " +
                     "where s.id = :sessionId and (t.id = :userId or l.id = :userId)")
       Optional<JaasParticipantDto> findVideoParticipant(
                     @Param("sessionId") Long sessionId,
                     @Param("userId") Long userId);

       @Query("select s from Session s " +
                     "join fetch s.teacher " +
                     "join fetch s.learner " +
                     "where s.id = :id")
       Optional<Session> findByIdWithParticipants(
                     @Param("id") Long id);

       @Query("select s from Session s " +
                     "where (s.teacher.id = :userId or s.learner.id = :userId) " +
                     "and s.sessionDate = :date " +
                     "and s.status in ('SCHEDULED','IN_PROGRESS') " +
                     "and s.startTime < :endTime " +
                     "and s.endTime > :startTime")
       List<Session> findOverlapping(
                     @Param("userId") Long userId,
                     @Param("date") LocalDate date,
                     @Param("startTime") LocalTime startTime,
                     @Param("endTime") LocalTime endTime);

       long countByStatus(SessionStatus status);

       long countByExchangeRequestIdAndStatus(
                     Long exchangeRequestId,
                     SessionStatus status);
}
