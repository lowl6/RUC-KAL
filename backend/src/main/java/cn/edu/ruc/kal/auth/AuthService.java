package cn.edu.ruc.kal.auth;

import cn.edu.ruc.kal.admin.AuditLog;
import cn.edu.ruc.kal.admin.AuditLogRepository;
import cn.edu.ruc.kal.auth.AuthDtos.*;
import cn.edu.ruc.kal.common.BizException;
import cn.edu.ruc.kal.security.JwtUtil;
import cn.edu.ruc.kal.user.User;
import cn.edu.ruc.kal.user.UserRepository;
import cn.edu.ruc.kal.verify.MailService;
import cn.edu.ruc.kal.verify.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final AuditLogRepository auditRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    private final VerificationService verifier;
    private final MailService mailer;

    @Value("${kal.jwt.expire-minutes}")
    private long expireMinutes;

    /* ============== 邮箱验证码 ============== */

    public void sendEmailCode(EmailCodeReq req) {
        if (!verifier.verifyCaptcha(req.captchaId(), req.captchaCode())) {
            throw new BizException("图形验证码错误或已过期");
        }
        String email = req.email().toLowerCase().trim();
        if ("register".equals(req.purpose())) {
            if (!email.endsWith("@ruc.edu.cn")) {
                throw new BizException("自助注册仅支持 @ruc.edu.cn 校内邮箱；校外用户请联系管理员邀请开通");
            }
            if (userRepo.existsByEmail(email)) {
                throw new BizException("该邮箱已注册，请直接登录");
            }
        } else if ("reset".equals(req.purpose())) {
            if (!userRepo.existsByEmail(email)) {
                throw new BizException("该邮箱尚未注册");
            }
        }
        try {
            String code = verifier.issueEmailCode(email);
            mailer.sendCode(email, code);
        } catch (VerificationService.TooFastException tfe) {
            throw new BizException(429, tfe.getMessage());
        }
    }

    /* ============== 注册 ============== */

    @Transactional
    public TokenResp register(RegisterReq req) {
        String email = req.email().toLowerCase().trim();
        if (!email.endsWith("@ruc.edu.cn")) {
            throw new BizException("自助注册仅支持 @ruc.edu.cn 校内邮箱");
        }
        if (!verifier.verifyEmailCode(email, req.emailCode())) {
            throw new BizException("邮箱验证码错误或已过期，请重新获取");
        }
        if (userRepo.existsByEmail(email)) {
            throw new BizException("该邮箱已注册");
        }
        User u = User.builder()
                .userId("u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .email(email)
                .passwordHash(encoder.encode(req.password()))
                .name(req.name())
                .displayName(req.name())
                .deptName(req.deptName())
                .grade(req.grade())
                .role(User.Role.student)
                .status(User.Status.active)
                .notifyEmail(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepo.save(u);
        return issueToken(u);
    }

    /* ============== 找回密码 ============== */

    @Transactional
    public TokenResp resetPassword(ResetPasswordReq req) {
        String email = req.email().toLowerCase().trim();
        User u = userRepo.findByEmail(email).orElseThrow(() -> new BizException(404, "该邮箱尚未注册"));
        if (!verifier.verifyEmailCode(email, req.emailCode())) {
            throw new BizException("邮箱验证码错误或已过期，请重新获取");
        }
        u.setPasswordHash(encoder.encode(req.password()));
        u.setUpdatedAt(LocalDateTime.now());
        userRepo.save(u);
        verifier.clearEmailState(email);
        return issueToken(u);
    }

    /* ============== 登录 ============== */

    @Transactional
    public TokenResp login(LoginReq req) {
        if (!verifier.verifyCaptcha(req.captchaId(), req.captchaCode())) {
            throw new BizException("图形验证码错误或已过期");
        }
        User u = userRepo.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> new BizException(401, "邮箱或密码错误"));
        verifyPassword(u, req.password());
        return issueToken(u);
    }

    @Transactional
    public TokenResp adminLogin(AdminLoginReq req) {
        if (!verifier.verifyCaptcha(req.captchaId(), req.captchaCode())) {
            throw new BizException("图形验证码错误或已过期");
        }
        User u = userRepo.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> new BizException(401, "账号或密码错误"));
        verifyPassword(u, req.password());
        if (u.getRole() != User.Role.admin && u.getRole() != User.Role.super_admin) {
            throw new BizException(403, "该账号无管理后台权限");
        }
        auditRepo.save(AuditLog.builder()
                .actorId(u.getUserId())
                .actorName(u.getDisplayName())
                .action("admin_login")
                .createdAt(LocalDateTime.now())
                .detail("管理员登录")
                .build());
        return issueToken(u);
    }

    /* ============== 工具 ============== */

    private void verifyPassword(User u, String raw) {
        if (u.getStatus() == User.Status.disabled) throw new BizException(403, "账号已停用");
        if (u.getStatus() == User.Status.banned)   throw new BizException(403, "账号已被封禁");
        if (!encoder.matches(raw, u.getPasswordHash())) throw new BizException(401, "邮箱或密码错误");
        u.setLastLoginAt(LocalDateTime.now());
        userRepo.save(u);
    }

    private TokenResp issueToken(User u) {
        List<String> perms = u.getPermsCsv() == null || u.getPermsCsv().isBlank()
                ? List.of()
                : Arrays.stream(u.getPermsCsv().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        String token = jwt.issue(u.getUserId(), u.getRole().name(), perms);
        return new TokenResp(token, expireMinutes, toView(u));
    }

    public static UserView toView(User u) {
        List<String> perms = u.getPermsCsv() == null || u.getPermsCsv().isBlank()
                ? List.of()
                : Arrays.stream(u.getPermsCsv().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return new UserView(
                u.getUserId(), u.getEmail(), u.getName(), u.getDisplayName(),
                u.getDeptName(), u.getGrade(), u.getAvatarUrl(),
                u.getRole().name(), u.getStatus().name(), perms,
                u.getNotifyEmail());
    }
}
