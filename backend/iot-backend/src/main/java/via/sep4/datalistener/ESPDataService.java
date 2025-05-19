package via.sep4.datalistener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import via.sep4.model.InvalidMeasurement;
import via.sep4.model.PlantExperiment;
import via.sep4.model.PlantMeasurements;
import via.sep4.model.WaterPumpEvent;
import via.sep4.processing.DataValidator;
import via.sep4.processing.DataValidator.ValidationResult;
import via.sep4.repository.InvalidMeasurementRepository;
import via.sep4.repository.PlantMeasurementsRepository;
import via.sep4.repository.WaterPumpEventRepository;
import via.sep4.service.ExperimentConfigService;
import via.sep4.types.TriggerType;

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
    private WaterPumpEventRepository waterPumpEventRepository;

    @Autowired
    private ExperimentConfigService experimentConfigService;

    private WaterPumpEvent currentPumpEvent = null;

    private final Pattern numericPattern = Pattern.compile("(Distance|Temp|Humidity|Soil): (\\d+\\.?\\d*)");
    private final Pattern lightPattern = Pattern.compile("Light: (\\d+)% \\(raw: (\\d+)\\)");
    private final Pattern motionPattern = Pattern.compile("Motion: (\\w+)");
    private final Pattern pumpPattern = Pattern.compile("Pump: (ON|OFF)");

    public void processData(String data) {
        logger.info("Processing data: {}", data);

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
        processLight(extractedData.get("Light"), measurement, experimentId, data);
        processLightRaw(extractedData.get("LightRaw"), measurement, experimentId, data);
        processMotion(extractedData.get("Motion"), measurement, experimentId, data);
        processWaterPump(extractedData.get("Pump"), experiment);

        if (hasMeasurements(measurement)) {
            measurementsRepository.save(measurement);
            logger.info("Saved measurement with valid data points for experiment ID: {}", experimentId);
        } else {
            logger.info("No valid measurements were found, not saving to measurements table");
        }
    }

    private Map<String, String> extractMeasurements(String data) {
        Map<String, String> extractedData = new HashMap<>();
        Matcher numericMatcher = numericPattern.matcher(data);
        while (numericMatcher.find()) {
            String label = numericMatcher.group(1);
            String value = numericMatcher.group(2);
            extractedData.put(label, value);
            logger.debug("Extracted {}: {}", label, value);
        }

        Matcher lightMatcher = lightPattern.matcher(data);
        if (lightMatcher.find()) {
            String lightPercentage = lightMatcher.group(1);
            String lightRaw = lightMatcher.group(2);
            extractedData.put("Light", lightPercentage);
            extractedData.put("LightRaw", lightRaw);
            logger.debug("Extracted Light: {}%, LightRaw: {}", lightPercentage, lightRaw);
        }

        Matcher motionMatcher = motionPattern.matcher(data);
        if (motionMatcher.find()) {
            String motionValue = motionMatcher.group(1);
            extractedData.put("Motion", motionValue);
            logger.debug("Extracted Motion: {}", motionValue);
        }

        Matcher pumpMatcher = pumpPattern.matcher(data);
        if (pumpMatcher.find()) {
            String pumpStatus = pumpMatcher.group(1);
            extractedData.put("Pump", pumpStatus);
            logger.debug("Extracted Pump: {}", pumpStatus);
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
        measurement.setMotionSensor("No");
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

    private void processMotion(String motionValue, PlantMeasurements measurement, Long experimentId,
            String rawData) {
        if (motionValue == null) {
            logger.debug("No motion value found");
            return;
        }

        try {
            String motion = motionValue.trim();
            ValidationResult result = dataValidator.validateMotionSensor(motion);

            if (result == ValidationResult.VALIDATION_SUCCESS) {
                measurement.setMotionSensor(motion);
                logger.debug("Valid motion: {}", motion);
            } else {
                String errorMessage = "Motion validation failed: " + dataValidator.getErrorMessage(result);
                logger.warn(errorMessage);

                storeInvalidMeasurement(experimentId,
                        "Motion: " + motionValue,
                        errorMessage);
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid motion format: {}", motionValue);
            storeInvalidMeasurement(experimentId,
                    "Motion: " + motionValue,
                    "Invalid motion format");
        }
    }

    private void processLight(String lightValue, PlantMeasurements measurement, Long experimentId,
            String rawData) {
        if (lightValue == null) {
            logger.debug("No light value found");
        }

        try {
            double lightPercentage = Double.parseDouble(lightValue);

            ValidationResult validationResult = dataValidator.validateLightAmount(lightPercentage);
            if (validationResult != ValidationResult.VALIDATION_SUCCESS) {
                String errorMessage = dataValidator.getErrorMessage(validationResult);
                logger.warn("Light validation failed: {}", errorMessage);
                storeInvalidMeasurement(experimentId, "Light: " + lightValue, errorMessage);
                return;
            }

            measurement.setLysMængde(lightPercentage);

            logger.debug("Processed light value: {}%", lightPercentage);
        } catch (NumberFormatException e) {
            logger.warn("Invalid light format: {}", lightValue);
            storeInvalidMeasurement(experimentId,
                    "Light: " + lightValue,
                    "Invalid light format");
        }
    }

    private void processLightRaw(String lightRawValue, PlantMeasurements measurement, Long experimentId,
            String rawData) {
        if (lightRawValue == null) {
            logger.debug("No raw light value found");
            return;
        }

        try {
            double lightRaw = Double.parseDouble(lightRawValue);

            ValidationResult validationResult = dataValidator.validateLightRaw(lightRaw);
            if (validationResult != ValidationResult.VALIDATION_SUCCESS) {
                logger.warn("Raw light validation failed: {}", validationResult);
                storeInvalidMeasurement(experimentId,
                        "LightRaw: " + lightRawValue,
                        "Raw light validation failed: " + validationResult);
                return;
            }

            measurement.setLysMængdeRaw(lightRaw);
            logger.debug("Processed raw light value: {}", lightRaw);
        } catch (NumberFormatException e) {
            logger.warn("Invalid raw light format: {}", lightRawValue);
            storeInvalidMeasurement(experimentId,
                    "LightRaw: " + lightRawValue,
                    "Invalid raw light format");
        }
    }

    private void processWaterPump(String pumpStatus, PlantExperiment experiment) {
        if (pumpStatus == null) {
            logger.debug("No pump status found");
            return;
        }

        boolean isRunning = "ON".equals(pumpStatus);

        if (isRunning && currentPumpEvent == null) {
            currentPumpEvent = new WaterPumpEvent();
            currentPumpEvent.setExperiment(experiment);
            currentPumpEvent.setStartTime(LocalDateTime.now());
            currentPumpEvent.setTriggerType(TriggerType.AUTOMATIC);
            logger.info("Water pump started at {}", currentPumpEvent.getStartTime());
        } else if (!isRunning && currentPumpEvent != null) {
            currentPumpEvent.setEndTime(LocalDateTime.now());

            double durationSeconds = java.time.Duration.between(
                    currentPumpEvent.getStartTime(),
                    currentPumpEvent.getEndTime()).toSeconds();

            currentPumpEvent.setDurationSeconds(durationSeconds);

            waterPumpEventRepository.save(currentPumpEvent);
            logger.info("Water pump stopped. Event saved with duration: {} seconds", durationSeconds);

            currentPumpEvent = null;
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
}
