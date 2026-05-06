package cn.edu.ruc.kal.message;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kal_conversation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @Column(length = 32)
    private String conversationId;

    @Column(length = 32)
    private String userAId;

    @Column(length = 32)
    private String userBId;

    @Column(length = 200)
    private String contextLabel;

    @Column(length = 200)
    private String lastMessage;

    private LocalDateTime lastMessageAt;

    /** A 端最近一次「读到这里」的时间戳，用于 unread 计算 */
    private LocalDateTime lastReadAtA;

    /** B 端最近一次「读到这里」的时间戳，用于 unread 计算 */
    private LocalDateTime lastReadAtB;

    private Boolean expired;

    private LocalDateTime createdAt;
}
