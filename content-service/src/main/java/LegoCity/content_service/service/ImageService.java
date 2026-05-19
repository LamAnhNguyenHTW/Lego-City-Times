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
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
        Files.createDirectories(storageRoot());
    }

    @Transactional
    public ImageResponse uploadImage(MultipartFile file, Long articleId, String altText, String caption) throws IOException {
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename(), file.getContentType());
        String fileName = UUID.randomUUID() + "." + extension;
        Path dest = storageRoot().resolve(fileName).normalize();
        if (!dest.startsWith(storageRoot())) {
            throw new BadRequestException("Ungultiger Dateiname");
        }

        Files.createDirectories(dest.getParent());
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

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
    public List<ImageResponse> getImages(boolean unattachedOnly) {
        List<ArticleImage> images = unattachedOnly
                ? imageRepository.findByArticleIsNull()
                : imageRepository.findAll();

        return images.stream()
                .sorted(Comparator.comparing(ArticleImage::getUploadedAt).reversed())
                .map(this::toResponse)
                .toList();
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
        Path filePath = storageRoot().resolve(image.getFileName()).normalize();
        if (!filePath.startsWith(storageRoot())) {
            throw new ResourceNotFoundException("Bilddatei nicht gefunden: " + id);
        }

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
            Files.deleteIfExists(storageRoot().resolve(fileName).normalize());
        } catch (IOException ignored) {
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Datei ist leer");
        }
        if (file.getSize() > maxFileSize) {
            throw new BadRequestException("Datei uberschreitet die maximale Grosse von " + (maxFileSize / 1024 / 1024) + " MB");
        }

        List<String> allowed = Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .toList();
        if (!allowed.contains(file.getContentType())) {
            throw new BadRequestException("Dateityp nicht erlaubt: " + file.getContentType());
        }
    }

    private String extractExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (extension.matches("jpe?g|png|webp|gif")) {
                return extension.equals("jpeg") ? "jpg" : extension;
            }
        }

        return switch (contentType == null ? "" : contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "jpg";
        };
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

    private Path storageRoot() {
        return Path.of(uploadDir).toAbsolutePath().normalize();
    }
}
