package cn.edu.ruc.kal.verify;

import java.security.SecureRandom;

/**
 * 用纯 SVG 绘制 4 字验证码，前端直接 inline。
 * 字符 / 干扰线 / 噪点都使用人大红色系，与全站一致。
 */
public final class CaptchaSvg {

    private static final SecureRandom RNG = new SecureRandom();

    private static final String[] PALETTE = {
            "#861a12", "#a15448", "#610b08", "#94352b", "#bf6a5d", "#c7bcb9"
    };

    private CaptchaSvg() {}

    public static String render(String text) {
        int w = 132, h = 44;
        StringBuilder sb = new StringBuilder(2048);
        sb.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ")
          .append(w).append(' ').append(h).append("' width='").append(w).append("' height='").append(h)
          .append("' role='img' aria-label='captcha'>");

        // 背景
        sb.append("<rect width='100%' height='100%' rx='6' ry='6' fill='#fdf7f5' stroke='#e6dad6'/>");

        // 干扰线 3 条
        for (int i = 0; i < 3; i++) {
            sb.append("<path d='M ")
              .append(rand(0, 20)).append(' ').append(rand(8, h - 8))
              .append(" Q ").append(rand(w / 3, w * 2 / 3)).append(' ').append(rand(0, h))
              .append(' ').append(rand(w - 20, w)).append(' ').append(rand(8, h - 8))
              .append("' stroke='").append(pick()).append("' stroke-width='")
              .append(0.6 + RNG.nextDouble() * 0.6)
              .append("' fill='none' opacity='0.55'/>");
        }

        // 噪点 18 个
        for (int i = 0; i < 18; i++) {
            sb.append("<circle cx='").append(rand(2, w - 2))
              .append("' cy='").append(rand(2, h - 2))
              .append("' r='").append(0.6 + RNG.nextDouble() * 1.2).append("' fill='")
              .append(pick()).append("' opacity='0.45'/>");
        }

        // 文字
        int n = text.length();
        int slot = (w - 24) / n;
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            int cx = 14 + slot * i + slot / 2 + rand(-3, 3);
            int cy = h / 2 + rand(-3, 5) + 9;
            int rot = rand(-20, 20);
            sb.append("<text x='").append(cx).append("' y='").append(cy)
              .append("' fill='").append(PALETTE[i % PALETTE.length])
              .append("' font-size='24' font-weight='600'")
              .append(" font-family=\"&quot;Cormorant Garamond&quot;,&quot;Source Han Serif SC&quot;,serif\"")
              .append(" text-anchor='middle' transform='rotate(").append(rot).append(' ')
              .append(cx).append(' ').append(cy).append(")'>")
              .append(c).append("</text>");
        }

        sb.append("</svg>");
        return sb.toString();
    }

    private static int rand(int lo, int hi) { return lo + RNG.nextInt(Math.max(1, hi - lo)); }
    private static String pick() { return PALETTE[RNG.nextInt(PALETTE.length)]; }
}
