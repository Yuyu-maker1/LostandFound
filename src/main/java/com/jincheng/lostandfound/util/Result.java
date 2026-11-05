package com.jincheng.lostandfound.util;

import java.util.List;

public class Result<T> {
    private int code;       // 状态码 0-成功, 1-失败
    private String msg;     // 操作结果描述信息
    private int count;      // 返回的数据数量（用于分页显示）
    private List<T> data;   // 存储列表数据（如查询结果）
    private T data1;
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public T getData1() {
        return data1;
    }

    public void setData1(T data1) {
        this.data1 = data1;
    }
}
