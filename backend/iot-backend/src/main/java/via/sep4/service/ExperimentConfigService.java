package via.sep4.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import via.sep4.model.PlantExperiment;
import via.sep4.model.SystemConfiguration;
import via.sep4.repository.PlantExperimentRepository;
import via.sep4.repository.SystemConfigurationRepository;

@Service
public class ExperimentConfigService {
    private static final Logger logger = LoggerFactory.getLogger(ExperimentConfigService.class);
    private static final String CURRENT_EXPERIMENT_KEY = "current_experiment_id";
    private static final Long DEFAULT_EXPERIMENT_ID = 1L;

    @Autowired
    private PlantExperimentRepository experimentRepository;

    @Autowired
    private SystemConfigurationRepository configRepository;

    public synchronized Long getCurrentExperimentId() {
        return configRepository.findById(CURRENT_EXPERIMENT_KEY)
                .map(config -> {
                    try {
                        return Long.parseLong(config.getConfigValue());
                    } catch (NumberFormatException e) {
                        logger.error("Invalid experiment ID in configuration: {}", config.getConfigValue());
                        return DEFAULT_EXPERIMENT_ID;
                    }
                })
                .orElse(DEFAULT_EXPERIMENT_ID);
    }

    public synchronized boolean setCurrentExperimentId(Long experimentId) {
        Optional<PlantExperiment> experiment = experimentRepository.findById(experimentId);
        if (experiment.isPresent()) {
            SystemConfiguration config = configRepository.findById(CURRENT_EXPERIMENT_KEY)
                    .orElse(new SystemConfiguration());

            config.setConfigKey(CURRENT_EXPERIMENT_KEY);
            config.setConfigValue(experimentId.toString());
            configRepository.save(config);

            logger.info("Current experiment set to ID: {}, Name: {}",
                    experimentId, experiment.get().getName());
            return true;
        } else {
            logger.warn("Attempted to set current experiment to non-existent ID: {}", experimentId);
            return false;
        }
    }

    public Optional<PlantExperiment> getCurrentExperiment() {
        Long currentId = getCurrentExperimentId();
        Optional<PlantExperiment> experiment = experimentRepository.findById(currentId);
        return experiment;
    }
}
