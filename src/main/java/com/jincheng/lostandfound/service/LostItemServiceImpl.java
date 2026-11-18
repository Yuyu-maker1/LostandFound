package com.jincheng.lostandfound.service;

import com.jincheng.lostandfound.dao.LostItemMapper;
import com.jincheng.lostandfound.pojo.LostItem;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class LostItemServiceImpl implements LostItemService {

    @Resource
    private LostItemMapper lostItemMapper;

    @Override
    public boolean publish(LostItem item) {
        return lostItemMapper.insert(item) > 0;
    }

    @Override
    public int countMine(String userId, Integer auditStatus, String keyword) {
        return lostItemMapper.countMine(userId, auditStatus, keyword);
    }

    @Override
    public List<LostItem> pageMine(String userId, Integer auditStatus, String keyword, int start, int limit) {
        return lostItemMapper.selectMine(userId, auditStatus, keyword, start, limit);
    }

    @Override
    public LostItem getById(Integer lostId) {
        return lostItemMapper.selectById(lostId);
    }

    @Override
    public boolean markFound(Integer lostId, String userId) {
        return lostItemMapper.markFound(lostId, userId) > 0;
    }

    @Override
    public boolean resubmit(Integer lostId, String userId) {
        return lostItemMapper.resubmitById(lostId, userId) > 0;
    }

    @Override
    public int countAdmin(Integer auditStatus, Integer status, String keyword) {
        return lostItemMapper.countAdmin(auditStatus, status, keyword);
    }

    @Override
    public List<LostItem> pageAdmin(Integer auditStatus, Integer status, String keyword, int start, int limit) {
        return lostItemMapper.selectAdminPage(auditStatus, status, keyword, start, limit);
    }

    @Override
    public boolean audit(Integer lostId, Integer auditStatus, String auditRemark) {
        return lostItemMapper.audit(lostId, auditStatus, auditRemark) > 0;
    }
}
