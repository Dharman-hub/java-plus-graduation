package ru.practicum.stats.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.client.exception.StatsServerUnavailable;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Component
public class StatClient {

    private static final Logger log = LoggerFactory.getLogger(StatClient.class);

    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;

    @Autowired
    public StatClient(DiscoveryClient discoveryClient,
                      @Value("${stats-service-id}") String statsServiceId) {
        this.discoveryClient = discoveryClient;
        this.statsServiceId = statsServiceId;
        this.retryTemplate = createRetryTemplate();
        this.restClient = RestClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .defaultStatusHandler(HttpStatusCode::is4xxClientError, (request, response) -> {
                    log.error("Client error: {} - {}", response.getStatusCode(),
                            new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
                })
                .defaultStatusHandler(HttpStatusCode::is5xxServerError, (request, response) -> {
                    log.error("Server error: {} - {}", response.getStatusCode(),
                            new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
                })
                .build();
    }

    public StatClient(RestClient restClient) {
        this.restClient = restClient;
        this.discoveryClient = null;
        this.retryTemplate = null;
        this.statsServiceId = null;
    }

    public void hit(String app, String uri, String ip, LocalDateTime timestamp) {
        EndpointHitDto dto = EndpointHitDto.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(timestamp)
                .build();

        try {
            restClient.post()
                    .uri(makeUri("/hit"))
                    .body(dto)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Hit successfully sent to stats-service: app={}, uri={}, ip={}, timestamp={}",
                    app, uri, ip, timestamp);
        } catch (Exception e) {
            log.error("Failed to send hit: {}", e.getMessage(), e);
        }
    }

    public List<ViewStatsDto> getStat(LocalDateTime start,
                                      LocalDateTime end,
                                      List<String> uris,
                                      Boolean unique) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", start.format(formatter))
                .queryParam("end", end.format(formatter))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                builder.queryParam("uris", uri);
            }
        }

        try {
            List<ViewStatsDto> stats = restClient.get()
                    .uri(makeUri(builder.build().encode().toString()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ViewStatsDto>>() {
                    });

            log.info(
                    "Successfully requesting parameters to stats-service: start={}, end={}, uris={}, unique={} and received stats. Count={}",
                    start, end, uris, unique, stats != null ? stats.size() : 0
            );

            return stats;
        } catch (Exception e) {
            log.error("Failed to get stats: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String makeUri(String path) {
        if (discoveryClient == null) {
            return path;
        }

        ServiceInstance instance = retryTemplate.execute(context -> getInstance());

        return "http://" + instance.getHost() + ":" + instance.getPort() + path;
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(statsServiceId)
                    .getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }

    private RetryTemplate createRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}