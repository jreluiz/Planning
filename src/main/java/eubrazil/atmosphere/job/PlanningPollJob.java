package eubrazil.atmosphere.job;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.kie.api.runtime.StatelessKieSession;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import eubr.atmosphere.tma.entity.qualitymodel.Attribute;
import eubr.atmosphere.tma.entity.qualitymodel.CompositeAttribute;
import eubr.atmosphere.tma.entity.qualitymodel.ConfigurationProfile;
import eubr.atmosphere.tma.entity.qualitymodel.Plan;
import eubr.atmosphere.tma.entity.qualitymodel.Status;
import eubr.atmosphere.tma.utils.ListUtils;
import eubr.atmosphere.tma.utils.MessagePlanning;
import eubr.atmosphere.tma.utils.TreeUtils;
import eubrazil.atmosphere.config.appconfig.PropertiesManager;
import eubrazil.atmosphere.config.quartz.SchedulerConfig;
import eubrazil.atmosphere.kafka.ConsumerCreator;
import eubrazil.atmosphere.kafka.kafkaManager;
import eubrazil.atmosphere.qualitymodel.SpringContextBridge;
import eubrazil.atmosphere.service.TrustworthinessService;
import eubrazil.atmosphere.util.drools.DroolsUtility;

/**
 * Planning Poll Job
 * 
 * Define regras bases (do tipo score < threshold) para cada atributo.
 * So habilita regras de atributos filhos caso a regra base do atributo pai for verdadeira
	
 * Todo atributo possui uma regra base que será score < threshold e outras regras que estendem dessa regra. 
 * Por exemplo: para privacidade temos a regra base (no atributo PRIVACY) score < th e outras regras: score = score anterior 
 * (com ações específicas = multiplicar k por 2) e score > score anterior (com ações específicas = incrementar k de 1) -> essas outras duas 
 * regras estendem da regra base (score < th), ou seja, elas só serão ativadas se a regra base for verdadeira.

 * Supondo que teremos condições/ações nos atributos INFORMATION_LOSS e REIDENTIFICATION_RISK (filhos de PRIVACY): então tais condições/ações 
 * serão ativadas somente se a regra base do atributo pai for ativado, ou seja: score < th para o atributo PRIVACY.
 * 
 * @author JorgeLuiz
 */
@Component
@DisallowConcurrentExecution
@PropertySource(ignoreResourceNotFound = true, value = "classpath:config.properties")
public class PlanningPollJob implements Job {

	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) {
		
		LOGGER.info("---Starting planning execution--------------------------");
		
		Consumer<Long, String> consumer = ConsumerCreator.createConsumer();
        int noMessageFound = 0;
        int maxNoMessageFoundCount = Integer.parseInt(
                PropertiesManager.getInstance().getProperty("maxNoMessageFoundCount"));
		
		try {
            while (true) {
            	ConsumerRecords<Long, String> consumerRecords = consumer.poll(1000);

                // 1000 is the time in milliseconds consumer will wait if no record is found at broker.
                if (consumerRecords.count() == 0) {
                    noMessageFound++;

                    if (noMessageFound > maxNoMessageFoundCount) {
                      // If no message found count is reached to threshold exit loop.
                        sleep(2000);
                    } else {
                        continue;
                    }
                }

                // Manipulate the records
                consumerRecords.forEach(record -> {
                	processRecord(record);
                 });

                // commits the offset of record to broker.
                consumer.commitAsync();
                sleep(5000);

            }
		} finally {
            consumer.close();
        }
	}

	private void processRecord(ConsumerRecord<Long, String> record) {
        String messageString = record.value();
        MessagePlanning messagePlanning = new Gson().fromJson(messageString, MessagePlanning.class);
        LOGGER.info(record.toString());
        LOGGER.info("ConfigurationProfileId: {} / Offset: {}", messagePlanning.getConfigurationProfileId(), record.offset());

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
		//compile and run attribute rules building the adaptation plan
		Integer planId = executeAttributeRulesBuildingAdaptationPlan(trustworthinessAttribute, null);
		
		// perform adaptation
		performAdaptation(planId);
    }
	
	private Integer executeAttributeRulesBuildingAdaptationPlan(Attribute attr, Plan plan) {
		
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
		
		// Verify that all rules (of attributes) have been executed (RULE ATTRIBUTE STATUS), and change the PLAN STATUS
		//execute children rules
		if (attr instanceof CompositeAttribute) {
			List<Attribute> children = ((CompositeAttribute) attr).getChildren();
			Comparator<Attribute> compareByWeight = (Attribute a1, Attribute a2) -> a1.getActivePreference().getWeight().compareTo(a2.getActivePreference().getWeight());
			Collections.sort(children, Collections.reverseOrder(compareByWeight)); // executes the attribute with the highest weight first (weight of Preference)
			for (Attribute attributeChild : children) {
				if ( !attributeChild.equals(attr) ) {
					executeAttributeRulesBuildingAdaptationPlan(attributeChild, plan);
				}
			}
		}
		
		return plan.getPlanId();
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
	
	private void performAdaptation(Integer planId) {
        LOGGER.info("Adaptation will be performed!");
        kafkaManager kafkaManager = new kafkaManager();
        try {
            kafkaManager.addItemKafka(planId.toString());
        } catch (InterruptedException e) {
            LOGGER.warn(e.getMessage(), e);
        } catch (ExecutionException e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }
	
	@Bean(name = "jobBean1")
	public JobDetailFactoryBean job() {
		return SchedulerConfig.createJobDetail(this.getClass());
	}

	@Bean(name = "jobBean1Trigger")
	public CronTriggerFactoryBean jobTrigger(@Qualifier("jobBean1") JobDetail jobDetail) {
		LocalDateTime now = LocalDateTime.now();
		int second = now.getSecond();
		int minute = now.getMinute() + 1;
		int hour = now.getHour();
		int dayOfMonth = now.getDayOfMonth();
		int month = now.getMonthValue();
		int year = now.getYear();
		StringBuilder sb = new StringBuilder();
		sb.append(second).append(" ");
		sb.append(minute).append(" ");
		sb.append(hour).append(" ");
		sb.append(dayOfMonth).append(" ");
		sb.append(month).append(" ");
		sb.append("?").append(" ");
		sb.append(year);
		
		return SchedulerConfig.createCronTrigger(jobDetail, sb.toString());
	}
	
	private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}