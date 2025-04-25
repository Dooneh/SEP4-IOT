package via.sep4.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class PlantMeasurements {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "experiment_id", nullable = false)
    @JsonIgnore
    private PlantExperiment experiment;

    private double luftTemperatur;
    private double luftfugtighed;
    private double jordFugtighed;

    private String lysIndstilling;
    private double lysHøjesteIntensitet;
    private double lysLavesteIntensitet;
    private double lysGennemsnit;

    private double afstandTilHøjde;

    private double vandTidFraSidste;
    private double vandMængde;
    private double vandFrekvens;

    private LocalDateTime timestamp;
}
