package com.jincheng.lostandfound;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.jincheng.lostandfound.dao")
@SpringBootApplication
public class LostandFoundApplication {

    public static void main(String[] args) {

        SpringApplication.run(LostandFoundApplication.class, args);
    }

}
