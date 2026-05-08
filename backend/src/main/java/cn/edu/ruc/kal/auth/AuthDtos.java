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
            /** 学位类型：本科 / 研究生 / 教师；其他值（含"工作人员"/"校外"）一律拒绝 */
            String degreeType,
            @NotBlank String emailCode,
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
