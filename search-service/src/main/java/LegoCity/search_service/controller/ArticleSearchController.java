package LegoCity.search_service.controller;

import LegoCity.search_service.dto.ArticleSearchResponse;
import LegoCity.search_service.dto.PageResponse;
import LegoCity.search_service.service.ArticleSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class ArticleSearchController {

    private final ArticleSearchService searchService;

    @GetMapping("/articles")
    public PageResponse<ArticleSearchResponse> searchArticles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String tagId,
            @RequestParam(required = false) String author,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return searchService.search(q, categoryId, tagId, author, page, size, sort);
    }
}
