package com.jincheng.lostandfound.util;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 文件上传工具类。
 * 本项目用到的两个方法：
 * 1) 取文件后缀；2) 用“时间戳 + 随机数 + 后缀”生成新文件名。
 * 文档中的 getHost() 用于拼接绝对 URL，但本项目按需求存相对路径。
 */
public final class UploadFileUtils {

    /** 上传目录：{项目根}/upload/ */
    public static String getUploadDir() {
        return System.getProperty("user.dir") + File.separator + "upload" + File.separator;
    }

    private UploadFileUtils() {
    }

    /**
     * 获取文件后缀（含点，如 .jpg）
     */
    public static String getSuffixName(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            throw new RuntimeException("获取文件后缀失败");
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 生成文件名称：当前日期时间 + 两位随机数 + 文件后缀
     */
    public static String getNewFileName(String suffixName) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        int random = new Random().nextInt(100);
        return sdf.format(new Date()) + random + suffixName;
    }
}
