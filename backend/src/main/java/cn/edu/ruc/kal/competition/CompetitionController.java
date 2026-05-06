package cn.edu.ruc.kal.competition;

import cn.edu.ruc.kal.common.ApiResponse;
import cn.edu.ruc.kal.common.BizException;
import cn.edu.ruc.kal.competition.CompetitionDtos.NewsView;
import cn.edu.ruc.kal.competition.CompetitionDtos.View;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionRepository repo;
    private final CompetitionNewsRepository newsRepo;

    @GetMapping("/competitions")
    public ApiResponse<List<View>> list() {
        return ApiResponse.ok(repo.findAll().stream().map(CompetitionDtos::toView).toList());
    }

    @GetMapping("/competitions/{id}")
    public ApiResponse<View> get(@PathVariable("id") String id) {
        Competition c = repo.findById(id).orElseThrow(() -> new BizException(404, "比赛不存在"));
        return ApiResponse.ok(CompetitionDtos.toView(c));
    }

    @GetMapping("/competitions/{id}/news")
    public ApiResponse<List<NewsView>> newsByCompetition(@PathVariable("id") String id) {
        return ApiResponse.ok(newsRepo.findByCompetitionIdOrderByPublishAtDesc(id)
                .stream().map(CompetitionDtos::toView).toList());
    }

    @GetMapping("/news")
    public ApiResponse<List<NewsView>> latestNews() {
        return ApiResponse.ok(newsRepo.findAll().stream()
                .filter(n -> !"hidden".equals(n.getStatus()))
                .sorted((a, b) -> {
                    if (a.getPublishAt() == null) return 1;
                    if (b.getPublishAt() == null) return -1;
                    return b.getPublishAt().compareTo(a.getPublishAt());
                })
                .map(CompetitionDtos::toView)
                .toList());
    }

    @GetMapping("/news/{id}")
    public ApiResponse<NewsView> newsDetail(@PathVariable("id") String id) {
        var n = newsRepo.findById(id).orElseThrow(() -> new BizException(404, "资讯不存在"));
        return ApiResponse.ok(CompetitionDtos.toView(n));
    }
}
