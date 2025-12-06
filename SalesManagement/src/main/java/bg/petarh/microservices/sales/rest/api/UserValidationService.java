package bg.petarh.microservices.sales.rest.api;


import bg.petarh.microservices.sales.dto.api.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.naming.ServiceUnavailableException;

@Service
public class UserValidationService {

    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://localhost:8080/v1}")
    private String userServiceUrl;

    @Value("${user.validation.enabled:true}")
    private boolean validationEnabled;

    @Value("${user.validation.timeout:5000}")
    private int validationTimeout;

    private static final Logger log = LoggerFactory.getLogger(UserValidationService.class);

    public UserValidationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean validateUserExists(String userId) throws ServiceUnavailableException {
        if (!validationEnabled) {
            log.warn("User validation is disabled - accepting userId {} without verification", userId);
            return true;
        }

        String url = userServiceUrl + "/users/" + userId;

        try {
            long startTime = System.currentTimeMillis();

            ResponseEntity<UserDTO> response = restTemplate.getForEntity(url, UserDTO.class);

            long duration = System.currentTimeMillis() - startTime;
            log.info("User validation for {} took {}ms - Status: {}",
                    userId, duration, response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                return true;
            }
            throw new RuntimeException("User not found");

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("User {} not found in User Service", userId);
            return false;

        } catch (ResourceAccessException e) {
            log.error("Cannot reach User Service - Network error or timeout: {}", e.getMessage());
            throw new ServiceUnavailableException(e.getMessage());
        } catch (HttpServerErrorException e) {
            log.error("User Service returned server error: {}", e.getStatusCode());
            throw new ServiceUnavailableException(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error validating user {}: {}", userId, e.getMessage());
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

}
