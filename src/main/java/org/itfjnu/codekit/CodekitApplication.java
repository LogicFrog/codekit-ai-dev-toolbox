package org.itfjnu.codekit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.itfjnu.codekit")
public class CodekitApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodekitApplication.class, args);
    }

}
