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

    List<User1> selectAll();

    User1 login(@Param("name") String name, @Param("pwd") String pwd);

    User1 selectByStaffNo(@Param("staffNo") String staffNo);

    User1 selectByPhone(@Param("phone") String phone);

    User1 selectByRoleAndIdCard(@Param("role") String role, @Param("idCard") String idCard);

    int countEnabledSuperAdminsExcept(@Param("name") String name);

    int updateStatus(@Param("name") String name, @Param("status") Integer status);

    boolean updateByPrimaryKeySelective(User1 record);
}