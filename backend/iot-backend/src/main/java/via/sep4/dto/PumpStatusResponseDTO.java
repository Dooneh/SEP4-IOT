package via.sep4.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import via.sep4.types.TriggerType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PumpStatusResponseDTO {
    private boolean running;
    private TriggerType lastTriggerType;
    private LocalDateTime lastStartTime;
    private Double lastDuration;
}
