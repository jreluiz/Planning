package eubrazil.atmosphere.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eubr.atmosphere.tma.entity.qualitymodel.Attribute;

/**
 * Dashboard services
 * @author Felipe Gaia
 */
@Repository
public interface AttributeRepository extends CrudRepository<Attribute, Long> {

	@Override
	Iterable<Attribute> findAll();	
}