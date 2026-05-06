package cn.edu.ruc.kal.message;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kal_message", indexes = {
        @Index(name = "idx_msg_conv", columnList = "conversationId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32, nullable = false)
    private String conversationId;

    @Column(length = 32, nullable = false)
    private String senderId;

    @Column(length = 1500)
    private String content;

    private LocalDateTime createdAt;
}
