package com.skillexchange.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private static final long MAX_PROFILE_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE
    );

    private final Path fileStorageLocation;

    public FileUploadController(
            @Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.fileStorageLocation = Paths.get(uploadsDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void initializeStorage() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not initialize file storage", ex);
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please select an image to upload."));
        }

        if (file.getSize() > MAX_PROFILE_IMAGE_BYTES) {
            return ResponseEntity.status(413).body(Map.of("message", "Profile image must be 5 MB or smaller."));
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Only JPEG and PNG images are supported."));
        }

        try {
            // Validate the actual image bytes instead of trusting the client filename/content type.
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "The uploaded file is not a valid image."));
            }

            String extension = switch (contentType) {
                case MediaType.IMAGE_JPEG_VALUE -> ".jpg";
                case MediaType.IMAGE_PNG_VALUE -> ".png";
                default -> throw new IllegalStateException("Unsupported image type");
            };

            // Generate the stored name ourselves; never persist a user-controlled filename.
            String newFileName = UUID.randomUUID() + extension;
            Path targetLocation = this.fileStorageLocation.resolve(newFileName).normalize();
            if (!targetLocation.getParent().equals(this.fileStorageLocation)) {
                throw new SecurityException("Invalid upload destination");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            String fileDownloadUri = "/uploads/" + newFileName;

            return ResponseEntity.ok(Map.of("url", fileDownloadUri));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body(
                    Map.of("message", "Could not store the image. Please try again."));
        }
    }
}
