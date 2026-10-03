package ru.practicum.stats.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private DateTimeFormatter formatter;
    private String start;
    private String end;

    @BeforeEach
    void setUp() throws Exception {
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        addTestHit("test-app", "/test1", "192.168.1.1");
        addTestHit("test-app", "/test1", "192.168.1.2");
        addTestHit("test-app", "/test2", "192.168.1.1");

        start = LocalDateTime.now().minusDays(1).format(formatter);
        end = LocalDateTime.now().plusDays(1).format(formatter);
    }

    @Test
    void addStat_ShouldReturnCreated() throws Exception {
        EndpointHitDto dto = EndpointHitDto.builder()
                .app("test-app")
                .uri("/test")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void getStats_WithoutFilters_ShouldReturnStats() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", start)
                        .param("end", end))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getStats_WithUris_ShouldReturnFilteredStats() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", start)
                        .param("end", end)
                        .param("uris", "/test1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].uri").value("/test1"));
    }

    @Test
    void getStats_WithUniqueFlag_ShouldCountUniqueIps() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", start)
                        .param("end", end)
                        .param("unique", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hits").value(2));
    }

    @Test
    void getStats_WithMultipleUris_ShouldReturnMultipleStats() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", start)
                        .param("end", end)
                        .param("uris", "/test1", "/test2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getStats_WithInvalidDate_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "invalid-date")
                        .param("end", "2024-12-31 23:59:59"))
                .andExpect(status().isInternalServerError());
    }

    private void addTestHit(String app, String uri, String ip) throws Exception {
        EndpointHitDto dto = EndpointHitDto.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }
}