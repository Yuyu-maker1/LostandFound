package com.jincheng.lostandfound.controller;

import com.jincheng.lostandfound.pojo.LostItem;
import com.jincheng.lostandfound.service.LostItemService;
import com.jincheng.lostandfound.util.Result;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 管理端寻物启事审核。
 * 权限：admin / super_admin。
 */
@RestController
@RequestMapping("/admin/lost")
public class AdminLostController {

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_SUPER_ADMIN = "super_admin";

    @Resource
    private LostItemService lostItemService;

    /** 审核列表（分页） */
    @GetMapping
    public Result<LostItem> list(@RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "limit", defaultValue = "10") int limit,
                                 @RequestParam(value = "auditStatus", required = false) Integer auditStatus,
                                 @RequestParam(value = "status", required = false) Integer status,
                                 @RequestParam(value = "keyword", required = false) String keyword,
                                 HttpSession session,
                                 HttpServletResponse response) {
        Result<LostItem> result = new Result<>();
        if (!isAdmin(session)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            result.setCode(1);
            result.setMsg("无权限");
            return result;
        }
        int safePage = Math.max(page, 1);
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        int total = lostItemService.countAdmin(auditStatus, status, kw);
        result.setCode(0);
        result.setMsg("ok");
        result.setCount(total);
        result.setData(lostItemService.pageAdmin(auditStatus, status, kw,
                (safePage - 1) * safeLimit, safeLimit));
        return result;
    }

    /** 审核详情 */
    @GetMapping("/{id}")
    public Result<LostItem> detail(@PathVariable("id") Integer id,
                                   HttpSession session,
                                   HttpServletResponse response) {
        Result<LostItem> result = new Result<>();
        if (!isAdmin(session)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            result.setCode(1);
            result.setMsg("无权限");
            return result;
        }
        LostItem item = lostItemService.getById(id);
        if (item == null) {
            result.setCode(1);
            result.setMsg("记录不存在");
            return result;
        }
        result.setCode(0);
        result.setMsg("ok");
        result.setData1(item);
        return result;
    }

    /** 审核：1 通过，2 驳回（驳回原因必填） */
    @PostMapping("/{id}/audit")
    public Result<String> audit(@PathVariable("id") Integer id,
                                @RequestBody Map<String, Object> params,
                                HttpSession session,
                                HttpServletResponse response) {
        Result<String> result = new Result<>();
        if (!isAdmin(session)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            result.setCode(1);
            result.setMsg("无权限");
            return result;
        }

        Integer auditStatus = toInt(params.get("auditStatus"));
        String remark = params.get("remark") == null ? "" : params.get("remark").toString().trim();
        if (auditStatus == null || (auditStatus != 1 && auditStatus != 2)) {
            result.setCode(1);
            result.setMsg("审核结果不正确");
            return result;
        }
        if (auditStatus == 2) {
            if (!StringUtils.hasText(remark)) {
                result.setCode(1);
                result.setMsg("请填写驳回原因");
                return result;
            }
            if (remark.length() > 255) {
                result.setCode(1);
                result.setMsg("驳回原因不能超过 255 字");
                return result;
            }
        } else {
            remark = null;
        }

        if (!lostItemService.audit(id, auditStatus, remark)) {
            result.setCode(1);
            result.setMsg("审核失败，记录不存在");
            return result;
        }
        result.setCode(0);
        result.setMsg(auditStatus == 1 ? "审核通过" : "已驳回");
        return result;
    }

    private boolean isAdmin(HttpSession session) {
        Object role = session.getAttribute("role");
        return ROLE_ADMIN.equals(role) || ROLE_SUPER_ADMIN.equals(role);
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
}
