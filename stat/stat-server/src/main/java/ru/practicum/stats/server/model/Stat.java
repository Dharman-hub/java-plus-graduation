package ru.practicum.stats.server.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "statistics")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "app_id")
    private App app;

    private String uri;
    private String ip;
    private LocalDateTime timestamp;

    public String getAppName() {
        return app != null ? app.getName() : null;
    }
}
