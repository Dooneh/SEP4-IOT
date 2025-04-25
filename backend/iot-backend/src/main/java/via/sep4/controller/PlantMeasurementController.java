package via.sep4.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import via.sep4.exceptions.ResourceNotFoundException;
import via.sep4.exceptions.ValidationException;
import via.sep4.model.InvalidMeasurement;
import via.sep4.model.PlantExperiment;
import via.sep4.model.PlantMeasurements;
import via.sep4.processing.DataConverter;
import via.sep4.processing.DataValidator;
import via.sep4.repository.InvalidMeasurementRepository;
import via.sep4.repository.PlantExperimentRepository;
import via.sep4.repository.PlantMeasurementsRepository;

@RestController
@RequestMapping("/api/measurements")
public class PlantMeasurementController {

    private static final Logger logger = LoggerFactory.getLogger(PlantMeasurementController.class);

    @Autowired
    private PlantExperimentRepository experimentRepository;

    @Autowired
    private PlantMeasurementsRepository measurementsRepository;

    @Autowired
    private InvalidMeasurementRepository invalidMeasurementRepository;

    @Autowired
    private DataValidator dataValidator;

    @GetMapping("/{experimentId}/invalid")
    public ResponseEntity<List<InvalidMeasurement>> getInvalidMeasurements(@PathVariable Long experimentId) {
        if (!experimentRepository.existsById(experimentId)) {
            throw new ResourceNotFoundException("Experiment not found with id: " + experimentId);
        }

        List<InvalidMeasurement> invalidMeasurements = invalidMeasurementRepository.findByExperimentId(experimentId);
        return ResponseEntity.ok(invalidMeasurements);
    }

