package org.itfjnu.codekit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@SpringBootApplication(scanBasePackages = "org.itfjnu.codekit")
public class CodekitApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodekitApplication.class, args);
    }

}
