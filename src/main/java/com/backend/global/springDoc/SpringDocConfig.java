package com.backend.global.springDoc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "실시간 경매 API", version = "beta", description = "BE 실시간 경매 API 서버 문서입니다."))
public class SpringDocConfig {
}
