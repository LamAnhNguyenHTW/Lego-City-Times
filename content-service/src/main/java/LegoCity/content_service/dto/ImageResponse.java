package LegoCity.content_service.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageResponse {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private String url;
    private String altText;
    private String caption;
    private Long articleId;
    private LocalDateTime uploadedAt;
}
