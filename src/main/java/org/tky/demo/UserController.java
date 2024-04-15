package org.tky.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final NonTransactionalUserService nonTransactionalUserService;
    private final TransactionalUserService transactionalUserService;

    public UserController(UserRepository userRepository, NonTransactionalUserService nonTransactionalUserService, TransactionalUserService transactionalUserService) {
        this.userRepository = userRepository;
        this.nonTransactionalUserService = nonTransactionalUserService;
        this.transactionalUserService = transactionalUserService;
    }

    @GetMapping("{id}")
    public ResponseEntity<User> getUsers(@PathVariable long id) {
        var user = userRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(user);
    }

    @PostMapping("/on_controller")
    public ResponseEntity<User> createUserOnController(@RequestBody UserForm userForm) {
        // saveが呼び出される時に自動的にトランザクションが開始され、完了後時自動的にcloseしてる。
        // [nio-8080-exec-1] org.tky.demo.UserController              : start save: User(id=0, name=John Doe)
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-1] org.tky.demo.UserController              : end save: User(id=8, name=John Doe)
        var user = new User(userForm.getName());
        log.info("start save: {}", user);
        userRepository.save(user);
        log.info("end save: {}", user);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/on_controller_rollback1")
    public ResponseEntity<User> createUserOnControllerRollback1(@RequestBody UserForm userForm) throws RuntimeException {
        // save完了後に自動的にcommitしているので、そのあとに例外をthrowしてもロールバックされない。
        // (実行後DBを直接selectして確認)
        // [nio-8080-exec-2] org.tky.demo.UserController              : start save: User(id=0, name=John Doe)
        //[nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        //[nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        //[nio-8080-exec-2] org.tky.demo.UserController              : end save: User(id=10, name=John Doe)
        //[demo] [nio-8080-exec-2] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: java.lang.RuntimeException: rollback test] with root cause
        var user = new User(userForm.getName());
        log.info("start save: {}", user);
        userRepository.save(user);
        log.info("end save: {}", user);
        if (true) {
            throw new RuntimeException("rollback test");
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/on_non_transactional_service")
    public ResponseEntity<User> createUserOnNonTransactionalService(@RequestBody UserForm userForm) {
        // [nio-8080-exec-1] o.tky.demo.NonTransactionalUserService   : start service
        // [nio-8080-exec-1] o.tky.demo.NonTransactionalUserService   : start save: User(id=0, name=John Doe)
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-1] o.tky.demo.NonTransactionalUserService   : end save: User(id=11, name=John Doe)
        var user = nonTransactionalUserService.createUser(userForm.getName());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/on_non_transactional_service_rollback1")
    public ResponseEntity<User> createUserOnNonTransactionalServiceRollback1(@RequestBody UserForm userForm) {
        // save終了時にcommitされるのでその後で例外を投げてもrollbackされない
        // [nio-8080-exec-3] o.tky.demo.NonTransactionalUserService   : start service
        // [nio-8080-exec-3] o.tky.demo.NonTransactionalUserService   : start save: User(id=0, name=John Doe)
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-3] o.tky.demo.NonTransactionalUserService   : end save: User(id=12, name=John Doe)
        //[nio-8080-exec-3] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: java.lang.RuntimeException: rollback test] with root cause
        var user = nonTransactionalUserService.createUserAndError(userForm.getName());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/on_transactional_service")
    public ResponseEntity<User> createUserNonTransactionalService(@RequestBody UserForm userForm) {
        // transactionalなserviceを呼び出している
        // start serviceの前にtransactionが開始、end saveの後にcommitされる
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.tky.demo.TransactionalUserService.createUser]
        // [nio-8080-exec-1] org.tky.demo.TransactionalUserService    : start service
        // [nio-8080-exec-1] org.tky.demo.TransactionalUserService    : start save: User(id=0, name=John Doe)
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-1] org.tky.demo.TransactionalUserService    : end save: User(id=13, name=John Doe)
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.tky.demo.TransactionalUserService.createUser]
        var user = transactionalUserService.createUser(userForm.getName());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/on_transactional_service_rollback1")
    public ResponseEntity<User> createUserOnTransactionalServiceRollback1(@RequestBody UserForm userForm) {
        // transaction終了前に例外をthrowしたのでrollbackされUserが保存されない
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.tky.demo.TransactionalUserService.createUserAndError]
        // [nio-8080-exec-3] org.tky.demo.TransactionalUserService    : start service
        // [nio-8080-exec-3] org.tky.demo.TransactionalUserService    : start save: User(id=0, name=John Doe)
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-3] org.tky.demo.TransactionalUserService    : end save: User(id=14, name=John Doe)
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.tky.demo.TransactionalUserService.createUserAndError] after exception: java.lang.RuntimeException: rollback test
        // [nio-8080-exec-3] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: java.lang.RuntimeException: rollback test] with root cause
        var user = transactionalUserService.createUserAndError(userForm.getName());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/create_user_twice")
    public ResponseEntity<List<User>> createUserTwice(@RequestBody UserForm userForm) {
        // transactionalなserviceを2回呼び出している
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.tky.demo.TransactionalUserService.createUser]
        // [nio-8080-exec-3] org.tky.demo.TransactionalUserService    : start service
        // [nio-8080-exec-3] org.tky.demo.TransactionalUserService    : start save: User(id=0, name=John Doe_1)
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-3] org.tky.demo.TransactionalUserService    : end save: User(id=17, name=John Doe_1)
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.tky.demo.TransactionalUserService.createUser]
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.tky.demo.TransactionalUserService.createUser]
        // [nio-8080-exec-3] org.tky.demo.TransactionalUserService    : start service
        // [nio-8080-exec-3] org.tky.demo.TransactionalUserService    : start save: User(id=0, name=John Doe_2)
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-3] org.tky.demo.TransactionalUserService    : end save: User(id=18, name=John Doe_2)
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.tky.demo.TransactionalUserService.createUser]
        var user1 = transactionalUserService.createUser(userForm.getName() + "_1");
        var user2 = transactionalUserService.createUser(userForm.getName() + "_2");
        return ResponseEntity.ok(List.of(user1, user2));
    }

    @PostMapping("/create_user_twice_within_error")
    public ResponseEntity<List<User>> createUserTwiceWithinError(@RequestBody UserForm userForm) {
        // transactionalなserviceを完了後に例外をthrowしている
        // 最初のtransactionはcommitされているので
        // 一つだけ正常にUserがDBに保存される
        // [nio-8080-exec-6] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.tky.demo.TransactionalUserService.createUser]
        // [nio-8080-exec-6] org.tky.demo.TransactionalUserService    : start service
        // [nio-8080-exec-6] org.tky.demo.TransactionalUserService    : start save: User(id=0, name=John Doe_1)
        // [nio-8080-exec-6] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-6] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-6] org.tky.demo.TransactionalUserService    : end save: User(id=21, name=John Doe_1)
        // [nio-8080-exec-6] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.tky.demo.TransactionalUserService.createUser]
        // [nio-8080-exec-6] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: java.lang.RuntimeException: rollback test] with root cause
        var user1 = transactionalUserService.createUser(userForm.getName() + "_1");
        if (true) {
            throw new RuntimeException("rollback test");
        }
        var user2 = transactionalUserService.createUser(userForm.getName() + "_2");
        return ResponseEntity.ok(List.of(user1, user2));
    }

    @PostMapping("/create_couple_of_users")
    public ResponseEntity<List<User>> createCoupleOfUsers(@RequestBody UserForm userForm) {
        // transactionalなserviceからさらに別のtransactionalなserviceを呼び出している
        // 同一threadだと同一transactionになる
        // そのため最初に取得したtransactionを使いまわして最後にcommitされる
        // https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html#page-title
        // https://docs.spring.io/spring-framework/docs/5.3.6/reference/html/data-access.html#jdbc-DataSourceTransactionManager
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.tky.demo.TransactionalUserService.createCoupleOfUsers] (ここで開始)
        // [nio-8080-exec-3] org.tky.demo.TransactionalUserService    : start service
        // [nio-8080-exec-3] org.tky.demo.TransactionalUserService    : start save: User(id=0, name=John Doe_1)
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-3] org.tky.demo.TransactionalUserService    : end save: User(id=24, name=John Doe_1)
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.tky.demo.AnotherTransactionalService.createUser]
        // [nio-8080-exec-3] o.tky.demo.AnotherTransactionalService   : start another service
        // [nio-8080-exec-3] o.tky.demo.AnotherTransactionalService   : start save: User(id=0, name=John Doe_2)
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-3] o.tky.demo.AnotherTransactionalService   : end save: User(id=25, name=John Doe_2)
        // [nio-8080-exec-3] o.tky.demo.AnotherTransactionalService   : end another service
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.tky.demo.AnotherTransactionalService.createUser]
        // [nio-8080-exec-3] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.tky.demo.TransactionalUserService.createCoupleOfUsers] (ここでcommit)
        var users = transactionalUserService.createCoupleOfUsers(userForm.getName() + "_1", userForm.getName() + "_2");
        return ResponseEntity.ok(users);
    }
    @PostMapping("/create_couple_of_users_rollback1")
    public ResponseEntity<List<User>> createCoupleOfUsersRollback1(@RequestBody UserForm userForm) {
        // transactionalなserviceからさらに別のtransactionalなserviceを呼び出し
        // その最後で例外をthrowしている
        // 同一transaction上でcommitされる前に例外をthrowしているのでcommitされない
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.tky.demo.TransactionalUserService.createCoupleOfUsersAndErrorAtLast]
        // [nio-8080-exec-2] org.tky.demo.TransactionalUserService    : start service
        // [nio-8080-exec-2] org.tky.demo.TransactionalUserService    : start save: User(id=0, name=John Doe_1)
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-2] org.tky.demo.TransactionalUserService    : end save: User(id=28, name=John Doe_1)
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.tky.demo.AnotherTransactionalService.createUser]
        // [nio-8080-exec-2] o.tky.demo.AnotherTransactionalService   : start another service
        // [nio-8080-exec-2] o.tky.demo.AnotherTransactionalService   : start save: User(id=0, name=John Doe_2)
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-2] o.tky.demo.AnotherTransactionalService   : end save: User(id=29, name=John Doe_2)
        // [nio-8080-exec-2] o.tky.demo.AnotherTransactionalService   : end another service
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.tky.demo.AnotherTransactionalService.createUser]
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.tky.demo.TransactionalUserService.createCoupleOfUsersAndErrorAtLast] after exception: java.lang.RuntimeException: rollback test
        // [nio-8080-exec-2] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: java.lang.RuntimeException: rollback test] with root cause
        var users = transactionalUserService.createCoupleOfUsersAndErrorAtLast(userForm.getName() + "_1", userForm.getName() + "_2");
        return ResponseEntity.ok(users);
    }

    @PostMapping("/create_couple_of_users_rollback2")
    public ResponseEntity<List<User>> createCoupleOfUsersRollback2(@RequestBody UserForm userForm) {
        // transactionalなserviceからさらに別のtransactionalなserviceを呼び出し
        // 2つめのserviceの中で例外をthrowしている
        // 全データがrollbackされる
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.tky.demo.TransactionalUserService.createCoupleOfUsersAndErrorNextService]
        // [nio-8080-exec-2] org.tky.demo.TransactionalUserService    : start service
        // [nio-8080-exec-2] org.tky.demo.TransactionalUserService    : start save: User(id=0, name=John Doe_1)
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-2] org.tky.demo.TransactionalUserService    : end save: User(id=32, name=John Doe_1)
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.tky.demo.AnotherTransactionalService.createUserAndError]
        // [nio-8080-exec-2] o.tky.demo.AnotherTransactionalService   : start another service
        // [nio-8080-exec-2] o.tky.demo.AnotherTransactionalService   : start save: User(id=0, name=John Doe_2)
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-2] o.tky.demo.AnotherTransactionalService   : end save: User(id=33, name=John Doe_2)
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.tky.demo.AnotherTransactionalService.createUserAndError] after exception: java.lang.RuntimeException: rollback test
        // [nio-8080-exec-2] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.tky.demo.TransactionalUserService.createCoupleOfUsersAndErrorNextService] after exception: java.lang.RuntimeException: rollback test
        // [nio-8080-exec-2] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: java.lang.RuntimeException: rollback test] with root cause
        var users = transactionalUserService.createCoupleOfUsersAndErrorNextService(userForm.getName() + "_1", userForm.getName() + "_2");
        return ResponseEntity.ok(users);
    }
    @PostMapping("/create_couple_of_users_rollback3")
    public ResponseEntity<List<User>> createCoupleOfUsersRollback3(@RequestBody UserForm userForm) {
        // transactionalなserviceからさらに別のtransactionalなserviceを呼び出し
        // 2つめのserviceの中で例外をthrow
        // その例外をcatchしている
        // catchしたが最初のデータも含めて、全データ保存されない
        // [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
        // [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
        // [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 1 ms
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.tky.demo.TransactionalUserService.createCoupleOfUsersAndErrorNextServiceAndCatch]
        // [nio-8080-exec-1] org.tky.demo.TransactionalUserService    : start service
        // [nio-8080-exec-1] org.tky.demo.TransactionalUserService    : start save: User(id=0, name=John Doe_1)
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-1] org.tky.demo.TransactionalUserService    : end save: User(id=36, name=John Doe_1)
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.tky.demo.AnotherTransactionalService.createUserAndError]
        // [nio-8080-exec-1] o.tky.demo.AnotherTransactionalService   : start another service
        // [nio-8080-exec-1] o.tky.demo.AnotherTransactionalService   : start save: User(id=0, name=John Doe_2)
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-1] o.tky.demo.AnotherTransactionalService   : end save: User(id=37, name=John Doe_2)
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.tky.demo.AnotherTransactionalService.createUserAndError] after exception: java.lang.RuntimeException: rollback test
        // [nio-8080-exec-1] org.tky.demo.TransactionalUserService    : catch error
        // [nio-8080-exec-1] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.tky.demo.TransactionalUserService.createCoupleOfUsersAndErrorNextServiceAndCatch]
        // [nio-8080-exec-1] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: org.springframework.transaction.UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only] with root cause
        var users = transactionalUserService.createCoupleOfUsersAndErrorNextServiceAndCatch(userForm.getName() + "_1", userForm.getName() + "_2");
        return ResponseEntity.ok(users);
    }

    @PostMapping("/create_couple_of_users_rollback4")
    public ResponseEntity<List<User>> createCoupleOfUsersRollback4(@RequestBody UserForm userForm) {
        // 2つとも正常に保存される
        // transactionalなserviceから別のnon transactionalなserviceを呼び出す
        // 2つめのserviceの中で例外をthrow
        // その例外をcatchしている
        // non transactionalなserviceで例外をthrowしてもtransactionalなserviceでcatchすればrollbackされずcommitされる
        // [nio-8080-exec-4] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.tky.demo.TransactionalUserService.createCoupleOfUsersAndErrorFromNonTransactionalService]
        // [nio-8080-exec-4] org.tky.demo.TransactionalUserService    : start service
        // [nio-8080-exec-4] org.tky.demo.TransactionalUserService    : start save: User(id=0, name=John Doe_1)
        // [nio-8080-exec-4] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-4] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-4] org.tky.demo.TransactionalUserService    : end save: User(id=44, name=John Doe_1)
        // [nio-8080-exec-4] o.tky.demo.NonTransactionalUserService   : start service
        // [nio-8080-exec-4] o.tky.demo.NonTransactionalUserService   : start save: User(id=0, name=John Doe_2)
        // [nio-8080-exec-4] o.s.t.i.TransactionInterceptor           : Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-4] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]
        // [nio-8080-exec-4] o.tky.demo.NonTransactionalUserService   : end save: User(id=45, name=John Doe_2)
        // [nio-8080-exec-4] org.tky.demo.TransactionalUserService    : catch error
        // [nio-8080-exec-4] o.s.t.i.TransactionInterceptor           : Completing transaction for [org.tky.demo.TransactionalUserService.createCoupleOfUsersAndErrorFromNonTransactionalService]
        var users = transactionalUserService.createCoupleOfUsersAndErrorFromNonTransactionalService(userForm.getName() + "_1", userForm.getName() + "_2");
        return ResponseEntity.ok(users);
    }
}
