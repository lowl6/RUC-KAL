package cn.edu.ruc.kal.report;

import cn.edu.ruc.kal.admin.AuditLog;
import cn.edu.ruc.kal.admin.AuditLogRepository;
import cn.edu.ruc.kal.common.ApiResponse;
import cn.edu.ruc.kal.common.BizException;
import cn.edu.ruc.kal.security.CurrentUser;
import cn.edu.ruc.kal.user.User;
import cn.edu.ruc.kal.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final Set<String> TARGET_TYPES = Set.of("project", "forum_post", "news");

    private final AuditLogRepository auditRepo;
    private final UserRepository userRepo;

    @PostMapping
    public ApiResponse<Void> create(@RequestBody ReportReq req, HttpServletRequest request) {
        String uid = CurrentUser.requireUserId();
        String targetType = req.getTargetType() == null ? "" : req.getTargetType().trim();
        String targetId = req.getTargetId() == null ? "" : req.getTargetId().trim();
        String reason = req.getReason() == null ? "" : req.getReason().trim();

        if (!TARGET_TYPES.contains(targetType)) throw new BizException(400, "不支持的举报类型");
        if (targetId.isBlank()) throw new BizException(400, "举报目标不能为空");
        if (reason.isBlank()) throw new BizException(400, "请填写举报原因");

        String actorName = userRepo.findById(uid).map(User::getDisplayName).orElse(uid);
        auditRepo.save(AuditLog.builder()
                .actorId(uid)
                .actorName(actorName)
                .action("report_create")
                .targetType(targetType)
                .targetId(targetId)
                .detail(reason)
                .ip(resolveIp(request))
                .createdAt(LocalDateTime.now())
                .build());
        return ApiResponse.ok();
    }

    private static String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Data
    public static class ReportReq {
        @NotBlank
        private String targetType;
        @NotBlank
        private String targetId;
        @NotBlank
        private String reason;
    }
}