package cn.edu.ruc.kal.competition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

/**
 * 定时把每个 Competition 的 status 字段刷成「按日期推算」的最新值。
 * 这样：
 *  - 仪表盘 countByStatus(...) 的统计口径始终准确；
 *  - 管理员在赛事表单里无需手动维护状态字段。
 *
 * 任何一次 admin upsert 也会立即写入派生状态（见 AdminController#upsertCompetition），
 * 此处的定时任务只是兜底每天 0 点 5 分根据当前日期再扫一遍。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CompetitionStatusScheduler {

    private final CompetitionRepository repo;

    @PostConstruct
    public void onStartup() {
        try {
            int n = refreshAll();
            log.info("[competition-status] 启动同步完成，刷新 {} 条记录", n);
        } catch (Exception e) {
            log.warn("[competition-status] 启动同步失败：{}", e.getMessage());
        }
    }

    /** 每天 00:05 重新计算所有比赛的状态字段。 */
    @Scheduled(cron = "0 5 0 * * *")
    public void daily() {
        int n = refreshAll();
        log.info("[competition-status] 每日同步完成，刷新 {} 条记录", n);
    }

    @Transactional
    public int refreshAll() {
        int touched = 0;
        for (Competition c : repo.findAll()) {
            String s = CompetitionDtos.computeStatus(c.getRegisterStart(), c.getRegisterEnd(), c.getStatus());
            try {
                Competition.Status next = Competition.Status.valueOf(s);
                if (c.getStatus() != next) {
                    c.setStatus(next);
                    repo.save(c);
                    touched++;
                }
            } catch (Exception ignore) {
                // 不影响其它条目
            }
        }
        return touched;
    }
}
