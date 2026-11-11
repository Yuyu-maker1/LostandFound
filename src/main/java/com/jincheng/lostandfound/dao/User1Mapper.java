package com.jincheng.lostandfound.dao;

import com.jincheng.lostandfound.pojo.User1;
import com.jincheng.lostandfound.pojo.User1Example;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface User1Mapper {
    long countByExample(User1Example example);

    int deleteByExample(User1Example example);

    int deleteByPrimaryKey(String name);

    int insert(User1 record);

    int insertSelective(User1 record);

    List<User1> selectByExample(User1Example example);

    User1 selectByPrimaryKey(String name);

    int updateByExampleSelective(@Param("record") User1 record, @Param("example") User1Example example);

    int updateByExample(@Param("record") User1 record, @Param("example") User1Example example);

//    int updateByPrimaryKeySelective(User1 record);

    int updateByPrimaryKey(User1 record);

    User1 findTopUser1();

    /** 管理员分页查询（只含 admin / super_admin） */
    List<User1> selectAdminPage(@Param("stationId") Integer stationId,
                                @Param("status") Integer status,
                                @Param("start") int start,
                                @Param("limit") int limit);

    /** 管理员总记录数（与分页查询同条件） */
    int countAdmins(@Param("stationId") Integer stationId,
                    @Param("status") Integer status);

    User1 login(@Param("name") String name, @Param("pwd") String pwd);

    User1 selectByStaffNo(@Param("staffNo") String staffNo);

    User1 selectByPhone(@Param("phone") String phone);

    User1 selectByRoleAndIdCard(@Param("role") String role, @Param("idCard") String idCard);

    int countEnabledSuperAdminsExcept(@Param("name") String name);

    int updateStatus(@Param("name") String name, @Param("status") Integer status);

    /** 旅客 / 乘务员分页查询（联表带出车站名称） */
    List<User1> selectUserPage(@Param("role") String role,
                               @Param("status") Integer status,
                               @Param("auditStatus") Integer auditStatus,
                               @Param("start") int start,
                               @Param("limit") int limit);

    /** 旅客 / 乘务员总记录数（与分页查询同条件） */
    int countUsers(@Param("role") String role,
                   @Param("status") Integer status,
                   @Param("auditStatus") Integer auditStatus);

    /** 乘务员审核：更新审核状态、备注、审核时间与启用状态 */
    int auditAttendant(@Param("name") String name,
                       @Param("auditStatus") Integer auditStatus,
                       @Param("auditRemark") String auditRemark,
                       @Param("status") Integer status);

    /** 更新头像相对路径 */
    int updateUserImg(@Param("name") String name, @Param("userimg") String userimg);

    boolean updateByPrimaryKeySelective(User1 record);
}