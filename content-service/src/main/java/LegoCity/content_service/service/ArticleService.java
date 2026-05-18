package LegoCity.content_service.service;

import LegoCity.content_service.dto.*;
import LegoCity.content_service.exception.BadRequestException;
import LegoCity.content_service.exception.ResourceNotFoundException;
import LegoCity.content_service.model.*;
import LegoCity.content_service.repository.ArticleRepository;
import LegoCity.content_service.repository.CategoryRepository;
import LegoCity.content_service.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ImageService imageService;

    @Transactional(readOnly = true)
    public PageResponse<ArticleSummaryResponse> getArticles(
            ArticleStatus status, Long categoryId, Long tagId, String search, Pageable pageable) {
        Page<Article> page;
        if (search != null && !search.isBlank()) {
            page = articleRepository.search(search, pageable);
        } else if (categoryId != null && status != null) {
            page = articleRepository.findByCategoryIdAndStatus(categoryId, status, pageable);
        } else if (categoryId != null) {
            page = articleRepository.findByCategoryId(categoryId, pageable);
        } else if (tagId != null) {
            page = articleRepository.findByTagsId(tagId, pageable);
        } else if (status != null) {
            page = articleRepository.findByStatus(status, pageable);
        } else {
            page = articleRepository.findAll(pageable);
        }
        return toPageResponse(page, this::toSummaryResponse);
    }

    @Transactional
    public ArticleResponse getArticleById(Long id) {
        Article article = findById(id);
        ArticleResponse response = toResponse(article);
        articleRepository.incrementViewCount(id);
        return response;
    }

    @Transactional
    public ArticleResponse getArticleBySlug(String slug) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Artikel nicht gefunden mit Slug: " + slug));
        ArticleResponse response = toResponse(article);
        articleRepository.incrementViewCount(article.getId());
        return response;
    }

    @Transactional
    public ArticleResponse createArticle(ArticleRequest request) {
        Article article = Article.builder()
                .title(request.getTitle())
                .slug(generateUniqueSlug(request.getTitle(), null))
                .subtitle(request.getSubtitle())
                .content(request.getContent())
                .author(request.getAuthor())
                .status(ArticleStatus.DRAFT)
                .coverImageUrl(request.getCoverImageUrl())
                .build();

        applyCategory(article, request.getCategoryId());
        applyTags(article, request.getTagIds());

        return toResponse(articleRepository.save(article));
    }

    @Transactional
    public ArticleResponse updateArticle(Long id, ArticleRequest request) {
        Article article = findById(id);

        if (!article.getTitle().equals(request.getTitle())) {
            article.setSlug(generateUniqueSlug(request.getTitle(), id));
        }
        article.setTitle(request.getTitle());
        article.setSubtitle(request.getSubtitle());
        article.setContent(request.getContent());
        article.setAuthor(request.getAuthor());
        article.setCoverImageUrl(request.getCoverImageUrl());

        applyCategory(article, request.getCategoryId());
        applyTags(article, request.getTagIds());

        return toResponse(articleRepository.save(article));
    }

    @Transactional
    public void deleteArticle(Long id) {
        Article article = findById(id);
        article.getImages().forEach(img -> imageService.deleteFile(img.getFileName()));
        articleRepository.delete(article);
    }

    @Transactional
    public ArticleResponse publishArticle(Long id) {
        Article article = findById(id);
        if (article.getStatus() == ArticleStatus.PUBLISHED) {
            throw new BadRequestException("Artikel ist bereits veröffentlicht");
        }
        article.setStatus(ArticleStatus.PUBLISHED);
        article.setPublishedAt(LocalDateTime.now());
        return toResponse(articleRepository.save(article));
    }

    @Transactional
    public ArticleResponse archiveArticle(Long id) {
        Article article = findById(id);
        article.setStatus(ArticleStatus.ARCHIVED);
        return toResponse(articleRepository.save(article));
    }

    private Article findById(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artikel nicht gefunden: " + id));
    }

    private void applyCategory(Article article, Long categoryId) {
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Kategorie nicht gefunden: " + categoryId));
            article.setCategory(category);
        } else {
            article.setCategory(null);
        }
    }

    private void applyTags(Article article, Set<Long> tagIds) {
        if (tagIds != null) {
            Set<Tag> tags = new HashSet<>(tagRepository.findByIdIn(tagIds));
            article.setTags(tags);
        }
    }

    private String generateUniqueSlug(String title, Long excludeId) {
        String base = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        boolean taken = excludeId != null
                ? articleRepository.existsBySlugAndIdNot(base, excludeId)
                : articleRepository.existsBySlug(base);

        if (!taken) return base;

        int i = 1;
        while (true) {
            String candidate = base + "-" + i;
            boolean candidateTaken = excludeId != null
                    ? articleRepository.existsBySlugAndIdNot(candidate, excludeId)
                    : articleRepository.existsBySlug(candidate);
            if (!candidateTaken) return candidate;
            i++;
        }
    }

    private ArticleResponse toResponse(Article a) {
        return ArticleResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .slug(a.getSlug())
                .subtitle(a.getSubtitle())
                .content(a.getContent())
                .author(a.getAuthor())
                .category(a.getCategory() != null ? toCategoryResponse(a.getCategory()) : null)
                .status(a.getStatus().name())
                .coverImageUrl(a.getCoverImageUrl())
                .tags(a.getTags().stream().map(this::toTagResponse).collect(Collectors.toSet()))
                .images(a.getImages().stream().map(imageService::toResponse).toList())
                .viewCount(a.getViewCount())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .publishedAt(a.getPublishedAt())
                .build();
    }

    private ArticleSummaryResponse toSummaryResponse(Article a) {
        return ArticleSummaryResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .slug(a.getSlug())
                .subtitle(a.getSubtitle())
                .author(a.getAuthor())
                .category(a.getCategory() != null ? toCategoryResponse(a.getCategory()) : null)
                .status(a.getStatus().name())
                .coverImageUrl(a.getCoverImageUrl())
                .tags(a.getTags().stream().map(this::toTagResponse).collect(Collectors.toSet()))
                .viewCount(a.getViewCount())
                .createdAt(a.getCreatedAt())
                .publishedAt(a.getPublishedAt())
                .build();
    }

    private CategoryResponse toCategoryResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private TagResponse toTagResponse(Tag t) {
        return TagResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .slug(t.getSlug())
                .build();
    }

    private <T, R> PageResponse<R> toPageResponse(Page<T> page, Function<T, R> mapper) {
        return PageResponse.<R>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
