package com.jincheng.lostandfound.util;

import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 旅客端 token 存储。
 * 管理端沿用 HttpSession，旅客端（uni-app）不依赖 Cookie，
 * 因此用内存 Map 保存 token -> 用户信息，拦截器按请求头校验。
 * 说明：单机内存实现，重启后 token 失效；后续多实例部署时替换为 Redis。
 */
@Component
public class AppTokenStore {

    /** token 有效期：24 小时 */
    private static final long TTL = 24 * 60 * 60 * 1000L;

    private final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    public String issue(String name, String role) {
        cleanExpired();
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new TokenInfo(name, role, System.currentTimeMillis()));
        return token;
    }

    public TokenInfo resolve(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        TokenInfo info = tokens.get(token);
        if (info == null) {
            return null;
        }
        if (System.currentTimeMillis() - info.getIssuedAt() > TTL) {
            tokens.remove(token);
            return null;
        }
        return info;
    }

    public void invalidate(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, TokenInfo>> it = tokens.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().getIssuedAt() > TTL) {
                it.remove();
            }
        }
    }

    public static class TokenInfo {
        private final String name;
        private final String role;
        private final long issuedAt;

        public TokenInfo(String name, String role, long issuedAt) {
            this.name = name;
            this.role = role;
            this.issuedAt = issuedAt;
        }

        public String getName() {
            return name;
        }

        public String getRole() {
            return role;
        }

        public long getIssuedAt() {
            return issuedAt;
        }
    }
}
