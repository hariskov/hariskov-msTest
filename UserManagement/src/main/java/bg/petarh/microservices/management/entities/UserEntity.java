package bg.petarh.microservices.management.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Table(name = "users")
@Entity
public class UserEntity {

    @Id
    @UuidGenerator
    private String id;
    private String name;

    public UserEntity() {
    }

    public UserEntity(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public UserEntity(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
