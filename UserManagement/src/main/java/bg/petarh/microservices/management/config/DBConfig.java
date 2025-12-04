package bg.petarh.microservices.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DBConfig {

    @Bean(name = "shard0DataSource")
    @ConfigurationProperties("datasource.shard0")
    public DataSource shard0DataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "shard1DataSource")
    @ConfigurationProperties("datasource.shard1")
    public DataSource shard1DataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "shard2DataSource")
    @ConfigurationProperties("datasource.shard2")
    public DataSource shard2DataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "allShards")
    public Map<Integer, DataSource> allShards() {
        Map<Integer, DataSource> shards = new HashMap<>();
        shards.put(0, shard0DataSource());
        shards.put(1, shard1DataSource());
        shards.put(2, shard2DataSource());
        return shards;
    }
}
