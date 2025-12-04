package bg.petarh.microservices.management.controllers;

import bg.petarh.microservices.management.dto.UserDTO;
import bg.petarh.microservices.management.entities.UserEntity;
import bg.petarh.microservices.management.services.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserEntity> getUserId(@PathVariable("id") String id) {
        UserEntity user = userService.getUserById(id);
        return ResponseEntity.ok().body(user);
    }

    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserEntity>> getUsersByName(@RequestParam("name") String name) {
        List<UserEntity> users = userService.getUsersByName(name);
        return ResponseEntity.ok().body(users);
    }

    @RequestMapping(value = "", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserEntity> saveUser(@RequestBody UserDTO user) {
        UserEntity savedEntity = this.userService.createUser(user.name());
        return ResponseEntity.ok(savedEntity);

    }
}
