package cn.edu.ruc.kal.user;

import cn.edu.ruc.kal.auth.AuthService;
import cn.edu.ruc.kal.auth.AuthDtos.UserView;
import cn.edu.ruc.kal.common.ApiResponse;
import cn.edu.ruc.kal.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepo;

    @GetMapping("/{id}")
    public ApiResponse<UserView> get(@PathVariable("id") String id) {
        User u = userRepo.findById(id).orElseThrow(() -> new BizException(404, "用户不存在"));
        return ApiResponse.ok(AuthService.toView(u));
    }
}
