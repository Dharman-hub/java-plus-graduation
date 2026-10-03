package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "events")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "initiator_id")
    private User initiator;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "published_on")
    private LocalDateTime published;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime created = LocalDateTime.now();

    @Enumerated(value = EnumType.STRING)
    private EventState state;

    @Column(name = "confirmed_requests")
    private Integer confirmedRequests;

    @Column(name = "participant_limit")
    private Integer participantLimit;

    @Column(name = "request_moderation")
    private Boolean requestModeration;

    private Boolean paid;
    private String title;
    private String annotation;
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Event event = (Event) o;

        if (event.id != null) {
            return Objects.equals(id, event.id);
        }

        return Objects.equals(title, event.title) &&
                Objects.equals(eventDate, event.eventDate) &&
                Objects.equals(initiator, event.initiator);
    }


    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hashCode(id);
        }
        return Objects.hash(title, eventDate, initiator);
    }
}