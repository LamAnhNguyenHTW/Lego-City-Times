package LegoCity.content_service.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleResponse {
    private Long id;
    private String title;
    private String slug;
    private String subtitle;
    private String content;
    private String author;
    private CategoryResponse category;
    private String status;
    private String coverImageUrl;
    private Set<TagResponse> tags;
    private List<ImageResponse> images;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
