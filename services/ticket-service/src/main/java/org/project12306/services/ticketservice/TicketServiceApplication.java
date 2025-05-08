package org.project12306.services.ticketservice;

import cn.hippo4j.core.enable.EnableDynamicThreadPool;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 购票服务应用启动器
 */
@SpringBootApplication
@EnableDynamicThreadPool
@MapperScan("org.project12306.services.ticketservice.dao.mapper")
@EnableFeignClients
public class TicketServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
}
