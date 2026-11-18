package com.jincheng.lostandfound.controller;

import com.jincheng.lostandfound.pojo.LostItem;
import com.jincheng.lostandfound.service.FoundItemService;
import com.jincheng.lostandfound.service.LostItemService;
import com.jincheng.lostandfound.util.AppTokenStore;
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
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 用户端寻物启事（遗失物品登记）接口。
 * 仅旅客账号可发布；乘务员工作账号不能发布寻物启事。
 */
@RestController
@RequestMapping("/app")
public class LostController {

    private static final String ROLE_USER = "user";

    /** 车次：G/D/C/Z/T/K/S/Y/L + 1-4 位数字，或纯数字 2-5 位 */
    private static final Pattern TRAIN_NO_PATTERN =
            Pattern.compile("^([GDCZTKSYL]\\d{1,4}|\\d{2,5})$");

    @Resource
    private LostItemService lostItemService;

    @Resource
    private FoundItemService foundItemService;

    @Resource
    private AppTokenStore appTokenStore;

    /** 发布寻物启事 */
    @PostMapping("/lost")
    public Result<LostItem> publish(@RequestBody Map<String, Object> params,
                                    HttpServletRequest request) {
        Result<LostItem> result = new Result<>();
        AppTokenStore.TokenInfo token = currentToken(request);
        if (token == null) {
            return fail(result, "登录已过期，请重新登录");
        }
        if (!ROLE_USER.equals(token.getRole())) {
            return fail(result, "乘务员工作账号不能发布寻物启事，请使用个人旅客账号");
        }

        String title = text(params.get("title"));
        String description = text(params.get("description"));
        String itemImage = text(params.get("itemImage"));
        String trainNo = text(params.get("trainNo")).toUpperCase(Locale.ROOT);
        String carriageNo = text(params.get("carriageNo"));
        String seatNo = text(params.get("seatNo"));
        String travelDate = text(params.get("travelDate"));
        String lostLocation = text(params.get("lostLocation"));
        Integer categoryId = toInt(params.get("categoryId"));

        if (!StringUtils.hasText(title)) {
            return fail(result, "请输入物品名称");
        }
        if (title.length() > 50) {
            return fail(result, "物品名称不能超过 50 字");
        }
        if (!foundItemService.categoryExists(categoryId)) {
            return fail(result, "请选择正确的物品分类");
        }
        if (description.length() > 255) {
            return fail(result, "特征描述不能超过 255 字");
        }
        if (!StringUtils.hasText(itemImage) || !itemImage.startsWith("/upload/")) {
            return fail(result, "请上传物品照片");
        }
        if (!TRAIN_NO_PATTERN.matcher(trainNo).matches()) {
            return fail(result, "车次格式不正确");
        }
        if (carriageNo.length() > 10 || seatNo.length() > 10) {
            return fail(result, "车厢号或座位号过长");
        }

        Date travel = parseDate(travelDate);
        if (travel == null) {
            return fail(result, "请选择正确的乘车日期");
        }
        Date today = parseDate(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        if (travel.after(today)) {
            return fail(result, "乘车日期不能晚于今天");
        }

        Date lostTime = parseTime(text(params.get("lostTime")));
        if (lostTime == null) {
            return fail(result, "请选择正确的丢失时间");
        }
        if (lostTime.after(new Date())) {
            return fail(result, "丢失时间不能晚于当前时间");
        }
        if (!StringUtils.hasText(lostLocation)) {
            return fail(result, "请输入丢失地点");
        }
        if (lostLocation.length() > 50) {
            return fail(result, "丢失地点不能超过 50 字");
        }

        LostItem item = new LostItem();
        item.setUserId(token.getName());
        item.setCategoryId(categoryId);
        item.setTitle(title);
        item.setDescription(description.isEmpty() ? null : description);
        item.setItemImage(itemImage);
        item.setTrainNo(trainNo);
        item.setCarriageNo(carriageNo.isEmpty() ? null : carriageNo);
        item.setSeatNo(seatNo.isEmpty() ? null : seatNo);
        item.setTravelDate(travelDate);
        item.setLostTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lostTime));
        item.setLostLocation(lostLocation);
        item.setStatus(0);
        item.setAuditStatus(0);

        if (!lostItemService.publish(item)) {
            return fail(result, "发布失败，请稍后重试");
        }

        result.setCode(0);
        result.setMsg("提交成功，请等待管理员审核");
        result.setData1(item);
        return result;
    }

    /** 我的寻物发布（支持物品名称/车次模糊查询） */
    @GetMapping("/lost/mine")
    public Result<LostItem> mine(@RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "limit", defaultValue = "10") int limit,
                                 @RequestParam(value = "auditStatus", required = false) Integer auditStatus,
                                 @RequestParam(value = "keyword", required = false) String keyword,
                                 HttpServletRequest request) {
        Result<LostItem> result = new Result<>();
        AppTokenStore.TokenInfo token = currentToken(request);
        if (token == null) {
            result.setCode(1);
            result.setMsg("登录已过期，请重新登录");
            return result;
        }
        int safePage = Math.max(page, 1);
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        int total = lostItemService.countMine(token.getName(), auditStatus, kw);
        result.setCode(0);
        result.setMsg("ok");
        result.setCount(total);
        result.setData(lostItemService.pageMine(token.getName(), auditStatus, kw,
                (safePage - 1) * safeLimit, safeLimit));
        return result;
    }

    /** 发布者标记为已找到 */
    @PostMapping("/lost/{id}/found")
    public Result<String> markFound(@PathVariable("id") Integer id, HttpServletRequest request) {
        Result<String> result = new Result<>();
        AppTokenStore.TokenInfo token = currentToken(request);
        if (token == null) {
            result.setCode(1);
            result.setMsg("登录已过期，请重新登录");
            return result;
        }
        if (!lostItemService.markFound(id, token.getName())) {
            result.setCode(1);
            result.setMsg("操作失败，记录不存在或当前状态不允许标记");
            return result;
        }
        result.setCode(0);
        result.setMsg("已标记为找到");
        return result;
    }

    /** 驳回后重新提交审核 */
    @PostMapping("/lost/{id}/resubmit")
    public Result<String> resubmit(@PathVariable("id") Integer id, HttpServletRequest request) {
        Result<String> result = new Result<>();
        AppTokenStore.TokenInfo token = currentToken(request);
        if (token == null) {
            result.setCode(1);
            result.setMsg("登录已过期，请重新登录");
            return result;
        }
        if (!lostItemService.resubmit(id, token.getName())) {
            result.setCode(1);
            result.setMsg("重新提交失败，仅已驳回的记录可以重新提交");
            return result;
        }
        result.setCode(0);
        result.setMsg("已重新提交，请等待管理员审核");
        return result;
    }

    private AppTokenStore.TokenInfo currentToken(HttpServletRequest request) {
        return appTokenStore.resolve(request.getHeader("Authorization"));
    }

    private String text(Object value) {
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

    private Date parseTime(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.trim().replace('T', ' ');
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern);
                format.setLenient(false);
                return format.parse(value);
            } catch (Exception ignored) {
                // 尝试下一种格式
            }
        }
        return null;
    }

    private Date parseDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            format.setLenient(false);
            return format.parse(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Result<LostItem> fail(Result<LostItem> result, String msg) {
        result.setCode(1);
        result.setMsg(msg);
        return result;
    }
}
