package LegoCity.content_service.service;

import LegoCity.content_service.model.Article;
import LegoCity.content_service.model.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchSyncService {

    private final RestClient.Builder restClientBuilder;

    @Value("${app.search-service.url:http://localhost:8081}")
    private String searchServiceUrl;

    public void syncArticle(Article article) {
        try {
            RestClient restClient = restClientBuilder.baseUrl(searchServiceUrl).build();
            
            Map<String, Object> body = new HashMap<>();
            body.put("id", article.getId().toString());
            body.put("title", article.getTitle());
            body.put("subtitle", article.getSubtitle());
            body.put("content", article.getContent());
            body.put("author", article.getAuthor());
            body.put("slug", article.getSlug());
            body.put("coverImageUrl", article.getCoverImageUrl());
            body.put("status", article.getStatus().name());
            
            if (article.getCategory() != null) {
                body.put("categoryId", article.getCategory().getId().toString());
                body.put("categoryName", article.getCategory().getName());
            }
            
            if (article.getTags() != null) {
                body.put("tagIds", article.getTags().stream().map(t -> t.getId().toString()).toList());
                body.put("tagNames", article.getTags().stream().map(Tag::getName).toList());
            }
            
            if (article.getPublishedAt() != null) {
                body.put("publishedAt", article.getPublishedAt().toInstant(ZoneOffset.UTC).toString());
            }

            restClient.post()
                    .uri("/internal/search/articles/index")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully synced article {} ({}) to search-service", article.getId(), article.getStatus());
        } catch (Exception e) {
            log.error("Failed to sync article {} to search-service: {}", article.getId(), e.getMessage());
        }
    }

    public void deleteArticle(Long id) {
        try {
            RestClient restClient = restClientBuilder.baseUrl(searchServiceUrl).build();
            restClient.delete()
                    .uri("/internal/search/articles/" + id)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully deleted article {} from search-service", id);
        } catch (Exception e) {
            log.error("Failed to delete article {} from search-service: {}", id, e.getMessage());
        }
    }
}
