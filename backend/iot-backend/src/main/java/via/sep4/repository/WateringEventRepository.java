package via.sep4.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import via.sep4.model.WateringEvent;

import java.util.List;

public interface WateringEventRepository extends JpaRepository<WateringEvent, Long> {
    List<WateringEvent> findByExperimentIdAndEndTimeIsNull(Long experimentId);
}