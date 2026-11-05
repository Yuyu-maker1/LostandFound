package com.jincheng.lostandfound.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class User1 {
    private String name;

    private String pwd;

    private String userimg;

    /**
     * 角色：super_admin（超级管理员）/ admin（普通管理员）
     */
    private String role;

    /** 员工工号（新增管理员必填，唯一） */
    private String staffNo;

    /** 所属车站ID（关联 station.id，新增管理员必填） */
    private Integer stationId;

    /** 所属车站名称（联表查询结果，非数据库字段） */
    private String stationName;

    /** 真实姓名（新增管理员必填） */
    private String realName;

    /** 手机号（新增管理员必填，唯一） */
    private String phone;

    /** 身份证号（选填，与 role 联合唯一） */
    private String idCard;

    /** 备注（选填，最长 200 字） */
    private String remark;

    /** 状态：1-启用 0-停用 */
    private Integer status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd == null ? null : pwd.trim();
    }

    /**
     * 管理端已移除头像功能；字段保留供后续 uni-app 旅客端使用，
     * 但不再随管理端接口（登录、/admin/find 等）序列化输出。
     */
    @JsonIgnore
    public String getUserimg() {
        return userimg;
    }

    public void setUserimg(String userimg) {
        this.userimg = userimg == null ? null : userimg.trim();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role == null ? null : role.trim();
    }

    public String getStaffNo() {
        return staffNo;
    }

    public void setStaffNo(String staffNo) {
        this.staffNo = staffNo == null ? null : staffNo.trim();
    }

    public Integer getStationId() {
        return stationId;
    }

    public void setStationId(Integer stationId) {
        this.stationId = stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName == null ? null : realName.trim();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone == null ? null : phone.trim();
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard == null ? null : idCard.trim();
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark == null ? null : remark.trim();
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String toString(){
        return "Auser{" + "name='" + name + '\'' + ", userimg='" + userimg + '\'' + '}';
    }
}