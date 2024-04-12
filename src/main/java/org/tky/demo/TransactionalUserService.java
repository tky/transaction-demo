package org.tky.demo;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@Slf4j
public class TransactionalUserService {

    private final UserRepository userRepository;
    private final AnotherTransactionalService anotherTransactionalService;

    public TransactionalUserService(UserRepository userRepository, AnotherTransactionalService anotherTransactionalService) {
        this.userRepository = userRepository;
        this.anotherTransactionalService = anotherTransactionalService;
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

    public List<User> createCoupleOfUsers(String name1, String name2) {
        log.info("start service");
        var user1 = new User(name1);
        log.info("start save: {}", user1);
        userRepository.save(user1);
        log.info("end save: {}", user1);
        var user2 = anotherTransactionalService.createUser(name2);
        return List.of(user1, user2);
    }
    public List<User> createCoupleOfUsersAndErrorAtLast(String name1, String name2) {
        log.info("start service");
        var user1 = new User(name1);
        log.info("start save: {}", user1);
        userRepository.save(user1);
        log.info("end save: {}", user1);
        var user2 = anotherTransactionalService.createUser(name2);
        if (true) {
            throw new RuntimeException("rollback test");
        }
        return List.of(user1, user2);
    }

    public List<User> createCoupleOfUsersAndErrorNextService(String name1, String name2) {
        log.info("start service");
        var user1 = new User(name1);
        log.info("start save: {}", user1);
        userRepository.save(user1);
        log.info("end save: {}", user1);
        var user2 = anotherTransactionalService.createUserAndError(name2);
        return List.of(user1, user2);
    }

    public List<User> createCoupleOfUsersAndErrorNextServiceAndCatch(String name1, String name2) {
        log.info("start service");
        var user1 = new User(name1);
        log.info("start save: {}", user1);
        userRepository.save(user1);
        log.info("end save: {}", user1);
        try {
            var user2 = anotherTransactionalService.createUserAndError(name2);
        } catch (RuntimeException e) {
            log.error("catch error");
        }
        return List.of(user1, user1);
    }

}
