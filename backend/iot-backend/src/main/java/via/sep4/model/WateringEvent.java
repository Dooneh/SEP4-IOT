package via.sep4.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class WateringEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;

    private Long experimentId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer durationSeconds;

    public WateringEvent(Long id, LocalDateTime timestamp, Long experimentId) {
        this.id = id;
        this.timestamp = timestamp;
        this.experimentId = experimentId;
    }

    public WateringEvent() {}

    public WateringEvent(LocalDateTime timestamp, Long experimentId) {
        this.timestamp = timestamp;
        this.experimentId = experimentId;
    }
}