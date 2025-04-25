package via.sep4.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import via.sep4.model.PlantExperiment;

@Repository
public interface PlantExperimentRepository extends JpaRepository<PlantExperiment, Long> {
    List<PlantExperiment> findByPlantSpecies(String plantSpecies);

    Optional<PlantExperiment> findByName(String name);
}
