package eubrazil.atmosphere.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eubr.atmosphere.tma.entity.qualitymodel.Plan;

@Repository
public interface PlanRepository extends CrudRepository<Plan, Integer> {
	
}
