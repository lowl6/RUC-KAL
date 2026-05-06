package cn.edu.ruc.kal.auth;

import cn.edu.ruc.kal.auth.AuthDtos.*;
import cn.edu.ruc.kal.common.ApiResponse;
import cn.edu.ruc.kal.common.BizException;
import cn.edu.ruc.kal.security.CurrentUser;
import cn.edu.ruc.kal.user.User;
import cn.edu.ruc.kal.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepo;

    @PostMapping("/email-code")
    public ApiResponse<Void> emailCode(@RequestBody @Valid EmailCodeReq req) {
        authService.sendEmailCode(req);
        return ApiResponse.ok();
    }

    @PostMapping("/register")
    public ApiResponse<TokenResp> register(@RequestBody @Valid RegisterReq req) {
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/reset-password")
    public ApiResponse<TokenResp> resetPassword(@RequestBody @Valid ResetPasswordReq req) {
        return ApiResponse.ok(authService.resetPassword(req));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResp> login(@RequestBody @Valid LoginReq req) {
        return ApiResponse.ok(authService.login(req));
    }

    @PostMapping("/admin-login")
    public ApiResponse<TokenResp> adminLogin(@RequestBody @Valid AdminLoginReq req) {
        return ApiResponse.ok(authService.adminLogin(req));
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me() {
        String uid = CurrentUser.requireUserId();
        User u = userRepo.findById(uid).orElseThrow(() -> new BizException(401, "未登录"));
        return ApiResponse.ok(AuthService.toView(u));
    }

    /** 用户开启 / 关闭新私信邮件提醒 */
    @PatchMapping("/notify-email")
    public ApiResponse<Void> updateNotifyEmail(@RequestBody Map<String, Boolean> req) {
        String uid = CurrentUser.requireUserId();
        User u = userRepo.findById(uid).orElseThrow(() -> new BizException(401, "未登录"));
        u.setNotifyEmail(Boolean.TRUE.equals(req.get("enabled")));
        userRepo.save(u);
        return ApiResponse.ok();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }
}
