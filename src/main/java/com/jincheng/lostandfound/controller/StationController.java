package com.jincheng.lostandfound.controller;

import com.jincheng.lostandfound.pojo.Station;
import com.jincheng.lostandfound.service.StationService;
import com.jincheng.lostandfound.util.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/stations")
public class StationController {
    @Resource
    private StationService stationService;

    private static final String ROLE_SUPER_ADMIN = "super_admin";

    /**
     * 车站下拉查询：登录后的管理员均可调用，支持关键字模糊匹配，只返启用项。
     */
    @GetMapping
    public Result<Station> list(@RequestParam(value = "keyword", required = false) String keyword) {
        Result<Station> result = new Result<>();
        List<Station> stations = stationService.listEnabled(keyword);
        result.setCode(0);
        result.setMsg("ok");
        result.setCount(stations.size());
        result.setData(stations);
        return result;
    }

    /**
     * 新增车站：仅超级管理员。
     */
    @PostMapping
    public ResponseEntity<Result<Map<String, Object>>> add(@RequestBody Map<String, Object> params,
                                                           HttpSession session) {
        Result<Map<String, Object>> result = new Result<>();
        Object role = session.getAttribute("role");
        if (role == null || !ROLE_SUPER_ADMIN.equals(role.toString())) {
            result.setCode(HttpStatus.FORBIDDEN.value());
            result.setMsg("无权限：仅超级管理员可新增车站");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }

        String stationName = params.get("stationName") == null ? "" : params.get("stationName").toString().trim();
        String city = params.get("city") == null ? "" : params.get("city").toString().trim();

        if (!StringUtils.hasText(stationName)) {
            return badRequest(result, "车站名称不能为空");
        }
        if (stationName.length() < 2 || stationName.length() > 20) {
            return badRequest(result, "车站名称需为 2-20 个字符");
        }
        if (StringUtils.hasText(city) && city.length() > 20) {
            return badRequest(result, "城市名称最长 20 个字符");
        }
        if (stationService.getByName(stationName) != null) {
            return badRequest(result, "车站已存在");
        }
        if (!stationService.addStation(stationName, StringUtils.hasText(city) ? city : null)) {
            return badRequest(result, "新增车站失败");
        }

        Station station = stationService.getByName(stationName);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", station == null ? null : station.getId());
        item.put("stationName", stationName);
        item.put("city", StringUtils.hasText(city) ? city : null);
        result.setCode(0);
        result.setMsg("新增车站成功");
        result.setData1(item);
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Result<Map<String, Object>>> badRequest(
            Result<Map<String, Object>> result, String msg) {
        result.setCode(1);
        result.setMsg(msg);
        return ResponseEntity.ok(result);
    }
}
