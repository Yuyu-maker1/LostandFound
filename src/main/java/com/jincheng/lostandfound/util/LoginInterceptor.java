package com.jincheng.lostandfound.util;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component //标记当前类成为一个组件，spring在启动的时候就自动扫描加载了
public class LoginInterceptor implements HandlerInterceptor {

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_SUPER_ADMIN = "super_admin";

    @Resource
    private AppTokenStore appTokenStore;

    //preHandle 调用controller方法之前执行，当返回值为true,继续往下执行，
    // 如果返回值为false,会中断所有操作
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的token
        String token = request.getHeader("Authorization");
        String uri = request.getRequestURI();

        // ===== 旅客端 /app/**：校验内存 token，不依赖 Cookie =====
        if (uri.startsWith("/app/")) {
            if (appTokenStore.resolve(token) != null) {
                return true;
            }
            return unauthorized(response);
        }

        // ===== 管理端 /admin/**：沿用 HttpSession，并校验管理员角色 =====
        HttpSession session = request.getSession();
        String sessionToken = (String) session.getAttribute("token");
        Object role = session.getAttribute("role");

        if (token != null && token.equals(sessionToken)
                && (ROLE_ADMIN.equals(role) || ROLE_SUPER_ADMIN.equals(role))) {
            // token验证通过，继续执行
            return true;
        }
        return unauthorized(response);
    }

    private boolean unauthorized(HttpServletResponse response) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"code\":1,\"msg\":\"未登录或登录已过期\"}");
        return false;
    }

    //postHandle 调用controller方法之后，但在前端视图解析之前
    //通过这个方法可以对请求得到的数据做进一步修改，然后再去前端
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }
    //afterCompletion controller请求和前端视图渲染都完成之后
    //通过这个方法可以实现一些资源清理和日志记录功能
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
