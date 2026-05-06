package cn.edu.ccst.communitysocialmain;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.edu.ccst.communitysocialmain.mapper")
public class CommunitySocialMainApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunitySocialMainApplication.class, args);
    }

}