package com.jincheng.lostandfound.dao;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 旅客端首页聚合查询。
 * 注意：announcement、found_item 表当前数据库尚未创建，
 * 调用方必须先做表存在性判断，表不存在时直接返回空数组。
 */
public interface HomeMapper {

    int countTable(@Param("tableName") String tableName);

    List<Map<String, Object>> selectAnnouncements(@Param("limit") int limit);

    int countFoundItems();

    List<Map<String, Object>> selectFoundItems(@Param("offset") int offset,
                                               @Param("pageSize") int pageSize);
}
