package LegoCity.search_service.config;

import LegoCity.search_service.document.ArticleDocument;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

@Configuration
@RequiredArgsConstructor
public class ElasticsearchConfig {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchConfig.class);

    private final ElasticsearchOperations elasticsearchOperations;

    @Bean
    public ApplicationRunner articleIndexBootstrap(
            @Value("${app.elasticsearch.bootstrap-index:true}") boolean bootstrapIndex
    ) {
        return args -> {
            if (!bootstrapIndex) {
                return;
            }

            IndexOperations indexOperations = elasticsearchOperations.indexOps(ArticleDocument.class);
            if (!indexOperations.exists()) {
                indexOperations.create();
                indexOperations.putMapping(indexOperations.createMapping());
                log.info("Created Elasticsearch index for articles");
            }
        };
    }
}
