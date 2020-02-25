package eubrazil.atmosphere.job;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
		
		//get root attribute
		Attribute trustworthinessAttribute = TreeUtils.getInstance().getRootAttribute(configurationActor);
		//build dynamic attribute rules
		trustworthinessAttribute.buildAttributeRules();
		//compile and run attribute rules
		executeAttributeRules(trustworthinessAttribute);

		LOGGER.info("PlanningPollJob - end of execution..");
	}
	
	private void executeAttributeRules(Attribute attr) {
		
		if (attr != null && ListUtils.isNotEmpty(attr.getRules())) {
	        //Create a session to operate Drools in memory
			DroolsUtility utility = new DroolsUtility();
			try {
				StatelessKieSession session = utility.loadSession(attr.getRules(), PropertiesManager.getInstance().getProperty("template_rules"));
				session.setGlobal("attribute", attr);
				session.execute(attr);
			} catch (Exception e) {
				String msg = "PlanningPollJob - Error compiling and executing the attribute rules: " + attr.getName();
				LOGGER.info(msg);
				e.printStackTrace();
			}
		}
		
		//execute children rules
		if (attr instanceof CompositeAttribute) {
			List<Attribute> children = ((CompositeAttribute) attr).getChildren();
			Comparator<Attribute> compareByWeight = (Attribute a1, Attribute a2) -> a1.getActivePreference().getWeight().compareTo(a2.getActivePreference().getWeight());
			Collections.sort(children, Collections.reverseOrder(compareByWeight)); // executa primeiro o atributo com maior peso (weight de Preference)
			for (Attribute attributeChild : children) {
				if ( !attributeChild.equals(attr) ) {
					executeAttributeRules(attributeChild);
				}
			}
		}
		
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
