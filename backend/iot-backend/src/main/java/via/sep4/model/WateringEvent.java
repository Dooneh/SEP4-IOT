package via.sep4.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class WateringEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;

    private Long experimentId;


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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getExperimentId() { return experimentId; }
    public void setExperimentId(Long experimentId) { this.experimentId = experimentId; }
}
