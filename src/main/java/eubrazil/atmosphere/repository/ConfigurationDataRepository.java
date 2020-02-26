package eubrazil.atmosphere.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eubr.atmosphere.tma.entity.qualitymodel.ConfigurationData;

@Repository
public interface ConfigurationDataRepository extends CrudRepository<ConfigurationData, Integer> {
	
}
