package LegoCity.content_service.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleSummaryResponse {
    private Long id;
    private String title;
    private String slug;
    private String subtitle;
    private String author;
    private CategoryResponse category;
    private String status;
    private String coverImageUrl;
    private Set<TagResponse> tags;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}
