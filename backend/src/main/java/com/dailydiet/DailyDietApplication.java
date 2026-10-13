package com.dailydiet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.dailydiet.**.mapper")
public class DailyDietApplication {

    public static void main(String[] args) {
        SpringApplication.run(DailyDietApplication.class, args);
    }
}
