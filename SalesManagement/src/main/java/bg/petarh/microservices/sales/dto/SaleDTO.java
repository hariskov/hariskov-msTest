package bg.petarh.microservices.sales.dto;

public class SaleDTO {
    String id;
    int amount;
    Boolean active;
    String userId;

    public SaleDTO() {
    }

    public SaleDTO(String id, int amount, Boolean active, String userId) {
        this.id = id;
        this.amount = amount;
        this.active = active;
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

