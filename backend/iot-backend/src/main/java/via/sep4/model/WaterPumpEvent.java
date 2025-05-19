package via.sep4.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import via.sep4.types.TriggerType;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class WaterPumpEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "experiment_id", nullable = false)
    private PlantExperiment experiment;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double durationSeconds;

    @Enumerated(EnumType.STRING)
    private TriggerType triggerType;
}
