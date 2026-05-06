package cn.edu.ruc.kal.news;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从微信公众号文章 URL（{@code https://mp.weixin.qq.com/s/...}）抓取元信息。
 *
 * <p>公众号文章页面对未登录的桌面 UA 是开放的，{@code og:title / og:image / nickname /
 * publish_time / ct} 这些字段可以稳定取到，无需 cookie。</p>
 *
 * <p>失败原因：网络不通 / 风控（少数文章会跳验证页）/ 链接被原作者删除。
 * 任何失败都不会抛异常，返回 null 由调用方记账。</p>
 */
@Service
@Slf4j
public class WechatLinkImporter {

    /** 桌面 Chrome UA，足以拿到完整 og 元数据。 */
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(6);
    private static final Duration READ_TIMEOUT    = Duration.ofSeconds(10);

    /** og:title  → 文章标题 */
    private static final Pattern P_OG_TITLE =
            Pattern.compile("<meta\\s+property=\"og:title\"\\s+content=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    /** og:description → 摘要（公众号有时为空） */
    private static final Pattern P_OG_DESC =
            Pattern.compile("<meta\\s+property=\"og:description\"\\s+content=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    /** og:image → 封面 */
    private static final Pattern P_OG_IMAGE =
            Pattern.compile("<meta\\s+property=\"og:image\"\\s+content=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    /** var nickname = htmlDecode("公众号名"); → 来源 */
    private static final Pattern P_NICKNAME =
            Pattern.compile("var\\s+nickname\\s*=\\s*htmlDecode\\(\"([^\"]*)\"\\)\\s*;");
    /** 兜底：var nickname = "..."; */
    private static final Pattern P_NICKNAME_PLAIN =
            Pattern.compile("var\\s+nickname\\s*=\\s*\"([^\"]*)\"\\s*;");
    /** var ct = "1730851200"; → 发布时间（秒） */
    private static final Pattern P_CT =
            Pattern.compile("var\\s+ct\\s*=\\s*\"(\\d{10})\"\\s*;");
    /** 备用 publish_time = "2024-11-06" */
    private static final Pattern P_PUBLISH_TIME =
            Pattern.compile("var\\s+publish_time\\s*=\\s*\"([^\"]*)\"\\s*;");
    /** 风控页特征：「环境异常」 */
    private static final Pattern P_CAPTCHA =
            Pattern.compile("环境异常|完成验证后即可继续访问", Pattern.UNICODE_CASE);

    public Result fetch(String url) {
        if (url == null || !url.startsWith("https://mp.weixin.qq.com/")) {
            return Result.fail("仅支持 https://mp.weixin.qq.com/s/... 形式的公众号链接");
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(READ_TIMEOUT)
                    .header("User-Agent", UA)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                return Result.fail("HTTP " + resp.statusCode());
            }
            String html = resp.body();
            if (html == null || html.isEmpty()) {
                return Result.fail("响应为空");
            }
            if (P_CAPTCHA.matcher(html).find()) {
                return Result.fail("被微信风控拦截（环境异常）— 稍后重试或换网络重试");
            }

            String title = first(html, P_OG_TITLE);
            if (title == null || title.isBlank()) {
                return Result.fail("未在页面中找到 og:title — 链接可能已失效");
            }

            String summary  = first(html, P_OG_DESC);
            String cover    = first(html, P_OG_IMAGE);
            String nickname = first(html, P_NICKNAME);
            if (nickname == null) nickname = first(html, P_NICKNAME_PLAIN);

            LocalDateTime publishAt = null;
            String ctSec = first(html, P_CT);
            if (ctSec != null) {
                try {
                    long sec = Long.parseLong(ctSec);
                    publishAt = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(sec), ZoneId.systemDefault());
                } catch (NumberFormatException ignore) {}
            }
            if (publishAt == null) {
                String pt = first(html, P_PUBLISH_TIME);
                if (pt != null && !pt.isBlank()) {
                    try {
                        // 形如 "2024-11-06" 或 "2024-11-06 12:34"
                        String s = pt.length() == 10 ? pt + " 00:00" : pt;
                        publishAt = LocalDateTime.parse(s.replace(' ', 'T'));
                    } catch (Exception ignore) {}
                }
            }

            return Result.ok(
                    decode(title),
                    decode(nickname),
                    decode(summary),
                    cover,
                    publishAt
            );
        } catch (Exception e) {
            log.warn("[wx-import] 抓取失败 url={} err={}", url, e.toString());
            return Result.fail("抓取失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    private static String first(String html, Pattern p) {
        Matcher m = p.matcher(html);
        return m.find() ? m.group(1) : null;
    }

    private static String decode(String s) {
        if (s == null) return null;
        // og:title 里常见的 HTML 实体（&ldquo; &rdquo; &amp; ...）
        return HtmlUtils.htmlUnescape(s).trim();
    }

    /** 抓取结果（成功 / 失败统一对外） */
    public record Result(
            boolean ok,
            String error,
            String title,
            String source,
            String summary,
            String coverUrl,
            LocalDateTime publishAt
    ) {
        public static Result ok(String title, String source, String summary,
                                String coverUrl, LocalDateTime publishAt) {
            return new Result(true, null, title, source, summary, coverUrl, publishAt);
        }
        public static Result fail(String error) {
            return new Result(false, error, null, null, null, null, null);
        }
    }
}
