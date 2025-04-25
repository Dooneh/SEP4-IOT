package via.sep4.repository;

import via.sep4.model.PlantMeasurements;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantMeasurementsRepository extends JpaRepository<PlantMeasurements, Long> {
    List<PlantMeasurements> findByExperimentId(Long experimentId);

    List<PlantMeasurements> findByExperimentIdAndTimestampBetween(
            Long experimentId,
            LocalDateTime startTime,
            LocalDateTime endTime);

    List<PlantMeasurements> findTop10ByExperimentIdOrderByTimestampDesc(Long experimentId);
}
