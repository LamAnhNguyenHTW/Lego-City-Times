package LegoCity.content_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lego City Times — Content Service API")
                        .description("REST API für das Nachrichtenportal: Artikel, Kategorien, Tags und Bilder")
                        .version("1.0.0")
                        .contact(new Contact().name("Lego City Times").email("redaktion@legocitytimes.lc")));
    }
}
