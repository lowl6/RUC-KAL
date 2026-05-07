package cn.edu.ruc.kal.verify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * 邮件发送：
 * - 当 {@code kal.mail.enabled=true} 且 SMTP 凭据完整时，走原生 SMTP；
 * - 否则降级为日志输出（开发环境下完全可用）。
 */
@Slf4j
@Service
public class MailService {

     @Value("${spring.mail.host:}")
     private String host;

     @Value("${spring.mail.port:465}")
     private int port;

     @Value("${spring.mail.username:}")
     private String username;

     @Value("${spring.mail.password:}")
     private String password;

     @Value("${spring.mail.properties.mail.smtp.ssl.enable:true}")
     private boolean sslEnabled;

     @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
     private boolean startTlsEnabled;

     @Value("${spring.mail.properties.mail.smtp.connectiontimeout:15000}")
     private int connectionTimeoutMs;

     @Value("${spring.mail.properties.mail.smtp.timeout:15000}")
     private int readTimeoutMs;

    @Value("${kal.mail.from:KAL 知行创坊 <no-reply@example.com>}")
    private String from;

    @Value("${kal.mail.enabled:false}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled
                && host != null && !host.isBlank()
                && username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }

    /** 验证码邮件 */
    public void sendCode(String to, String code) {
        String subject = "【知行创坊】您的邮箱验证码";
        String body = String.format("""
            您好，

            您正在通过知行创坊（KAL）注册账号或重置敏感操作。
            您的验证码为：%s
            该验证码 10 分钟内有效；如果不是您本人操作，请忽略此邮件。

            —— 中国人民大学 知行创坊
            """, code);
        send(to, subject, body);
    }

    /** 新私信通知 */
    @Async
    public void sendDirectMessageNotice(String to, String fromUser, String preview, String link) {
        String subject = "【知行创坊】您收到了一条新私信";
        String body = String.format("""
            您好，

            您在知行创坊收到了来自 %s 的新私信：

              「%s」

            前往查看：%s

            如希望关闭此提醒，请在「我的中心 · 账户设置」关闭邮件通知。
            —— 中国人民大学 知行创坊
            """, fromUser, preview, link);
        send(to, subject, body);
    }

    private void send(String to, String subject, String body) {
        if (!isEnabled()) {
            log.info("[mail-stub] -> {} | {}", to, subject);
            log.info("\n=========== EMAIL (from {}) ===========\nTo: {}\nSubject: {}\n\n{}=========================================",
                    from, to, subject, body);
            return;
        }
        try {
            sendViaSmtp(to, subject, body);
            log.info("[mail] sent -> {} | {}", to, subject);
        } catch (Exception e) {
            log.warn("[mail] send failed to {}: {}; falling back to log", to, e.getMessage());
            log.info("\n=========== EMAIL (FALLBACK · from {}) ===========\nTo: {}\nSubject: {}\n\n{}=========================================",
                    from, to, subject, body);
        }
    }

    private void sendViaSmtp(String to, String subject, String body) throws IOException {
        String sender = extractAddress(from);
        try (SmtpConnection conn = SmtpConnection.open(host, port, sslEnabled, connectionTimeoutMs, readTimeoutMs)) {
            conn.expect(220);
            conn.command("EHLO localhost", 250);

            if (startTlsEnabled && !sslEnabled) {
                conn.command("STARTTLS", 220);
                conn.upgradeToTls(host, port, readTimeoutMs);
                conn.command("EHLO localhost", 250);
            }

            conn.command("AUTH LOGIN", 334);
            conn.command(base64(username), 334);
            conn.command(base64(password), 235);
            conn.command("MAIL FROM:<" + sender + ">", 250);
            conn.command("RCPT TO:<" + to + ">", 250);
            conn.command("DATA", 354);
            conn.writeData(renderMessage(sender, to, subject, body));
            conn.command("QUIT", 221);
        }
    }

    private static String renderMessage(String from, String to, String subject, String body) {
        String encodedSubject = mimeBase64Header(subject);
        String encodedBody = Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(body.getBytes(StandardCharsets.UTF_8));
        return "From: <" + from + ">\r\n"
                + "To: <" + to + ">\r\n"
                + "Subject: " + encodedSubject + "\r\n"
                + "Date: " + DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()) + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: base64\r\n"
                + "\r\n"
                + encodedBody + "\r\n.\r\n";
    }

    private static String mimeBase64Header(String value) {
        return "=?UTF-8?B?" + base64(value) + "?=";
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String extractAddress(String raw) {
        if (raw == null) return "";
        int left = raw.indexOf('<');
        int right = raw.indexOf('>');
        if (left >= 0 && right > left) {
            return raw.substring(left + 1, right).trim();
        }
        return raw.trim();
    }

    private static final class SmtpConnection implements AutoCloseable {
        private Socket socket;
        private BufferedReader reader;
        private BufferedWriter writer;
        private final int timeoutMs;

        private SmtpConnection(Socket socket, int timeoutMs) throws IOException {
            this.socket = socket;
            this.timeoutMs = timeoutMs;
            this.socket.setSoTimeout(timeoutMs);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        static SmtpConnection open(String host, int port, boolean sslEnabled, int connectTimeoutMs, int readTimeoutMs) throws IOException {
            Socket socket;
            if (sslEnabled) {
                socket = SSLSocketFactory.getDefault().createSocket(host, port);
            } else {
                socket = new Socket(host, port);
            }
            socket.setSoTimeout(readTimeoutMs);
            return new SmtpConnection(socket, readTimeoutMs);
        }

        void upgradeToTls(String host, int port, int readTimeoutMs) throws IOException {
            // SSLSocketFactory.getDefault() 声明返回 SocketFactory（没有 createSocket(Socket,...) 重载），必须强转。
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            Socket tlsSocket = factory.createSocket(socket, host, port, true);
            tlsSocket.setSoTimeout(readTimeoutMs);
            this.socket = tlsSocket;
            this.reader = new BufferedReader(new InputStreamReader(tlsSocket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(tlsSocket.getOutputStream(), StandardCharsets.UTF_8));
        }

        void expect(int code) throws IOException {
            SmtpReply reply = readReply();
            if (reply.code != code) {
                throw new IOException("SMTP expected " + code + " but got " + reply.code + " :: " + reply.message);
            }
        }

        void command(String command, int expectedCode) throws IOException {
            writer.write(command);
            writer.write("\r\n");
            writer.flush();
            SmtpReply reply = readReply();
            if (reply.code != expectedCode) {
                throw new IOException("SMTP command failed [" + command + "] => " + reply.code + " :: " + reply.message);
            }
        }

        void writeData(String data) throws IOException {
            writer.write(data);
            writer.flush();
            SmtpReply reply = readReply();
            if (reply.code != 250) {
                throw new IOException("SMTP DATA failed => " + reply.code + " :: " + reply.message);
            }
        }

        private SmtpReply readReply() throws IOException {
            String first = reader.readLine();
            if (first == null) {
                throw new IOException("SMTP connection closed unexpectedly");
            }

            StringBuilder message = new StringBuilder(first);
            while (first.length() >= 4 && first.charAt(3) == '-') {
                first = reader.readLine();
                if (first == null) {
                    break;
                }
                message.append(" | ").append(first);
            }

            int code = Integer.parseInt(message.substring(0, 3));
            return new SmtpReply(code, message.toString());
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    private record SmtpReply(int code, String message) {}
}
