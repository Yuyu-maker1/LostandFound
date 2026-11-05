package com.jincheng.lostandfound.util;

import java.security.MessageDigest;

/**
 * MD5 加密工具类
 * 用于用户密码加密存储与登录时的密码比对
 */
public class MD5Util {

    /**
     * 对字符串进行 MD5 加密
     *
     * @param str     待加密的明文字符串
     * @param charset 字符编码（如 "UTF-8"）
     * @return 32 位小写十六进制 MD5 加密结果，若加密失败返回 null
     */
    public static String MD5Encode(String str, String charset) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes;
            if (charset == null || charset.isEmpty()) {
                bytes = md.digest(str.getBytes());
            } else {
                bytes = md.digest(str.getBytes(charset));
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                // 转成 16 进制，不足 2 位前面补 0
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
