package cn.edu.ruc.kal.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AuthPrincipal {
    private String userId;
    private String role;
    private List<String> perms;

    public boolean isAdmin() {
        return "admin".equals(role) || "super_admin".equals(role);
    }
}
