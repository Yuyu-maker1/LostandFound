package com.jincheng.lostandfound.service;

import java.util.Map;

public interface HomeService {

    /**
     * 首页聚合数据：公告、分类、最新招领（分页）。
     * 公告表 / 招领表不存在时对应列表返回空数组。
     */
    Map<String, Object> loadHome(int pageNum, int pageSize);
}
