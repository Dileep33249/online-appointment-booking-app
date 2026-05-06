package com.app.appointment.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI appointmentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Appointment Service API")
                        .description("Slots, booking, cancellation and appointment history APIs")
                        .version("1.0"))
                .externalDocs(new ExternalDocumentation()
                        .description("MediBook Appointment Service"));
    }
}
