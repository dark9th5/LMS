package com.lmspilot.course.application.mapper;

import com.lmspilot.course.application.dto.command.CategoryCommand;
import com.lmspilot.course.application.dto.request.CategoryRequest;
import org.springframework.stereotype.Component;

@Component
public class CategoryDtoMapper {
    public CategoryCommand.Create toCreateCommand(CategoryRequest request) {
        return new CategoryCommand.Create(request.code(), request.name(), request.parentId(), request.sortOrder());
    }

    public CategoryCommand.Update toUpdateCommand(CategoryRequest request) {
        return new CategoryCommand.Update(request.code(), request.name(), request.parentId(), request.sortOrder());
    }
}
