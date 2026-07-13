package com.telina.demo.endpoint.rest.controller.image;

import com.telina.demo.repository.model.ImageStatus;
import java.util.UUID;

public record ImageSubmissionResponse(UUID id, ImageStatus status) {}
