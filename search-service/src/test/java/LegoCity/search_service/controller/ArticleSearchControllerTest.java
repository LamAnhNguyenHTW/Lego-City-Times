package LegoCity.search_service.controller;

import LegoCity.search_service.dto.ArticleSearchResponse;
import LegoCity.search_service.dto.PageResponse;
import LegoCity.search_service.service.ArticleSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArticleSearchController.class)
class ArticleSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleSearchService searchService;

    @Test
    void searchArticlesReturnsPage() throws Exception {
        PageResponse<ArticleSearchResponse> response = PageResponse.<ArticleSearchResponse>builder()
                .content(List.of(ArticleSearchResponse.builder()
                        .id("article-1")
                        .title("Lego City Opens New Train Station")
                        .author("Reporter")
                        .slug("lego-city-opens-new-train-station")
                        .publishedAt(Instant.parse("2026-05-18T10:00:00Z"))
                        .build()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(searchService.search(eq("train"), eq("city"), eq("transport"), eq("Reporter"), eq(0), eq(20), eq("relevance")))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/search/articles")
                        .param("q", "train")
                        .param("categoryId", "city")
                        .param("tagId", "transport")
                        .param("author", "Reporter")
                        .param("sort", "relevance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("article-1"))
                .andExpect(jsonPath("$.content[0].title").value("Lego City Opens New Train Station"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }
}
