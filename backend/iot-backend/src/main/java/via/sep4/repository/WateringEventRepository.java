package via.sep4.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import via.sep4.model.WateringEvent;

public interface WateringEventRepository extends JpaRepository<WateringEvent, Long> {
    //  add custom query methods here For example, to find watering events by experiment ID:
    // List<WateringEvent> findByExperimentId(Long experimentId);
    // Can methods to find watering events by timestamp or other criteria if needed.

}
