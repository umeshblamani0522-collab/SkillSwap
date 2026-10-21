
package com.skillexchange.service;

import com.skillexchange.entity.CreditAccount;
import com.skillexchange.entity.ExchangeRequest;
import com.skillexchange.entity.Session;
import com.skillexchange.entity.User;
import com.skillexchange.exception.ResourceNotFoundException;
import com.skillexchange.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {

        private final UserRepository userRepository;
        private final UserSkillRepository userSkillRepository;
        private final CreditAccountRepository creditAccountRepository;
        private final CreditTransactionRepository creditTransactionRepository;
        private final ExchangeRequestRepository exchangeRequestRepository;
        private final SessionRepository sessionRepository;
        private final RatingRepository ratingRepository;
        private final ReportRepository reportRepository;
        private final MessageRepository messageRepository;
        private final NotificationRepository notificationRepository;
        private final AuditLogRepository auditLogRepository;
        private final EmailVerificationTokenRepository emailVerificationTokenRepository;
        private final PasswordResetTokenRepository passwordResetTokenRepository;
        private final AvailabilityRepository availabilityRepository;
        private final VideoCallRequestRepository videoCallRequestRepository;
        private final ChatConnectionRepository chatConnectionRepository;
        private final PartnerVideoRoomRepository partnerVideoRoomRepository;

        @Transactional
        public void deleteAccount(Long targetUserId, Long adminId) {

                User user = userRepository.findById(targetUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                // Protect the currently logged-in administrator.
                if (adminId != null && adminId.equals(targetUserId)) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "You cannot delete your own account from Admin User Management.");
                }

                // Find exchange requests involving this user.
                List<ExchangeRequest> exchanges = exchangeRequestRepository.findBySenderIdOrReceiverId(
                                targetUserId, targetUserId);

                List<Long> exchangeIds = exchanges.stream()
                                .map(ExchangeRequest::getId)
                                .toList();

                // Collect sessions involving the user and sessions belonging
                // to exchange requests that will be removed.
                Map<Long, Session> sessionsById = new LinkedHashMap<>();

                sessionRepository
                                .findByTeacherIdOrLearnerIdOrderByCreatedAtDesc(
                                                targetUserId, targetUserId)
                                .forEach(s -> sessionsById.put(s.getId(), s));

                if (!exchangeIds.isEmpty()) {
                        sessionRepository.findByExchangeRequestIdIn(exchangeIds)
                                        .forEach(s -> sessionsById.put(s.getId(), s));
                }

                List<Session> sessions = List.copyOf(sessionsById.values());

                List<Long> sessionIds = sessions.stream()
                                .map(Session::getId)
                                .toList();

                // Remove video requests associated with this user or these sessions.
                videoCallRequestRepository.deleteBySenderIdOrReceiverId(
                                targetUserId, targetUserId);

                if (!sessionIds.isEmpty()) {
                        videoCallRequestRepository.deleteBySessionIdIn(sessionIds);
                }

                // Remove chat and video-room references.
                chatConnectionRepository.deleteReferencingUser(targetUserId);
                partnerVideoRoomRepository.clearLastJoinedUser(targetUserId);
                partnerVideoRoomRepository.deleteReferencingUser(targetUserId);

                // Remove tokens and availability records.
                emailVerificationTokenRepository.deleteByUserId(targetUserId);
                passwordResetTokenRepository.deleteByUserId(targetUserId);
                availabilityRepository.deleteByUserId(targetUserId);

                CreditAccount account = creditAccountRepository
                                .findByUserId(targetUserId)
                                .orElse(null);

                // Delete transactions belonging to this user's credit account.
                if (account != null) {
                        creditTransactionRepository.deleteByAccountId(account.getId());
                }

                // Preserve other accounts' transactions by detaching references
                // to sessions being removed.
                if (!sessionIds.isEmpty()) {
                        creditTransactionRepository.detachFromSessions(sessionIds);
                        ratingRepository.deleteBySessionIdIn(sessionIds);
                }

                ratingRepository.deleteByRaterIdOrRatedUserId(
                                targetUserId, targetUserId);

                messageRepository.deleteBySenderIdOrReceiverId(
                                targetUserId, targetUserId);

                notificationRepository.deleteByUserId(targetUserId);

                // Remove reports submitted by this user.
                // Reports submitted by other users can remain as moderation history.
                reportRepository.deleteByReporterId(targetUserId);

                // Preserve audit records but clear the deleted user's admin reference.
                auditLogRepository.detachAdminReference(targetUserId);

                // Delete sessions before exchange requests.
                if (!sessions.isEmpty()) {
                        sessionRepository.deleteAll(sessions);
                }

                if (!exchanges.isEmpty()) {
                        exchangeRequestRepository.deleteAll(exchanges);
                }

                userSkillRepository.deleteByUserId(targetUserId);

                if (account != null) {
                        creditAccountRepository.delete(account);
                }

                // Delete the account. No email block is created here.
                userRepository.delete(user);
        }
}
