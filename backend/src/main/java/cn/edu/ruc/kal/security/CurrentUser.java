package cn.edu.ruc.kal.security;

import cn.edu.ruc.kal.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {}

    public static AuthPrincipal getOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthPrincipal p)) return null;
        return p;
    }

    public static AuthPrincipal require() {
        AuthPrincipal p = getOrNull();
        if (p == null) throw new BizException(401, "未登录");
        return p;
    }

    public static String requireUserId() {
        return require().getUserId();
    }
}
