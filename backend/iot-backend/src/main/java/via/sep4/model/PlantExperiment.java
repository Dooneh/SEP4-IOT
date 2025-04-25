package via.sep4.model;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class PlantExperiment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private String plantSpecies;

    private LocalDate startDate;

    private LocalDate endDate;

    @OneToMany(mappedBy = "experiment", cascade = CascadeType.ALL)
    private List<PlantMeasurements> temperatureReadings;
}
