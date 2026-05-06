package cn.edu.ruc.kal.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AuthDtos {

    public record LoginReq(
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotBlank String captchaId,
            @NotBlank String captchaCode
    ) {}

    public record RegisterReq(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank String name,
            String deptName,
            String grade,
            @NotBlank String emailCode,
            // 兼容老前端字段，但已不再强制（注册阶段仅校验邮箱验证码）
            String captchaId,
            String captchaCode
    ) {}

    public record AdminLoginReq(
            @NotBlank String email,
            @NotBlank String password,
            @NotBlank String captchaId,
            @NotBlank String captchaCode
    ) {}

    public record EmailCodeReq(
            @NotBlank @Email String email,
            @NotBlank String captchaId,
            @NotBlank String captchaCode,
            /** register | reset 等场景标识 */
            String purpose
    ) {}

    public record ResetPasswordReq(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank String emailCode
    ) {}

    public record TokenResp(
            String token,
            long expireMinutes,
            UserView user
    ) {}

    public record UserView(
            String userId,
            String email,
            String name,
            String displayName,
            String deptName,
            String grade,
            String avatarUrl,
            String role,
            String status,
            List<String> perms,
            Boolean notifyEmail
    ) {}
}
