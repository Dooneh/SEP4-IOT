package via.sep4;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import via.sep4.controller.PlantExperimentController;
import via.sep4.model.PlantExperiment;
import via.sep4.repository.PlantExperimentRepository;
import via.sep4.repository.PlantMeasurementsRepository;
import via.sep4.service.ExperimentConfigService;

public class PlantExperimentControllerTest {
    private MockMvc mockMvc;

    @Mock
    private PlantExperimentRepository experimentRepository;

    @Mock
    private PlantMeasurementsRepository measurementsRepository;

    @Mock
    private ExperimentConfigService experimentConfigService;

    @InjectMocks
    private PlantExperimentController controller;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetAllExperiments() throws Exception {
        PlantExperiment experiment = new PlantExperiment();
        experiment.setId(1L);
        experiment.setName("Experiment 1");
        experiment.setDescription("Description");
        experiment.setPlantSpecies("Species");

        List<PlantExperiment> experiments = List.of(experiment);
        when(experimentRepository.findAll()).thenReturn(experiments);

        mockMvc.perform(get("/api/experiments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Experiment 1"));
    }

    @Test
    void testGetExperimentById() throws Exception {
        PlantExperiment experiment = new PlantExperiment();
        experiment.setId(1L);
        experiment.setName("Experiment 1");
        experiment.setDescription("Description");
        experiment.setPlantSpecies("Species");

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

        mockMvc.perform(get("/api/experiments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Experiment 1"));
    }

    @Test
    void testGetExperimentById_NotFound() throws Exception {
        when(experimentRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/experiments/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateExperiment() throws Exception {
        PlantExperiment inputExperiment = new PlantExperiment();
        inputExperiment.setName("Experiment 1");
        inputExperiment.setDescription("Description");
        inputExperiment.setPlantSpecies("Species");

        PlantExperiment savedExperiment = new PlantExperiment();
        savedExperiment.setId(1L);
        savedExperiment.setName("Experiment 1");
        savedExperiment.setDescription("Description");
        savedExperiment.setPlantSpecies("Species");

        when(experimentRepository.save(any(PlantExperiment.class))).thenReturn(savedExperiment);

        mockMvc.perform(post("/api/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputExperiment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Experiment 1"));
    }

    @Test
    void testDeleteExperiment() throws Exception {
        when(experimentRepository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/experiments/1"))
                .andExpect(status().isNoContent());

        verify(experimentRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteExperiment_NotFound() throws Exception {
        when(experimentRepository.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/experiments/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testActivateExperiment() throws Exception {
        when(experimentConfigService.setCurrentExperimentId(1L)).thenReturn(true);

        mockMvc.perform(put("/api/experiments/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.experimentId").value(1));
    }

    @Test
    void testActivateExperiment_NotFound() throws Exception {
        when(experimentConfigService.setCurrentExperimentId(1L)).thenReturn(false);

        mockMvc.perform(put("/api/experiments/1/activate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testGetActiveExperiment() throws Exception {
        PlantExperiment experiment = new PlantExperiment();
        experiment.setId(1L);
        experiment.setName("Active Experiment");
        experiment.setDescription("Active Description");
        experiment.setPlantSpecies("Active Species");

        LocalDate startDate = LocalDate.of(2024, 12, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        experiment.setStartDate(startDate);
        experiment.setEndDate(endDate);

        when(experimentConfigService.getCurrentExperiment()).thenReturn(Optional.of(experiment));

        mockMvc.perform(get("/api/experiments/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experimentId").value(1))
                .andExpect(jsonPath("$.name").value("Active Experiment"))
                .andExpect(jsonPath("$.description").value("Active Description"))
                .andExpect(jsonPath("$.plantSpecies").value("Active Species"))
                .andExpect(jsonPath("$.startDate").isArray())
                .andExpect(jsonPath("$.startDate[0]").value(2024))
                .andExpect(jsonPath("$.startDate[1]").value(12))
                .andExpect(jsonPath("$.startDate[2]").value(1))

                .andExpect(jsonPath("$.endDate").isArray())
                .andExpect(jsonPath("$.endDate[0]").value(2024))
                .andExpect(jsonPath("$.endDate[1]").value(12))
                .andExpect(jsonPath("$.endDate[2]").value(31));
    }

    @Test
    void testGetActiveExperiment_NotFound() throws Exception {
        when(experimentConfigService.getCurrentExperiment()).thenReturn(Optional.empty());
        when(experimentConfigService.getCurrentExperimentId()).thenReturn(1L);

        mockMvc.perform(get("/api/experiments/active"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.experimentId").value(1));
    }
}
