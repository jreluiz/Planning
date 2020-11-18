package eubrazil.atmosphere.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eubr.atmosphere.tma.entity.qualitymodel.ActionPlan;
import eubr.atmosphere.tma.entity.qualitymodel.ActionRule;
import eubr.atmosphere.tma.entity.qualitymodel.ConfigurationProfile;
import eubr.atmosphere.tma.entity.qualitymodel.Plan;
import eubrazil.atmosphere.repository.ActionPlanRepository;
import eubrazil.atmosphere.repository.ActionRuleRepository;
import eubrazil.atmosphere.repository.ConfigurationDataRepository;
import eubrazil.atmosphere.repository.ConfigurationProfileRepository;
import eubrazil.atmosphere.repository.PlanRepository;
import eubrazil.atmosphere.service.TrustworthinessService;

/**
 * Implementation of trustworthiness services
 * @author JorgeLuiz
 */
@Service
public class TrustworthinessServiceImpl implements TrustworthinessService {

	@Autowired
	private ConfigurationProfileRepository configurationProfileRepository;

	@Autowired
	private ActionRuleRepository actionRuleRepository;

	@Autowired
	private PlanRepository planRepository;
	
	@Autowired
	private ActionPlanRepository actionPlanRepository;
	
	@Autowired
	private ConfigurationDataRepository configurationDataRepository;
	
	@Override
	public List<ConfigurationProfile> findConfigurationProfileInstance(Integer configurationProfileID) {
		return configurationProfileRepository.findConfigurationProfileInstance(configurationProfileID);
	}
	
	@Override
	public Optional<ActionRule> findActionRuleById(Integer actionRuleId) {
		return actionRuleRepository.findById(actionRuleId);
	}
	
	@Override
	public Plan savePlan(Plan plan) {
		return planRepository.save(plan);
	}
	
	@Override
	public Optional<Plan> findPlanById(Integer planId) {
		return planRepository.findById(planId);
	}
	
	@Override
	public ActionPlan saveNewActionPlan(ActionPlan actionPlan) {
		return actionPlanRepository.save(actionPlan);
	}

}
