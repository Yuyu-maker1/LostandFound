package com.jincheng.lostandfound.service;

import com.jincheng.lostandfound.dao.FoundItemMapper;
import com.jincheng.lostandfound.pojo.FoundItem;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class FoundItemServiceImpl implements FoundItemService {

    @Resource
    private FoundItemMapper foundItemMapper;

    @Override
    public List<Map<String, Object>> listCategories() {
        return foundItemMapper.selectCategories();
    }

    @Override
    public boolean categoryExists(Integer categoryId) {
        return categoryId != null && foundItemMapper.countCategory(categoryId) > 0;
    }

    @Override
    public boolean publish(FoundItem item) {
        return foundItemMapper.insert(item) > 0;
    }

    @Override
    public int countMine(String userId, Integer auditStatus, String keyword) {
        return foundItemMapper.countMine(userId, auditStatus, keyword);
    }

    @Override
    public List<FoundItem> pageMine(String userId, Integer auditStatus, String keyword, int start, int limit) {
        return foundItemMapper.selectMine(userId, auditStatus, keyword, start, limit);
    }

    @Override
    public FoundItem getById(Integer foundId) {
        return foundItemMapper.selectById(foundId);
    }

    @Override
    public boolean off(Integer foundId, String userId) {
        return foundItemMapper.offById(foundId, userId) > 0;
    }

    @Override
    public boolean resubmit(Integer foundId, String userId) {
        return foundItemMapper.resubmitById(foundId, userId) > 0;
    }

    @Override
    public int countAdmin(Integer auditStatus, Integer status, String keyword) {
        return foundItemMapper.countAdmin(auditStatus, status, keyword);
    }

    @Override
    public List<FoundItem> pageAdmin(Integer auditStatus, Integer status, String keyword, int start, int limit) {
        return foundItemMapper.selectAdminPage(auditStatus, status, keyword, start, limit);
    }

    @Override
    public boolean audit(Integer foundId, Integer auditStatus, String auditRemark) {
        return foundItemMapper.audit(foundId, auditStatus, auditRemark) > 0;
    }
}
