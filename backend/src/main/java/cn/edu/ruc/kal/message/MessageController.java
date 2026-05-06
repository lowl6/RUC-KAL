package cn.edu.ruc.kal.message;

import cn.edu.ruc.kal.common.ApiResponse;
import cn.edu.ruc.kal.common.BizException;
import cn.edu.ruc.kal.security.CurrentUser;
import cn.edu.ruc.kal.user.User;
import cn.edu.ruc.kal.user.UserRepository;
import cn.edu.ruc.kal.verify.MailService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ConversationRepository convRepo;
    private final MessageRepository msgRepo;
    private final UserRepository userRepo;
    private final MailService mailService;

    @Value("${kal.web.base-url:http://localhost:5173}")
    private String webBaseUrl;

    /* ============== 列表 ============== */

    @GetMapping("/conversations")
    public ApiResponse<List<Map<String, Object>>> conversations() {
        String uid = CurrentUser.requireUserId();
        var convs = convRepo.findByUserAIdOrUserBIdOrderByLastMessageAtDesc(uid, uid);
        List<Map<String, Object>> out = convs.stream().map(c -> attachUnread(c, uid)).toList();
        return ApiResponse.ok(out);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Object>> unreadCount() {
        String uid = CurrentUser.requireUserId();
        long total = 0;
        var convs = convRepo.findByUserAIdOrUserBIdOrderByLastMessageAtDesc(uid, uid);
        for (Conversation c : convs) {
            total += unreadOf(c, uid);
        }
        return ApiResponse.ok(Map.of("unread", total));
    }

    /* ============== 历史 + 标记已读 ============== */

    @GetMapping("/conversations/{id}")
    @Transactional
    public ApiResponse<List<Message>> history(@PathVariable("id") String id) {
        String uid = ensureMember(id);
        var list = msgRepo.findByConversationIdOrderByCreatedAtAsc(id);
        markRead(id, uid);
        return ApiResponse.ok(list);
    }

    @PostMapping("/conversations/{id}/read")
    @Transactional
    public ApiResponse<Void> read(@PathVariable("id") String id) {
        String uid = ensureMember(id);
        markRead(id, uid);
        return ApiResponse.ok();
    }

    /* ============== 打开 / 发送 ============== */

    @PostMapping("/conversations/{otherUserId}")
    public ApiResponse<Conversation> openOrGet(@PathVariable("otherUserId") String otherUserId) {
        String me = CurrentUser.requireUserId();
        if (me.equals(otherUserId)) throw new BizException("不能给自己发消息");
        String a = me.compareTo(otherUserId) < 0 ? me : otherUserId;
        String b = me.compareTo(otherUserId) < 0 ? otherUserId : me;
        return ApiResponse.ok(convRepo.findByUserAIdOrUserBIdOrderByLastMessageAtDesc(me, me).stream()
                .filter(c -> (c.getUserAId().equals(a) && c.getUserBId().equals(b)))
                .findFirst()
                .orElseGet(() -> {
                    Conversation c = Conversation.builder()
                            .conversationId("c_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                            .userAId(a).userBId(b)
                            .createdAt(LocalDateTime.now())
                            .lastMessageAt(LocalDateTime.now())
                            .expired(false)
                            .build();
                    return convRepo.save(c);
                }));
    }

    @PostMapping("/conversations/{id}/send")
    @Transactional
    public ApiResponse<Message> send(@PathVariable("id") String id, @RequestBody SendReq req) {
        String uid = ensureMember(id);
        Conversation conv = convRepo.findById(id).orElseThrow();

        Message m = msgRepo.save(Message.builder()
                .conversationId(id).senderId(uid).content(req.getContent())
                .createdAt(LocalDateTime.now()).build());

        conv.setLastMessage(req.getContent().length() > 100
                ? req.getContent().substring(0, 100) : req.getContent());
        conv.setLastMessageAt(LocalDateTime.now());

        // 发送方自己默认已读到当下
        if (uid.equals(conv.getUserAId())) conv.setLastReadAtA(LocalDateTime.now());
        else                               conv.setLastReadAtB(LocalDateTime.now());
        convRepo.save(conv);

        // 异步邮件通知（如对方开启了通知）
        String otherId = uid.equals(conv.getUserAId()) ? conv.getUserBId() : conv.getUserAId();
        userRepo.findById(otherId).ifPresent(other -> {
            if (Boolean.TRUE.equals(other.getNotifyEmail()) && other.getEmail() != null) {
                String fromName = userRepo.findById(uid).map(User::getDisplayName).orElse("一位同学");
                String preview = req.getContent().length() > 80
                        ? req.getContent().substring(0, 80) + "..."
                        : req.getContent();
                mailService.sendDirectMessageNotice(
                        other.getEmail(), fromName, preview,
                        webBaseUrl + "/messages?conversation=" + id);
            }
        });

        return ApiResponse.ok(m);
    }

    /* ============== 工具 ============== */

    private long unreadOf(Conversation c, String uid) {
        LocalDateTime lastRead;
        if (uid.equals(c.getUserAId())) lastRead = c.getLastReadAtA();
        else                            lastRead = c.getLastReadAtB();
        if (lastRead == null) lastRead = c.getCreatedAt() == null
                ? LocalDateTime.of(2000, 1, 1, 0, 0) : c.getCreatedAt();
        return msgRepo.countByConversationIdAndSenderIdNotAndCreatedAtAfter(
                c.getConversationId(), uid, lastRead);
    }

    private Map<String, Object> attachUnread(Conversation c, String uid) {
        Map<String, Object> m = new HashMap<>();
        m.put("conversationId", c.getConversationId());
        m.put("userAId", c.getUserAId());
        m.put("userBId", c.getUserBId());
        m.put("contextLabel", c.getContextLabel());
        m.put("lastMessage", c.getLastMessage());
        m.put("lastMessageAt", c.getLastMessageAt());
        m.put("expired", c.getExpired());
        m.put("createdAt", c.getCreatedAt());
        m.put("unread", unreadOf(c, uid));
        return m;
    }

    private void markRead(String convId, String uid) {
        Conversation c = convRepo.findById(convId).orElseThrow();
        if (uid.equals(c.getUserAId())) c.setLastReadAtA(LocalDateTime.now());
        else                            c.setLastReadAtB(LocalDateTime.now());
        convRepo.save(c);
    }

    private String ensureMember(String convId) {
        String uid = CurrentUser.requireUserId();
        Conversation c = convRepo.findById(convId).orElseThrow(() -> new BizException(404, "会话不存在"));
        if (!uid.equals(c.getUserAId()) && !uid.equals(c.getUserBId())) {
            throw new BizException(403, "无权访问该会话");
        }
        return uid;
    }

    @Data
    public static class SendReq { @NotBlank private String content; }
}
