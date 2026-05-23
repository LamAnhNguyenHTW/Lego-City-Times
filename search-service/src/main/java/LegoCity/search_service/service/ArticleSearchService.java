package LegoCity.search_service.service;

import LegoCity.search_service.document.ArticleDocument;
import LegoCity.search_service.dto.ArticleIndexRequest;
import LegoCity.search_service.dto.ArticleSearchResponse;
import LegoCity.search_service.dto.ArticleStatus;
import LegoCity.search_service.dto.PageResponse;
import LegoCity.search_service.repository.ArticleSearchRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleSearchService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ArticleSearchRepository repository;
    private final ElasticsearchOperations elasticsearchOperations;
    @Qualifier("indexingExecutor")
    private final Executor indexingExecutor;

    public void index(ArticleIndexRequest request) {
        try {
            indexingExecutor.execute(() -> indexNow(request));
        } catch (RejectedExecutionException ex) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "Indexing queue is full", ex);
        }
    }

    private void indexNow(ArticleIndexRequest request) {
        if (request.getStatus() != ArticleStatus.PUBLISHED) {
            repository.deleteById(request.getId());
            return;
        }

        try {
            repository.save(toDocument(request));
        } catch (RuntimeException ex) {
            log.error("Failed to index article {} asynchronously", request.getId(), ex);
        }
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    public PageResponse<ArticleSearchResponse> search(
            String q,
            String categoryId,
            String tagId,
            String author,
            int page,
            int size,
            String sort
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String effectiveSort = StringUtils.hasText(sort) ? sort : (StringUtils.hasText(q) ? "relevance" : "publishedAt");
        Pageable pageable = PageRequest.of(safePage, safeSize, sortFor(effectiveSort));

        NativeQuery query = NativeQuery.builder()
                .withQuery(searchQuery(q, categoryId, tagId, author))
                .withPageable(pageable)
                .build();

        SearchHits<ArticleDocument> hits = elasticsearchOperations.search(query, ArticleDocument.class);
        List<ArticleSearchResponse> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toResponse)
                .toList();

        long total = hits.getTotalHits();
        int totalPages = safeSize == 0 ? 0 : (int) Math.ceil((double) total / safeSize);

        return PageResponse.<ArticleSearchResponse>builder()
                .content(content)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .last(totalPages == 0 || safePage >= totalPages - 1)
                .build();
    }

    private Query searchQuery(
            String q,
            String categoryId,
            String tagId,
            String author
    ) {
        return Query.of(query -> query.bool(bool -> {
            if (StringUtils.hasText(q)) {
                bool.must(must -> must.multiMatch(multi -> multi
                        .query(q)
                        .fields("title^3", "subtitle^2", "content")));
            } else {
                bool.must(must -> must.matchAll(matchAll -> matchAll));
            }

            if (StringUtils.hasText(categoryId)) {
                bool.filter(filter -> filter.term(term -> term.field("categoryId").value(categoryId)));
            }
            if (StringUtils.hasText(tagId)) {
                bool.filter(filter -> filter.term(term -> term.field("tagIds").value(tagId)));
            }
            if (StringUtils.hasText(author)) {
                bool.filter(filter -> filter.term(term -> term.field("author").value(author)));
            }
            return bool;
        }));
    }

    private Sort sortFor(String sort) {
        if ("publishedAt".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "publishedAt");
        }
        if (!"relevance".equalsIgnoreCase(sort)) {
            throw new IllegalArgumentException("sort must be either 'relevance' or 'publishedAt'");
        }
        return Sort.unsorted();
    }

    private ArticleDocument toDocument(ArticleIndexRequest request) {
        return ArticleDocument.builder()
                .id(request.getId())
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .content(request.getContent())
                .author(request.getAuthor())
                .slug(request.getSlug())
                .categoryId(request.getCategoryId())
                .categoryName(request.getCategoryName())
                .tagIds(request.getTagIds())
                .tagNames(request.getTagNames())
                .publishedAt(request.getPublishedAt())
                .coverImageUrl(request.getCoverImageUrl())
                .build();
    }

    private ArticleSearchResponse toResponse(ArticleDocument document) {
        return ArticleSearchResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .subtitle(document.getSubtitle())
                .author(document.getAuthor())
                .slug(document.getSlug())
                .categoryId(document.getCategoryId())
                .categoryName(document.getCategoryName())
                .tagIds(document.getTagIds())
                .tagNames(document.getTagNames())
                .publishedAt(document.getPublishedAt())
                .coverImageUrl(document.getCoverImageUrl())
                .build();
    }
}
