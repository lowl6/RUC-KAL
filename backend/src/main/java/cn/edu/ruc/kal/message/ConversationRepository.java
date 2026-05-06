package cn.edu.ruc.kal.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    List<Conversation> findByUserAIdOrUserBIdOrderByLastMessageAtDesc(String a, String b);
}
