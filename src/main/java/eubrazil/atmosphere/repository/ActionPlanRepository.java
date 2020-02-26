package eubrazil.atmosphere.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eubr.atmosphere.tma.entity.qualitymodel.ActionPlan;

@Repository
public interface ActionPlanRepository extends CrudRepository<ActionPlan, Long> {
	
}
