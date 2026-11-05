package com.jincheng.lostandfound.service;

import com.jincheng.lostandfound.dao.User1Mapper;
import com.jincheng.lostandfound.pojo.User1;
import com.jincheng.lostandfound.util.MD5Util;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class User1ServiceImpl implements User1Service{
    @Resource
    private User1Mapper user1Mapper;

    @Override
    public User1 topUser1() {
        return user1Mapper.findTopUser1();
    }

    /**
     * 用户登录校验：先对明文密码做 MD5 加密，再与数据库比对
     */
    @Override
    public User1 userlogin(String name, String pwd) {
        String pwdmd5 = MD5Util.MD5Encode(pwd, "UTF-8");
        return user1Mapper.login(name, pwdmd5);
    }

    @Override
    public boolean updateUser(String name, String pwd) {
        User1 user1=new User1();
        user1.setName(name);
        user1.setPwd(pwd);
        return user1Mapper.updateByPrimaryKeySelective(user1);
    }

    @Override
    public User1 getByName(String name) {
        return user1Mapper.selectByPrimaryKey(name);
    }

    @Override
    public List<User1> listAll() {
        return user1Mapper.selectAll();
    }

    @Override
    public User1 getByStaffNo(String staffNo) {
        return user1Mapper.selectByStaffNo(staffNo);
    }

    @Override
    public User1 getByPhone(String phone) {
        return user1Mapper.selectByPhone(phone);
    }

    @Override
    public User1 getByRoleAndIdCard(String role, String idCard) {
        return user1Mapper.selectByRoleAndIdCard(role, idCard);
    }

    /**
     * 新增管理员：角色固定为 admin、状态默认启用，密码由调用方传入 MD5 值
     */
    @Override
    public boolean addAdmin(User1 user1) {
        return user1Mapper.insertSelective(user1) > 0;
    }

    @Override
    public boolean registerUser(User1 user1) {
        return user1Mapper.insertSelective(user1) > 0;
    }

    @Override
    public int countEnabledSuperAdminsExcept(String name) {
        return user1Mapper.countEnabledSuperAdminsExcept(name);
    }

    @Override
    public boolean updateStatus(String name, Integer status) {
        return user1Mapper.updateStatus(name, status) > 0;
    }
}
