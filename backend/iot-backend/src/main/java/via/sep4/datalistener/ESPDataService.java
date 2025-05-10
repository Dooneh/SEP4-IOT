package via.sep4.datalistener;

import org.antlr.v4.runtime.misc.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import via.sep4.model.InvalidMeasurement;
import via.sep4.model.PlantExperiment;
import via.sep4.model.PlantMeasurements;
import via.sep4.processing.DataValidator;
import via.sep4.processing.DataValidator.ValidationResult;
import via.sep4.repository.InvalidMeasurementRepository;
import via.sep4.repository.PlantMeasurementsRepository;
import via.sep4.service.ExperimentConfigService;
import via.sep4.model.WateringEvent;
import via.sep4.repository.WateringEventRepository;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.*;

@Service
public class ESPDataService {
    private static final Logger logger = LoggerFactory.getLogger(ESPDataService.class);

    @Autowired
    private DataValidator dataValidator;

    @Autowired
    private PlantMeasurementsRepository measurementsRepository;

    @Autowired
    private InvalidMeasurementRepository invalidMeasurementRepository;

    @Autowired
    private ExperimentConfigService experimentConfigService;

    @Autowired
    private WateringEventRepository wateringEventRepository;

    @Autowired
    private WaterPump waterPump;

    private LocalDateTime lastWateringTime;


    Pattern pumpPattern = Pattern.compile("Pump: (ON|OFF)");

    private final Pattern pattern = Pattern.compile("(Distance|Temp|Humidity|Soil): (\\d+\\.?\\d*)");


    public void processData(String data) {
        logger.info("Processing data: {}", data);

        // Check if the data contains pump commands
        Matcher pumpMatcher = pumpPattern.matcher(data);
        if (pumpMatcher.find()) {
            String pumpCommand = pumpMatcher.group(1);
            processPumpCommand(pumpCommand);
            return; // Process only pump command and return
        }

        Map<String, String> extractedData = extractMeasurements(data);
        if (extractedData.isEmpty()) {
            logger.warn("No valid data found in input string: {}", data);
            storeInvalidMeasurement(null, data, "No valid data could be extracted");
            return;
        }

        Long experimentId = experimentConfigService.getCurrentExperimentId();
        Optional<PlantExperiment> experimentOptional = experimentConfigService.getCurrentExperiment();

        if (!experimentOptional.isPresent()) {
            logger.error("No active experiment found with ID: {}", experimentId);
            storeInvalidMeasurement(experimentId, data, "Active experiment not found");
            return;
        }

        PlantExperiment experiment = experimentOptional.get();

        PlantMeasurements measurement = new PlantMeasurements();
        measurement.setExperiment(experiment);
        measurement.setTimestamp(LocalDateTime.now());

        initializeDefaultValues(measurement);

        processTemperature(extractedData.get("Temp"), measurement, experimentId, data);
        processHumidity(extractedData.get("Humidity"), measurement, experimentId, data);
        processSoilMoisture(extractedData.get("Soil"), measurement, experimentId, data);
        processDistance(extractedData.get("Distance"), measurement, experimentId, data);

        if (hasMeasurements(measurement)) {
            measurementsRepository.save(measurement);
            logger.info("Saved measurement with valid data points for experiment ID: {}", experimentId);
        } else {
            logger.info("No valid measurements were found, not saving to measurements table");
        }
    }

    private Map<String, String> extractMeasurements(String data) {
        Map<String, String> extractedData = new HashMap<>();
        Matcher matcher = pattern.matcher(data);

        while (matcher.find()) {
            String label = matcher.group(1);
            String value = matcher.group(2);
            extractedData.put(label, value);
            logger.debug("Extracted {}: {}", label, value);
        }

        return extractedData;
    }

    private void initializeDefaultValues(PlantMeasurements measurement) {
        measurement.setLuftTemperatur(0);
        measurement.setLuftfugtighed(0);
        measurement.setJordFugtighed(0);
        measurement.setAfstandTilHøjde(0);
        measurement.setLysHøjesteIntensitet(0);
        measurement.setLysLavesteIntensitet(0);
        measurement.setLysGennemsnit(0);
        measurement.setVandTidFraSidste(0);
        measurement.setVandMængde(0);
        measurement.setVandFrekvens(0);
    }

    private boolean hasMeasurements(PlantMeasurements measurement) {
        return measurement.getLuftTemperatur() != 0 ||
                measurement.getLuftfugtighed() != 0 ||
                measurement.getJordFugtighed() != 0 ||
                measurement.getAfstandTilHøjde() != 0;
    }

