package LegoCity.content_service.repository;

import LegoCity.content_service.model.ArticleImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleImageRepository extends JpaRepository<ArticleImage, Long> {
    List<ArticleImage> findByArticleId(Long articleId);
    List<ArticleImage> findByArticleIsNull();
}
