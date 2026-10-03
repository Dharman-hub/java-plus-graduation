package ru.practicum.ewm.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CompilationsGetParams {
    private Boolean pinned;

    @Builder.Default
    private Integer from = 0;

    @Builder.Default
    private Integer size = 10;
}