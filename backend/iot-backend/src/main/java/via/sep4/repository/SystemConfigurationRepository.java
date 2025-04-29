package via.sep4.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import via.sep4.model.SystemConfiguration;

@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, String> {

}
