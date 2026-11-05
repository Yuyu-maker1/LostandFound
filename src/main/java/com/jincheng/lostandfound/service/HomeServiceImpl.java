package com.jincheng.lostandfound.service;

import com.jincheng.lostandfound.dao.HomeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HomeServiceImpl implements HomeService {

    private static final Logger log = LoggerFactory.getLogger(HomeServiceImpl.class);

    /** 公告表名（当前库未建表，建表后自动接入真实数据） */
    private static final String TABLE_ANNOUNCEMENT = "announcement";
    /** 招领表名（当前库未建表，建表后自动接入真实数据） */
    private static final String TABLE_FOUND = "found_item";

    @Resource
    private HomeMapper homeMapper;

    /**
     * 分类先用代码常量，待 category 表建好后再改为查库。
     */
    private static final List<Map<String, Object>> CATEGORIES;

    static {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(category(1, "证件", "证"));
        list.add(category(2, "电子产品", "电"));
        list.add(category(3, "行李", "包"));
        list.add(category(4, "衣物", "衣"));
        list.add(category(5, "其他", "他"));
        CATEGORIES = Collections.unmodifiableList(list);
    }

    private static Map<String, Object> category(int id, String name, String iconText) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("name", name);
        item.put("iconText", iconText);
        return item;
    }

    @Override
    public Map<String, Object> loadHome(int pageNum, int pageSize) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);

        List<Map<String, Object>> announcements = Collections.emptyList();
        List<Map<String, Object>> foundList = Collections.emptyList();
        int total = 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", CATEGORIES);
        result.put("pageNum", safePageNum);
        result.put("pageSize", safePageSize);

        try {
            if (tableExists(TABLE_ANNOUNCEMENT)) {
                announcements = homeMapper.selectAnnouncements(5);
            } else {
                // 待建表后接真实数据：announcement 表不存在，返回空数组
                log.info("announcement 表不存在，首页公告返回空数组");
            }

            if (tableExists(TABLE_FOUND)) {
                total = homeMapper.countFoundItems();
                if (total > 0) {
                    int offset = (safePageNum - 1) * safePageSize;
                    foundList = homeMapper.selectFoundItems(offset, safePageSize);
                }
            } else {
                // 待建表后接真实数据：found_item 表不存在，返回空数组
                log.info("found_item 表不存在，首页招领返回空数组");
            }
        } catch (Exception e) {
            // 表结构不匹配或数据库异常时降级为空数据，保证首页可打开
            log.warn("首页聚合查询失败，已降级为空数据: {}", e.getMessage());
            announcements = Collections.emptyList();
            foundList = Collections.emptyList();
            total = 0;
        }

        result.put("announcements", announcements);
        result.put("foundList", foundList);
        result.put("total", total);
        return result;
    }

    private boolean tableExists(String tableName) {
        return homeMapper.countTable(tableName) > 0;
    }
}
