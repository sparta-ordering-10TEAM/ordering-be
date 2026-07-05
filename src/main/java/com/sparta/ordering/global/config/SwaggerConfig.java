package com.sparta.ordering.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        servers = {
                @Server(url = "http://localhost:8080", description = "로컬 서버")
        })
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Authorization"
)
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("Sparta_Ordering_[10Team]")
                .description("""
                        ### 📌 도메인별 담당자 및 개발 범위
                        
                        | 도메인 | 담당자 | 개발 범위 |
                        | :--- | :--- | :--- |
                        | **사용자 (User)** | 강소율 | 회원가입/로그인, Spring Security & JWT 보안 인프라 구축 |
                        | **주문 & 상품 (Order & Product)** | 한원태 | - |
                        | **결제 & 배송 (Payment & Delivery)** | 김우태 | - |
                        | **음식점 (Restaurant)** | 안병규 | 음식점 CRUD와 카테고리, 영업 상태, 배달 조건, 위치 정보 관리 기능 |
                        | **리뷰/평점 & AI 상품 설명 (Review & AI)** | 김태현 | - |
                        """)
                .version("1.0.0");
    }
}