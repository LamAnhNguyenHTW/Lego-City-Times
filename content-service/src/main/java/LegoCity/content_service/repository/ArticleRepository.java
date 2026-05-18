package LegoCity.content_service.repository;

import LegoCity.content_service.model.Article;
import LegoCity.content_service.model.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Page<Article> findByStatus(ArticleStatus status, Pageable pageable);

    Page<Article> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Article> findByCategoryIdAndStatus(Long categoryId, ArticleStatus status, Pageable pageable);

    Page<Article> findByTagsId(Long tagId, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Article> search(String query, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(Long id);
}
