package org.tky.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NonTransactionalUserService {

    private final UserRepository userRepository;

    public NonTransactionalUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String name) {
        log.info("start service");
        var user = new User(name);
        log.info("start save: {}", user);
        userRepository.save(user);
        log.info("end save: {}", user);
        return user;
    }

    public User createUserAndError(String name) {
        log.info("start service");
        var user = new User(name);
        log.info("start save: {}", user);
        userRepository.save(user);
        log.info("end save: {}", user);
        if (true) {
            throw new RuntimeException("rollback test");
        }
        return user;
    }
}
