package com.app.provider.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI providerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Provider Service API")
                        .description("Doctor/provider profile management APIs")
                        .version("1.0"))
                .externalDocs(new ExternalDocumentation()
                        .description("MediBook Provider Service"));
    }
}
