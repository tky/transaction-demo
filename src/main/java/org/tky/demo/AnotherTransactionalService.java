package org.tky.demo;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Transactional
@Slf4j
public class AnotherTransactionalService {

    private final UserRepository userRepository;

    public AnotherTransactionalService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String name) {
        log.info("start another service");
        var user = new User(name);
        log.info("start save: {}", user);
        userRepository.save(user);
        log.info("end save: {}", user);
        log.info("end another service");
        return user;
    }

    public User createUserAndError(String name) {
        log.info("start another service");
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
