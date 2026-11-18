package com.jincheng.lostandfound.service;

import com.jincheng.lostandfound.pojo.LostItem;

import java.util.List;

public interface LostItemService {

    boolean publish(LostItem item);

    int countMine(String userId, Integer auditStatus, String keyword);

    List<LostItem> pageMine(String userId, Integer auditStatus, String keyword, int start, int limit);

    LostItem getById(Integer lostId);

    boolean markFound(Integer lostId, String userId);

    boolean resubmit(Integer lostId, String userId);

    int countAdmin(Integer auditStatus, Integer status, String keyword);

    List<LostItem> pageAdmin(Integer auditStatus, Integer status, String keyword, int start, int limit);

    boolean audit(Integer lostId, Integer auditStatus, String auditRemark);
}
