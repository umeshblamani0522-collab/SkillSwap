package com.skillexchange.repository;

import com.skillexchange.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("select m from Message m where " +
            "(m.sender.id = :userA and m.receiver.id = :userB) or " +
            "(m.sender.id = :userB and m.receiver.id = :userA) " +
            "order by m.createdAt asc")
    Page<Message> findConversation(@Param("userA") Long userA, @Param("userB") Long userB, Pageable pageable);

    long countByReceiverIdAndReadFalse(Long receiverId);

    // Delete messages sent or received by a particular user
    void deleteBySenderIdOrReceiverId(
            Long senderId,
            Long receiverId);
}
