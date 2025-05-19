package via.sep4.service;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import via.sep4.model.PlantExperiment;
import via.sep4.model.WaterPumpEvent;
import via.sep4.repository.WaterPumpEventRepository;
import via.sep4.types.TriggerType;

@Service
@RequiredArgsConstructor
public class WaterPumpService {
    private static final Logger logger = LoggerFactory.getLogger(WaterPumpService.class);

    private final WaterPumpEventRepository waterPumpEventRepository;

    private final ExperimentConfigService experimentConfigService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private WaterPumpEvent currentEvent = null;
    private boolean pumpRunning = false;

    public WaterPumpEvent startManualWatering() {
        if (pumpRunning) {
            throw new IllegalStateException("Pump is already running");
        }

        PlantExperiment experiment = experimentConfigService.getCurrentExperiment()
                .orElseThrow(() -> new RuntimeException("No active experiment"));

        WaterPumpEvent event = new WaterPumpEvent();
        event.setExperiment(experiment);
        event.setStartTime(LocalDateTime.now());
        event.setTriggerType(TriggerType.MANUAL);

        currentEvent = waterPumpEventRepository.save(event);
        pumpRunning = true;

        sendPumpCommand("pump on");

        return currentEvent;
    }

    public WaterPumpEvent stopManualWatering() {
        if (!pumpRunning || currentEvent == null) {
            throw new IllegalStateException("Pump is not running");
        }

        sendPumpCommand("pump off");

        return completePumpEvent();
    }

    public WaterPumpEvent runPumpForDuration(long durationMs) {
        if (pumpRunning) {
            throw new IllegalStateException("Pump is already running");
        }

        PlantExperiment experiment = experimentConfigService.getCurrentExperiment()
                .orElseThrow(() -> new RuntimeException("No active experiment"));

        WaterPumpEvent event = new WaterPumpEvent();
        event.setExperiment(experiment);
        event.setStartTime(LocalDateTime.now());
        event.setTriggerType(TriggerType.MANUAL);
        event.setDurationSeconds(durationMs / 1000.0);

        currentEvent = waterPumpEventRepository.save(event);
        pumpRunning = true;

        sendPumpCommand("pump run " + (durationMs / 1000));

        scheduler.schedule(() -> {
            completePumpEvent();
        }, durationMs, TimeUnit.MILLISECONDS);

        return currentEvent;
    }

    public boolean isPumpRunning() {
        return pumpRunning;
    }

    public WaterPumpEvent getLastPumpEvent() {
        return waterPumpEventRepository
                .findFirstByExperimentIdOrderByStartTimeDesc(
                        experimentConfigService.getCurrentExperimentId())
                .orElse(null);
    }

    private WaterPumpEvent completePumpEvent() {
        if (currentEvent != null) {
            currentEvent.setEndTime(LocalDateTime.now());
            double durationSeconds = java.time.Duration.between(
                    currentEvent.getStartTime(),
                    currentEvent.getEndTime()).toSeconds();

            currentEvent.setDurationSeconds(durationSeconds);
            currentEvent = waterPumpEventRepository.save(currentEvent);
        }

        pumpRunning = false;
        return currentEvent;
    }

    private void sendPumpCommand(String command) {
        logger.info("Sending pump command: {}", command);
        // TODO: Send kommandoer til microcontroller via TCP...
    }
}
