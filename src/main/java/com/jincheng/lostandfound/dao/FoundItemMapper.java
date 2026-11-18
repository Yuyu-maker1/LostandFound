package com.jincheng.lostandfound.dao;

import com.jincheng.lostandfound.pojo.FoundItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface FoundItemMapper {

    /** 物品分类 */
    List<Map<String, Object>> selectCategories();

    /** 分类是否存在 */
    int countCategory(@Param("categoryId") Integer categoryId);

    /** 发布招领 */
    int insert(FoundItem item);

    /** 我的发布总数 */
    int countMine(@Param("userId") String userId,
                  @Param("auditStatus") Integer auditStatus,
                  @Param("keyword") String keyword);

    /** 我的发布分页 */
    List<FoundItem> selectMine(@Param("userId") String userId,
                               @Param("auditStatus") Integer auditStatus,
                               @Param("keyword") String keyword,
                               @Param("start") int start,
                               @Param("limit") int limit);

    /** 详情（含发布者信息） */
    FoundItem selectById(@Param("foundId") Integer foundId);

    /** 发布者下架 */
    int offById(@Param("foundId") Integer foundId,
                @Param("userId") String userId);

    /** 驳回后重新提交审核 */
    int resubmitById(@Param("foundId") Integer foundId,
                     @Param("userId") String userId);

    /** 管理端审核列表总数 */
    int countAdmin(@Param("auditStatus") Integer auditStatus,
                   @Param("status") Integer status,
                   @Param("keyword") String keyword);

    /** 管理端审核列表分页 */
    List<FoundItem> selectAdminPage(@Param("auditStatus") Integer auditStatus,
                                    @Param("status") Integer status,
                                    @Param("keyword") String keyword,
                                    @Param("start") int start,
                                    @Param("limit") int limit);

    /** 管理员审核 */
    int audit(@Param("foundId") Integer foundId,
              @Param("auditStatus") Integer auditStatus,
              @Param("auditRemark") String auditRemark);
}
