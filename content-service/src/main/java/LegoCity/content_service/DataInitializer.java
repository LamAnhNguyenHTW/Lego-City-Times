package LegoCity.content_service;

import LegoCity.content_service.model.*;
import LegoCity.content_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
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

    @Override
    @Transactional
    public void run(String... args) {
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
                        "Die Brücke wird 500 Noppen lang sein und soll bis zum Herbst fertiggestellt werden. " +
                        "Experten schätzen, dass dadurch täglich 10.000 Fahrzeuge weniger durch das Stadtzentrum fahren werden.")
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
                        "mit 3:1 gewonnen und sich damit den lang ersehnten Meistertitel gesichert. " +
                        "Kapitän Brix Hooper erzielte zwei Tore und wurde zum Spieler des Turniers gekürt. " +
                        "Die ganze Stadt feiert bis in die frühen Morgenstunden.")
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
                .content("Ab nächstem Monat werden in Lego City erstmals autonome Polizeifahrzeuge getestet. " +
                        "Die mit modernster Brick-Intelligence ausgestatteten Autos sollen den Stadtverkehr " +
                        "überwachen und bei Unfällen automatisch Alarm schlagen. Datenschützer haben bereits " +
                        "Bedenken angemeldet.")
                .author("Sophie Noppe")
                .category(technologie)
                .status(ArticleStatus.DRAFT)
                .tags(Set.of(breaking, international))
                .build());
    }
}
