package cn.edu.ruc.kal.seed;

import cn.edu.ruc.kal.common.JsonText;
import cn.edu.ruc.kal.competition.Competition;
import cn.edu.ruc.kal.competition.CompetitionNews;
import cn.edu.ruc.kal.competition.CompetitionNewsRepository;
import cn.edu.ruc.kal.competition.CompetitionRepository;
import cn.edu.ruc.kal.forum.ForumComment;
import cn.edu.ruc.kal.forum.ForumCommentRepository;
import cn.edu.ruc.kal.forum.ForumPost;
import cn.edu.ruc.kal.forum.ForumPostRepository;
import cn.edu.ruc.kal.personalcard.PersonalCard;
import cn.edu.ruc.kal.personalcard.PersonalCardRepository;
import cn.edu.ruc.kal.project.Project;
import cn.edu.ruc.kal.project.ProjectRepository;
import cn.edu.ruc.kal.project.ProjectRole;
import cn.edu.ruc.kal.user.User;
import cn.edu.ruc.kal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final ProjectRepository projectRepo;
    private final PersonalCardRepository cardRepo;
    private final CompetitionRepository competitionRepo;
    private final CompetitionNewsRepository newsRepo;
    private final ForumPostRepository forumRepo;
    private final ForumCommentRepository forumCommentRepo;
    private final PasswordEncoder enc;

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) {
            log.info("[seed] data already exists, skipped.");
            return;
        }
        log.info("[seed] inserting demo data...");
        seedUsers();
        seedCompetitions();
        seedNews();
        seedPersonalCards();
        seedProjects();
        seedForum();
        seedComments();
        log.info("[seed] done. users={}, projects={}, cards={}, forum={}, competitions={}, news={}, comments={}",
                userRepo.count(), projectRepo.count(), cardRepo.count(), forumRepo.count(),
                competitionRepo.count(), newsRepo.count(), forumCommentRepo.count());
    }

    /* ===================== 用户（含管理员） ===================== */

    private final List<User> users = new ArrayList<>();

    private void seedUsers() {
        // 超级管理员（建议上线后立即登录修改密码）
        users.add(addUser("u_admin_super",
                "kal-superadmin@ruc.edu.cn",
                "Kal#Super-2026@Admin",
                "知行总管理员", "知行总管理员", "管理团队", null,
                User.Role.super_admin,
                "user.read,user.write,user.role,project.moderate,forum.moderate,competition.write,system.config,audit.read"));

        // 运营管理员
        users.add(addUser("u_admin_ops",
                "kal-ops@ruc.edu.cn",
                "Kal#Ops-2026@Console",
                "运营管理员", "运营管理员", "管理团队", null,
                User.Role.admin,
                "user.read,project.moderate,forum.moderate,competition.write,audit.read"));

        users.add(addUser("u_li_yan",   "li.yan@ruc.edu.cn",   "Kal@2026", "李雁",   "李雁",   "信息学院",     "2023级", User.Role.student, null));
        users.add(addUser("u_zhao_xin", "zhao.xin@ruc.edu.cn", "Kal@2026", "赵昕",   "赵昕",   "商学院",       "2023级", User.Role.student, null));
        users.add(addUser("u_chen_yu",  "chen.yu@ruc.edu.cn",  "Kal@2026", "陈宇",   "陈宇",   "新闻学院",     "2022级", User.Role.student, null));
        users.add(addUser("u_wang_qi",  "wang.qi@ruc.edu.cn",  "Kal@2026", "王琦",   "王琦",   "财政金融学院", "2024级", User.Role.student, null));
        users.add(addUser("u_sun_meng", "sun.meng@ruc.edu.cn", "Kal@2026", "孙萌",   "孙萌",   "信息学院",     "2023级", User.Role.student, null));
        users.add(addUser("u_liu_xin",  "liu.xin@ruc.edu.cn",  "Kal@2026", "刘欣",   "刘欣",   "艺术学院",     "2023级", User.Role.student, null));
        users.add(addUser("u_prof_lin", "lin.shu@ruc.edu.cn",  "Kal@2026", "林书",   "林书 老师", "商学院",   null,     User.Role.teacher, null));
    }

    private User addUser(String id, String email, String pwd, String name, String displayName,
                         String dept, String grade, User.Role role, String perms) {
        User u = User.builder()
                .userId(id).email(email).passwordHash(enc.encode(pwd))
                .name(name).displayName(displayName)
                .deptName(dept).grade(grade)
                .role(role).status(User.Status.active)
                .permsCsv(perms)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        return userRepo.save(u);
    }

    /* ===================== 比赛 ===================== */

    private void seedCompetitions() {
        save(Competition.builder()
                .competitionId("cp_chuanhuibei").name("中国国际大学生创新大赛").shortName("中国国际大学生创新大赛").initial("创")
                .level(Competition.Level.national).organizer("教育部")
                .registerStart(LocalDate.now().minusDays(20)).registerEnd(LocalDate.now().plusDays(40))
                .projectCount(0).status(Competition.Status.urgent)
                .description("聚焦创新、创造、创业的国家级综合赛事。设高教、职教、国际、产业命题等多个赛道，鼓励真问题、真团队、真成长。")
                .prize("国家级金、银、铜奖；优秀作品入选成果展。")
                .scheduleNote("初赛 6 月 · 复赛 8 月 · 总决赛 10 月")
                .contactEmail("kal-competition@ruc.edu.cn").contactPhone("010-6251-1234")
                .officialLinksJson(JsonText.stringify(List.of(
                        Map.of("label", "官方主页", "url", "https://cy.ncss.cn/"),
                        Map.of("label", "校内通知", "url", "https://www.ruc.edu.cn/")
                )))
                .qrCodesJson(JsonText.stringify(List.of(
                        Map.of("label", "官方公众号", "imageUrl", "/icons/qr-placeholder.svg"),
                        Map.of("label", "校内组队群", "imageUrl", "/icons/qr-placeholder.svg")
                )))
                .build());

        save(Competition.builder()
                .competitionId("cp_challenge").name("“挑战杯”全国大学生课外学术科技作品竞赛").shortName("挑战杯").initial("挑")
                .level(Competition.Level.national).organizer("团中央")
                .registerStart(LocalDate.now().minusDays(10)).registerEnd(LocalDate.now().plusDays(70))
                .projectCount(0).status(Competition.Status.active)
                .description("面向本科生的高规格学术科创赛事。鼓励基础研究、应用研究与社会调查类成果。")
                .prize("特等奖、一等奖、二等奖、三等奖；优秀作品出版成果集。")
                .scheduleNote("立项 3 月 · 校赛 5 月 · 全国赛 11 月")
                .contactEmail("kal-competition@ruc.edu.cn").contactPhone("010-6251-1234")
                .officialLinksJson(JsonText.stringify(List.of(
                        Map.of("label", "官方主页", "url", "https://www.tiaozhanbei.net/"),
                        Map.of("label", "申报手册", "url", "https://www.tiaozhanbei.net/notice/")
                )))
                .qrCodesJson(JsonText.stringify(List.of(
                        Map.of("label", "校赛通知群", "imageUrl", "/icons/qr-placeholder.svg")
                )))
                .build());

        save(Competition.builder()
                .competitionId("cp_mathmodel").name("全国大学生数学建模竞赛").shortName("数学建模").initial("数")
                .level(Competition.Level.national).organizer("中国工业与应用数学学会")
                .registerStart(LocalDate.now().plusDays(30)).registerEnd(LocalDate.now().plusDays(90))
                .projectCount(0).status(Competition.Status.upcoming)
                .description("经典学科赛事，三人一队，72 小时建模与论文写作，考验团队的协作与抗压能力。")
                .prize("国家一等奖、国家二等奖、各赛区奖项。")
                .scheduleNote("报名 6-7 月 · 比赛 9 月初连续 72 小时")
                .contactEmail("kal-competition@ruc.edu.cn")
                .officialLinksJson(JsonText.stringify(List.of(
                        Map.of("label", "官方主页", "url", "http://www.mcm.edu.cn/")
                )))
                .qrCodesJson(JsonText.stringify(List.of(
                        Map.of("label", "建模交流群", "imageUrl", "/icons/qr-placeholder.svg")
                )))
                .build());

        save(Competition.builder()
                .competitionId("cp_business").name("“创青春”全国大学生创业大赛").shortName("创青春").initial("青")
                .level(Competition.Level.national).organizer("团中央")
                .registerStart(LocalDate.now().minusDays(60)).registerEnd(LocalDate.now().minusDays(5))
                .projectCount(0).status(Competition.Status.ended)
                .description("创业方向的代表性赛事，强调商业模式可行性与社会价值。")
                .prize("金银铜奖 + 创投基金对接机会。")
                .scheduleNote("本届已闭幕，下一届预计 2027 年 3 月启动。")
                .officialLinksJson(JsonText.stringify(List.of(
                        Map.of("label", "往届获奖项目", "url", "https://www.tiaozhanbei.net/")
                )))
                .build());
    }

    private void save(Competition c) { competitionRepo.save(c); }

    /* ===================== 资讯 ===================== */

    private void seedNews() {
        newsRepo.save(CompetitionNews.builder()
                .newsId("nw_001").competitionId("cp_chuanhuibei")
                .title("第 11 届中国国际大学生创新大赛 · 校内动员会")
                .source("学校教务处").summary("校内动员会议程、立项时间表、校赛评审规则一次说清楚。")
                .content("一、立项申报\n校内立项申报截止：报名截止日前 14 天。\n\n二、校赛评审\n采用「书面评审 + 路演答辩」两轮机制，每队答辩 8 分钟，问答 5 分钟。\n\n三、推荐名额\n根据校赛成绩按学院加权推荐，重点扶持跨学科组队项目。")
                .link("https://www.ruc.edu.cn/")
                .publishAt(LocalDateTime.now().minusDays(2))
                .status("published").sortOrder(0).build());

        newsRepo.save(CompetitionNews.builder()
                .newsId("nw_002").competitionId("cp_challenge")
                .title("挑战杯 · 立项辅导沙龙第 3 期")
                .source("校团委").summary("围绕「学术问题如何转化为可竞赛课题」展开，欢迎已有想法的同学带方案现场讨论。")
                .content("时间：每周三 19:00\n地点：知行楼 305\n本期主题：从一篇课程论文到一份课题立项书。\n\n建议带：\n1) 你已经写过的最像研究的一段文字\n2) 一个你想搞清楚的问题\n3) 任何已有数据 / 访谈记录")
                .publishAt(LocalDateTime.now().minusDays(5))
                .status("published").sortOrder(0).build());

        newsRepo.save(CompetitionNews.builder()
                .newsId("nw_003").competitionId(null)
                .title("知行创坊 · 学期赛事日历（建议收藏）")
                .source("知行创坊").summary("把全年主流赛事按时间轴排好，方便对照自己的学业节奏选择赛道。")
                .content("• 春季：挑战杯立项、数学建模培训\n• 夏季：互联网+ 报名、暑期集训\n• 秋季：建模赛、各类省赛冲刺\n• 冬季：复盘 + 论文产出 + 项目延展")
                .publishAt(LocalDateTime.now().minusDays(8))
                .status("published").sortOrder(0).build());

        /* ===== 来自「人大就业创业」公众号的 4 条原文链接 =====
           说明：公众号文章必须在微信内打开，标题与摘要由管理员在「资讯发布」页核对补全。
           以下链接、来源、发布时间均为转载入口，点击后跳转至原文。 */
        final String src = "人大就业创业（微信公众号）";
        final String[][] articles = new String[][] {
                { "nw_rucemp_001", "https://mp.weixin.qq.com/s/fhIV_t0HObfihum8Ny1WAw" },
                { "nw_rucemp_002", "https://mp.weixin.qq.com/s/ZPP8VGEuXIbguFOp2GDeDA" },
                { "nw_rucemp_003", "https://mp.weixin.qq.com/s/ENthdGZ7sk4PAmwZjX0tMg" },
                { "nw_rucemp_004", "https://mp.weixin.qq.com/s/gLR4HqoHY2ixeGSGmJ48RQ" },
        };
        int idx = 1;
        for (String[] a : articles) {
            newsRepo.save(CompetitionNews.builder()
                    .newsId(a[0])
                    .competitionId(null)
                    .title("人大就业创业 · 公众号推送 #" + idx)
                    .source(src)
                    .summary("来自「人大就业创业」公众号原文，点击右侧「查看原文」跳转微信阅读。")
                    .content("本条资讯为公众号原文转载入口，正文请前往原文阅读。\n\n如需把它登记为「比赛」并由系统按日期自动维护状态，请前往：管理后台 → 赛事发布 → 新建。")
                    .link(a[1])
                    .publishAt(LocalDateTime.now().minusDays(idx))
                    .status("published").sortOrder(idx)
                    .build());
            idx++;
        }
    }

    /* ===================== 个人卡 ===================== */

    private void seedPersonalCards() {
        cardRepo.save(PersonalCard.builder()
                .cardId("pc_li_yan").userId("u_li_yan").displayName("李雁")
                .targetRole("产品 / 用户研究").weeklyHours(8).vacationAvailable(true)
                .skillsCsv("用户访谈,信息架构,Figma,Notion")
                .selfIntro("信息学院 2023 级。习惯把模糊问题拆成可被验证的小步骤；偏好可被讨论的产品文档而非空中楼阁。")
                .interestedCompetitionsCsv("中国国际大学生创新大赛,挑战杯")
                .visibility("public").status(PersonalCard.Status.active)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        cardRepo.save(PersonalCard.builder()
                .cardId("pc_chen_yu").userId("u_chen_yu").displayName("陈宇")
                .targetRole("内容 / 品牌").weeklyHours(6).vacationAvailable(false)
                .skillsCsv("文案,品牌叙事,视频脚本")
                .selfIntro("新闻学院在读，关注「内容如何带来留存」。可以把理念翻成观众能记住的句子。")
                .interestedCompetitionsCsv("挑战杯")
                .visibility("public").status(PersonalCard.Status.active)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        cardRepo.save(PersonalCard.builder()
                .cardId("pc_sun_meng").userId("u_sun_meng").displayName("孙萌")
                .targetRole("前端工程师").weeklyHours(10).vacationAvailable(true)
                .skillsCsv("Vue3,TypeScript,可视化")
                .selfIntro("写界面的人。喜欢小步快跑，重视交互细节，可独立完成中等规模的前端项目。")
                .interestedCompetitionsCsv("中国国际大学生创新大赛,数学建模")
                .visibility("public").status(PersonalCard.Status.active)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    /* ===================== 项目卡 ===================== */

    private void seedProjects() {
        Project p1 = Project.builder()
                .projectId("p_kal_demo_1").projectName("校园闲置物品流转小程序")
                .oneLiner("让二手物品在熟人圈中更有温度地流动。")
                .projectType(Project.Type.entrepreneurship)
                .competitionShort("中国国际大学生创新大赛").competitionTarget("全国铜奖以上")
                .competitionDeadline(LocalDate.now().plusDays(60))
                .teamDeadline(LocalDate.now().plusDays(14))
                .currentMembers(2).neededCount(3).weeklyHours(8)
                .detail("我们用过两个月做了用户访谈和原型，已经明确目标用户与场景。现在要把它打磨成可投赛事的完整方案与可演示原型。")
                .tagsCsv("小程序,创业,熟人社交")
                .creatorId("u_li_yan")
                .viewCount(86).applyCount(4)
                .status(Project.Status.recruiting)
                .createdAt(LocalDateTime.now().minusDays(3)).updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        p1.getRoles().add(ProjectRole.builder().project(p1).roleName("前端工程师").count(1).skills("Vue3, 微信小程序").build());
        p1.getRoles().add(ProjectRole.builder().project(p1).roleName("视觉设计").count(1).skills("Figma, 品牌").build());
        p1.getRoles().add(ProjectRole.builder().project(p1).roleName("商业策划").count(1).skills("商业模式画布").build());
        projectRepo.save(p1);

        Project p2 = Project.builder()
                .projectId("p_kal_demo_2").projectName("城市步行可达性可视化")
                .oneLiner("用公开地图数据，让一座城市的「能不能走到」变得可被讨论。")
                .projectType(Project.Type.innovation)
                .competitionShort("挑战杯").competitionTarget("省赛入围")
                .competitionDeadline(LocalDate.now().plusDays(80))
                .teamDeadline(LocalDate.now().plusDays(21))
                .currentMembers(3).neededCount(2).weeklyHours(6)
                .detail("已经爬好北京六个城区的 OSM 数据，正在做可达性算法。需要会写报告 / 海报的同学一起把成果讲清楚。")
                .tagsCsv("数据可视化,城市,空间分析")
                .creatorId("u_zhao_xin")
                .viewCount(54).applyCount(2)
                .status(Project.Status.recruiting)
                .createdAt(LocalDateTime.now().minusDays(7)).updatedAt(LocalDateTime.now().minusDays(2))
                .build();
        p2.getRoles().add(ProjectRole.builder().project(p2).roleName("数据分析").count(1).skills("Python, 空间统计").build());
        p2.getRoles().add(ProjectRole.builder().project(p2).roleName("内容 / 海报").count(1).skills("学术文案, 排版").build());
        projectRepo.save(p2);

        Project p3 = Project.builder()
                .projectId("p_kal_demo_3").projectName("人大食堂菜品推荐")
                .oneLiner("一个不打扰人的、轻量推荐机制：今天我应该去哪个食堂、点什么。")
                .projectType(Project.Type.creation)
                .competitionShort("数学建模").competitionTarget("校级一等奖")
                .competitionDeadline(LocalDate.now().plusDays(35))
                .teamDeadline(LocalDate.now().plusDays(10))
                .currentMembers(1).neededCount(2).weeklyHours(5)
                .detail("已经收集到 3 个食堂的菜品/价格历史数据，希望搭一个轻量推荐模型，配一个简单网页界面。")
                .tagsCsv("推荐算法,校园生活,数据建模")
                .creatorId("u_wang_qi")
                .viewCount(31).applyCount(1)
                .status(Project.Status.recruiting)
                .createdAt(LocalDateTime.now().minusDays(2)).updatedAt(LocalDateTime.now())
                .build();
        p3.getRoles().add(ProjectRole.builder().project(p3).roleName("算法 / 后端").count(1).skills("Python, 推荐系统").build());
        p3.getRoles().add(ProjectRole.builder().project(p3).roleName("前端").count(1).skills("HTML, JS").build());
        projectRepo.save(p3);
    }

    /* ===================== 论坛 ===================== */

    private void seedForum() {
        forumRepo.save(ForumPost.builder()
                .postId("fp_demo_1").title("写一份能跑得起来的项目卡，比想象中更难")
                .content("最近翻了很多卡片，想分享几个判断「这个项目卡能不能聊」的小标准：\n\n1. 它告诉我「我能加入做什么」，而不是只在介绍这个项目本身有多酷；\n2. 它写了一个具体的近况，比如「目前已经做完哪一步」，而不是悬空的愿景；\n3. 它愿意承认自己缺什么——缺什么才是真正请人来一起做的理由。\n\n仅此一点：不要把项目卡写成路演 PPT。")
                .excerpt("从一名同行的视角，谈谈如何写出真正能引发共鸣的项目卡。")
                .topic("经验分享").authorId("u_li_yan").authorName("李雁")
                .pinned(true).essence(true)
                .viewCount(213).replyCount(2).likeCount(57)
                .status(ForumPost.Status.published)
                .createdAt(LocalDateTime.now().minusDays(4)).lastReplyAt(LocalDateTime.now().minusHours(3)).build());

        forumRepo.save(ForumPost.builder()
                .postId("fp_demo_2").title("征队友 | 挑战杯 城市可达性方向，需要写得动报告的文科同学")
                .content("项目方向是 OSM + 空间统计，已经跑通最小可视化。\n\n现在卡在「报告怎么讲清楚」这一步：希望有一位文科背景、能把研究问题讲成故事的同学加入。\n\n每周 4-6 小时，能在六周内完成省赛初稿的同学优先。")
                .excerpt("项目方向是 OSM + 空间统计，已经跑通最小可视化。")
                .topic("找队友").authorId("u_zhao_xin").authorName("赵昕")
                .pinned(false).essence(false)
                .viewCount(78).replyCount(1).likeCount(11)
                .status(ForumPost.Status.published)
                .createdAt(LocalDateTime.now().minusDays(2)).lastReplyAt(LocalDateTime.now().minusHours(8)).build());

        forumRepo.save(ForumPost.builder()
                .postId("fp_demo_3").title("校赛申报答辩，PPT 节奏要怎么排？")
                .content("过来人聊聊：评审注意力其实只有大约 6 分钟，你的故事节奏要按「问题—痛点—方案—证据—我们能持续做」这五拍来排。\n\n避免一上来就讲技术细节；评审记不住技术，他们记住的是「你为什么是合适的人」。")
                .excerpt("评审注意力其实只有大约 6 分钟，你的故事节奏要怎么排。")
                .topic("赛事问答").authorId("u_prof_lin").authorName("林书 老师")
                .pinned(false).essence(true)
                .viewCount(155).replyCount(1).likeCount(34)
                .status(ForumPost.Status.published)
                .createdAt(LocalDateTime.now().minusDays(6)).lastReplyAt(LocalDateTime.now().minusDays(1)).build());
    }

    /* ===================== 评论 ===================== */

    private void seedComments() {
        forumCommentRepo.save(ForumComment.builder()
                .postId("fp_demo_1").authorId("u_chen_yu").authorName("陈宇")
                .content("第 3 点真的关键。承认缺人，比假装齐备更让别人愿意聊。")
                .likeCount(6).status("published").createdAt(LocalDateTime.now().minusHours(20)).build());

        forumCommentRepo.save(ForumComment.builder()
                .postId("fp_demo_1").authorId("u_sun_meng").authorName("孙萌")
                .content("我会补一句：别只写「需要前端」，写「需要前端做这件事」。具体到任务才有勾子。")
                .likeCount(4).status("published").createdAt(LocalDateTime.now().minusHours(6)).build());

        forumCommentRepo.save(ForumComment.builder()
                .postId("fp_demo_2").authorId("u_chen_yu").authorName("陈宇")
                .content("感兴趣，下午私信你看看现有可视化。")
                .likeCount(2).status("published").createdAt(LocalDateTime.now().minusHours(2)).build());

        forumCommentRepo.save(ForumComment.builder()
                .postId("fp_demo_3").authorId("u_li_yan").authorName("李雁")
                .content("「评审记住的是为什么是你」这句话，我截图存了。")
                .likeCount(8).status("published").createdAt(LocalDateTime.now().minusHours(48)).build());
    }
}
