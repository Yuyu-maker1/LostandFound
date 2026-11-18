package com.jincheng.lostandfound.controller;

import com.jincheng.lostandfound.pojo.FoundItem;
import com.jincheng.lostandfound.pojo.Station;
import com.jincheng.lostandfound.pojo.User1;
import com.jincheng.lostandfound.service.FoundItemService;
import com.jincheng.lostandfound.service.StationService;
import com.jincheng.lostandfound.service.User1Service;
import com.jincheng.lostandfound.util.AppTokenStore;
import com.jincheng.lostandfound.util.Result;
import com.jincheng.lostandfound.util.UploadFileUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 用户端失物登记（招领信息）接口。
 * 乘务员：官方主通道，保管方式固定交服务台；
 * 旅客：补充通道，可选交服务台/自行保管。
 */
@RestController
@RequestMapping("/app")
public class FoundController {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ATTENDANT = "attendant";

    private static final int CUSTODY_DESK = 0;
    private static final int CUSTODY_SELF = 1;

    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;

    /** 车次：G/D/C/Z/T/K/S/Y/L + 1-4 位数字，或纯数字 2-5 位 */
    private static final Pattern TRAIN_NO_PATTERN =
            Pattern.compile("^([GDCZTKSYL]\\d{1,4}|\\d{2,5})$");

    @Resource
    private FoundItemService foundItemService;

    @Resource
    private User1Service user1Service;

    @Resource
    private StationService stationService;

    @Resource
    private AppTokenStore appTokenStore;

    /** 物品分类 */
    @GetMapping("/categories")
    public Result<Map<String, Object>> categories() {
        Result<Map<String, Object>> result = new Result<>();
        List<Map<String, Object>> items = foundItemService.listCategories();
        result.setCode(0);
        result.setMsg("ok");
        result.setCount(items.size());
        result.setData(items);
        return result;
    }

