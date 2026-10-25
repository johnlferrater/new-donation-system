package project2.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import project2.entity.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void administratorLockQueryReturnsOnlyAdministratorAccounts() {
        userRepository.save(user("admin-account", "ADMIN"));
        userRepository.save(user("regular-account", "USER"));

        List<User> administrators = userRepository.findAdministratorsForUpdate();

        assertEquals(1, administrators.size());
        assertEquals("admin-account", administrators.get(0).getUsername());
    }

    private User user(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.org");
        user.setPassword("not-used-by-repository-test");
        user.setRole(role);
        return user;
    }
}
