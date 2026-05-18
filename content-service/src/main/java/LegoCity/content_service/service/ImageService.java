package LegoCity.content_service.service;

import LegoCity.content_service.dto.ImageResponse;
import LegoCity.content_service.exception.BadRequestException;
import LegoCity.content_service.exception.ResourceNotFoundException;
import LegoCity.content_service.model.Article;
import LegoCity.content_service.model.ArticleImage;
import LegoCity.content_service.repository.ArticleImageRepository;
import LegoCity.content_service.repository.ArticleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ArticleImageRepository imageRepository;
    private final ArticleRepository articleRepository;

    @Value("${app.image.upload-dir}")
    private String uploadDir;

    @Value("${app.image.max-size:10485760}")
    private long maxFileSize;

    @Value("${app.image.allowed-types:image/jpeg,image/png,image/webp,image/gif}")
    private String allowedTypes;

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(Path.of(uploadDir));
    }

    @Transactional
    public ImageResponse uploadImage(MultipartFile file, Long articleId, String altText, String caption) throws IOException {
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + "." + extension;
        Path dest = Path.of(uploadDir).resolve(fileName);
        Files.createDirectories(dest.getParent());
        Files.copy(file.getInputStream(), dest);

        String url = "/uploads/images/" + fileName;

        ArticleImage.ArticleImageBuilder builder = ArticleImage.builder()
                .fileName(fileName)
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .url(url)
                .altText(altText)
                .caption(caption);

        if (articleId != null) {
            Article article = articleRepository.findById(articleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Artikel nicht gefunden: " + articleId));
            builder.article(article);
        }

        return toResponse(imageRepository.save(builder.build()));
    }

    @Transactional(readOnly = true)
    public ImageResponse getImage(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getImagesByArticle(Long articleId) {
        return imageRepository.findByArticleId(articleId).stream().map(this::toResponse).toList();
    }

    public Resource serveImage(Long id) throws MalformedURLException {
        ArticleImage image = findById(id);
        Path filePath = Path.of(uploadDir).resolve(image.getFileName());
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) {
            throw new ResourceNotFoundException("Bilddatei nicht gefunden: " + id);
        }
        return resource;
    }

    @Transactional
    public ImageResponse attachToArticle(Long imageId, Long articleId) {
        ArticleImage image = findById(imageId);
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Artikel nicht gefunden: " + articleId));
        image.setArticle(article);
        return toResponse(imageRepository.save(image));
    }

    @Transactional
    public void deleteImage(Long id) {
        ArticleImage image = findById(id);
        deleteFile(image.getFileName());
        imageRepository.delete(image);
    }

    void deleteFile(String fileName) {
        try {
            Files.deleteIfExists(Path.of(uploadDir).resolve(fileName));
        } catch (IOException ignored) {
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new BadRequestException("Datei ist leer");
        if (file.getSize() > maxFileSize)
            throw new BadRequestException("Datei überschreitet die maximale Größe von " + (maxFileSize / 1024 / 1024) + " MB");
        List<String> allowed = Arrays.asList(allowedTypes.split(","));
        if (!allowed.contains(file.getContentType()))
            throw new BadRequestException("Dateityp nicht erlaubt: " + file.getContentType());
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private ArticleImage findById(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bild nicht gefunden: " + id));
    }

    ImageResponse toResponse(ArticleImage img) {
        return ImageResponse.builder()
                .id(img.getId())
                .fileName(img.getFileName())
                .originalFileName(img.getOriginalFileName())
                .contentType(img.getContentType())
                .fileSize(img.getFileSize())
                .url(img.getUrl())
                .altText(img.getAltText())
                .caption(img.getCaption())
                .articleId(img.getArticle() != null ? img.getArticle().getId() : null)
                .uploadedAt(img.getUploadedAt())
                .build();
    }
}
