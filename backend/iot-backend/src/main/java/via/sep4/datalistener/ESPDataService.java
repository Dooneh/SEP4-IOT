package via.sep4.datalistener;

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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.*;

@Service
public class ESPDataService {
    private static final Logger logger = LoggerFactory.getLogger(ESPDataService.class);

    @Autowired
    DataValidator dataValidator = new DataValidator();

    @Autowired
    private PlantMeasurementsRepository measurementsRepository;

    @Autowired
    private InvalidMeasurementRepository invalidMeasurementRepository;

    @Autowired
    private ExperimentConfigService experimentConfigService;

    Pattern pattern = Pattern.compile("(Distance|Temp|Humidity|Soil): (\\d+)");

    // int distance = -1; // default if missing
    // float temperature = -1;
    // int humidity = -1;

    public void processData(String data) {
        // System.out.println("Processing data: " + data);

        logger.info("Processing data: {}", data);

        Matcher matcher = pattern.matcher(data);

        Integer distance = null;
        Float temperature = null;
        Integer humidity = null;
        Integer soilMoisture = null;

        while (matcher.find()) {
            String label = matcher.group(1);
            String value = matcher.group(2);

            try {
                switch (label) {
                    case "Distance":
                        distance = Integer.parseInt(value);
                        break;
                    case "Temp":
                        temperature = Float.parseFloat(value);
                        break;
                    case "Humidity":
                        humidity = Integer.parseInt(value);
                        break;
                    case "Soil":
                        soilMoisture = Integer.parseInt(value);
                        logger.debug("Extracted soil moisture: {}", soilMoisture);
                        break;
                }
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse {} value: {}", label, value);
            }
        }

        System.out.println("Distance: " + distance); // Midlertidig til test
        System.out.println("Temp: " + temperature); // Til test
        System.out.println("Humidity: " + humidity); // Til test
        System.out.println("Humidity: " + soilMoisture); // Til test

        Long experimentId = experimentConfigService.getCurrentExperimentId();
        Optional<PlantExperiment> experimentOptional = experimentConfigService.getCurrentExperiment();

        if (!experimentOptional.isPresent()) {
            logger.error("No active experiment found with ID: {}", experimentId);
            storeInvalidMeasurement(experimentId, data, "Active experiment not found");
            return;
        }

        PlantExperiment experiment = experimentOptional.get();

        List<String> validationErrors = validateMeasurements(temperature, humidity, distance, soilMoisture);

        if (!validationErrors.isEmpty()) {
            String errorMessage = String.join(", ", validationErrors);
            logger.warn("Validation errors: {}", errorMessage);
            storeInvalidMeasurement(experimentId, data, errorMessage);
        } else {
            try {
                PlantMeasurements measurement = createMeasurement(experiment, temperature, humidity, soilMoisture,
                        distance);
                measurementsRepository.save(measurement);
                logger.info("Successfully saved measurement for experiment ID: {}", experimentId);
            } catch (Exception e) {
                logger.error("Error saving measurement: {}", e.getMessage());
                storeInvalidMeasurement(experimentId, data, "Error saving measurement: " + e.getMessage());
            }
        }
    }

    private List<String> validateMeasurements(Float temperature, Integer humidity, Integer soilMoisture,
            Integer distance) {
        List<String> errors = new ArrayList<>();

        if (temperature != null) {
            ValidationResult tempResult = dataValidator.validateTemperature(temperature);
            if (tempResult != ValidationResult.VALIDATION_SUCCESS) {
                errors.add(dataValidator.getErrorMessage(tempResult));
            }
        } else {
            errors.add("Temperature measurement is missing");
        }

        if (humidity != null) {
            ValidationResult humidityResult = dataValidator.validateHumidity(humidity);
            if (humidityResult != ValidationResult.VALIDATION_SUCCESS) {
                errors.add(dataValidator.getErrorMessage(humidityResult));
            }
        } else {
            errors.add("Humidity measurement is missing");
        }

        if (soilMoisture != null) {
            ValidationResult soilResult = dataValidator.validateSoilMoisture(soilMoisture);
            if (soilResult != ValidationResult.VALIDATION_SUCCESS) {
                errors.add(dataValidator.getErrorMessage(soilResult));
            }
        } else {
            errors.add("Soil moisture measurement is missing");
        }

        if (distance != null) {
            ValidationResult heightResult = dataValidator.validateHeight(distance);
            if (heightResult != ValidationResult.VALIDATION_SUCCESS) {
                errors.add(dataValidator.getErrorMessage(heightResult));
            }
        }

        return errors;
    }

    private PlantMeasurements createMeasurement(PlantExperiment experiment, Float temperature, Integer humidity,
            Integer soilMoisture, Integer distance) {
        PlantMeasurements measurement = new PlantMeasurements();

        measurement.setExperiment(experiment);

        measurement.setTimestamp(LocalDateTime.now());

        if (temperature != null) {
            measurement.setLuftTemperatur(temperature);
        }

        if (humidity != null) {
            measurement.setLuftfugtighed(humidity);
        }

        if (soilMoisture != null) {
            measurement.setJordFugtighed(soilMoisture);
        }

        if (distance != null) {
            measurement.setAfstandTilHøjde(distance);
        }

        measurement.setLysHøjesteIntensitet(0);
        measurement.setLysLavesteIntensitet(0);
        measurement.setLysGennemsnit(0);
        measurement.setVandTidFraSidste(0);
        measurement.setVandMængde(0);
        measurement.setVandFrekvens(0);

        return measurement;
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

    // if (dataValidator.validateHeight(distance) ==
    // DataValidator.ValidationResult.VALIDATION_SUCCESS){
    // plantMeasurementController.addMeasurement()e
    // }
    // else throw new ValidationException("Validation exception type " +
    // (dataValidator.validateTemperature(distance) ==
    // DataValidator.ValidationResult.VALIDATION_SUCCESS));

    // if (dataValidator.validateTemperature(temp) ==
    // DataValidator.ValidationResult.VALIDATION_SUCCESS){
    // plantMeasurementController.addMeasurement()
    // }
    // else throw new ValidationException("Validation exception type " +
    // (dataValidator.validateTemperature(temp) ==
    // DataValidator.ValidationResult.VALIDATION_SUCCESS));

    // if (dataValidator.validateHumidity(humidity) ==
    // DataValidator.ValidationResult.VALIDATION_SUCCESS){
    // plantMeasurementController.addMeasurement()
    // }
    // else throw new ValidationException("Validation exception type " +
    // (dataValidator.validateHumidity(humidity) ==
    // DataValidator.ValidationResult.VALIDATION_SUCCESS));

}
