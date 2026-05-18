package LegoCity.content_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lego City Times — Content Service API")
                        .description("REST API für das Nachrichtenportal: Artikel, Kategorien, Tags und Bilder.\n\n" +
                                "**Authentifizierung:** POST `/api/v1/auth/login` → Token kopieren → oben rechts \"Authorize\" klicken.")
                        .version("1.0.0")
                        .contact(new Contact().name("Lego City Times").email("redaktion@legocitytimes.lc")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT-Token aus /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
