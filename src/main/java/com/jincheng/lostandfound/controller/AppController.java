package com.jincheng.lostandfound.controller;

import com.jincheng.lostandfound.pojo.Station;
import com.jincheng.lostandfound.pojo.User1;
import com.jincheng.lostandfound.service.HomeService;
import com.jincheng.lostandfound.service.StationService;
import com.jincheng.lostandfound.service.User1Service;
import com.jincheng.lostandfound.util.AppTokenStore;
import com.jincheng.lostandfound.util.MD5Util;
import com.jincheng.lostandfound.util.Result;
import com.jincheng.lostandfound.util.UploadFileUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 用户端（uni-app）接口。
 * 鉴权：/app/login、/app/register、/app/stations 放行，
 * 其余接口由 LoginInterceptor 校验 Authorization。
 */
@RestController
@RequestMapping("/app")
public class AppController {

    /** 旅客账号角色 */
    private static final String ROLE_USER = "user";
    /** 乘务员角色（需审核通过后才能登录） */
    private static final String ROLE_ATTENDANT = "attendant";

    private static final int AUDIT_PENDING = 0;//待审核
    private static final int AUDIT_PASSED = 1;//审核通过
    private static final int AUDIT_REJECTED = 2;//审核拒绝

    /** 头像大小上限：2MB */
    private static final long MAX_AVATAR_SIZE = 2L * 1024 * 1024;

    /** 账号：3-20 位字母、数字或下划线 */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    /** 手机号：11 位，1 开头，第二位 3-9 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /** 员工工号：4-20 位字母或数字 */
    private static final Pattern STAFF_NO_PATTERN = Pattern.compile("^[A-Za-z0-9]{4,20}$");

    @Resource
    private User1Service user1Service;

    @Resource
    private AppTokenStore appTokenStore;

    @Resource
    private HomeService homeService;

    @Resource
    private StationService stationService;

