package via.sep4;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import via.sep4.controller.PlantMeasurementController;
import via.sep4.model.InvalidMeasurement;
import via.sep4.model.PlantExperiment;
import via.sep4.model.PlantMeasurements;
import via.sep4.processing.DataValidator;
import via.sep4.repository.InvalidMeasurementRepository;
import via.sep4.repository.PlantExperimentRepository;
import via.sep4.repository.PlantMeasurementsRepository;

public class PlantMeasurementControllerTest {
    private MockMvc mockMvc;

    @Mock
    private PlantExperimentRepository experimentRepository;

    @Mock
    private PlantMeasurementsRepository measurementsRepository;

    @Mock
    private InvalidMeasurementRepository invalidMeasurementRepository;

    @Mock
    private DataValidator dataValidator;

    @InjectMocks
    private PlantMeasurementController controller;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetInvalidMeasurements() throws Exception {
        long experimentId = 1L;
        InvalidMeasurement invalidMeasurement = new InvalidMeasurement();
        invalidMeasurement.setId(1L);
        invalidMeasurement.setExperimentId(experimentId);
        invalidMeasurement.setRawData("raw data");
        invalidMeasurement.setValidationError("validation error");
        invalidMeasurement.setReceivedAt(LocalDateTime.now());

        List<InvalidMeasurement> invalidMeasurements = List.of(invalidMeasurement);

        when(experimentRepository.existsById(experimentId)).thenReturn(true);
        when(invalidMeasurementRepository.findByExperimentId(experimentId)).thenReturn(invalidMeasurements);

        mockMvc.perform(get("/api/measurements/{experimentId}/invalid", experimentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].experimentId").value(experimentId))
                .andExpect(jsonPath("$[0].rawData").value("raw data"))
                .andExpect(jsonPath("$[0].validationError").value("validation error"));
    }

    @Test
    void testGetInvalidMeasurementById() throws Exception {
        long measurementId = 1L;
        InvalidMeasurement invalidMeasurement = new InvalidMeasurement();
        invalidMeasurement.setId(measurementId);
        invalidMeasurement.setExperimentId(1L);
        invalidMeasurement.setRawData("raw data");
        invalidMeasurement.setValidationError("validation error");
        invalidMeasurement.setReceivedAt(LocalDateTime.now());

        when(invalidMeasurementRepository.findById(measurementId)).thenReturn(Optional.of(invalidMeasurement));

        mockMvc.perform(get("/api/measurements/invalid/{id}", measurementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(measurementId))
                .andExpect(jsonPath("$.experimentId").value(1))
                .andExpect(jsonPath("$.rawData").value("raw data"))
                .andExpect(jsonPath("$.validationError").value("validation error"));
    }

    @Test
    void testGetInvalidMeasurementById_NotFound() throws Exception {
        when(invalidMeasurementRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/measurements/invalid/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetMeasurementById() throws Exception {
        long measurementId = 1L;
        PlantExperiment experiment = new PlantExperiment();
        experiment.setId(1L);

        PlantMeasurements measurement = new PlantMeasurements();
        measurement.setId(measurementId);
        measurement.setExperiment(experiment);
        measurement.setLuftTemperatur(22.5);
        measurement.setLuftfugtighed(45.0);
        measurement.setJordFugtighed(70.0);
        measurement.setTimestamp(LocalDateTime.now());

        when(measurementsRepository.findById(measurementId)).thenReturn(Optional.of(measurement));

        mockMvc.perform(get("/api/measurements/{id}", measurementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(measurementId))
                .andExpect(jsonPath("$.luftTemperatur").value(22.5))
                .andExpect(jsonPath("$.luftfugtighed").value(45.0))
                .andExpect(jsonPath("$.jordFugtighed").value(70.0));
    }

    @Test
    void testGetMeasurementById_NotFound() throws Exception {
        when(measurementsRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/measurements/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAddMeasurement() throws Exception {
        long experimentId = 1L;
        PlantExperiment experiment = new PlantExperiment();
        experiment.setId(experimentId);

        Map<String, String> sensorData = new HashMap<>();
        sensorData.put("Luft_temperatur", "22.5");
        sensorData.put("Luftfugtighed", "45.0");
        sensorData.put("Jord_fugtighed", "70.0");
        sensorData.put("Lys_højeste_intensitet", "1000");
        sensorData.put("Lys_laveste_intensitet", "500");
        sensorData.put("Lys_indstilling", "AUTO");
        sensorData.put("Lys_gennemsnit", "750");
        sensorData.put("Afstand_til_Højde", "30");
        sensorData.put("Vand_tid_fra_sidste", "360");
        sensorData.put("Vand_mængde", "250");
        sensorData.put("Vand_frekvens", "8");
        sensorData.put("Tidsstempel", LocalDateTime.now().toString());

        PlantMeasurements savedMeasurement = new PlantMeasurements();
        savedMeasurement.setId(1L);
        savedMeasurement.setExperiment(experiment);
        savedMeasurement.setLuftTemperatur(22.5);
        savedMeasurement.setLuftfugtighed(45.0);
        savedMeasurement.setJordFugtighed(70.0);

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
        when(dataValidator.validate(any())).thenReturn(DataValidator.ValidationResult.VALIDATION_SUCCESS);
        when(measurementsRepository.save(any(PlantMeasurements.class))).thenReturn(savedMeasurement);

        mockMvc.perform(post("/api/measurements/{experimentId}", experimentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sensorData)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.luftTemperatur").value(22.5))
                .andExpect(jsonPath("$.luftfugtighed").value(45.0))
                .andExpect(jsonPath("$.jordFugtighed").value(70.0));

        verify(measurementsRepository).save(any(PlantMeasurements.class));
    }

    @Test
    void testAddMeasurement_ExperimentNotFound() throws Exception {
        long experimentId = 1L;
        Map<String, String> sensorData = new HashMap<>();
        sensorData.put("Luft_temperatur", "22.5");
        sensorData.put("Luftfugtighed", "45.0");
        sensorData.put("Jord_fugtighed", "70.0");

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/measurements/{experimentId}", experimentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sensorData)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteInvalidMeasurement() throws Exception {
        when(invalidMeasurementRepository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/measurements/invalid/1"))
                .andExpect(status().isNoContent());

        verify(invalidMeasurementRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteInvalidMeasurement_NotFound() throws Exception {
        when(invalidMeasurementRepository.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/measurements/invalid/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUploadMeasurements() throws Exception {
        long experimentId = 1L;
        PlantExperiment experiment = new PlantExperiment();
        experiment.setId(experimentId);

        String csvContent = "Luft_temperatur,Luftfugtighed,Jord_fugtighed,Lys_højeste_intensitet,Lys_laveste_intensitet\n"
                +
                "22.5,45.0,70.0,1000,500\n" +
                "23.0,46.0,72.0,1100,550";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes());

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
        when(measurementsRepository.save(any(PlantMeasurements.class))).thenAnswer(invocation -> {
            PlantMeasurements m = invocation.getArgument(0);
            m.setId(1L);
            return m;
        });

        mockMvc.perform(multipart("/api/measurements/{experimentId}/upload", experimentId)
                .file(file)
                .param("hasHeaders", "true")
                .param("delimiter", ","))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").isNumber())
                .andExpect(jsonPath("$.successCount").isNumber())
                .andExpect(jsonPath("$.status").value("success"));
    }
}
