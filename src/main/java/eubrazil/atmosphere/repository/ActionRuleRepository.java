package eubrazil.atmosphere.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eubr.atmosphere.tma.entity.qualitymodel.ActionRule;

@Repository
public interface ActionRuleRepository extends CrudRepository<ActionRule, Integer> {
	
}
