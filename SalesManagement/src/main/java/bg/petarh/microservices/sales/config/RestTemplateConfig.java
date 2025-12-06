package bg.petarh.microservices.sales.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Value("${http.client.connect-timeout:2000}")
    private int connectTimeout;

    @Value("${http.client.read-timeout:5000}")
    private int readTimeout;

    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory();

        factory.setConnectionRequestTimeout(connectTimeout);  // Time to establish connection
        factory.setReadTimeout(readTimeout);        // Time to read response

        return new RestTemplate(factory);
    }
}