package bg.petarh.microservices.management.services;

import bg.petarh.microservices.management.config.ShardRouter;
import bg.petarh.microservices.management.entities.UserEntity;
import bg.petarh.microservices.management.repository.UserShardedRepository;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserShardedRepository repository;
    private final ShardRouter shardRouter;

    public UserService(UserShardedRepository repository, ShardRouter shardRouter) {
        this.repository = repository;
        this.shardRouter = shardRouter;
    }

    // Fast: Queries ONE shard based on ID
    public UserEntity getUserById(String userId) {
        int shardIndex = shardRouter.determineShardIndex(userId);
        DataSource ds = shardRouter.getShard(shardIndex);
        return repository.findById(userId, ds);
    }

    //    // Slow: Queries ALL shards
    public List<UserEntity> getUsersByName(String name) {
        List<UserEntity> results = new ArrayList<>();

        for (int shardIndex = 0; shardIndex < shardRouter.getShardCount(); shardIndex++) {
            DataSource ds = shardRouter.getShard(shardIndex);
            List<UserEntity> shardResults = repository.findByName(name, ds);
            results.addAll(shardResults);
        }

        return results;
    }

    // Creates user and saves to correct shard
    public UserEntity createUser(String name) {
        UserEntity user = new UserEntity();
        user.setName(name);
        // ID will be generated in repository.save()
        user.setId(java.util.UUID.randomUUID().toString());

        int shardIndex = shardRouter.determineShardIndex(user.getId());
        user.setShardIndex(shardIndex);

        DataSource ds = shardRouter.getShard(shardIndex);
        return repository.save(user, ds);
    }
}
