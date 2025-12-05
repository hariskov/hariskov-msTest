package bg.petarh.microservices.sales.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import bg.petarh.microservices.sales.entities.SaleEntity;
import bg.petarh.microservices.sales.services.SalesService;

@RestController
@RequestMapping(value = "/v1/sales")
public class SalesController {

    private final SalesService salesService;

    SalesController(final SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SaleEntity>> getSales() {
        return ResponseEntity.ok(salesService.getAllSales());
    }

    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SaleEntity> createSale(@RequestBody SaleEntity sale) {
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
