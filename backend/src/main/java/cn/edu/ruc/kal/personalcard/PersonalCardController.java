package cn.edu.ruc.kal.personalcard;

import cn.edu.ruc.kal.common.ApiResponse;
import cn.edu.ruc.kal.common.BizException;
import cn.edu.ruc.kal.common.PageResult;
import cn.edu.ruc.kal.security.CurrentUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PersonalCardController {

    private final PersonalCardRepository repo;

    @GetMapping("/public/personal-cards")
    public ApiResponse<PageResult<View>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = repo.search(keyword, role, PersonalCard.Status.active,
                PageRequest.of(Math.max(0, page - 1), Math.min(size, 50)));
        return ApiResponse.ok(PageResult.of(p, PersonalCardController::toView));
    }

    @GetMapping("/public/personal-cards/{id}")
    public ApiResponse<View> get(@PathVariable("id") String id) {
        var c = repo.findById(id).orElseThrow(() -> new BizException(404, "卡片不存在"));
        return ApiResponse.ok(toView(c));
    }

    @GetMapping("/personal-cards/mine")
    public ApiResponse<View> mine() {
        String uid = CurrentUser.requireUserId();
        return ApiResponse.ok(repo.findByUserId(uid).map(PersonalCardController::toView).orElse(null));
    }

    @PostMapping("/personal-cards")
    public ApiResponse<View> upsert(@RequestBody UpsertReq req) {
        String uid = CurrentUser.requireUserId();
        var existing = repo.findByUserId(uid).orElse(null);
        var card = existing != null ? existing : PersonalCard.builder()
                .cardId("pc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .userId(uid)
                .visibility("public")
                .status(PersonalCard.Status.active)
                .createdAt(LocalDateTime.now())
                .build();
        card.setDisplayName(req.getDisplayName());
        card.setTargetRole(req.getTargetRole());
        card.setWeeklyHours(req.getWeeklyHours());
        card.setVacationAvailable(req.getVacationAvailable());
        card.setSkillsCsv(req.getSkills() == null ? null : String.join(",", req.getSkills()));
        card.setSelfIntro(req.getSelfIntro());
        card.setInterestedCompetitionsCsv(req.getInterestedCompetitions() == null ? null
                : String.join(",", req.getInterestedCompetitions()));
        card.setVisibility(req.getVisibility() == null ? "public" : req.getVisibility());
        card.setUpdatedAt(LocalDateTime.now());
        repo.save(card);
        return ApiResponse.ok(toView(card));
    }

    @Data
    public static class UpsertReq {
        private String displayName;
        private String targetRole;
        private Integer weeklyHours;
        private Boolean vacationAvailable;
        private List<String> skills;
        private String selfIntro;
        private List<String> interestedCompetitions;
        private String visibility;
    }

    @Data
    @AllArgsConstructor
    public static class View {
        private String cardId;
        private String userId;
        private String displayName;
        private String targetRole;
        private Integer weeklyHours;
        private Boolean vacationAvailable;
        private List<String> skills;
        private String selfIntro;
        private List<String> interestedCompetitions;
        private String visibility;
        private String status;
    }

    public static View toView(PersonalCard c) {
        return new View(
                c.getCardId(), c.getUserId(), c.getDisplayName(), c.getTargetRole(),
                c.getWeeklyHours(), c.getVacationAvailable(),
                csv(c.getSkillsCsv()), c.getSelfIntro(),
                csv(c.getInterestedCompetitionsCsv()),
                c.getVisibility(),
                c.getStatus() == null ? null : c.getStatus().name()
        );
    }

    private static List<String> csv(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }
}
