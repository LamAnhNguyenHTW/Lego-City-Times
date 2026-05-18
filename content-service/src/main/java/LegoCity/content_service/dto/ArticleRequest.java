package LegoCity.content_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleRequest {

    @NotBlank(message = "Titel darf nicht leer sein")
    private String title;

    private String subtitle;

    @NotBlank(message = "Inhalt darf nicht leer sein")
    private String content;

    @NotBlank(message = "Autor darf nicht leer sein")
    private String author;

    private Long categoryId;

    private Set<Long> tagIds;

    private String coverImageUrl;
}
