package eubrazil.atmosphere.service;


import java.util.List;
import java.util.Optional;

import eubr.atmosphere.tma.entity.qualitymodel.ActionPlan;
import eubr.atmosphere.tma.entity.qualitymodel.ActionRule;
import eubr.atmosphere.tma.entity.qualitymodel.ConfigurationProfile;
import eubr.atmosphere.tma.entity.qualitymodel.Plan;

/**
 * Trustworthiness services
 * @author JorgeLuiz
 */
public interface TrustworthinessService {

	public List<ConfigurationProfile> findConfigurationProfileInstance(Integer configurationProfileID);

	public Optional<ActionRule> findActionRuleById(Integer actionRuleId);

	public Plan savePlan(Plan plan);

	public Optional<Plan> findPlanById(Integer planId);

	public ActionPlan saveNewActionPlan(ActionPlan actionPlan);
	
}
