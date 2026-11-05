package com.jincheng.lostandfound.controller;

import cn.hutool.captcha.ShearCaptcha;
import com.jincheng.lostandfound.pojo.Station;
import com.jincheng.lostandfound.pojo.User1;
import com.jincheng.lostandfound.service.StationService;
import com.jincheng.lostandfound.service.User1Service;
import com.jincheng.lostandfound.util.MD5Util;
import com.jincheng.lostandfound.util.Result;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class User1Controller {
    @Resource
    User1Service user1Service;

    @Resource
    StationService stationService;

    /** 超级管理员角色标识 */
    private static final String ROLE_SUPER_ADMIN = "super_admin";
    /** 普通管理员角色标识 */
    private static final String ROLE_ADMIN = "admin";

    @GetMapping("/find")
    public User1 find(){
        User1 user1 = user1Service.topUser1();
        System.out.println(user1);
        return user1;
    }

    @PostMapping("/sys_login")
    public Result<User1> syslogin(@RequestBody Map<String, Object> params, HttpSession session) {
        Result<User1> result = new Result<>();
        String name = params.get("username").toString();
        String password = params.get("password").toString();

        if (!StringUtils.hasText(name) || !StringUtils.hasText(password)) {
            result.setCode(1);
            result.setMsg("用户名和密码不能为空！");
            return result;
        }

        // ===== 第一步：校验图形验证码（存 Session，按教程写法）=====
        Object verifyCodeObj = params.get("verifyCode");
        String verifyCode = verifyCodeObj == null ? "" : verifyCodeObj.toString().trim();

        // 验证验证码是否为空
        if (!StringUtils.hasText(verifyCode)) {
            result.setCode(1);
            result.setMsg("验证码不能为空！");
            return result;
        }

        // 从 session 中获取验证码
        ShearCaptcha sessionCaptcha = (ShearCaptcha) session.getAttribute("verifyCode");
        if (sessionCaptcha == null) {
            result.setCode(1);
            result.setMsg("验证码已过期，请重新获取！");
            return result;
        }

        // 验证验证码是否正确
        if (!sessionCaptcha.verify(verifyCode)) {
            result.setCode(1);
            result.setMsg("验证码错误！");
            return result;
        }

        // 一次性使用：校验通过后立即移除，防止重放
        session.removeAttribute("verifyCode");

        // ===== 第二步：校验账号密码 =====
        User1 user1 = user1Service.userlogin(name, password);
        if (user1 != null) {
            // 停用账号：即使密码正确也不允许登录
            if (user1.getStatus() != null && user1.getStatus() == 0) {
                result.setCode(1);
                result.setMsg("账号已停用，请联系管理员");
                return result;
            }
            // 登录成功后签发会话令牌，前端后续请求通过 Authorization 头携带
            String token = UUID.randomUUID().toString().replace("-", "");
            session.setAttribute("token", token);
            session.setAttribute("name", user1.getName());   // 修改用户信息/密码时定位当前账号
            session.setAttribute("role", user1.getRole());   // 角色随会话传递，供越权校验使用

            result.setCode(0);
            result.setMsg("登录成功！");
            result.setToken(token);
            result.setData1(user1);
        } else {
            // 密码不匹配时，若账号本身已停用，也给出明确提示
            User1 disabledUser = user1Service.getByName(name);
            if (disabledUser != null && disabledUser.getStatus() != null && disabledUser.getStatus() == 0) {
                result.setCode(1);
                result.setMsg("账号已停用，请联系管理员");
                return result;
            }
            result.setCode(1);
            result.setMsg("用户名或密码错误，登录失败！");
        }
        return result;
    }

    /**
     * 校验原密码是否正确。
     * 以数据库中的密码为权威来源（不再使用登录时写入 session 的密码副本），
     * 并对 MD5 结果做忽略大小写与去空格比较，避免大小写/尾部空格导致的误判。
     */
    @PostMapping("/password")
    @ResponseBody
    public boolean password(@RequestBody Map<String, Object> params, HttpSession
            session) {
        String name = (String) session.getAttribute("name");
        Object oldPwdObj = params.get("oldPwd");
        if (name == null || oldPwdObj == null) {
            return false;
        }
        User1 user1 = user1Service.getByName(name);
        if (user1 == null || user1.getPwd() == null) {
            return false;
        }
        String inputOldPwd = MD5Util.MD5Encode(oldPwdObj.toString().trim(), "UTF-8");
        return user1.getPwd().trim().equalsIgnoreCase(inputOldPwd);
    }

    /**
     * 修改当前登录管理员密码（仅密码，不再读写头像字段）。
     */
    @PostMapping("/userInfo")
    @ResponseBody
    public boolean userInfoUpdate(@RequestBody Map<String, Object> params,
                                  HttpSession session) {
        String name = (String) session.getAttribute("name");
        Object newPwdObj = params.get("newPwd");
        if (name == null || newPwdObj == null || !StringUtils.hasText(newPwdObj.toString())) {
            return false;
        }
        return user1Service.updateUser(
                name,
                MD5Util.MD5Encode(newPwdObj.toString(), "UTF-8")
        );
    }

    /**
     * 管理员列表（仅超级管理员）。
     * 不返回 pwd、userimg，避免管理端暴露密码散列与已下线的头像信息。
     */
    @GetMapping("/admins")
    public ResponseEntity<Result<Map<String, Object>>> listAdmins(HttpSession session) {
        Result<Map<String, Object>> result = new Result<>();
        if (!isSuperAdmin(session)) {
            return forbidden(result);
        }
        List<Map<String, Object>> admins = new ArrayList<>();
        for (User1 user1 : user1Service.listAll()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", user1.getName());
            item.put("role", user1.getRole());
            item.put("staffNo", user1.getStaffNo());
            item.put("stationId", user1.getStationId());
            item.put("stationName", user1.getStationName());
            item.put("realName", user1.getRealName());
            item.put("phone", user1.getPhone());
            item.put("idCard", user1.getIdCard());
            item.put("remark", user1.getRemark());
            item.put("status", user1.getStatus());
            admins.add(item);
        }
        result.setCode(0);
        result.setMsg("ok");
        result.setCount(admins.size());
        result.setData(admins);
        return ResponseEntity.ok(result);
    }

    /**
     * 新增普通管理员（仅超级管理员）。
     * 角色固定为 admin、状态默认启用；忽略请求中的 role/status，禁止创建 super_admin。
     * 工号、车站、真实姓名、手机号必填；身份证选填且与 role 联合唯一。
     */
    @PostMapping("/admins")
    public ResponseEntity<Result<Map<String, Object>>> addAdmin(@RequestBody Map<String, Object> params,
                                                                HttpSession session) {
        Result<Map<String, Object>> result = new Result<>();
        if (!isSuperAdmin(session)) {
            return forbidden(result);
        }

        String name = str(params.get("name"));
        String pwd = params.get("pwd") == null ? "" : params.get("pwd").toString();
        String staffNo = str(params.get("staffNo"));
        String realName = str(params.get("realName"));
        String phone = str(params.get("phone"));
        String idCard = str(params.get("idCard"));
        String remark = str(params.get("remark"));
        Integer stationId = toInt(params.get("stationId"));

        if (!StringUtils.hasText(name)) {
            return badRequest(result, "用户名不能为空");
        }
        if (!name.matches("^[A-Za-z0-9_]{3,20}$")) {
            return badRequest(result, "用户名需为 3-20 位字母、数字或下划线");
        }
        if (!StringUtils.hasText(pwd)) {
            return badRequest(result, "密码不能为空");
        }
        if (!pwd.matches("^(?=.*[A-Za-z])(?=.*\\d).{6,20}$")) {
            return badRequest(result, "密码需为 6-20 位，且至少包含字母和数字");
        }
        if (!StringUtils.hasText(staffNo)) {
            return badRequest(result, "员工工号不能为空");
        }
        if (!staffNo.matches("^[A-Za-z0-9]{6,12}$")) {
            return badRequest(result, "员工工号需为 6-12 位字母或数字");
        }
        if (stationId == null) {
            return badRequest(result, "所属车站不能为空");
        }
        Station station = stationService.getEnabledById(stationId);
        if (station == null) {
            return badRequest(result, "所属车站不存在或已停用");
        }
        if (!StringUtils.hasText(realName) || realName.length() < 2 || realName.length() > 20) {
            return badRequest(result, "真实姓名需为 2-20 个字符");
        }
        if (!StringUtils.hasText(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            return badRequest(result, "请输入正确的 11 位手机号");
        }
        if (StringUtils.hasText(idCard) && !idCard.matches("^(\\d{15}|\\d{17}[\\dXx])$")) {
            return badRequest(result, "身份证号格式不正确");
        }
        if (StringUtils.hasText(remark) && remark.length() > 200) {
            return badRequest(result, "备注最长 200 字");
        }
        if (user1Service.getByName(name) != null) {
            return badRequest(result, "用户名已存在");
        }
        if (user1Service.getByStaffNo(staffNo) != null) {
            return badRequest(result, "员工工号已存在");
        }
        if (user1Service.getByPhone(phone) != null) {
            return badRequest(result, "手机号已存在");
        }
        if (StringUtils.hasText(idCard)
                && user1Service.getByRoleAndIdCard(ROLE_ADMIN, idCard) != null) {
            return badRequest(result, "身份证号已存在");
        }

        User1 user1 = new User1();
        user1.setName(name);
        user1.setPwd(MD5Util.MD5Encode(pwd, "UTF-8"));
        user1.setRole(ROLE_ADMIN);          // 固定普通管理员，忽略请求中的 role
        user1.setStatus(1);                 // 默认启用，忽略请求中的 status
        user1.setStaffNo(staffNo);
        user1.setStationId(stationId);
        user1.setRealName(realName);
        user1.setPhone(phone);
        user1.setIdCard(StringUtils.hasText(idCard) ? idCard : null);
        user1.setRemark(StringUtils.hasText(remark) ? remark : null);

        try {
            if (!user1Service.addAdmin(user1)) {
                return badRequest(result, "新增管理员失败");
            }
        } catch (DuplicateKeyException e) {
            return badRequest(result, "工号、手机号或身份证已存在");
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("role", ROLE_ADMIN);
        item.put("status", 1);
        item.put("staffNo", staffNo);
        item.put("stationId", stationId);
        item.put("stationName", station.getStationName());
        item.put("realName", realName);
        item.put("phone", phone);
        result.setCode(0);
        result.setMsg("新增管理员成功");
        result.setData1(item);
        return ResponseEntity.ok(result);
    }

    /**
     * 启用/停用管理员（仅超级管理员）。
     * 保护：不能停用自己；不能停用最后一个处于启用状态的超级管理员。
     */
    @PostMapping("/admins/{name}/status")
    public ResponseEntity<Result<Map<String, Object>>> changeStatus(@PathVariable("name") String name,
                                                                    @RequestBody Map<String, Object> params,
                                                                    HttpSession session) {
        Result<Map<String, Object>> result = new Result<>();
        if (!isSuperAdmin(session)) {
            return forbidden(result);
        }
        Integer status = toInt(params.get("status"));
        if (status == null || (status != 0 && status != 1)) {
            return badRequest(result, "状态值只能为 0（停用）或 1（启用）");
        }
        User1 target = user1Service.getByName(name);
        if (target == null) {
            return badRequest(result, "管理员不存在");
        }

        String currentName = (String) session.getAttribute("name");
        if (status == 0) {
            if (name.equals(currentName)) {
                return badRequest(result, "不能停用当前登录账号");
            }
            if (ROLE_SUPER_ADMIN.equals(target.getRole())
                    && user1Service.countEnabledSuperAdminsExcept(name) == 0) {
                return badRequest(result, "至少保留一个处于启用状态的超级管理员");
            }
        }

        if (!user1Service.updateStatus(name, status)) {
            return badRequest(result, "状态更新失败");
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("status", status);
        result.setCode(0);
        result.setMsg(status == 1 ? "已启用" : "已停用");
        result.setData1(item);
        return ResponseEntity.ok(result);
    }

    private String str(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private Integer toInt(Object value) {
        if (value == null || !StringUtils.hasText(value.toString())) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ResponseEntity<Result<Map<String, Object>>> badRequest(
            Result<Map<String, Object>> result, String msg) {
        result.setCode(1);
        result.setMsg(msg);
        return ResponseEntity.ok(result);
    }

    private boolean isSuperAdmin(HttpSession session) {
        Object role = session.getAttribute("role");
        return role != null && ROLE_SUPER_ADMIN.equals(role.toString());
    }

    private ResponseEntity<Result<Map<String, Object>>> forbidden(Result<Map<String, Object>> result) {
        result.setCode(HttpStatus.FORBIDDEN.value());
        result.setMsg("无权限：仅超级管理员可操作");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }
}
