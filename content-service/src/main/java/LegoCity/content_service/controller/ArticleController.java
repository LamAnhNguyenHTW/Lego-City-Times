package LegoCity.content_service.controller;

import LegoCity.content_service.dto.ArticleRequest;
import LegoCity.content_service.dto.ArticleResponse;
import LegoCity.content_service.dto.ArticleSummaryResponse;
import LegoCity.content_service.dto.PageResponse;
import LegoCity.content_service.model.ArticleStatus;
import LegoCity.content_service.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
@Tag(name = "Artikel", description = "Verwaltung von Nachrichtenartikeln")
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    @Operation(summary = "Artikel auflisten — filterbar nach Status, Kategorie, Tag, Suche")
    public ResponseEntity<PageResponse<ArticleSummaryResponse>> getArticles(
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(articleService.getArticles(status, categoryId, tagId, search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Artikel nach ID abrufen (erhöht Aufrufzähler)")
    public ResponseEntity<ArticleResponse> getArticleById(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getArticleById(id));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Artikel nach Slug abrufen (erhöht Aufrufzähler)")
    public ResponseEntity<ArticleResponse> getArticleBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(articleService.getArticleBySlug(slug));
    }

    @PostMapping
    @Operation(summary = "Neuen Artikel erstellen (Status: DRAFT)")
    public ResponseEntity<ArticleResponse> createArticle(@Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(articleService.createArticle(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Artikel aktualisieren")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.ok(articleService.updateArticle(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Artikel löschen")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/publish")
    @Operation(summary = "Artikel veröffentlichen")
    public ResponseEntity<ArticleResponse> publishArticle(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.publishArticle(id));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Artikel archivieren")
    public ResponseEntity<ArticleResponse> archiveArticle(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.archiveArticle(id));
    }
}
