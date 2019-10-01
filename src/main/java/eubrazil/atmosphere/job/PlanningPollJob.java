package eubrazil.atmosphere.job;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.kie.api.runtime.StatelessKieSession;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;

import eubr.atmosphere.tma.entity.qualitymodel.CompositeAttribute;
import eubr.atmosphere.tma.entity.qualitymodel.ConfigurationProfile;
import eubr.atmosphere.tma.entity.qualitymodel.HistoricalData;
import eubr.atmosphere.tma.entity.qualitymodel.PrivacyObject;
import eubr.atmosphere.tma.entity.qualitymodel.Rule;
import eubr.atmosphere.tma.entity.qualitymodel.TrustworthinessObject;
import eubr.atmosphere.tma.utils.ListUtils;
import eubr.atmosphere.tma.utils.TreeUtils;
import eubrazil.atmosphere.config.appconfig.PropertiesManager;
import eubrazil.atmosphere.config.quartz.SchedulerConfig;
import eubrazil.atmosphere.qualitymodel.SpringContextBridge;
import eubrazil.atmosphere.service.TrustworthinessService;
import eubrazil.atmosphere.util.drools.DroolsUtility;

/**
 * Planning Poll Job
 * @author JorgeLuiz
 */
@Component
@DisallowConcurrentExecution
@PropertySource(ignoreResourceNotFound = true, value = "classpath:config.properties")
public class PlanningPollJob implements Job {

	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Value("${trigger.job.time}")
	private String triggerJobTime;

	@Override
	public void execute(JobExecutionContext jobExecutionContext) {
		LOGGER.info("PlanningPollJob - execution..");

		TrustworthinessService trustworthinessService = SpringContextBridge.services().getTrustworthinessService();
		Integer trustworthinessConfigProfileID = Integer.parseInt(PropertiesManager.getInstance().getProperty("trustworthiness_configuration_profile_id"));
		List<ConfigurationProfile> configProfileList = trustworthinessService.findConfigurationProfileInstance(trustworthinessConfigProfileID);

		if (ListUtils.isEmpty(configProfileList)) {
			LOGGER.error("Quality Model is not defined in the database.");
			return;
		}

		ConfigurationProfile configurationActor =  ListUtils.getFirstElement(configProfileList);
		LOGGER.info("TrustworthinessQualityModel (TrustworthinessPollJob) - ConfigurationProfile: " + configurationActor);
		
		CompositeAttribute trustworthiness = TreeUtils.getInstance().getRootAttribute(configurationActor);
		
		// building drools rules dynamically
		trustworthiness.initRules();
		Map<CompositeAttribute, List<Rule>> compositeRules = trustworthiness.buildRules(PrivacyObject.class.getName());
		
		Iterator<?> it = compositeRules.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry) it.next();
	        LOGGER.info(pair.getKey() + " = " + pair.getValue());
	        
	        List<Rule> rules = (List<Rule>) pair.getValue();
	        
	        //Create a session to operate Drools in memory
			DroolsUtility utility = new DroolsUtility();
			try {
				StatelessKieSession session = utility.loadSession(rules,
						PropertiesManager.getInstance().getProperty("template_rules"));
				
				CompositeAttribute ca = (CompositeAttribute) pair.getKey();
				List<HistoricalData> historicalDataList = ca.getPreference().getAttribute().getHistoricaldata();
				
				// sort historical data list by instant
				Comparator<HistoricalData> compareByInstant = (HistoricalData h1, HistoricalData h2) -> h1.getId()
						.getInstant().compareTo(h2.getId().getInstant());
				Collections.sort(historicalDataList, compareByInstant);
				
				// get last historical data element
				HistoricalData lastHistoricalData = ListUtils.getLastElement(historicalDataList);
				// get second last historical data element
				HistoricalData secondLastHistoricalData = null;
				try { 
					secondLastHistoricalData = historicalDataList.get(ListUtils.size(historicalDataList) - 2);
				} catch (IndexOutOfBoundsException e) {
					LOGGER.info("Historical data has not second last element.");
				}
				
				TrustworthinessObject to = new PrivacyObject();
				to.setScore(lastHistoricalData.getValue());
				to.setThreshold(ca.getPreference().getThreshold());
				if (secondLastHistoricalData != null) {
					((PrivacyObject) to).setPreviousScore(secondLastHistoricalData.getValue());
				}
				
				session.setGlobal("trustworthinessObject", to);
				session.execute(to);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    
	        
	    }
		
		LOGGER.info("PlanningPollJob - end of execution..");
	}
	
	@Bean(name = "jobBean1")
	public JobDetailFactoryBean job() {
		return SchedulerConfig.createJobDetail(this.getClass());
	}

	@Bean(name = "jobBean1Trigger")
	public CronTriggerFactoryBean jobTrigger(@Qualifier("jobBean1") JobDetail jobDetail) {
		return SchedulerConfig.createCronTrigger(jobDetail, triggerJobTime + " * * * * ?");
	}

}
