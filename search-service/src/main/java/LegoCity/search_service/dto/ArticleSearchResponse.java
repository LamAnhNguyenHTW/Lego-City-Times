package LegoCity.search_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleSearchResponse {
    private String id;
    private String title;
    private String subtitle;
    private String author;
    private String slug;
    private String categoryId;
    private String categoryName;
    private List<String> tagIds;
    private List<String> tagNames;
    private Instant publishedAt;
    private String coverImageUrl;
}
