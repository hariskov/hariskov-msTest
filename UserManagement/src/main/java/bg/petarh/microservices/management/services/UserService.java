package bg.petarh.microservices.management.services;

import bg.petarh.microservices.management.entities.UserEntity;
import bg.petarh.microservices.management.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity getUser(String id) {
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("user with id " + id + "not found"));
        return user;
    }
}
