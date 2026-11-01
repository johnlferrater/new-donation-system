package project2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;
import project2.entity.ProfilePicture;
import project2.entity.User;
import project2.repository.ProfilePictureRepository;
import project2.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfilePictureRepository profilePictureRepository;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userRepository, new BCryptPasswordEncoder(), profilePictureRepository);
    }

    @Test
    void cannotDemoteTheLastAdministrator() {
        User admin = user("only-admin", "ADMIN");
        User update = new User();
        update.setEmail("admin@example.org");
        update.setRole("USER");

        when(userRepository.findById("only-admin")).thenReturn(Optional.of(admin));
        when(userRepository.findAdministratorsForUpdate()).thenReturn(List.of(admin));

        var actor = new UsernamePasswordAuthenticationToken("other-admin", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertThrows(IllegalArgumentException.class, () -> controller.update("only-admin", update, actor));
        verify(userRepository, never()).save(any());
    }

    @Test
    void actorWithoutAuthoritiesCannotUpdateAnotherAccount() {
        User target = user("target", "USER");
        User update = new User();
        update.setEmail("target@example.org");

        var actor = new UsernamePasswordAuthenticationToken("other-user", "n/a");
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.update("target", update, actor));
        verify(userRepository, never()).save(any());
    }

    @Test
    void cannotDeleteTheLastAdministrator() {
        User admin = user("only-admin", "ADMIN");
        when(userRepository.findById("only-admin")).thenReturn(Optional.of(admin));
        when(userRepository.findAdministratorsForUpdate()).thenReturn(List.of(admin));

        var actor = new UsernamePasswordAuthenticationToken("another-admin", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertThrows(IllegalArgumentException.class, () -> controller.delete("only-admin", actor));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void rejectsNonImageBytesEvenWhenContentTypeClaimsImage() {
        User user = user("sample", "USER");
        when(userRepository.findById("sample")).thenReturn(Optional.of(user));

        MultipartFile file = new MockMultipartFile("file", "fake.png", "image/png",
                "not an image".getBytes(StandardCharsets.UTF_8));
        var actor = new UsernamePasswordAuthenticationToken("sample", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThrows(IllegalArgumentException.class,
                () -> controller.uploadProfilePicture(file, actor));
        verify(profilePictureRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void storesAndServesValidPng() {
        User user = user("sample", "USER");
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        when(userRepository.findById("sample")).thenReturn(Optional.of(user));
        when(profilePictureRepository.findById("sample")).thenReturn(Optional.empty());

        MultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", png);
        var actor = new UsernamePasswordAuthenticationToken("sample", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        Map<String, String> response = controller.uploadProfilePicture(file, actor);

        assertEquals("db", response.get("profilePicture"));
        assertEquals("/api/users/me/profile-picture", response.get("url"));
        assertEquals("db", user.getProfilePicture());
        verify(userRepository).save(user);

        ArgumentCaptor<ProfilePicture> captor = ArgumentCaptor.forClass(ProfilePicture.class);
        verify(profilePictureRepository).save(captor.capture());
        ProfilePicture saved = captor.getValue();
        assertEquals("sample", saved.getUsername());
        assertEquals("image/png", saved.getContentType());
        assertArrayEquals(png, saved.getImageData());
        assertNotNull(saved.getUpdatedAt());

        when(profilePictureRepository.findById("sample")).thenReturn(Optional.of(saved));
        ResponseEntity<byte[]> image = controller.getProfilePicture("me", actor);

        assertEquals(HttpStatus.OK, image.getStatusCode());
        assertEquals("image/png", image.getHeaders().getContentType().toString());
        assertEquals("nosniff", image.getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("no-cache", image.getHeaders().getCacheControl());
        assertArrayEquals(png, image.getBody());
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
