package com.jincheng.lostandfound.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;
    @Override

    public void addInterceptors(InterceptorRegistry registry) {
        // 注册登录拦截器
        registry.addInterceptor(loginInterceptor)
                // 管理端与旅客端都需要登录校验
                .addPathPatterns("/admin/**", "/app/**")
                // 放行管理端登录、旅客端登录/注册、管理端图形验证码
                .excludePathPatterns("/admin/sys_login", "/app/login", "/app/register", "/util/verifycode");
    }
}
