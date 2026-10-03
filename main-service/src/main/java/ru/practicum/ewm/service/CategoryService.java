package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    List<CategoryDto> getCategories(int from, int size);

    CategoryDto getCategoryById(Long catId);

    CategoryDto createCategory(NewCategoryDto request);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(Long catId, NewCategoryDto request);
}