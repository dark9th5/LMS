package com.lmspilot.course.application.dto.response;

import com.lmspilot.course.domain.enums.RecordStatus;
import java.util.UUID;

public record CategoryResponse(UUID id, String code, String name, UUID parentId, RecordStatus status, int sortOrder) {
}