    /**
     * 登录：允许 user（旅客）与 attendant（乘务员）。
     * 乘务员必须先审核通过（audit_status=1）且启用（status=1）才能登录。
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
        if (user.getPwd() == null || !user.getPwd().trim().equals(inputMd5)) {
            result.setCode(1);
            result.setMsg("账号或密码错误");
            return result;
        }

        // 只有旅客与乘务员可以登录用户端
        if (!ROLE_USER.equals(user.getRole()) && !ROLE_ATTENDANT.equals(user.getRole())) {
            result.setCode(1);
            result.setMsg("该账号无法登录用户端");
            return result;
        }

        // 乘务员审核状态先于启用状态判断（待审核账号 status=0，不能误报“已停用”）
        if (ROLE_ATTENDANT.equals(user.getRole())) {
            Integer auditStatus = user.getAuditStatus();
            if (auditStatus == null || auditStatus == AUDIT_PENDING) {
                result.setCode(1);
                result.setMsg("账号待审核，请等待管理员审核");
                return result;
            }
            if (auditStatus == AUDIT_REJECTED) {
                result.setCode(1);
                result.setMsg(StringUtils.hasText(user.getAuditRemark())
                        ? "账号审核未通过：" + user.getAuditRemark()
                        : "账号审核未通过，请联系管理员");
                return result;
            }
        }

        // status：1 启用，0 停用
        if (user.getStatus() == null || user.getStatus() != 1) {
            result.setCode(1);
            result.setMsg("账号已停用，请联系管理员");
            return result;
        }

        String token = appTokenStore.issue(user.getName(), user.getRole());

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("name", user.getName());
        userInfo.put("role", user.getRole());
        userInfo.put("realName", user.getRealName());
        userInfo.put("phone", user.getPhone());
        userInfo.put("userimg", user.getUserimg());
        userInfo.put("auditStatus", user.getAuditStatus());
        userInfo.put("staffNo", user.getStaffNo());

        result.setCode(0);
        result.setMsg("登录成功");
        result.setToken(token);
        result.setData1(userInfo);
        return result;
    }

    /**
     * 注册：role=user（旅客，注册即可用） / role=attendant（乘务员，待审核）。
     * 旅客：账号、密码、手机号必填；乘务员额外必填真实姓名、员工工号、所属车站。
     * 服务端强制设置角色、状态与审核状态，忽略请求中的 status/auditStatus。
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, Object> params) {
        Result<Map<String, Object>> result = new Result<>();
        String role = text(params.get("role"));
        if (!StringUtils.hasText(role)) {
            role = ROLE_USER;
        }
        if (!ROLE_USER.equals(role) && !ROLE_ATTENDANT.equals(role)) {
            return fail(result, "注册类型不正确");
        }

        String name = text(params.get("name"));
        String pwd = text(params.get("pwd"));
        String phone = text(params.get("phone"));
        String realName = text(params.get("realName"));
        String staffNo = text(params.get("staffNo"));
        Integer stationId = toInt(params.get("stationId"));

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

        if (ROLE_ATTENDANT.equals(role)) {
            // 乘务员资料
            if (!StringUtils.hasText(realName) || realName.length() < 2 || realName.length() > 20) {
                return fail(result, "真实姓名需为 2-20 个字");
            }
            if (!StringUtils.hasText(staffNo)) {
                return fail(result, "请输入员工工号");
            }
            if (!STAFF_NO_PATTERN.matcher(staffNo).matches()) {
                return fail(result, "员工工号需为 4-20 位字母或数字");
            }
            if (stationId == null) {
                return fail(result, "请选择所属车站");
            }
            Station station = stationService.getEnabledById(stationId);
            if (station == null) {
                return fail(result, "所属车站不存在或已停用");
            }
        } else if (StringUtils.hasText(realName)
                && (realName.length() < 2 || realName.length() > 20)) {
            return fail(result, "真实姓名需为 2-20 个字");
        }

        // 先查重，给出友好提示
        if (user1Service.getByName(name) != null) {
            return fail(result, "账号已存在");
        }
        if (user1Service.getByPhone(phone) != null) {
            return fail(result, "手机号已注册");
        }
        if (ROLE_ATTENDANT.equals(role) && user1Service.getByStaffNo(staffNo) != null) {
            return fail(result, "员工工号已存在");
        }

        User1 user = new User1();
        user.setName(name);
        user.setPwd(MD5Util.MD5Encode(pwd, "UTF-8"));
        user.setPhone(phone);
        user.setRealName(StringUtils.hasText(realName) ? realName : null);
        user.setRole(role);
        if (ROLE_ATTENDANT.equals(role)) {
            user.setStaffNo(staffNo);
            user.setStationId(stationId);
            user.setStatus(0);              // 待审核期间不可用
            user.setAuditStatus(AUDIT_PENDING);
        } else {
            user.setStatus(1);              // 旅客注册即可用
            user.setAuditStatus(AUDIT_PASSED);
        }

        try {
            user1Service.registerUser(user);
        } catch (DuplicateKeyException e) {
            // 并发提交时可能绕过上面的查重，唯一约束冲突转为友好提示
            String message = String.valueOf(e.getMessage());
            if (message.contains("uk_user1_phone")) {
                return fail(result, "手机号已注册");
            }
            if (message.contains("uk_user1_staff_no")) {
                return fail(result, "员工工号已存在");
            }
            return fail(result, "账号已存在");
        }

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("name", name);
        userInfo.put("role", role);
        userInfo.put("realName", user.getRealName());
        userInfo.put("phone", phone);
        userInfo.put("auditStatus", user.getAuditStatus());

        result.setCode(0);
        result.setMsg(ROLE_ATTENDANT.equals(role) ? "提交成功，请等待管理员审核" : "注册成功");
        result.setData1(userInfo);
        return result;
    }

    /**
     * 公开只读：启用中的车站列表（注册页选择所属车站用）。
     * 只返回 id 与车站名，不暴露城市、启用状态等内部字段。
     */
    @GetMapping("/stations")
    public Result<Map<String, Object>> stations() {
        Result<Map<String, Object>> result = new Result<>();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Station station : stationService.listAllEnabled()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", station.getId());
            item.put("stationName", station.getStationName());
            items.add(item);
        }
        result.setCode(0);
        result.setMsg("ok");
        result.setCount(items.size());
        result.setData(items);
        return result;
    }

    /**
     * 修改密码：校验原密码后更新 MD5。
     * 入参：oldPwd、newPwd；当前账号从 Authorization token 解析。
     */
    @PostMapping("/password")
    public Result<String> changePassword(@RequestBody Map<String, Object> params,
                                         HttpServletRequest request) {
        Result<String> result = new Result<>();
        String name = currentName(request);
        if (name == null) {
            result.setCode(1);
            result.setMsg("登录已过期，请重新登录");
            return result;
        }

        String oldPwd = text(params.get("oldPwd"));
        String newPwd = text(params.get("newPwd"));
        if (!StringUtils.hasText(oldPwd) || !StringUtils.hasText(newPwd)) {
            result.setCode(1);
            result.setMsg("请输入原密码和新密码");
            return result;
        }
        if (newPwd.length() < 6 || newPwd.length() > 20) {
            result.setCode(1);
            result.setMsg("新密码长度需为 6-20 位");
            return result;
        }
        if (!hasLetterAndDigit(newPwd)) {
            result.setCode(1);
            result.setMsg("新密码需同时包含字母和数字");
            return result;
        }

        User1 user = user1Service.getByName(name);
        if (user == null || user.getPwd() == null) {
            result.setCode(1);
            result.setMsg("账号不存在");
            return result;
        }
        String oldMd5 = MD5Util.MD5Encode(oldPwd, "UTF-8");
        if (!user.getPwd().trim().equalsIgnoreCase(oldMd5)) {
            result.setCode(1);
            result.setMsg("原密码不正确");
            return result;
        }

        boolean updated = user1Service.updateUser(name, MD5Util.MD5Encode(newPwd, "UTF-8"));
        if (!updated) {
            result.setCode(1);
            result.setMsg("密码修改失败");
            return result;
        }
        result.setCode(0);
        result.setMsg("密码修改成功");
        return result;
    }

    /**
     * 头像上传：只收 jpg/jpeg/png，大小不超过 2MB。
     * 文件存到 {项目根}/upload/，数据库只存相对路径 /upload/xxx.jpg。
     */
    @PostMapping("/userImg")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                       HttpServletRequest request) {
        Result<String> result = new Result<>();
        String name = currentName(request);
        //对用户给出的文件进行校验
        if (name == null) {
            result.setCode(1);
            result.setMsg("登录已过期，请重新登录");
            return result;
        }
        if (file == null || file.isEmpty()) {
            result.setCode(1);
            result.setMsg("请选择要上传的图片");
            return result;
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            result.setCode(1);
            result.setMsg("图片大小不能超过 2MB");
            return result;
        }

        String suffix;
        try {
            suffix = UploadFileUtils.getSuffixName(file).toLowerCase(Locale.ROOT);//取到文件名后缀后转小写
        } catch (RuntimeException e) {
            suffix = "";
        }
        if (!".jpg".equals(suffix) && !".jpeg".equals(suffix) && !".png".equals(suffix)) {
            result.setCode(1);
            result.setMsg("只支持 jpg/jpeg/png 格式的图片");
            return result;
        }

        String newFileName = UploadFileUtils.getNewFileName(suffix);
        String realPath = UploadFileUtils.getUploadDir();
        //上传目录由工具类统一管理，文档中为硬编码。
        File directory = new File(realPath);
        File destFile = new File(realPath + newFileName);
        try {
            if (!directory.exists() && !directory.mkdirs()) {
                result.setCode(1);
                result.setMsg("文件夹创建失败！");
                return result;
            }
            file.transferTo(destFile);
        } catch (IOException e) {
            result.setCode(1);
            result.setMsg("头像上传失败！");
            return result;
        }

        // 入库相对路径，而不是教程里的绝对 URL。
        String sqlImg = "/upload/" + newFileName;
        if (!user1Service.updateUserImg(name, sqlImg)) {
            result.setCode(1);
            result.setMsg("头像上传失败！");
            return result;
        }

        result.setCode(0);
        result.setMsg("头像上传成功！");
        result.setData1(sqlImg);
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

    /** 从 Authorization token 解析当前账号名 */
    private String currentName(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        AppTokenStore.TokenInfo info = appTokenStore.resolve(token);
        return info == null ? null : info.getName();
    }

    /** 取字符串并 trim，null 视为空串 */
    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    /** 安全转 Integer，非法值返回 null */
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
