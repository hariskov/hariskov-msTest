package bg.petarh.microservices.sales.controllers;

import bg.petarh.microservices.sales.dto.SaleDTO;
import bg.petarh.microservices.sales.services.SalesService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.naming.ServiceUnavailableException;
import java.util.List;

@RestController
@RequestMapping(value = "/v1/sales")
public class SalesController {

    private final SalesService salesService;

    SalesController(final SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SaleDTO>> getSales() {
        return ResponseEntity.ok(salesService.getAllSales());
    }

    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SaleDTO> createSale(@RequestBody SaleDTO sale) throws ServiceUnavailableException {
        return ResponseEntity.ok(salesService.createSale(sale));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> deleteSale(@PathVariable("id") String id) {
        try {
            salesService.deleteSaleById(id);
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
