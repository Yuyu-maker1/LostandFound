package com.jincheng.lostandfound.dao;

import com.jincheng.lostandfound.pojo.LostItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LostItemMapper {

    int insert(LostItem item);

    int countMine(@Param("userId") String userId,
                  @Param("auditStatus") Integer auditStatus,
                  @Param("keyword") String keyword);

    List<LostItem> selectMine(@Param("userId") String userId,
                              @Param("auditStatus") Integer auditStatus,
                              @Param("keyword") String keyword,
                              @Param("start") int start,
                              @Param("limit") int limit);

    LostItem selectById(@Param("lostId") Integer lostId);

    /** 发布者标记为已找到 */
    int markFound(@Param("lostId") Integer lostId,
                  @Param("userId") String userId);

    /** 驳回后重新提交审核 */
    int resubmitById(@Param("lostId") Integer lostId,
                     @Param("userId") String userId);

    int countAdmin(@Param("auditStatus") Integer auditStatus,
                   @Param("status") Integer status,
                   @Param("keyword") String keyword);

    List<LostItem> selectAdminPage(@Param("auditStatus") Integer auditStatus,
                                   @Param("status") Integer status,
                                   @Param("keyword") String keyword,
                                   @Param("start") int start,
                                   @Param("limit") int limit);

    int audit(@Param("lostId") Integer lostId,
              @Param("auditStatus") Integer auditStatus,
              @Param("auditRemark") String auditRemark);
}
