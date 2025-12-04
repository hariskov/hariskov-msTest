package bg.petarh.microservices.management.config;

import bg.petarh.microservices.management.entities.UserEntity;
import bg.petarh.microservices.management.repository.UserShardedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Component
@Profile("dev")  // Only runs in dev environment
public class TestDataLoader {
    private static final Logger log = LoggerFactory.getLogger(TestDataLoader.class);

    @Autowired
    private UserShardedRepository userShardedRepository;

    @Autowired
    private ShardRouter shardRouter;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void loadTestData() {
        try {
            loadUsersFromFile();
        } catch (IOException e) {
            log.warn("Could not load test data: {}", e.getMessage());
        }
    }

    private void loadUsersFromFile() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:test-data/users.json");

        List<UserData> userData = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<>() {
                }
        );

        for (UserData data : userData) {
            int shard = shardRouter.determineShardIndex(data.id);
            UserEntity user = new UserEntity(data.id(), data.name(), shard);
            userShardedRepository.save(user, shardRouter.getShard(shard));
            log.debug("Loaded user {} into shard {}", user.getName(), shard);
        }

        log.info("Loaded {} test users from file", userData.size());
    }


    private record UserData(String id, String name) {
    }
}