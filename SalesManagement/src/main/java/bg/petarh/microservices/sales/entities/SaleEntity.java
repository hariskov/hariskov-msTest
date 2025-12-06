package bg.petarh.microservices.sales.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "sales")
public class SaleEntity {

    @Id
    @UuidGenerator
    private String id;

    @Column(name = "amount")
    private int amount;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "user_id")
    private String userId;

    public SaleEntity(String id, int amount, boolean active, String userId) {
        this.id = id;
        this.amount = amount;
        this.active = active;
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public SaleEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(final int amount) {
        this.amount = amount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }
}
