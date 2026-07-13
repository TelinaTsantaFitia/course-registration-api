package com.telina.demo.endpoint.rest.controller.image;

import com.telina.demo.image.ImageService;
import com.telina.demo.repository.ImageRepository;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@AllArgsConstructor
public class ImageController {

  private final ImageService imageService;
  private final ImageRepository imageRepository;

  @PostMapping("/images")
  public ResponseEntity<ImageSubmissionResponse> submitImage(
      @RequestParam("file") MultipartFile file, @RequestParam("email") String email) {
    var result = imageService.submit(file, email);
    imageService.processAsync(result.entity().getId(), result.bytes(), result.contentType());

    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(new ImageSubmissionResponse(result.entity().getId(), result.entity().getStatus()));
  }

  @GetMapping("/images/{id}")
  public ImageStatusResponse getImageStatus(@PathVariable("id") UUID id) {
    var entity =
        imageRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));

    return new ImageStatusResponse(
        entity.getId(),
        entity.getMail(),
        entity.getNomFichier(),
        entity.getStatus(),
        entity.getCreatedAt());
  }
}
