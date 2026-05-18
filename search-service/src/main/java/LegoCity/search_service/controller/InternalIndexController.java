package LegoCity.search_service.controller;

import LegoCity.search_service.dto.ArticleIndexRequest;
import LegoCity.search_service.service.ArticleSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/search/articles")
@RequiredArgsConstructor
public class InternalIndexController {

    private final ArticleSearchService searchService;

    @PostMapping("/index")
    public ResponseEntity<Void> index(@Valid @RequestBody ArticleIndexRequest request) {
        searchService.index(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        searchService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
