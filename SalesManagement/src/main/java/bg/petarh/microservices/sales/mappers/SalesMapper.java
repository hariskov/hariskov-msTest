package bg.petarh.microservices.sales.mappers;

import bg.petarh.microservices.sales.dto.SaleDTO;
import bg.petarh.microservices.sales.entities.SaleEntity;

import java.util.List;

public class SalesMapper {

    public static List<SaleDTO> map(List<SaleEntity> entityList) {
        return entityList.stream().map(SalesMapper::map).toList();
    }

    public static SaleDTO map(SaleEntity saleEntity) {
        return new SaleDTO(saleEntity.getId(), saleEntity.getAmount(), saleEntity.isActive(), saleEntity.getUserId());
    }
}
