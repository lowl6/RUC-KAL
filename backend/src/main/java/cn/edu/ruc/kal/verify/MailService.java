package cn.edu.ruc.kal.verify;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 邮件发送：
 * - 当 {@code kal.mail.enabled=true} 且 {@link JavaMailSender} bean 可用时，走真实 SMTP；
 * - 否则降级为日志输出（开发环境下完全可用）。
 *
 * 当 {@code spring.mail.host} 为空时，Spring Boot 不会装配 JavaMailSender；此时即使
 * {@code kal.mail.enabled=true} 也会自动降级为日志，避免 NPE。
 */
@Slf4j
@Service
public class MailService {

    @Autowired(required = false)
    @Nullable
    private JavaMailSender mailSender;

    @Value("${kal.mail.from:KAL 知行创坊 <no-reply@example.com>}")
    private String from;

    @Value("${kal.mail.enabled:false}")
    private boolean enabled;

    public boolean isEnabled() { return enabled && mailSender != null; }

    /** 验证码邮件 */
    public void sendCode(String to, String code) {
        String subject = "【知行创坊】您的邮箱验证码";
        String body = String.format("""
            您好，

            您正在通过知行创坊（KAL）注册账号或重置敏感操作。
            您的验证码为：%s
            该验证码 10 分钟内有效；如果不是您本人操作，请忽略此邮件。

            —— 中国人民大学 知行创坊
            """, code);
        send(to, subject, body);
    }

    /** 新私信通知 */
    @Async
    public void sendDirectMessageNotice(String to, String fromUser, String preview, String link) {
        String subject = "【知行创坊】您收到了一条新私信";
        String body = String.format("""
            您好，

            您在知行创坊收到了来自 %s 的新私信：

              「%s」

            前往查看：%s

            如希望关闭此提醒，请在「我的中心 · 账户设置」关闭邮件通知。
            —— 中国人民大学 知行创坊
            """, fromUser, preview, link);
        send(to, subject, body);
    }

    private void send(String to, String subject, String body) {
        if (!isEnabled()) {
            log.info("[mail-stub] -> {} | {}", to, subject);
            log.info("\n=========== EMAIL (from {}) ===========\nTo: {}\nSubject: {}\n\n{}=========================================",
                    from, to, subject, body);
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(body);
            mailSender.send(m);
            log.info("[mail] sent -> {} | {}", to, subject);
        } catch (Exception e) {
            log.warn("[mail] send failed to {}: {}; falling back to log", to, e.getMessage());
            log.info("\n=========== EMAIL (FALLBACK · from {}) ===========\nTo: {}\nSubject: {}\n\n{}=========================================",
                    from, to, subject, body);
        }
    }
}
