package project2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import project2.entity.User;
import project2.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userRepository, new BCryptPasswordEncoder(), "uploads/profile");
    }

    @Test
    void cannotDemoteTheLastAdministrator() {
        User admin = user("only-admin", "ADMIN");
        User update = new User();
        update.setEmail("admin@example.org");
        update.setRole("USER");

        when(userRepository.findById("only-admin")).thenReturn(Optional.of(admin));
        when(userRepository.findAdministratorsForUpdate()).thenReturn(List.of(admin));

        var actor = new UsernamePasswordAuthenticationToken("other-admin", "n/a");
        assertThrows(IllegalArgumentException.class, () -> controller.update("only-admin", update, actor));
        verify(userRepository, never()).save(any());
    }

    @Test
    void cannotDeleteTheLastAdministrator() {
        User admin = user("only-admin", "ADMIN");
        when(userRepository.findById("only-admin")).thenReturn(Optional.of(admin));
        when(userRepository.findAdministratorsForUpdate()).thenReturn(List.of(admin));

        var actor = new UsernamePasswordAuthenticationToken("another-user", "n/a");
        assertThrows(IllegalArgumentException.class, () -> controller.delete("only-admin", actor));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void passwordIsNeverSerializedInUserResponses() throws Exception {
        User user = user("sample", "USER");
        user.setPassword("stored-hash-that-must-not-be-returned");

        String json = new ObjectMapper().writeValueAsString(user);
        assertFalse(json.contains("password"));
        assertFalse(json.contains("stored-hash-that-must-not-be-returned"));
    }

    private User user(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.org");
        user.setRole(role);
        return user;
    }
}
