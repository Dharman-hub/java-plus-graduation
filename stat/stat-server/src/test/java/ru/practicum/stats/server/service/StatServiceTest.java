package ru.practicum.stats.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.dao.AppStorage;
import ru.practicum.stats.server.dao.StatStorage;
import ru.practicum.stats.server.model.App;
import ru.practicum.stats.server.model.ViewStat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatServiceTest {

    @Mock
    private StatStorage statStorage;

    @Mock
    private AppStorage appStorage;

    @InjectMocks
    private StatService statService;

    private EndpointHitDto hitDto;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        end = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

        hitDto = EndpointHitDto.builder()
                .app("test-app")
                .uri("/test")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void addStat_WithNewApp_ShouldCreateAndSaveApp() {
        when(appStorage.findByName("test-app")).thenReturn(Optional.empty());
        when(appStorage.save(any(App.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statStorage.save(any())).thenAnswer(inv -> inv.getArgument(0));

        statService.addStat(hitDto);

        verify(appStorage).save(any(App.class));
        verify(statStorage).save(any());
    }

    @Test
    void addStat_WithExistingApp_ShouldNotCreateNewApp() {
        App existingApp = new App("test-app");
        existingApp.setId(1L);

        when(appStorage.findByName("test-app")).thenReturn(Optional.of(existingApp));
        when(statStorage.save(any())).thenAnswer(inv -> inv.getArgument(0));

        statService.addStat(hitDto);

        verify(appStorage, never()).save(any());
        verify(statStorage).save(any());
    }

    @Test
    void addStat_WithNullDto_ShouldThrowException() {
        assertThatThrownBy(() -> statService.addStat(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("null record");
    }

    @Test
    void getStats_WithoutFilters_ShouldReturnAllStats() {
        ViewStat mockView = createMockViewStat("test-app", "/test", 5L);
        when(statStorage.getStats(start, end, false, null))
                .thenReturn(List.of(mockView));

        List<ViewStatsDto> result = statService.getStats(start, end, false, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApp()).isEqualTo("test-app");
        assertThat(result.get(0).getUri()).isEqualTo("/test");
        assertThat(result.get(0).getHits()).isEqualTo(5L);
    }

    @Test
    void getStats_WithUris_ShouldReturnFilteredStats() {
        List<String> uris = List.of("/test1", "/test2");
        ViewStat mockView = createMockViewStat("test-app", "/test1", 3L);

        when(statStorage.getStats(start, end, false, uris))
                .thenReturn(List.of(mockView));

        List<ViewStatsDto> result = statService.getStats(start, end, false, uris);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUri()).isEqualTo("/test1");
    }

    private ViewStat createMockViewStat(String app, String uri, Long hits) {
        return new ViewStat() {

            @Override
            public String getAppName() {
                return app;
            }

            @Override
            public String getUri() {
                return uri;
            }

            @Override
            public Long getHits() {
                return hits;
            }
        };
    }
}