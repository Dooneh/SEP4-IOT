package via.sep4.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import via.sep4.model.WaterPumpEvent;

import java.util.List;

public interface WaterPumpEventRepository extends JpaRepository<WaterPumpEvent, Long> {
    List<WaterPumpEvent> findByExperimentIdAndEndTimeIsNull(Long experimentId);
}