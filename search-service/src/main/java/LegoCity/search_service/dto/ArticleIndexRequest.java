package LegoCity.search_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ArticleIndexRequest {
    @NotBlank
    private String id;

    @NotBlank
    private String title;

    private String subtitle;

    private String content;

    @NotBlank
    private String author;

    @NotBlank
    private String slug;

    private String categoryId;
    private String categoryName;
    private List<String> tagIds;
    private List<String> tagNames;
    private Instant publishedAt;
    private String coverImageUrl;

    @NotNull
    private ArticleStatus status;
}
