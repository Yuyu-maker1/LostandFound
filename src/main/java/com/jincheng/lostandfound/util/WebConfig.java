package com.jincheng.lostandfound.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

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
                // 放行管理端登录、旅客端登录/注册/公开车站、管理端图形验证码
                .excludePathPatterns("/admin/sys_login", "/app/login", "/app/register",
                        "/app/stations", "/util/verifycode");
    }

    /**
     * 静态资源映射：让上传的头像可以通过 /upload/xxx.jpg 访问。
     * 文件实际存放在 {项目根}/upload/，数据库只存相对路径。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = Paths.get(System.getProperty("user.dir"), "upload")
                .toUri().toString();
        // 目录在首次上传前可能不存在，此时 toUri() 不以 “/” 结尾；
        // 资源位置缺少结尾斜杠会导致 /upload/xxx.jpg 拼接错误并返回 404
        if (!uploadDir.endsWith("/")) {
            uploadDir = uploadDir + "/";
        }
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("classpath:/upload/", uploadDir);
    }
}
