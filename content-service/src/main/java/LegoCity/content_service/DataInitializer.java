package LegoCity.content_service;

import LegoCity.content_service.model.*;
import LegoCity.content_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedContent();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) return;

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

    private void seedContent() {
        if (categoryRepository.count() > 0) return;

        Category nachrichten = categoryRepository.save(Category.builder()
                .name("Nachrichten").slug("nachrichten").description("Aktuelle Neuigkeiten aus Lego City").build());
        Category sport = categoryRepository.save(Category.builder()
                .name("Sport").slug("sport").description("Sportereignisse in Lego City").build());
        Category technologie = categoryRepository.save(Category.builder()
                .name("Technologie").slug("technologie").description("Tech-Neuigkeiten und Innovationen").build());

        Tag breaking = tagRepository.save(Tag.builder().name("Breaking").slug("breaking").build());
        Tag lokal = tagRepository.save(Tag.builder().name("Lokal").slug("lokal").build());
        Tag international = tagRepository.save(Tag.builder().name("International").slug("international").build());
        Tag verkehr = tagRepository.save(Tag.builder().name("Verkehr").slug("verkehr").build());
        Tag legoland = tagRepository.save(Tag.builder().name("Legoland").slug("legoland").build());

        articleRepository.save(Article.builder()
                .title("Neue Brücke verbindet Lego City Ost mit West")
                .slug("neue-bruecke-verbindet-lego-city-ost-mit-west")
                .subtitle("Bürgermeister Brick enthüllt das Megaprojekt")
                .content("Lego City wächst! Bürgermeister Brick hat heute offiziell den Bau einer neuen " +
                        "Hängebrücke angekündigt, die den östlichen und westlichen Stadtteil verbinden soll. " +
                        "Die Brücke wird 500 Noppen lang sein und soll bis zum Herbst fertiggestellt werden.")
                .author("Emma Steinberg")
                .category(nachrichten)
                .status(ArticleStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now().minusDays(1))
                .tags(Set.of(breaking, lokal, verkehr))
                .build());

        articleRepository.save(Article.builder()
                .title("Lego City Eagles gewinnen Meisterschaft")
                .slug("lego-city-eagles-gewinnen-meisterschaft")
                .subtitle("Erster Titel nach zehn Jahren")
                .content("Die Lego City Eagles haben gestern Abend im Finale gegen die Duplo Dynamos " +
                        "mit 3:1 gewonnen und sich damit den lang ersehnten Meistertitel gesichert.")
                .author("Lars Klötzner")
                .category(sport)
                .status(ArticleStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now().minusHours(6))
                .tags(Set.of(lokal, legoland))
                .build());

        articleRepository.save(Article.builder()
                .title("Lego City testet autonome Polizeiautos")
                .slug("lego-city-testet-autonome-polizeiautos")
                .subtitle("KI-gesteuerte Fahrzeuge auf Probe")
                .content("Ab nächstem Monat werden in Lego City erstmals autonome Polizeifahrzeuge getestet.")
                .author("Sophie Noppe")
                .category(technologie)
                .status(ArticleStatus.DRAFT)
                .tags(Set.of(breaking, international))
                .build());
    }
}
