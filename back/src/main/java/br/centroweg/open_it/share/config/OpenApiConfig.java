package br.centroweg.open_it.share.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApi/Swagger configuration for API.
 * Provides comprehensive API documentation for book catalog management endpoints.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Open IT - Controlled Parking Lot API")
                        .description("""
                                RESTful API for a controlled parking lot system. Provides endpoints for vehicle entry registration, payment processing via PIX, and real-time status monitoring. Integrates with Node-RED for hardware control and Mercado Pago for payment handling.
                                """)
                        .version("1.0.0"));
    }
}
