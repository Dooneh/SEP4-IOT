package via.sep4.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import via.sep4.dto.ErrorResponseDTO;
import via.sep4.dto.PumpResponseDTO;
import via.sep4.dto.PumpStatusResponseDTO;
import via.sep4.model.WaterPumpEvent;
import via.sep4.repository.WaterPumpEventRepository;
import via.sep4.service.WaterPumpService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/waterpump")
@RequiredArgsConstructor
public class WaterPumpEventController {
    private static final Logger logger = LoggerFactory.getLogger(WaterPumpEventController.class);

    private final WaterPumpService waterPumpService;

    @PostMapping("/start")
    public ResponseEntity<?> startPump() {
        try {
            WaterPumpEvent event = waterPumpService.startManualWatering();
            return ResponseEntity.ok()
                    .body(new PumpResponseDTO("Pump started successfully", event.getId()));
        } catch (Exception e) {
            logger.error("Failed to start pump", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponseDTO("Failed to start pump: " + e.getMessage()));
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopPump() {
        try {
            WaterPumpEvent event = waterPumpService.stopManualWatering();
            return ResponseEntity.ok()
                    .body(new PumpResponseDTO("Pump stopped successfully",
                            event.getDurationSeconds() + " seconds"));
        } catch (Exception e) {
            logger.error("Failed to stop pump", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponseDTO("Failed to stop pump: " + e.getMessage()));
        }
    }

    @PostMapping("/run")
    public ResponseEntity<?> runPumpForDuration(
            @RequestParam(required = false) Integer seconds,
            @RequestParam(required = false) Integer milliseconds) {

        try {
            long durationMs = 0;
            if (seconds != null) {
                durationMs += seconds * 1000L;
            }
            if (milliseconds != null) {
                durationMs += milliseconds;
            }

            if (durationMs <= 0) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseDTO("Duration must be greater than 0"));
            }

            WaterPumpEvent event = waterPumpService.runPumpForDuration(durationMs);
            return ResponseEntity.ok()
                    .body(new PumpResponseDTO("Pump scheduled to run",
                            String.format("%.1f seconds", durationMs / 1000.0)));
        } catch (Exception e) {
            logger.error("Failed to schedule pump run", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponseDTO("Failed to schedule pump run: " + e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getPumpStatus() {
        try {
            boolean isRunning = waterPumpService.isPumpRunning();
            WaterPumpEvent lastEvent = waterPumpService.getLastPumpEvent();

            return ResponseEntity.ok(new PumpStatusResponseDTO(
                    isRunning,
                    lastEvent != null ? lastEvent.getTriggerType() : null,
                    lastEvent != null ? lastEvent.getStartTime() : null,
                    lastEvent != null ? lastEvent.getDurationSeconds() : null));
        } catch (Exception e) {
            logger.error("Failed to get pump status", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponseDTO("Failed to get pump status: " + e.getMessage()));
        }
    }
}
