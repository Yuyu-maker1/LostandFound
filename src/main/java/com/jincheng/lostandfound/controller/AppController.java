package com.jincheng.lostandfound.controller;

import com.jincheng.lostandfound.pojo.User1;
import com.jincheng.lostandfound.service.HomeService;
import com.jincheng.lostandfound.service.User1Service;
import com.jincheng.lostandfound.util.AppTokenStore;
import com.jincheng.lostandfound.util.MD5Util;
import com.jincheng.lostandfound.util.Result;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 旅客端（uni-app）接口。
 * 鉴权：/app/login 放行，其余接口由 LoginInterceptor 校验 Authorization。
 */
@RestController
@RequestMapping("/app")
public class AppController {

    /** 旅客账号角色；管理端使用 admin / super_admin，不允许登录旅客端 */
    private static final String ROLE_USER = "user";

    /** 账号：3-20 位字母、数字或下划线 */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    /** 手机号：11 位，1 开头，第二位 3-9 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @Resource
    private User1Service user1Service;

    @Resource
    private AppTokenStore appTokenStore;

    @Resource
    private HomeService homeService;

    /**
     * 旅客登录。
     * 入参：name、pwd（明文，后端 MD5 后与库中密码比对）
     * 只允许 role='user' 且 status=1 的账号登录；旅客端不启用图形验证码。
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> params) {
        Result<Map<String, Object>> result = new Result<>();
        String name = params.get("name") == null ? "" : params.get("name").toString().trim();
        String pwd = params.get("pwd") == null ? "" : params.get("pwd").toString();

        if (!StringUtils.hasText(name) || !StringUtils.hasText(pwd)) {
            result.setCode(1);
            result.setMsg("请输入账号和密码");
            return result;
        }

        User1 user = user1Service.getByName(name);
        if (user == null) {
            result.setCode(1);
            result.setMsg("账号或密码错误");
            return result;
        }

        String inputMd5 = MD5Util.MD5Encode(pwd.trim(), "UTF-8");
        if (user.getPwd() == null || !user.getPwd().trim().equalsIgnoreCase(inputMd5)) {
            result.setCode(1);
            result.setMsg("账号或密码错误");
            return result;
        }

        // status：1 启用，0 停用（与管理端一致）
        if (user.getStatus() == null || user.getStatus() != 1) {
            result.setCode(1);
            result.setMsg("账号已停用");
            return result;
        }

        if (!ROLE_USER.equals(user.getRole())) {
            result.setCode(1);
            result.setMsg("该账号不是旅客账号");
            return result;
        }

        String token = appTokenStore.issue(user.getName(), user.getRole());

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("name", user.getName());
        userInfo.put("role", user.getRole());
        userInfo.put("realName", user.getRealName());
        userInfo.put("phone", user.getPhone());

        result.setCode(0);
        result.setMsg("登录成功");
        result.setToken(token);
        result.setData1(userInfo);
        return result;
    }

    /**
     * 旅客注册。
     * 只接受 name、pwd、phone、realName 四个字段；
     * role 固定为 user、status 固定为 1，请求里传入的 role/status 一律忽略，
     * 防止注册接口造出管理员账号。
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, Object> params) {
        Result<Map<String, Object>> result = new Result<>();
        String name = text(params.get("name"));
        String pwd = text(params.get("pwd"));
        String phone = text(params.get("phone"));
        String realName = text(params.get("realName"));

        // 账号
        if (!StringUtils.hasText(name)) {
            return fail(result, "请输入账号");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            return fail(result, "账号需为 3-20 位字母、数字或下划线");
        }
        // 密码：6-20 位且同时包含字母与数字
        if (!StringUtils.hasText(pwd)) {
            return fail(result, "请输入密码");
        }
        if (pwd.length() < 6 || pwd.length() > 20) {
            return fail(result, "密码长度需为 6-20 位");
        }
        if (!hasLetterAndDigit(pwd)) {
            return fail(result, "密码需同时包含字母和数字");
        }
        // 手机号
        if (!StringUtils.hasText(phone)) {
            return fail(result, "请输入手机号");
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return fail(result, "手机号格式不正确");
        }
        // 真实姓名（选填）
        if (StringUtils.hasText(realName) && (realName.length() < 2 || realName.length() > 20)) {
            return fail(result, "真实姓名需为 2-20 个字");
        }

        // 先查重，给出友好提示
        if (user1Service.getByName(name) != null) {
            return fail(result, "账号已存在");
        }
        if (user1Service.getByPhone(phone) != null) {
            return fail(result, "手机号已注册");
        }

        User1 user = new User1();
        user.setName(name);
        user.setPwd(MD5Util.MD5Encode(pwd, "UTF-8"));
        user.setPhone(phone);
        user.setRealName(StringUtils.hasText(realName) ? realName : null);
        // 服务端强制旅客角色与启用状态，不读取请求中的 role/status
        user.setRole(ROLE_USER);
        user.setStatus(1);

        try {
            user1Service.registerUser(user);
        } catch (DuplicateKeyException e) {
            // 并发提交时可能绕过上面的查重，唯一约束冲突转为友好提示
            String message = String.valueOf(e.getMessage());
            return fail(result, message.contains("uk_user1_phone") ? "手机号已注册" : "账号已存在");
        }

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("name", name);
        userInfo.put("role", ROLE_USER);
        userInfo.put("realName", user.getRealName());
        userInfo.put("phone", phone);

        result.setCode(0);
        result.setMsg("注册成功");
        result.setData1(userInfo);
        return result;
    }

    /**
     * 首页聚合数据：公告、分类、最新招领（分页）。
     * 返回结构见 Result.data1：
     * { announcements:[], categories:[], foundList:[], total, pageNum, pageSize }
     */
    @GetMapping("/home")
    public Result<Map<String, Object>> home(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        Result<Map<String, Object>> result = new Result<>();
        Map<String, Object> data = homeService.loadHome(pageNum, pageSize);
        result.setCode(0);
        result.setMsg("ok");
        result.setCount(data.get("total") == null ? 0 : (Integer) data.get("total"));
        result.setData1(data);
        return result;
    }

    /** 取字符串并 trim，null 视为空串 */
    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    /** 统一返回失败结构 */
    private Result<Map<String, Object>> fail(Result<Map<String, Object>> result, String msg) {
        result.setCode(1);
        result.setMsg(msg);
        return result;
    }

    /** 密码必须同时包含 ASCII 字母与数字 */
    private boolean hasLetterAndDigit(String pwd) {
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < pwd.length(); i++) {
            char c = pwd.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                hasLetter = true;
            } else if (c >= '0' && c <= '9') {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }
}
