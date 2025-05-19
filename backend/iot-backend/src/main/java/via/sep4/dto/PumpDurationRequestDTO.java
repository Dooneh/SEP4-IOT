package via.sep4.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PumpDurationRequestDTO {
    private Integer seconds;
    private Integer milliseconds;
}
