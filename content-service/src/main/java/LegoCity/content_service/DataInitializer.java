package LegoCity.content_service;

import LegoCity.content_service.model.Category;
import LegoCity.content_service.model.Role;
import LegoCity.content_service.model.Tag;
import LegoCity.content_service.model.User;
import LegoCity.content_service.repository.ArticleRepository;
import LegoCity.content_service.repository.CategoryRepository;
import LegoCity.content_service.repository.TagRepository;
import LegoCity.content_service.repository.UserRepository;
import LegoCity.content_service.service.SearchSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SearchSyncService searchSyncService;

    private static final List<String> MOCK_ARTICLE_SLUGS = List.of(
            "gefangener-aus-transporter-geflohen",
            "brand-im-lagerhaus-geloescht",
            "neues-einkaufszentrum-geplant",
            "skatepark-wettbewerb-am-wochenende",
            "neuer-flughafen-eroeffnet",
            "neue-bruecke-verbindet-lego-city-ost-mit-west",
            "lego-city-eagles-gewinnen-meisterschaft",
            "lego-city-testet-autonome-polizeiautos");

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedTaxonomy();
        removeMockArticles();
    }

    private void seedUsers() {
        if (userRepository.count() > 0)
            return;

        userRepository.save(User.builder()
                .username("admin")
                .email("admin@legocitytimes.lc")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build());

        userRepository.save(User.builder()
                .username("leser")
                .email("leser@legocitytimes.lc")
                .password(passwordEncoder.encode("leser123"))
                .role(Role.USER)
                .build());
    }

    private void seedTaxonomy() {
        category("news", "News", "Aktuelle Meldungen aus LEGO City");
        category("polizei", "Polizei", "Einsatze, Fahndungen und Verkehrsmeldungen");
        category("politik", "Politik", "Politische Ereignisse und Entscheidungen");
        category("wirtschaft", "Wirtschaft", "Geschaft, Bauprojekte und Jobs");
        category("sport", "Sport", "Turniere, Rennen und Vereinsleben");
        category("kultur", "Kultur", "Kunst, Unterhaltung und Veranstaltungen");

        tag("breaking", "Breaking");
        tag("lokal", "Lokal");
        tag("verkehr", "Verkehr");
        tag("rettung", "Rettung");
        tag("city", "City");
    }

    private Category category(String slug, String name, String description) {
        return categoryRepository.findBySlug(slug)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .slug(slug)
                        .name(name)
                        .description(description)
                        .build()));
    }

    private Tag tag(String slug, String name) {
        return tagRepository.findBySlug(slug)
                .orElseGet(() -> tagRepository.save(Tag.builder()
                        .slug(slug)
                        .name(name)
                        .build()));
    }

    private void removeMockArticles() {
        for (String slug : MOCK_ARTICLE_SLUGS) {
            articleRepository.findBySlug(slug).ifPresent(article -> {
                Long id = article.getId();
                articleRepository.delete(article);
                searchSyncService.deleteArticle(id);
            });
        }
    }
}
