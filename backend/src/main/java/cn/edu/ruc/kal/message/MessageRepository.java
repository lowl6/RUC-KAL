package cn.edu.ruc.kal.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    long countByConversationIdAndSenderIdNotAndCreatedAtAfter(
            String conversationId, String senderId, LocalDateTime after);
}
