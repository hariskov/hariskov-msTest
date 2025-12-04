package bg.petarh.microservices.management.entities;

public class UserEntity {

    private String id;

    private String name;

    private Integer shardIndex;

    public UserEntity() {
    }

    public UserEntity(String message) {
        // lazyness to create structure for errors - for simplicity will reuse
        this("0", message, -1);
    }

    public UserEntity(String id, String name, Integer shardIndex) {
        this.id = id;
        this.name = name;
        this.shardIndex = shardIndex;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getShardIndex() {
        return shardIndex;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setShardIndex(Integer shardIndex) {
        this.shardIndex = shardIndex;
    }
}
