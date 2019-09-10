package eubrazil.atmosphere.planning;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eubr.atmosphere.tma.entity.qualitymodel.ActionRule;

public class AdaptationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptationManager.class);    
    
	public static void performAdaptation(Set<ActionRule> action) {
		LOGGER.info("Adaptation will be performed!");

		for (ActionRule actionRule : action) {
			System.out.println(actionRule.getActionName());
		}
		
	}

//    private static void addActionPlan(Plan plan, Action action) {
//        // TODO: when we change to more than one action, the execution order needs to be specified
//        int executionOrder = 1;
//        ActionPlan actionPlan = new ActionPlan(plan.getPlanId(), action.getActionId(), executionOrder);
//
//        for (Configuration config: action.getConfigurationList()) {
//            actionPlan.addConfiguration(new ConfigurationData(config.getConfigurationId(), config.getValue()));
//        }
//
//        plan.addAction(actionPlan);
//    }
//
//    private static Plan createPlan() {
//        Plan plan = new Plan();
//        plan.setValueTime(Instant.now().getEpochSecond());
//
//        plan.setMetricId(1);
//        plan.setQualityModelId(1);
//        plan.setStatus(PlanStatus.TO_DO);
//
//        int planId = planManager.saveNewPlan(plan);
//        plan.setPlanId(planId);
//        return plan;
//    }
}