package via.sep4.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import via.sep4.model.InvalidMeasurement;

@Repository
public interface InvalidMeasurementRepository extends JpaRepository<InvalidMeasurement, Long> {
    List<InvalidMeasurement> findByExperimentId(Long experimentId);
}
