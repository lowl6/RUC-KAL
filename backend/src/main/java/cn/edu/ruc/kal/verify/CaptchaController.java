package cn.edu.ruc.kal.verify;

import cn.edu.ruc.kal.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final VerificationService service;

    @GetMapping
    public ApiResponse<Map<String, String>> issue() {
        var c = service.issueCaptcha();
        return ApiResponse.ok(Map.of(
                "id",  c.id(),
                "svg", CaptchaSvg.render(c.code())
        ));
    }
}
