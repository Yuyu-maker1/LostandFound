package com.jincheng.lostandfound.util;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 验证码接口。
 * 生成 ShearCaptcha 后存入 HttpSession（属性名 verifyCode），直接以图片流输出；
 * 登录时从 Session 取出并校验，校验成功后立即移除（一次性）。
 */
@Controller
public class VerifyCodeController {

    /**
     * 获取图形验证码
     * 路径: GET /util/verifycode
     */
    @GetMapping("/util/verifycode")
    public void defaultKaptcha(HttpServletRequest httpServletRequest,
                              HttpServletResponse httpServletResponse) throws Exception {
        httpServletResponse.setHeader("Cache-Control", "no-store");
        httpServletResponse.setHeader("Pragma", "no-cache");
        httpServletResponse.setDateHeader("Expires", 0);
        httpServletResponse.setContentType("image/png");
        ShearCaptcha shearCaptcha = CaptchaUtil.createShearCaptcha(150, 30, 4, 2);
        // 验证码存入 session
        httpServletRequest.getSession().setAttribute("verifyCode", shearCaptcha);
        // 输出图片流
        shearCaptcha.write(httpServletResponse.getOutputStream());
    }

    /**
     * 申请滑动验证票据（已停用，仅为兼容保留）。
     *
     * @deprecated 管理端登录已改用 /util/verifycode 图形验证码，该接口不再被前端使用，
     *             保留以避免未知调用方 404，后续版本可删除。
     */
    @Deprecated
    @GetMapping("/util/slidercode")
    @ResponseBody
    public Result<String> sliderCode(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        String token = UUID.randomUUID().toString().replace("-", "");
        HttpSession session = request.getSession();
        session.setAttribute("sliderToken", token);
        session.setAttribute("sliderTokenTime", System.currentTimeMillis());

        Result<String> result = new Result<>();
        result.setCode(0);
        result.setMsg("ok");
        result.setData1(token);
        return result;
    }
}
