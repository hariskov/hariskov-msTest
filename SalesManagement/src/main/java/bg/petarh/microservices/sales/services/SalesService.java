package bg.petarh.microservices.sales.services;

import bg.petarh.microservices.sales.dto.SaleDTO;
import bg.petarh.microservices.sales.entities.SaleEntity;
import bg.petarh.microservices.sales.mappers.SalesMapper;
import bg.petarh.microservices.sales.repository.SalesRepository;
import bg.petarh.microservices.sales.rest.api.UserValidationService;
import org.springframework.stereotype.Service;

import javax.naming.ServiceUnavailableException;
import java.util.List;

@Service
public class SalesService {

    private final SalesRepository salesRepository;
    private final UserValidationService userValidationService;

    SalesService(final SalesRepository salesRepository, UserValidationService userValidationService) {
        this.salesRepository = salesRepository;
        this.userValidationService = userValidationService;
    }

    public List<SaleDTO> getAllSales() {
        return SalesMapper.map(this.salesRepository.findAll());
    }

    public SaleDTO createSale(final SaleDTO sale) throws ServiceUnavailableException {

        if (!userValidationService.validateUserExists(sale.getUserId())) {
            return null; //TODO - lazyness
        }

        SaleEntity newSale = new SaleEntity();
        newSale.setActive(true);
        newSale.setAmount(sale.getAmount());
        newSale.setUserId(sale.getUserId());
        return SalesMapper.map(salesRepository.save(newSale));
    }

    public void deleteSaleById(final String id) {
        SaleEntity dbSale = salesRepository.findById(id).orElseThrow(() -> new RuntimeException("no sale found by id"));
        dbSale.setActive(false);
        salesRepository.save(dbSale);
    }
}
