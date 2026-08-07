package com.lmspilot.course.application.dto.command;

import java.util.UUID;

public final class CategoryCommand {
    private CategoryCommand() {
    }

    public record Create(String code, String name, UUID parentId, Integer sortOrder) {
    }

    public record Update(String code, String name, UUID parentId, Integer sortOrder) {
    }
}