    /** 物品照片上传：jpg/jpeg/png，≤2MB，返回 /upload/xxx */
    @PostMapping({"/found/image", "/item/image"})
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file,
                                      HttpServletRequest request) {
        Result<String> result = new Result<>();
        if (currentToken(request) == null) {
            result.setCode(1);
            result.setMsg("登录已过期，请重新登录");
            return result;
        }
        if (file == null || file.isEmpty()) {
            result.setCode(1);
            result.setMsg("请选择物品照片");
            return result;
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            result.setCode(1);
            result.setMsg("图片大小不能超过 2MB");
            return result;
        }

        String suffix;
        try {
            suffix = UploadFileUtils.getSuffixName(file).toLowerCase(Locale.ROOT);
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
            result.setMsg("物品照片上传失败！");
            return result;
        }

        String path = "/upload/" + newFileName;
        result.setCode(0);
        result.setMsg("上传成功");
        result.setData1(path);
        return result;
    }

    /** 发布招领 */
    @PostMapping("/found")
    public Result<FoundItem> publish(@RequestBody Map<String, Object> params,
                                     HttpServletRequest request) {
        Result<FoundItem> result = new Result<>();
        AppTokenStore.TokenInfo token = currentToken(request);
        if (token == null) {
            return fail(result, "登录已过期，请重新登录");
        }

        String title = text(params.get("title"));
        String description = text(params.get("description"));
        String itemImage = text(params.get("itemImage"));
        String trainNo = text(params.get("trainNo")).toUpperCase(Locale.ROOT);
        String carriageNo = text(params.get("carriageNo"));
        String seatNo = text(params.get("seatNo"));
        String foundLocation = text(params.get("foundLocation"));
        String storageLocation = text(params.get("storageLocation"));
        Integer categoryId = toInt(params.get("categoryId"));
        Integer stationId = toInt(params.get("stationId"));
        Integer custody = toInt(params.get("custody"));

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
        if (!StringUtils.hasText(foundLocation)) {
            return fail(result, "请输入拾获地点");
        }
        if (foundLocation.length() > 50) {
            return fail(result, "拾获地点不能超过 50 字");
        }

        Date foundTime = parseTime(text(params.get("foundTime")));
        if (foundTime == null) {
            return fail(result, "请选择正确的拾获时间");
        }
        if (foundTime.after(new Date())) {
            return fail(result, "拾获时间不能晚于当前时间");
        }

        FoundItem item = new FoundItem();
        item.setUserId(token.getName());
        item.setCategoryId(categoryId);
        item.setTitle(title);
        item.setDescription(description.isEmpty() ? null : description);
        item.setItemImage(itemImage);
        item.setTrainNo(trainNo);
        item.setCarriageNo(carriageNo.isEmpty() ? null : carriageNo);
        item.setSeatNo(seatNo.isEmpty() ? null : seatNo);
        item.setFoundTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(foundTime));
        item.setFoundLocation(foundLocation);
        item.setStatus(0);
        item.setAuditStatus(0);

        if (ROLE_ATTENDANT.equals(token.getRole())) {
            // 乘务员：官方主通道，物品统一进服务台
            User1 user = user1Service.getByName(token.getName());
            if (user == null || user.getStationId() == null) {
                return fail(result, "当前乘务员账号未绑定所属车站，请联系管理员");
            }
            if (!StringUtils.hasText(storageLocation)) {
                return fail(result, "请填写保管地点");
            }
            if (storageLocation.length() > 50) {
                return fail(result, "保管地点不能超过 50 字");
            }
            item.setCustody(CUSTODY_DESK);
            item.setStationId(user.getStationId());
            item.setStorageLocation(storageLocation);
        } else if (ROLE_USER.equals(token.getRole())) {
            // 旅客：补充通道，可选交服务台/自行保管
            if (custody == null || (custody != CUSTODY_DESK && custody != CUSTODY_SELF)) {
                return fail(result, "请选择保管方式");
            }
            if (custody == CUSTODY_DESK) {
                Station station = stationService.getEnabledById(stationId);
                if (station == null) {
                    return fail(result, "请选择物品移交的车站");
                }
                item.setCustody(CUSTODY_DESK);
                item.setStationId(station.getId());
                item.setStorageLocation(station.getStationName() + "服务台");
            } else {
                item.setCustody(CUSTODY_SELF);
                item.setStationId(null);
                item.setStorageLocation("发布者自行保管");
            }
        } else {
            return fail(result, "当前账号无权发布招领信息");
        }

        if (!foundItemService.publish(item)) {
            return fail(result, "发布失败，请稍后重试");
        }

        result.setCode(0);
        result.setMsg("提交成功，请等待管理员审核");
        result.setData1(item);
        return result;
    }

    /** 我的发布（支持物品名称/车次模糊查询） */
    @GetMapping("/found/mine")
    public Result<FoundItem> mine(@RequestParam(value = "page", defaultValue = "1") int page,
                                  @RequestParam(value = "limit", defaultValue = "10") int limit,
                                  @RequestParam(value = "auditStatus", required = false) Integer auditStatus,
                                  @RequestParam(value = "keyword", required = false) String keyword,
                                  HttpServletRequest request) {
        Result<FoundItem> result = new Result<>();
        AppTokenStore.TokenInfo token = currentToken(request);
        if (token == null) {
            result.setCode(1);
            result.setMsg("登录已过期，请重新登录");
            return result;
        }
        int safePage = Math.max(page, 1);
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        int total = foundItemService.countMine(token.getName(), auditStatus, kw);
        result.setCode(0);
        result.setMsg("ok");
        result.setCount(total);
        result.setData(foundItemService.pageMine(token.getName(), auditStatus, kw,
                (safePage - 1) * safeLimit, safeLimit));
        return result;
    }

    /** 发布者下架 */
    @PostMapping("/found/{id}/off")
    public Result<String> off(@PathVariable("id") Integer id, HttpServletRequest request) {
        Result<String> result = new Result<>();
        AppTokenStore.TokenInfo token = currentToken(request);
        if (token == null) {
            result.setCode(1);
            result.setMsg("登录已过期，请重新登录");
            return result;
        }
        if (!foundItemService.off(id, token.getName())) {
            result.setCode(1);
            result.setMsg("下架失败，记录不存在或当前状态不允许下架");
            return result;
        }
        result.setCode(0);
        result.setMsg("已下架");
        return result;
    }

    /** 驳回后重新提交审核 */
    @PostMapping("/found/{id}/resubmit")
    public Result<String> resubmit(@PathVariable("id") Integer id, HttpServletRequest request) {
        Result<String> result = new Result<>();
        AppTokenStore.TokenInfo token = currentToken(request);
        if (token == null) {
            result.setCode(1);
            result.setMsg("登录已过期，请重新登录");
            return result;
        }
        if (!foundItemService.resubmit(id, token.getName())) {
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

    /** 支持 yyyy-MM-dd HH:mm:ss、yyyy-MM-dd HH:mm、yyyy-MM-ddTHH:mm */
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

    private Result<FoundItem> fail(Result<FoundItem> result, String msg) {
        result.setCode(1);
        result.setMsg(msg);
        return result;
    }
}
