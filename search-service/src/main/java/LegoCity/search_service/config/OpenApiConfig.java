package LegoCity.search_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI searchServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lego City Times Search Service API")
                        .version("v1")
                        .description("Search and indexing API for published articles"));
    }
}
