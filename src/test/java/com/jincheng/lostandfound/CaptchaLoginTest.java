package com.jincheng.lostandfound;

import cn.hutool.captcha.ShearCaptcha;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端图形验证码 + 登录联调测试（教程写法：验证码存 Session、登录时校验并一次性移除）。
 * 通过 MockMvc 复用同一个 Session，直接读取 Session 中的 ShearCaptcha 明文。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CaptchaLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void captchaLoginFlow() throws Exception {
        // 1. 获取验证码：图片流输出，验证码存 Session
        MvcResult captchaResult = mockMvc.perform(get("/util/verifycode"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn();
        MockHttpSession session = (MockHttpSession) captchaResult.getRequest().getSession(false);
        assertNotNull(session, "验证码应写入 Session");
        ShearCaptcha captcha = (ShearCaptcha) session.getAttribute("verifyCode");
        assertNotNull(captcha, "Session 中应存在 verifyCode");
        String code = captcha.getCode();

        // 2. 正确验证码 + admin/123456 → 登录成功
        Map<String, Object> okBody = login("admin", "123456", code);
        MvcResult okResult = mockMvc.perform(post("/admin/sys_login").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(json(okBody)))
                .andReturn();
        Map<?, ?> ok = body(okResult);
        assertEquals(0, ok.get("code"), "正确验证码应登录成功：" + ok.get("msg"));
        assertNotNull(ok.get("token"));
        assertEquals("super_admin", ((Map<?, ?>) ok.get("data1")).get("role"));

        // 3. 同一 Session 复用 → 验证码已一次性移除
        MvcResult reuseResult = mockMvc.perform(post("/admin/sys_login").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(json(okBody)))
                .andReturn();
        Map<?, ?> reuse = body(reuseResult);
        assertEquals(1, reuse.get("code"));
        assertTrue(String.valueOf(reuse.get("msg")).contains("验证码已过期"), "实际提示：" + reuse.get("msg"));

        // 4. 登录后带 token 调 /admin/admins → 会话链路正常
        String token = String.valueOf(ok.get("token"));
        MvcResult adminsResult = mockMvc.perform(get("/admin/admins").session(session)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(0, body(adminsResult).get("code"));

        // 5. 新 Session + 错误验证码 → 验证码错误
        MvcResult captcha2 = mockMvc.perform(get("/util/verifycode")).andReturn();
        MockHttpSession session2 = (MockHttpSession) captcha2.getRequest().getSession(false);
        MvcResult wrongResult = mockMvc.perform(post("/admin/sys_login").session(session2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(login("admin", "123456", "0000"))))
                .andReturn();
        Map<?, ?> wrong = body(wrongResult);
        assertEquals(1, wrong.get("code"));
        assertTrue(String.valueOf(wrong.get("msg")).contains("验证码错误"));

        // 6. 不传验证码 → 验证码不能为空
        MvcResult missingResult = mockMvc.perform(post("/admin/sys_login").session(session2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(login("admin", "123456", null))))
                .andReturn();
        Map<?, ?> missing = body(missingResult);
        assertEquals(1, missing.get("code"));
        assertEquals("验证码不能为空！", missing.get("msg"));
    }

    private Map<String, Object> login(String username, String password, String verifyCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
        if (verifyCode != null) {
            params.put("verifyCode", verifyCode);
        }
        return params;
    }

    private String json(Map<String, Object> params) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(params);
    }

    private Map<?, ?> body(MvcResult result) throws Exception {
        String text = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new com.fasterxml.jackson.databind.ObjectMapper().readValue(text, Map.class);
    }
}
