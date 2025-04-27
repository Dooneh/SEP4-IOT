package via.sep4.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import via.sep4.repository.PlantExperimentRepository;
import via.sep4.repository.PlantMeasurementsRepository;
import via.sep4.service.ExperimentConfigService;
import via.sep4.exceptions.ResourceNotFoundException;
import via.sep4.model.PlantExperiment;
import via.sep4.model.PlantMeasurements;
import via.sep4.processing.DataConverter;

@RestController
@RequestMapping("/api/experiments")
public class PlantExperimentController {

    private static final Logger logger = LoggerFactory.getLogger(PlantExperimentController.class);

    @Autowired
    private PlantExperimentRepository experimentRepository;

    @Autowired
    private PlantMeasurementsRepository measurementsRepository;

    @Autowired
    private ExperimentConfigService experimentConfigService;

    @GetMapping
    public ResponseEntity<List<PlantExperiment>> getAllExperiments() {
        List<PlantExperiment> experiments = experimentRepository.findAll();
        return ResponseEntity.ok(experiments);
    }

    @GetMapping("/{experimentId}")
    public ResponseEntity<PlantExperiment> getExperimentById(@PathVariable Long experimentId) {
        Optional<PlantExperiment> experiment = experimentRepository.findById(experimentId);
        return experiment.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/species/{species}")
    public ResponseEntity<List<PlantExperiment>> getExperimentsBySpecies(@PathVariable String species) {
        List<PlantExperiment> experiments = experimentRepository.findByPlantSpecies(species);
        return ResponseEntity.ok(experiments);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<PlantExperiment> getExperimentByName(@PathVariable String name) {
        Optional<PlantExperiment> experiment = experimentRepository.findByName(name);
        return experiment.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{experimentId}/measurements")
    public ResponseEntity<List<PlantMeasurements>> getExperimentMeasurements(
            @PathVariable Long experimentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        if (!experimentRepository.existsById(experimentId)) {
            throw new ResourceNotFoundException("Experiment not found with id: " + experimentId);
        }

        List<PlantMeasurements> measurements;
        if (startDate != null && endDate != null) {
            measurements = measurementsRepository.findByExperimentIdAndTimestampBetween(experimentId, startDate,
                    endDate);
        } else {
            measurements = measurementsRepository.findByExperimentId(experimentId);
        }

        return ResponseEntity.ok(measurements);
    }

    @GetMapping("/{experimentId}/measurements/latest")
    public ResponseEntity<List<PlantMeasurements>> getLatestMeasurements(@PathVariable Long experimentId) {
        if (!experimentRepository.existsById(experimentId)) {
            throw new ResourceNotFoundException("Experiment not found with id: " + experimentId);
        }

        List<PlantMeasurements> measurements = measurementsRepository
                .findTop10ByExperimentIdOrderByTimestampDesc(experimentId);

        return ResponseEntity.ok(measurements);
    }

    @GetMapping("/{experimentId}/export/csv")
    public ResponseEntity<String> exportToCsv(
            @PathVariable Long experimentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        if (!experimentRepository.existsById(experimentId)) {
            throw new ResourceNotFoundException("Experiment not found with id: " + experimentId);
        }

        List<PlantMeasurements> measurements;
        if (startDate != null && endDate != null) {
            measurements = measurementsRepository.findByExperimentIdAndTimestampBetween(experimentId, startDate,
                    endDate);
        } else {
            measurements = measurementsRepository.findByExperimentId(experimentId);
        }

        if (measurements.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        DataConverter converter = new DataConverter(true);

        List<String> headers = List.of(
                "timestamp", "luftTemperatur", "luftfugtighed", "jordFugtighed",
                "lysIndstilling", "lysHøjesteIntensitet", "lysLavesteIntensitet", "lysGennemsnit",
                "afstandTilHøjde", "vandTidFraSidste", "vandMængde", "vandFrekvens");
        converter.getHeaders().addAll(headers);

        for (PlantMeasurements m : measurements) {
            Map<String, String> row = new java.util.HashMap<>();
            row.put("timestamp", m.getTimestamp().toString());
            row.put("luftTemperatur", String.valueOf(m.getLuftTemperatur()));
            row.put("luftfugtighed", String.valueOf(m.getLuftfugtighed()));
            row.put("jordFugtighed", String.valueOf(m.getJordFugtighed()));
            row.put("lysIndstilling", m.getLysIndstilling() != null ? m.getLysIndstilling() : "");
            row.put("lysHøjesteIntensitet", String.valueOf(m.getLysHøjesteIntensitet()));
            row.put("lysLavesteIntensitet", String.valueOf(m.getLysLavesteIntensitet()));
            row.put("lysGennemsnit", String.valueOf(m.getLysGennemsnit()));
            row.put("afstandTilHøjde", String.valueOf(m.getAfstandTilHøjde()));
            row.put("vandTidFraSidste", String.valueOf(m.getVandTidFraSidste()));
            row.put("vandMængde", String.valueOf(m.getVandMængde()));
            row.put("vandFrekvens", String.valueOf(m.getVandFrekvens()));

            converter.getData().add(row);
        }

        try {
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("experiment_" + experimentId, ".csv");
            converter.exportToCsv(tempFile.toString(), ',');
            String csvContent = new String(java.nio.file.Files.readAllBytes(tempFile));
            java.nio.file.Files.delete(tempFile);

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.TEXT_PLAIN);
            httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=experiment_" + experimentId + "_data.csv");

            return new ResponseEntity<>(csvContent, httpHeaders, HttpStatus.OK);
        } catch (java.io.IOException e) {
            logger.error("Error generating CSV file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating CSV: " + e.getMessage());
        }
    }

    @GetMapping("/{experimentId}/export/json")
    public ResponseEntity<String> exportToJson(
            @PathVariable Long experimentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        if (!experimentRepository.existsById(experimentId)) {
            throw new ResourceNotFoundException("Experiment not found with id: " + experimentId);
        }

        List<PlantMeasurements> measurements;
        if (startDate != null && endDate != null) {
            measurements = measurementsRepository.findByExperimentIdAndTimestampBetween(experimentId, startDate,
                    endDate);
        } else {
            measurements = measurementsRepository.findByExperimentId(experimentId);
        }

        if (measurements.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        DataConverter converter = new DataConverter(true);

        List<String> headers = List.of(
                "timestamp", "luftTemperatur", "luftfugtighed", "jordFugtighed",
                "lysIndstilling", "lysHøjesteIntensitet", "lysLavesteIntensitet", "lysGennemsnit",
                "afstandTilHøjde", "vandTidFraSidste", "vandMængde", "vandFrekvens");
        converter.getHeaders().addAll(headers);

        for (PlantMeasurements m : measurements) {
            Map<String, String> row = new java.util.HashMap<>();
            row.put("timestamp", m.getTimestamp().toString());
            row.put("luftTemperatur", String.valueOf(m.getLuftTemperatur()));
            row.put("luftfugtighed", String.valueOf(m.getLuftfugtighed()));
            row.put("jordFugtighed", String.valueOf(m.getJordFugtighed()));
            row.put("lysIndstilling", m.getLysIndstilling());
            row.put("lysHøjesteIntensitet", String.valueOf(m.getLysHøjesteIntensitet()));
            row.put("lysLavesteIntensitet", String.valueOf(m.getLysLavesteIntensitet()));
            row.put("lysGennemsnit", String.valueOf(m.getLysGennemsnit()));
            row.put("afstandTilHøjde", String.valueOf(m.getAfstandTilHøjde()));
            row.put("vandTidFraSidste", String.valueOf(m.getVandTidFraSidste()));
            row.put("vandMængde", String.valueOf(m.getVandMængde()));
            row.put("vandFrekvens", String.valueOf(m.getVandFrekvens()));

            converter.getData().add(row);
        }

        try {
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("experiment_" + experimentId, ".json");
            converter.exportToJson(tempFile.toString());
            String jsonContent = new String(java.nio.file.Files.readAllBytes(tempFile));
            java.nio.file.Files.delete(tempFile);

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=experiment_" + experimentId + "_data.json");

            return new ResponseEntity<>(jsonContent, httpHeaders, HttpStatus.OK);
        } catch (java.io.IOException e) {
            logger.error("Error generating JSON file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating JSON: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<PlantExperiment> createExperiment(@RequestBody PlantExperiment experiment) {
        PlantExperiment savedExperiment = experimentRepository.save(experiment);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedExperiment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantExperiment> updateExperiment(@PathVariable Long id,
            @RequestBody PlantExperiment experiment) {
        if (!experimentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        experiment.setId(id);
        PlantExperiment updatedExperiment = experimentRepository.save(experiment);
        return ResponseEntity.ok(updatedExperiment);
    }

    @PutMapping("/{experimentId}/activate")
    public ResponseEntity<?> activateExperiment(@PathVariable Long experimentId) {
        if (experimentConfigService.setCurrentExperimentId(experimentId)) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Experiment " + experimentId + " activated for sensor data collection",
                    "experimentId", experimentId));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "message", "Experiment with ID " + experimentId + " not found"));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveExperiment() {
        Optional<PlantExperiment> experiment = experimentConfigService.getCurrentExperiment();

        if (experiment.isPresent()) {
            PlantExperiment activeExperiment = experiment.get();
            return ResponseEntity.ok(Map.of(
                    "experimentId", activeExperiment.getId(),
                    "name", activeExperiment.getName(),
                    "description", activeExperiment.getDescription(),
                    "plantSpecies", activeExperiment.getPlantSpecies(),
                    "startDate", activeExperiment.getStartDate(),
                    "endDate", activeExperiment.getEndDate()));
        } else {
            Long currentId = experimentConfigService.getCurrentExperimentId();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message", "Active experiment with ID " + currentId + " not found",
                            "experimentId", currentId));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExperiment(@PathVariable Long id) {
        if (!experimentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        experimentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
