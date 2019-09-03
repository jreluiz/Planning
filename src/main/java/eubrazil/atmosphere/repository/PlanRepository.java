package eubrazil.atmosphere.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eubr.atmosphere.tma.entity.plan.Plan;

/**
 * Planning services
 * @author Jorge Luiz
 */
@Repository
public interface PlanRepository extends CrudRepository<Plan, Long> {

	@Query(value = "SELECT distinct p FROM Plan p WHERE p.attribute.attributeId = ?1 order by p.valueTime desc")
	Plan getPlanIdByMetricAndConfigurationProfile(Integer attributeId);
	
}