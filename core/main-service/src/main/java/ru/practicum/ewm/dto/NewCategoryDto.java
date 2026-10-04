package ru.practicum.ewm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewCategoryDto {

    @NotBlank(message = "Имя категории не может быть пустым")
    @Size(max = 50, message = "Имя категории не должно быть длиннее 50 символов")
    private String name;
}