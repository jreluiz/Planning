package eubrazil.atmosphere.job;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import eubr.atmosphere.tma.entity.qualitymodel.Attribute;
import eubr.atmosphere.tma.entity.qualitymodel.CompositeAttribute;
import eubr.atmosphere.tma.entity.qualitymodel.ConfigurationProfile;
import eubr.atmosphere.tma.entity.qualitymodel.Preference;
import eubr.atmosphere.tma.entity.qualitymodel.PrivacyObject;
import eubr.atmosphere.tma.entity.qualitymodel.Rule;
import eubr.atmosphere.tma.utils.ListUtils;
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

	private static final Integer TRUSTWORTHINESS_CONFIGURATION_PROFILE_ID = 1;
	
	@Value("${trigger.job.time}")
	private String triggerJobTime;
	
	private static Date lastTimestampRead = null;

	@Override
	public void execute(JobExecutionContext jobExecutionContext) {
		LOGGER.info("PlanningPollJob - execution..");

		TrustworthinessService trustworthinessService = SpringContextBridge.services().getTrustworthinessService();
		List<ConfigurationProfile> configProfileList = trustworthinessService.findConfigurationProfileInstance(TRUSTWORTHINESS_CONFIGURATION_PROFILE_ID);

		if (ListUtils.isEmpty(configProfileList)) {
			LOGGER.error("Quality Model is not defined in the database.");
			return;
		}

		ConfigurationProfile configurationActor =  ListUtils.getFirstElement(configProfileList);
		LOGGER.info("TrustworthinessQualityModel (TrustworthinessPollJob) - ConfigurationProfile: " + configurationActor);
		
		CompositeAttribute trustworthiness = getRootAttribute(configurationActor);
		
		// building drools rules dynamically
		trustworthiness.initRules();
		Map<CompositeAttribute, Set<Rule>> compositeRules = trustworthiness.buildRules(PrivacyObject.class.getName());
		
		Iterator<?> it = compositeRules.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry) it.next();
	        LOGGER.info(pair.getKey() + " = " + pair.getValue());
	        
	        Set<Rule> rules = (Set<Rule>) pair.getValue();
	        
	        //AdaptationManager.performAdaptation(@{actions});
	        
//	        for (Rule rule : rules) {
//	        	System.out.println(rule.getConditions());
//				System.out.println(rule.getActions());
//			}
	        
	        //Create a session to operate Drools in memory
			DroolsUtility utility = new DroolsUtility();
			try {
				StatelessKieSession session = utility.loadSession(rules,
						"eubrazil/atmosphere/templates/Trustworthiness.drl");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    
	        
	    }
		
		LOGGER.info("PlanningPollJob - end of execution..");
	}
	
	private CompositeAttribute getRootAttribute(ConfigurationProfile configurationActor) {
		for (Preference preference : configurationActor.getPreferences()) {
			Attribute attr = preference.getAttribute();
			if ( attr.getAttributeType().isRoot() ) {
				return (CompositeAttribute) preference.getAttribute();
			}
		}
		return null;
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
