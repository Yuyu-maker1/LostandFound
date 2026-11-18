package com.jincheng.lostandfound.service;

import com.jincheng.lostandfound.pojo.FoundItem;

import java.util.List;
import java.util.Map;

public interface FoundItemService {

    List<Map<String, Object>> listCategories();

    boolean categoryExists(Integer categoryId);

    boolean publish(FoundItem item);

    int countMine(String userId, Integer auditStatus, String keyword);

    List<FoundItem> pageMine(String userId, Integer auditStatus, String keyword, int start, int limit);

    FoundItem getById(Integer foundId);

    boolean off(Integer foundId, String userId);

    boolean resubmit(Integer foundId, String userId);

    int countAdmin(Integer auditStatus, Integer status, String keyword);

    List<FoundItem> pageAdmin(Integer auditStatus, Integer status, String keyword, int start, int limit);

    boolean audit(Integer foundId, Integer auditStatus, String auditRemark);
}
