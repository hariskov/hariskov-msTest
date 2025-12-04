package bg.petarh.microservices.management.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

@Component
public class ShardRouter {

    private static final Logger log = LoggerFactory.getLogger(ShardRouter.class);

    @Autowired
    @Qualifier("allShards")
    private Map<Integer, DataSource> shards;

    public DataSource getShardForUser(String id) {
        int shardIndex = determineShardIndex(id);

        DataSource shard = getShard(shardIndex);

        if (shard == null) {
            // FAIL LOUD: Don't fall back to anything
            throw new IllegalStateException(
                    "No shard found for index: " + shardIndex +
                            ". This is a critical bug in routing logic."
            );
        }

        log.info("Username : {} , shard : {}", id, shard);

        return shard;
    }

    public DataSource getShard(int shardIndex) {
        return shards.get(shardIndex);
    }

    public int determineShardIndex(String id) {
        if (id == null || id.isEmpty()) {
            // FAIL LOUD: Don't allow null user IDs
            log.info("id wrong: {} ", id);
            throw new IllegalArgumentException(
                    "Cannot determine shard: userId is null or empty"
            );

        }

        int hash = id.hashCode();

        return Math.abs(hash % shards.size());
    }

    public int getShardCount() {
        return shards.size();
    }
}