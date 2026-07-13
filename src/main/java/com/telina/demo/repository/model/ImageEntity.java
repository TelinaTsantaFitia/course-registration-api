package com.telina.demo.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "images")
@Getter
@Setter
public class ImageEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private String mail;

  @Column(name = "nom_fichier", nullable = false)
  private String nomFichier;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ImageStatus status;

  @Column(name = "s3_key")
  private String s3Key;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
}
