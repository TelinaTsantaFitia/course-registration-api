package com.telina.demo.image;

import com.telina.demo.concurrency.AsyncConf;
import com.telina.demo.file.bucket.BucketComponent;
import com.telina.demo.mail.Email;
import com.telina.demo.mail.Mailer;
import com.telina.demo.repository.ImageRepository;
import com.telina.demo.repository.model.ImageEntity;
import com.telina.demo.repository.model.ImageStatus;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@AllArgsConstructor
public class ImageService {

  private static final Set<String> ACCEPTED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

  private final ImageRepository imageRepository;
  private final BucketComponent bucketComponent;
  private final Mailer mailer;

  public SubmissionResult submit(MultipartFile file, String email) {
    byte[] bytes = readBytes(file);
    String contentType = detectContentType(bytes);
    if (!ACCEPTED_CONTENT_TYPES.contains(contentType)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Only JPEG/PNG images are accepted (detected: " + contentType + ")");
    }

    var entity = new ImageEntity();
    entity.setId(UUID.randomUUID());
    entity.setMail(email);
    entity.setNomFichier(file.getOriginalFilename());
    entity.setStatus(ImageStatus.PENDING);
    entity.setCreatedAt(OffsetDateTime.now());

    imageRepository.save(entity);
    return new SubmissionResult(entity, bytes, contentType);
  }

  public record SubmissionResult(ImageEntity entity, byte[] bytes, String contentType) {}

  @Async(AsyncConf.IMAGE_PROCESSING_EXECUTOR)
  public void processAsync(UUID imageId, byte[] originalBytes, String contentType) {
    File tempOutput = null;
    try {
      String extension = "image/png".equals(contentType) ? "png" : "jpg";
      tempOutput = convertToGrayscale(originalBytes, extension);

      String bucketKey = "images/" + imageId + "-bw." + extension;
      bucketComponent.upload(tempOutput, bucketKey);

      var entity =
          imageRepository
              .findById(imageId)
              .orElseThrow(() -> new IllegalStateException("Image " + imageId + " not found"));
      entity.setS3Key(bucketKey);
      entity.setStatus(ImageStatus.DONE);
      imageRepository.save(entity);

      sendConfirmationEmail(entity, tempOutput);
    } catch (Exception e) {
      log.error("Failed to process image {}", imageId, e);
      imageRepository
          .findById(imageId)
          .ifPresent(
              entity -> {
                entity.setStatus(ImageStatus.FAILED);
                imageRepository.save(entity);
              });
    } finally {
      if (tempOutput != null) {
        tempOutput.delete();
      }
    }
  }

  private void sendConfirmationEmail(ImageEntity entity, File attachment) {
    try {
      var to = new InternetAddress(entity.getMail());
      var email =
          new Email(
              to,
              List.of(),
              List.of(),
              "Votre image a été traitée",
              "<p>Bonjour,</p><p>Votre image <b>"
                  + entity.getNomFichier()
                  + "</b> a été convertie en noir et blanc. Vous la trouverez en pièce jointe.</p>",
              List.of(attachment));
      mailer.accept(email);
    } catch (AddressException e) {
      throw new RuntimeException("Invalid recipient address: " + entity.getMail(), e);
    }
  }

  private File convertToGrayscale(byte[] originalBytes, String extension) {
    try {
      BufferedImage source = ImageIO.read(new ByteArrayInputStream(originalBytes));
      if (source == null) {
        throw new IOException("Unreadable image content");
      }
      BufferedImage grayscale =
          new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
      grayscale.getGraphics().drawImage(source, 0, 0, null);

      File output = File.createTempFile("image-bw-", "." + extension);
      ImageIO.write(grayscale, extension.equals("png") ? "png" : "jpg", output);
      return output;
    } catch (IOException e) {
      throw new RuntimeException("Failed to convert image to grayscale", e);
    }
  }

  private String detectContentType(byte[] bytes) {
    return new Tika().detect(bytes);
  }

  private byte[] readBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file", e);
    }
  }
}
