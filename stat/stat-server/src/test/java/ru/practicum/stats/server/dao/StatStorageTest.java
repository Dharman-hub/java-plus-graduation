package ru.practicum.stats.server.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.stats.server.model.App;
import ru.practicum.stats.server.model.Stat;
import ru.practicum.stats.server.model.ViewStat;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@ActiveProfiles("test")
class StatStorageTest {
    @Autowired
    private StatStorage statStorage;

    @Autowired
    private AppStorage appStorage;

    private LocalDateTime start;
    private LocalDateTime end;
    private App testApp;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        end = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

        testApp = appStorage.save(new App("test-app"));

        Stat stat1 = Stat.builder()
                .app(testApp)
                .uri("/test/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.of(2024, 6, 1, 10, 0, 0))
                .build();

        Stat stat2 = Stat.builder()
                .app(testApp)
                .uri("/test/1")
                .ip("192.168.1.2")
                .timestamp(LocalDateTime.of(2024, 6, 1, 11, 0, 0))
                .build();

        Stat stat3 = Stat.builder()
                .app(testApp)
                .uri("/test/2")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.of(2024, 6, 1, 12, 0, 0))
                .build();

        statStorage.saveAll(List.of(stat1, stat2, stat3));
    }

    @Test
    void getStats_WithoutUris_ShouldReturnAllStats() {
        List<ViewStat> result = statStorage.getStats(start, end, false, null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ViewStat::getUri)
                .containsExactlyInAnyOrder("/test/1", "/test/2");
    }

    @Test
    void getStats_WithUris_ShouldReturnFilteredStats() {
        List<String> uris = List.of("/test/1");
        List<ViewStat> result = statStorage.getStats(start, end, false, uris);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUri()).isEqualTo("/test/1");
        assertThat(result.get(0).getHits()).isEqualTo(2);
    }

    @Test
    void getStats_WithUniqueFlag_ShouldCountUniqueIps() {
        List<ViewStat> result = statStorage.getStats(start, end, true, null);

        ViewStat stat1 = result.stream()
                .filter(s -> s.getUri().equals("/test/1"))
                .findFirst()
                .orElseThrow();

        assertThat(stat1.getHits()).isEqualTo(2);

        ViewStat stat2 = result.stream()
                .filter(s -> s.getUri().equals("/test/2"))
                .findFirst()
                .orElseThrow();

        assertThat(stat2.getHits()).isEqualTo(1);
    }
}