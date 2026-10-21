package com.skillexchange.service;

import com.skillexchange.entity.PartnerVideoRoom;
import com.skillexchange.entity.User;
import com.skillexchange.exception.AccessDeniedCustomException;
import com.skillexchange.repository.PartnerVideoRoomRepository;
import com.skillexchange.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerVideoRoomServiceTest {

    @Mock private PartnerVideoRoomRepository roomRepository;
    @Mock private UserRepository userRepository;
    @Mock private MatchingService matchingService;

    @InjectMocks private PartnerVideoRoomService service;

    @Test
    void onlyRoomParticipantsCanJoin() {
        User user1 = User.builder().id(1L).username("one").build();
        User user2 = User.builder().id(2L).username("two").build();
        PartnerVideoRoom room = PartnerVideoRoom.builder()
                .roomId("room-token")
                .jitsiRoomName("SkillSwap-Partner-room-token")
                .user1(user1)
                .user2(user2)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(roomRepository.findByRoomId("room-token")).thenReturn(Optional.of(room));

        assertThrows(AccessDeniedCustomException.class,
                () -> service.authorizeJoin(99L, "room-token"));
    }
}
