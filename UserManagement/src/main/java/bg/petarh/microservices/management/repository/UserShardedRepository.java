package bg.petarh.microservices.management.repository;

import bg.petarh.microservices.management.entities.UserEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

@Repository
public class UserShardedRepository {

    public UserEntity findById(String userId, DataSource ds) {
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        List<UserEntity> results = jdbc.query(
                "SELECT id, name, shard_index FROM users WHERE id = ?",
                userRowMapper(),
                userId
        );
        return results.isEmpty() ? null : results.getFirst();
    }

    public List<UserEntity> findByName(String name, DataSource ds) {
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        return jdbc.query(
                "SELECT id, name, shard_index FROM users WHERE name = ?",
                userRowMapper(),
                name
        );
    }

    public UserEntity save(UserEntity user, DataSource ds) {
        JdbcTemplate jdbc = new JdbcTemplate(ds);

        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }

        jdbc.update(
                "INSERT INTO users (id, name, shard_index) VALUES (?, ?, ?) " +
                        "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, shard_index = EXCLUDED.shard_index",
                user.getId(),
                user.getName(),
                user.getShardIndex()
        );

        return user;
    }

    private RowMapper<UserEntity> userRowMapper() {
        return (rs, rowNum) -> {
            UserEntity user = new UserEntity();
            user.setId(rs.getString("id"));
            user.setName(rs.getString("name"));
            user.setShardIndex(rs.getInt("shard_index"));
            return user;
        };
    }
}