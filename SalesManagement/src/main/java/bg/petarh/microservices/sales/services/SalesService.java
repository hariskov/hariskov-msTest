package bg.petarh.microservices.sales.services;

import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.List;

import bg.petarh.microservices.sales.entities.SaleEntity;
import bg.petarh.microservices.sales.repository.SalesRepository;

@Service
public class SalesService {

    private final SalesRepository salesRepository;

    SalesService(final SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    public List<SaleEntity> getAllSales() {
        SaleEntity exampleSale = new SaleEntity();
        exampleSale.setActive(true);
        return this.salesRepository.findAll(Example.of(exampleSale));
    }

    public SaleEntity createSale(final SaleEntity sale) {
        SaleEntity newSale = new SaleEntity();
        newSale.setActive(true);
        newSale.setAmount(sale.getAmount());
        return salesRepository.save(newSale);
    }

    public void deleteSaleById(final String id) {
        SaleEntity dbSale = salesRepository.findById(id).orElseThrow(() -> new RuntimeException("no sale found by id"));
        dbSale.setActive(false);
        salesRepository.save(dbSale);
    }
}
