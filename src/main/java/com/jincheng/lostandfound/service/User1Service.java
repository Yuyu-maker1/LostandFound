package com.jincheng.lostandfound.service;

import com.jincheng.lostandfound.pojo.User1;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface User1Service {
    User1 topUser1();
    User1 userlogin(String name, String pwd);
    boolean updateUser(@Param("name") String name, @Param("pwd") String pwd);
    User1 getByName(String name);
    boolean addAdmin(User1 user1);
    /** 旅客注册：角色与状态由调用方设定 */
    boolean registerUser(User1 user1);
    User1 getByStaffNo(String staffNo);
    User1 getByPhone(String phone);
    User1 getByRoleAndIdCard(String role, String idCard);
    int countEnabledSuperAdminsExcept(String name);
    boolean updateStatus(String name, Integer status);

    /** 管理员分页：总数 */
    int countAdmins(Integer stationId, Integer status, String keyword);

    /** 管理员分页：当前页数据 */
    List<User1> pageAdmins(int start, int limit, Integer stationId, Integer status, String keyword);

    /** 旅客 / 乘务员分页：总数 */
    int countUsers(String role, Integer status, Integer auditStatus, String keyword);

    /** 旅客 / 乘务员分页：当前页数据 */
    List<User1> pageUsers(int start, int limit, String role, Integer status, Integer auditStatus, String keyword);

    /** 乘务员审核 */
    boolean auditAttendant(String name, Integer auditStatus, String auditRemark, Integer status);

    /** 更新头像相对路径 */
    boolean updateUserImg(String name, String userimg);
}
