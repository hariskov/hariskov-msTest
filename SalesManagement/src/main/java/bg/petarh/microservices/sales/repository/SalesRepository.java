package bg.petarh.microservices.sales.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bg.petarh.microservices.sales.entities.SaleEntity;

@Repository
public interface SalesRepository extends JpaRepository<SaleEntity, String> {

}
