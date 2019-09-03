package eubrazil.atmosphere.service;


import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;

import eubr.atmosphere.tma.entity.plan.Plan;
import eubr.atmosphere.tma.entity.qualitymodel.ConfigurationProfile;
import eubr.atmosphere.tma.entity.qualitymodel.Data;
import eubr.atmosphere.tma.entity.qualitymodel.HistoricalData;
import eubr.atmosphere.tma.entity.qualitymodel.Metric;

/**
 * Trustworthiness services
 * @author JorgeLuiz
 */
public interface TrustworthinessService {

	public List<Data> getLimitedDataListById(Integer probeId, Integer descriptionId, Integer resourceId,
			Pageable numSamples);
	
	public List<Data> getLimitedDataListByIdAndTimestamp(Integer probeId, Integer descriptionId, Integer resourceId, Date timestamp);

	public void save(HistoricalData historicalData);

	public List<ConfigurationProfile> findConfigurationProfileInstance(Integer configurationProfileID);

	public Date getLastTimestampInsertedForMetrics(Set<Metric> metrics);
	
	public Double getInstanceValueById();

	public Plan getPlanIdByMetricAndConfigurationProfile(Integer attributeId);
	
}
