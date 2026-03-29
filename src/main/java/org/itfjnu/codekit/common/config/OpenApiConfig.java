package org.itfjnu.codekit.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeKit API 文档")
                        .version("0.0.1-SNAPSHOT")
                        .description("CodeKit 个人程序员工具箱的后端 API 文档。")
                );
    }
}
