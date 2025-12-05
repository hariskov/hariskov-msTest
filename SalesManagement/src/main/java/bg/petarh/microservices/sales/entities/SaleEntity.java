package bg.petarh.microservices.sales.entities;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    public SaleEntity(final String id, final int amount, final boolean active) {
        this.id = id;
        this.amount = amount;
        this.active = active;
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
