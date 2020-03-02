package eubrazil.atmosphere.job;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
import eubr.atmosphere.tma.entity.qualitymodel.Plan;
import eubr.atmosphere.tma.entity.qualitymodel.Status;
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

		Integer trustworthinessConfigProfileID = Integer.parseInt(PropertiesManager.getInstance().getProperty("trustworthiness_configuration_profile_id"));
		TrustworthinessService trustworthinessService = SpringContextBridge.services().getTrustworthinessService();
		List<ConfigurationProfile> configProfileList = trustworthinessService.findConfigurationProfileInstance(trustworthinessConfigProfileID);

		if (ListUtils.isEmpty(configProfileList)) {
			LOGGER.error("Quality Model is not defined in the database.");
			return;
		}

		ConfigurationProfile configurationActor =  ListUtils.getFirstElement(configProfileList);
		LOGGER.info("(PlanningPollJob) TrustworthinessQualityModel - ConfigurationProfile: " + configurationActor);
		
		//get root attribute
		Attribute trustworthinessAttribute = TreeUtils.getInstance().getRootAttribute(configurationActor);
		//build dynamic attribute rules
		trustworthinessAttribute.buildAttributeRules();
		//compile and run attribute rules
		executeAttributeRules(trustworthinessAttribute, null);

		LOGGER.info("PlanningPollJob - end of execution..");
	}
	
	private void executeAttributeRules(Attribute attr, Plan plan) {
		
		if (plan == null) {
			plan = createPlan();
		}
		
		if (attr != null && ListUtils.isNotEmpty(attr.getRules())) {
			
			//configure plan
			attr.setPlan(plan);
			
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
		
		// TODO Verify that all rules (of attributes) have been executed (RULE ATTRIBUTE STATUS), and change the PLAN STATUS
		
		//execute children rules
		if (attr instanceof CompositeAttribute) {
			List<Attribute> children = ((CompositeAttribute) attr).getChildren();
			Comparator<Attribute> compareByWeight = (Attribute a1, Attribute a2) -> a1.getActivePreference().getWeight().compareTo(a2.getActivePreference().getWeight());
			Collections.sort(children, Collections.reverseOrder(compareByWeight)); // executa primeiro o atributo com maior peso (weight de Preference)
			for (Attribute attributeChild : children) {
				if ( !attributeChild.equals(attr) ) {
					executeAttributeRules(attributeChild, plan);
				}
			}
		}
		
	}
	
	private Plan createPlan() {
        Plan plan = new Plan();
        plan.setValueTime(new Timestamp(new Date().getTime()));
        plan.setStatus(Status.BUILDING.ordinal());

        TrustworthinessService trustworthinessService = SpringContextBridge.services().getTrustworthinessService();
        Plan createdPlan = trustworthinessService.saveNewPlan(plan);
        plan.setPlanId(createdPlan.getPlanId());
        
        return plan;
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
