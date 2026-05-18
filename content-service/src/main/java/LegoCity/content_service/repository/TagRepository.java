package LegoCity.content_service.repository;

import LegoCity.content_service.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    Optional<Tag> findBySlug(String slug);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    List<Tag> findByIdIn(Set<Long> ids);
}
