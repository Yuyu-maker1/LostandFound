package com.jincheng.lostandfound.service;

import com.jincheng.lostandfound.pojo.User1;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface User1Service {
    User1 topUser1();
    User1 userlogin(String name, String pwd);
    boolean updateUser(@Param("name") String name, @Param("pwd") String pwd);
    User1 getByName(String name);
    List<User1> listAll();
    boolean addAdmin(User1 user1);
    /** 旅客注册：角色与状态由调用方设定 */
    boolean registerUser(User1 user1);
    User1 getByStaffNo(String staffNo);
    User1 getByPhone(String phone);
    User1 getByRoleAndIdCard(String role, String idCard);
    int countEnabledSuperAdminsExcept(String name);
    boolean updateStatus(String name, Integer status);
}
