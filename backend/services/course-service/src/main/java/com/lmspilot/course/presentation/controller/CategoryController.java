package com.lmspilot.course.presentation.controller;

import com.lmspilot.course.application.dto.request.CategoryRequest;
import com.lmspilot.course.application.dto.response.CategoryResponse;
import com.lmspilot.course.application.interfaces.service.ICategoryService;
import com.lmspilot.course.application.mapper.CategoryDtoMapper;

import jakarta.validation.Valid;

import java.util.*;

import org.springframework.http.*;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final ICategoryService service;
    private final CategoryDtoMapper mapper;

    public CategoryController(ICategoryService s, CategoryDtoMapper mapper){
        service=s;
        this.mapper=mapper;
    }
    @GetMapping
    public List<CategoryResponse> list(){
        return service.categories();
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid
    @RequestBody CategoryRequest i){
        return service.createCategory(mapper.toCreateCommand(i));
    }
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable UUID id,@Valid
    @RequestBody CategoryRequest i){
        return service.updateCategory(id,mapper.toUpdateCommand(i));
    }
    @DeleteMapping("/{id}")
    public CategoryResponse deactivate(@PathVariable UUID id){
        return service.deactivateCategory(id);
    }

}
