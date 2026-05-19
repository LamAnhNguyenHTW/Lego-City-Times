package LegoCity.content_service.controller;

import LegoCity.content_service.dto.ImageResponse;
import LegoCity.content_service.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Tag(name = "Bilder", description = "Bild-Upload und -Verwaltung")
public class ImageController {

    private final ImageService imageService;

    @GetMapping
    @Operation(summary = "Alle hochgeladenen Bilder auflisten")
    public ResponseEntity<List<ImageResponse>> getImages(
            @RequestParam(defaultValue = "false") boolean unattachedOnly) {
        return ResponseEntity.ok(imageService.getImages(unattachedOnly));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bild hochladen (optional einem Artikel zuordnen)")
    public ResponseEntity<ImageResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) String altText,
            @RequestParam(required = false) String caption) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(imageService.uploadImage(file, articleId, altText, caption));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Bild-Metadaten abrufen")
    public ResponseEntity<ImageResponse> getImage(@PathVariable Long id) {
        return ResponseEntity.ok(imageService.getImage(id));
    }

    @GetMapping("/{id}/data")
    @Operation(summary = "Bilddatei herunterladen")
    public ResponseEntity<Resource> serveImage(@PathVariable Long id) throws IOException {
        ImageResponse meta = imageService.getImage(id);
        Resource resource = imageService.serveImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(meta.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + meta.getOriginalFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/article/{articleId}")
    @Operation(summary = "Alle Bilder eines Artikels abrufen")
    public ResponseEntity<List<ImageResponse>> getImagesByArticle(@PathVariable Long articleId) {
        return ResponseEntity.ok(imageService.getImagesByArticle(articleId));
    }

    @PatchMapping("/{imageId}/attach/{articleId}")
    @Operation(summary = "Bild einem Artikel zuordnen")
    public ResponseEntity<ImageResponse> attachToArticle(
            @PathVariable Long imageId, @PathVariable Long articleId) {
        return ResponseEntity.ok(imageService.attachToArticle(imageId, articleId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Bild löschen (inkl. Datei)")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}
