package com.telina.demo.endpoint.rest.controller.image;

import com.telina.demo.repository.model.ImageStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ImageStatusResponse(
    UUID id, String email, String filename, ImageStatus status, OffsetDateTime createdAt) {}
