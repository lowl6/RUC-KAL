package cn.edu.ruc.kal.competition;

import cn.edu.ruc.kal.common.JsonText;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class CompetitionDtos {

    /**
     * 根据「报名开始 / 截止日期」自动推算比赛状态：
     * <ul>
     *   <li>now &lt; registerStart  → upcoming（即将开放）</li>
     *   <li>now ∈ [start, end] 且距 end &gt; 7 天 → active（进行中）</li>
     *   <li>now ∈ [start, end] 且距 end ≤ 7 天 → urgent（即将截止）</li>
     *   <li>now &gt; registerEnd → ended（已结束）</li>
     * </ul>
     * 任一日期为空时，回退到入库时管理员手填的 status。
     */
    public static String computeStatus(LocalDate start, LocalDate end, Competition.Status fallback) {
        LocalDate today = LocalDate.now();
        if (start == null || end == null) return fallback == null ? "upcoming" : fallback.name();
        if (today.isBefore(start)) return "upcoming";
        if (today.isAfter(end))    return "ended";
        long daysLeft = ChronoUnit.DAYS.between(today, end);
        return daysLeft <= 7 ? "urgent" : "active";
    }

    public record View(
            String competitionId,
            String name,
            String shortName,
            String initial,
            String level,
            String organizer,
            LocalDate registerStart,
            LocalDate registerEnd,
            Integer projectCount,
            String status,
            String description,
            String posterUrl,
            String prize,
            String scheduleNote,
            String contactEmail,
            String contactPhone,
            List<Map<String, String>> officialLinks,
            List<Map<String, String>> qrCodes
    ) {}

    public record NewsView(
            String newsId,
            String competitionId,
            String title,
            String source,
            String summary,
            String content,
            String link,
            String coverUrl,
            LocalDateTime publishAt,
            String status,
            Integer sortOrder
    ) {}

    public static View toView(Competition c) {
        return new View(
                c.getCompetitionId(), c.getName(), c.getShortName(), c.getInitial(),
                c.getLevel() == null ? null : c.getLevel().name(),
                c.getOrganizer(), c.getRegisterStart(), c.getRegisterEnd(),
                c.getProjectCount(),
                computeStatus(c.getRegisterStart(), c.getRegisterEnd(), c.getStatus()),
                c.getDescription(),
                c.getPosterUrl(), c.getPrize(), c.getScheduleNote(),
                c.getContactEmail(), c.getContactPhone(),
                JsonText.parseList(c.getOfficialLinksJson()),
                JsonText.parseList(c.getQrCodesJson())
        );
    }

    public static NewsView toView(CompetitionNews n) {
        return new NewsView(
                n.getNewsId(), n.getCompetitionId(), n.getTitle(),
                n.getSource(), n.getSummary(), n.getContent(),
                n.getLink(), n.getCoverUrl(),
                n.getPublishAt(),
                n.getStatus(),
                n.getSortOrder()
        );
    }
}