    private void processTemperature(String tempValue, PlantMeasurements measurement, Long experimentId,
            String rawData) {
        if (tempValue == null) {
            logger.debug("No temperature value found");
            return;
        }

        try {
            Float temp = Float.parseFloat(tempValue);
            ValidationResult result = dataValidator.validateTemperature(temp);

            if (result == ValidationResult.VALIDATION_SUCCESS) {
                measurement.setLuftTemperatur(temp);
                logger.debug("Valid temperature: {}", temp);
            } else {
                String errorMessage = "Temperature validation failed: " + dataValidator.getErrorMessage(result);
                logger.warn(errorMessage);

                storeInvalidMeasurement(experimentId,
                        "Temp: " + tempValue,
                        errorMessage);
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid temperature format: {}", tempValue);
            storeInvalidMeasurement(experimentId,
                    "Temp: " + tempValue,
                    "Invalid temperature format");
        }
    }

    private void processHumidity(String humidityValue, PlantMeasurements measurement, Long experimentId,
            String rawData) {
        if (humidityValue == null) {
            logger.debug("No humidity value found");
            return;
        }

        try {
            Integer humidity = Integer.parseInt(humidityValue);
            ValidationResult result = dataValidator.validateHumidity(humidity);

            if (result == ValidationResult.VALIDATION_SUCCESS) {
                measurement.setLuftfugtighed(humidity);
                logger.debug("Valid humidity: {}", humidity);
            } else {
                String errorMessage = "Humidity validation failed: " + dataValidator.getErrorMessage(result);
                logger.warn(errorMessage);

                // Store the invalid humidity measurement
                storeInvalidMeasurement(experimentId,
                        "Humidity: " + humidityValue,
                        errorMessage);
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid humidity format: {}", humidityValue);
            storeInvalidMeasurement(experimentId,
                    "Humidity: " + humidityValue,
                    "Invalid humidity format");
        }
    }

    private void processSoilMoisture(String soilValue, PlantMeasurements measurement, Long experimentId,
            String rawData) {
        if (soilValue == null) {
            logger.debug("No soil moisture value found");
            return;
        }

        try {
            Integer soil = Integer.parseInt(soilValue);
            ValidationResult result = dataValidator.validateSoilMoisture(soil);

            if (result == ValidationResult.VALIDATION_SUCCESS) {
                measurement.setJordFugtighed(soil);
                logger.debug("Valid soil moisture: {}", soil);
            } else {
                String errorMessage = "Soil moisture validation failed: " + dataValidator.getErrorMessage(result);
                logger.warn(errorMessage);

                storeInvalidMeasurement(experimentId,
                        "Soil: " + soilValue,
                        errorMessage);
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid soil moisture format: {}", soilValue);
            storeInvalidMeasurement(experimentId,
                    "Soil: " + soilValue,
                    "Invalid soil moisture format");
        }
    }

    private void processDistance(String distanceValue, PlantMeasurements measurement, Long experimentId,
            String rawData) {
        if (distanceValue == null) {
            logger.debug("No distance value found");
            return;
        }

        try {
            Integer distance = Integer.parseInt(distanceValue);
            ValidationResult result = dataValidator.validateHeight(distance);

            if (result == ValidationResult.VALIDATION_SUCCESS) {
                measurement.setAfstandTilHøjde(distance);
                logger.debug("Valid distance: {}", distance);
            } else {
                String errorMessage = "Distance validation failed: " + dataValidator.getErrorMessage(result);
                logger.warn(errorMessage);

                storeInvalidMeasurement(experimentId,
                        "Distance: " + distanceValue,
                        errorMessage);
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid distance format: {}", distanceValue);
            storeInvalidMeasurement(experimentId,
                    "Distance: " + distanceValue,
                    "Invalid distance format");
        }
    }

    private void storeInvalidMeasurement(Long experimentId, String rawData, String errorMessage) {
        InvalidMeasurement invalidMeasurement = new InvalidMeasurement();
        invalidMeasurement.setExperimentId(experimentId);
        invalidMeasurement.setRawData(rawData);
        invalidMeasurement.setValidationError(errorMessage);
        invalidMeasurement.setReceivedAt(LocalDateTime.now());

        invalidMeasurementRepository.save(invalidMeasurement);
        logger.info("Stored invalid measurement: {}", errorMessage);
    }




/**
 * Process pump commands and manage watering events
 *
 * @param command ON or OFF command for the pump
 */
        private void processPumpCommand (String command){
            logger.info("Processing pump command: {}", command);

            Long experimentId = experimentConfigService.getCurrentExperimentId();
            Optional<PlantExperiment> experimentOptional = experimentConfigService.getCurrentExperiment();

            if (!experimentOptional.isPresent()) {
                logger.error("No active experiment found with ID: {}", experimentId);
                return;
            }

            PlantExperiment experiment = experimentOptional.get();

            if ("ON".equals(command)) {
                // Start the water pump
                boolean success = waterPump.startWatering();

                if (success) {
                    // Record the watering event
                    WateringEvent event = new WateringEvent();
                    event.setExperiment(experiment);
                    event.setStartTime(LocalDateTime.now());
                    event.setEndTime(null); // Will be set when pump turns OFF

                    // Calculate time since last watering if available
                    if (lastWateringTime != null) {
                        long minutesSinceLastWatering = ChronoUnit.MINUTES.between(lastWateringTime, LocalDateTime.now());
                        event.setMinutesSinceLastWatering((int) minutesSinceLastWatering);
                    }

                    // Save the watering event
                    WateringEvent savedEvent = wateringEventRepository.save(event);
                    logger.info("Recorded watering start event with ID: {}", savedEvent.getId());
                } else {
                    logger.error("Failed to start water pump");
                }
            } else if ("OFF".equals(command)) {
                // Stop the water pump
                boolean success = waterPump.stopWatering();

                if (success) {
                    // Find the latest watering event without an end time
                    List<WateringEvent> incompleteEvents = wateringEventRepository.findByExperimentIdAndEndTimeIsNull(experiment.getId());

                    if (!incompleteEvents.isEmpty()) {
                        // Update the most recent event with an end time
                        WateringEvent latestEvent = incompleteEvents.get(0);
                        latestEvent.setEndTime(LocalDateTime.now());

                        // Calculate duration in seconds
                        long durationSeconds = ChronoUnit.SECONDS.between(latestEvent.getStartTime(), latestEvent.getEndTime());
                        latestEvent.setDurationSeconds((int) durationSeconds);

                        // Update the event
                        wateringEventRepository.save(latestEvent);
                        logger.info("Updated watering event {} with end time and duration: {} seconds",
                                latestEvent.getId(), durationSeconds);

                        // Update the lastWateringTime for future reference
                        lastWateringTime = LocalDateTime.now();
                    } else {
                        logger.warn("Received PUMP OFF command but no active watering event was found");
                    }
                } else {
                    logger.error("Failed to stop water pump");
                }
            }
        }


    }
}

