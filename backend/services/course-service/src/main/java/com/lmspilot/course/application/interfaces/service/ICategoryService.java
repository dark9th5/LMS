package com.lmspilot.course.application.interfaces.service;

import com.lmspilot.course.application.dto.command.CategoryCommand;
import com.lmspilot.course.application.dto.response.CategoryResponse;
import java.util.List;
import java.util.UUID;

public interface ICategoryService {
    List<CategoryResponse> categories();
    CategoryResponse createCategory(CategoryCommand.Create command);
    CategoryResponse updateCategory(UUID id, CategoryCommand.Update command);
    CategoryResponse deactivateCategory(UUID id);
}