    @GetMapping("/invalid/{id}")
    public ResponseEntity<InvalidMeasurement> getInvalidMeasurementById(@PathVariable Long id) {
        Optional<InvalidMeasurement> invalidMeasurement = invalidMeasurementRepository.findById(id);
        return invalidMeasurement.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{measurementId}")
    public ResponseEntity<PlantMeasurements> getMeasurementById(@PathVariable Long measurementId) {
        PlantMeasurements measurement = measurementsRepository.findById(measurementId)
                .orElseThrow(() -> new ResourceNotFoundException("Measurement not found with id: " + measurementId));

        return ResponseEntity.ok(measurement);
    }

    @PostMapping("/{experimentId}")
    public ResponseEntity<?> addMeasurement(
            @PathVariable Long experimentId,
            @RequestBody Map<String, String> sensorData) {
        try {
            logger.info("Received measurement data for experiment ID: {}", experimentId);

            PlantExperiment experiment = experimentRepository.findById(experimentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Experiment not found with id: " + experimentId));

            try {
                validateSensorData(sensorData);

                PlantMeasurements measurement = createMeasurement(experiment, sensorData);
                PlantMeasurements savedMeasurement = measurementsRepository.save(measurement);

                logger.info("Successfully saved measurement for experiment ID: {}", experimentId);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedMeasurement);
            } catch (ValidationException e) {
                InvalidMeasurement invalidMeasurement = new InvalidMeasurement();
                invalidMeasurement.setExperimentId(experimentId);
                invalidMeasurement.setRawData(sensorData.toString());
                invalidMeasurement.setValidationError(e.getMessage());
                invalidMeasurement.setReceivedAt(LocalDateTime.now());

                invalidMeasurementRepository.save(invalidMeasurement);

                logger.warn("Stored invalid measurement with error: {}", e.getMessage());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", e.getMessage(), "status", "Invalid data stored for analysis"));
            }
        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing measurement data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error processing measurement data: " + e.getMessage()));
        }
    }

    @PostMapping("/{experimentId}/upload")
    public ResponseEntity<?> uploadMeasurements(
            @PathVariable Long experimentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "hasHeaders", defaultValue = "true") boolean hasHeaders,
            @RequestParam(value = "delimiter", defaultValue = ",") char delimiter) {

        try {
            PlantExperiment experiment = experimentRepository.findById(experimentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Experiment not found with id: " + experimentId));

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            DataConverter converter = new DataConverter(hasHeaders);

            if (!converter.parseInput(content, delimiter)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to parse CSV data"));
            }

            try {
                dataValidator.validateWithException(converter);
            } catch (ValidationException e) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }

            int totalRows = converter.getRows();
            int successCount = 0;
            List<String> errors = new java.util.ArrayList<>();

            for (int i = 0; i < totalRows; i++) {
                try {
                    Map<String, String> rowData = converter.getData().get(i);
                    PlantMeasurements measurement = createMeasurement(experiment, rowData);
                    measurementsRepository.save(measurement);
                    successCount++;
                } catch (Exception e) {
                    String error = String.format("Row %d: %s", i + 1, e.getMessage());
                    errors.add(error);
                    logger.error(error);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("totalRows", totalRows);
            response.put("successCount", successCount);
            response.put("status", successCount == totalRows ? "success" : "partial");

            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            logger.error("Error reading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error reading file: " + e.getMessage()));
        }
    }

    @DeleteMapping("/invalid/{id}")
    public ResponseEntity<Void> deleteInvalidMeasurement(@PathVariable Long id) {
        if (!invalidMeasurementRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        invalidMeasurementRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void validateSensorData(Map<String, String> sensorData) {
        StringBuilder csvBuilder = new StringBuilder();

        csvBuilder.append("Luft_temperatur,Luftfugtighed,Jord_fugtighed,");
        csvBuilder.append("Lys_højeste_intensitet,Lys_laveste_intensitet,Lys_indstilling,");
        csvBuilder.append("Lys_gennemsnit,Afstand_til_Højde,Vand_tid_fra_sidste,");
        csvBuilder.append("Vand_mængde,Vand_frekvens,Tidsstempel\n");

        csvBuilder.append(sensorData.getOrDefault("Luft_temperatur", "")).append(",");
        csvBuilder.append(sensorData.getOrDefault("Luftfugtighed", "")).append(",");
        csvBuilder.append(sensorData.getOrDefault("Jord_fugtighed", "")).append(",");
        csvBuilder.append(sensorData.getOrDefault("Lys_højeste_intensitet", "")).append(",");
        csvBuilder.append(sensorData.getOrDefault("Lys_laveste_intensitet", "")).append(",");
        csvBuilder.append(sensorData.getOrDefault("Lys_indstilling", "")).append(",");
        csvBuilder.append(sensorData.getOrDefault("Lys_gennemsnit", "")).append(",");
        csvBuilder.append(sensorData.getOrDefault("Afstand_til_Højde", "")).append(",");
        csvBuilder.append(sensorData.getOrDefault("Vand_tid_fra_sidste", "")).append(",");
        csvBuilder.append(sensorData.getOrDefault("Vand_mængde", "")).append(",");
        csvBuilder.append(sensorData.getOrDefault("Vand_frekvens", "")).append(",");
        csvBuilder.append(sensorData.getOrDefault("Tidsstempel", ""));

        DataConverter converter = new DataConverter(true);
        boolean parseSuccess = converter.parseInput(csvBuilder.toString(), ',');

        logger.info("DataConverter parse success: {}, Rows: {}, Columns: {}",
                parseSuccess, converter.getRows(), converter.getCols());

        if (!parseSuccess) {
            throw new ValidationException("Failed to parse sensor data");
        }

        DataValidator.ValidationResult result = dataValidator.validate(converter);
        if (result != DataValidator.ValidationResult.VALIDATION_SUCCESS) {
            throw new ValidationException(dataValidator.getErrorMessage(result));
        }
    }

    private PlantMeasurements createMeasurement(PlantExperiment experiment, Map<String, String> data) {
        PlantMeasurements measurement = new PlantMeasurements();
        measurement.setExperiment(experiment);

        measurement.setLuftTemperatur(parseDouble(data.get("Luft_temperatur")));
        measurement.setLuftfugtighed(parseDouble(data.get("Luftfugtighed")));
        measurement.setJordFugtighed(parseDouble(data.get("Jord_fugtighed")));

        if (data.containsKey("Lys_indstilling")) {
            measurement.setLysIndstilling(data.get("Lys_indstilling"));
        }

        if (data.containsKey("Lys_højeste_intensitet")) {
            measurement.setLysHøjesteIntensitet(parseDouble(data.get("Lys_højeste_intensitet")));
        }

        if (data.containsKey("Lys_laveste_intensitet")) {
            measurement.setLysLavesteIntensitet(parseDouble(data.get("Lys_laveste_intensitet")));
        }

        if (data.containsKey("Lys_gennemsnit")) {
            measurement.setLysGennemsnit(parseDouble(data.get("Lys_gennemsnit")));
        }

        if (data.containsKey("Afstand_til_Højde")) {
            measurement.setAfstandTilHøjde(parseDouble(data.get("Afstand_til_Højde")));
        }

        if (data.containsKey("Vand_tid_fra_sidste")) {
            measurement.setVandTidFraSidste(parseDouble(data.get("Vand_tid_fra_sidste")));
        }

        if (data.containsKey("Vand_mængde")) {
            measurement.setVandMængde(parseDouble(data.get("Vand_mængde")));
        }

        if (data.containsKey("Vand_frekvens")) {
            measurement.setVandFrekvens(parseDouble(data.get("Vand_frekvens")));
        }

        if (data.containsKey("Tidsstempel")) {
            try {
                measurement.setTimestamp(LocalDateTime.parse(data.get("Tidsstempel")));
            } catch (Exception e) {
                measurement.setTimestamp(LocalDateTime.now());
            }
        } else {
            measurement.setTimestamp(LocalDateTime.now());
        }

        return measurement;
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) {
            return 0.0;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
