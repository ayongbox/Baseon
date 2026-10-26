package io.github.ayongbox.baseon.demo.controller;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.core.model.R;
import io.github.ayongbox.baseon.starter.auth.support.TokenPair;
import io.github.ayongbox.baseon.starter.auth.support.TokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * 5 期验证接口：登录签发双 Token、刷新、登出、鉴权上下文读取。
 * 登录/刷新路径已在 baseon.auth.ignore-paths 默认放行清单中，无需携带 Token。
 */
@RestController
@RequestMapping("/auth")
public class AuthDemoController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;

    public AuthDemoController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * 登录：校验口令后签发双 Token。
     * 内置两个演示账号仅用于本地跑通鉴权链路；生产环境必须对接用户中心
     * 做口令校验与角色权限聚合，禁止硬编码账号。
     */
    @PostMapping("/login")
    public R<TokenPair> login(@RequestBody LoginRequest request) {
        LoginUser user = authenticate(request.getUsername(), request.getPassword());
        return R.ok(tokenService.issueTokenPair(user));
    }

    /** 刷新：用 refresh token 换绑同一会话的新 access token */
    @PostMapping("/refresh")
    public R<String> refresh(@RequestBody RefreshRequest request) {
        return R.ok(tokenService.refresh(request.getRefreshToken()));
    }

    /** 登出：access token 进黑名单，对应 refresh 会话一并删除 */
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            tokenService.invalidate(authorization.substring(BEARER_PREFIX.length()));
        }
        return R.ok();
    }

    /** 当前登录用户：验证 AuthTokenFilter 验签后的上下文写入 */
    @GetMapping("/me")
    public R<LoginUser> me() {
        return R.ok(SecurityUtil.get());
    }

    /**
     * 演示账号校验：admin 拥有创建与查询权限，viewer 只读。
     * 口令不匹配（含账号不存在）统一抛 1002，不泄露账号是否存在。
     */
    private LoginUser authenticate(String username, String password) {
        if ("admin".equals(username) && "123456".equals(password)) {
            return buildUser(1L, "admin", Set.of("user:create", "user:list"));
        }
        if ("viewer".equals(username) && "123456".equals(password)) {
            return buildUser(2L, "viewer", Set.of("user:list"));
        }
        throw new BizException(CommonErrorCode.UNAUTHORIZED);
    }

    private LoginUser buildUser(Long userId, String username, Set<String> permissions) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setUsername(username);
        user.setTenantId(1001L);
        user.setPermissions(permissions);
        return user;
    }

    /** 登录请求体 */
    public static class LoginRequest {

        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /** 刷新请求体 */
    public static class RefreshRequest {

        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}
