package LegoCity.content_service.service;

import LegoCity.content_service.dto.TagRequest;
import LegoCity.content_service.dto.TagResponse;
import LegoCity.content_service.exception.BadRequestException;
import LegoCity.content_service.exception.ResourceNotFoundException;
import LegoCity.content_service.model.Tag;
import LegoCity.content_service.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TagResponse getTagById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public TagResponse createTag(TagRequest request) {
        if (tagRepository.existsByName(request.getName())) {
            throw new BadRequestException("Tag existiert bereits: " + request.getName());
        }
        Tag tag = Tag.builder()
                .name(request.getName())
                .slug(toSlug(request.getName()))
                .build();
        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public TagResponse updateTag(Long id, TagRequest request) {
        Tag tag = findById(id);
        if (tagRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new BadRequestException("Tag-Name bereits vergeben: " + request.getName());
        }
        tag.setName(request.getName());
        tag.setSlug(toSlug(request.getName()));
        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public void deleteTag(Long id) {
        Tag tag = findById(id);
        if (!tag.getArticles().isEmpty()) {
            throw new BadRequestException("Tag wird noch von Artikeln verwendet und kann nicht gelöscht werden");
        }
        tagRepository.delete(tag);
    }

    private Tag findById(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag nicht gefunden: " + id));
    }

    private String toSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }

    TagResponse toResponse(Tag t) {
        return TagResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .slug(t.getSlug())
                .build();
    }
}
