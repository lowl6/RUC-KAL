package cn.edu.ruc.kal.verify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单机进程内的验证码 / 邮箱验证码缓存。
 * 不依赖 Redis；上线规模 ~1000 用户够用。
 */
@Slf4j
@Service
public class VerificationService {

    /* ============== 图形验证码 ============== */

    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);
    private static final Duration EMAIL_CODE_TTL = Duration.ofMinutes(10);
    private static final Duration EMAIL_CODE_COOLDOWN = Duration.ofMinutes(1);

    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final char[] EMAIL_CODE_CHARS = "0123456789".toCharArray();

    private final Map<String, Entry> captchaCache = new ConcurrentHashMap<>();
    private final Map<String, Entry> emailCodeCache = new ConcurrentHashMap<>();
    private final Map<String, Instant> emailLastSent = new ConcurrentHashMap<>();

    public Captcha issueCaptcha() {
        String id = UUID.randomUUID().toString().replace("-", "");
        String code = randomString(CAPTCHA_CHARS, 4);
        captchaCache.put(id, new Entry(code, Instant.now().plus(CAPTCHA_TTL)));
        sweep(captchaCache);
        return new Captcha(id, code);
    }

    public boolean verifyCaptcha(String id, String input) {
        if (id == null || input == null) return false;
        Entry e = captchaCache.remove(id);
        if (e == null) return false;
        if (Instant.now().isAfter(e.expiresAt)) return false;
        return e.value.equalsIgnoreCase(input.trim());
    }

    /* ============== 邮箱验证码 ============== */

    public String issueEmailCode(String email) {
        Instant last = emailLastSent.get(email);
        if (last != null && Duration.between(last, Instant.now()).compareTo(EMAIL_CODE_COOLDOWN) < 0) {
            long waitSec = EMAIL_CODE_COOLDOWN.minus(Duration.between(last, Instant.now())).getSeconds();
            throw new TooFastException("请求过于频繁，请 " + Math.max(1, waitSec) + " 秒后再试");
        }
        String code = randomString(EMAIL_CODE_CHARS, 6);
        emailCodeCache.put(email, new Entry(code, Instant.now().plus(EMAIL_CODE_TTL)));
        emailLastSent.put(email, Instant.now());
        sweep(emailCodeCache);
        return code;
    }

    public boolean verifyEmailCode(String email, String input) {
        if (email == null || input == null) return false;
        Entry e = emailCodeCache.get(email);
        if (e == null) return false;
        if (Instant.now().isAfter(e.expiresAt)) {
            emailCodeCache.remove(email);
            return false;
        }
        if (!e.value.equals(input.trim())) return false;
        emailCodeCache.remove(email);
        return true;
    }

    /** 重置密码等敏感操作成功后，清掉冷却与残留验证码，允许用户再次发起同类流程。 */
    public void clearEmailState(String email) {
        if (email == null) return;
        emailCodeCache.remove(email);
        emailLastSent.remove(email);
    }

    /* ============== 工具 ============== */

    private static String randomString(char[] pool, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(pool[RNG.nextInt(pool.length)]);
        return sb.toString();
    }

    private static void sweep(Map<String, Entry> cache) {
        Instant now = Instant.now();
        cache.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt));
    }

    public record Captcha(String id, String code) {}

    private record Entry(String value, Instant expiresAt) {}

    public static class TooFastException extends RuntimeException {
        public TooFastException(String m) { super(m); }
    }
}
