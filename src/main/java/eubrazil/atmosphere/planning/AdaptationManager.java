package eubrazil.atmosphere.planning;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eubr.atmosphere.tma.entity.qualitymodel.ActionPlan;
import eubr.atmosphere.tma.entity.qualitymodel.ActionRule;
import eubr.atmosphere.tma.entity.qualitymodel.Configuration;
import eubr.atmosphere.tma.entity.qualitymodel.ConfigurationData;
import eubr.atmosphere.tma.entity.qualitymodel.Plan;
import eubr.atmosphere.tma.entity.qualitymodel.PlanStatus;
import eubr.atmosphere.tma.utils.ListUtils;
import eubrazil.atmosphere.qualitymodel.SpringContextBridge;
import eubrazil.atmosphere.service.TrustworthinessService;

public class AdaptationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptationManager.class);    
    
	public static void performAdaptation(String actions) {
		LOGGER.info("Adaptation will be performed!");

		TrustworthinessService trustworthinessService = SpringContextBridge.services().getTrustworthinessService();
		
		// get the plan being built
		String planId = ListUtils.getFirstElement(Arrays.asList(actions.split(":")));
		Plan plan = trustworthinessService.findPlanById(Integer.parseInt(planId)).get();

		List<String> actionsIds = Arrays.asList(ListUtils.getLastElement(Arrays.asList(actions.split(":"))).split(","));
		if ( ListUtils.isNotEmpty(actionsIds) ) {
			
			for (int i = 0; i < actionsIds.size(); i++) {
				
				// get action by id
				Integer actionRuleId = Integer.parseInt(actionsIds.get(i));
				ActionRule actionRule = trustworthinessService.findActionRuleById(actionRuleId).get();
				
				// saving ActionPlan
				ActionPlan actionPlan = addActionPlan(plan, actionRule, i);
				trustworthinessService.saveNewActionPlan(actionPlan);

			}
		}
		
	}
	
	private static ActionPlan addActionPlan(Plan plan, ActionRule actionRule, int executionOrder) {
        ActionPlan actionPlan = new ActionPlan(plan.getPlanId(), actionRule.getActionRuleId(), executionOrder, PlanStatus.READY_TO_RUN.ordinal());
        
        // adding configurations data to actionPlan
        for (Configuration config: actionRule.getConfigurations()) {
			actionPlan.addConfigurationData(new ConfigurationData(plan.getPlanId(), config.getId().getActionRuleId(),
					config.getId().getConfigurationId(), config.getValue()));
        }

        plan.addActionPlan(actionPlan);
        
        return actionPlan;
    }

}