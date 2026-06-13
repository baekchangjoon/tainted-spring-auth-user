package com.tainted.authuser.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI authUserOpenApi() {
        return new OpenAPI().info(new Info()
                .title("auth-user-service API")
                .version("0.1.0")
                .description("소셜/게스트 로그인, 토큰 검증, 사용자 조회"));
    }
}
