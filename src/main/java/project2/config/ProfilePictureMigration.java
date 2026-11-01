package project2.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project2.entity.ProfilePicture;
import project2.entity.User;
import project2.repository.ProfilePictureRepository;
import project2.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.logging.Logger;

@Component
public class ProfilePictureMigration implements CommandLineRunner {

    private static final Logger LOGGER = Logger.getLogger(ProfilePictureMigration.class.getName());
    private static final Path UPLOAD_DIR = Paths.get("uploads", "profile").toAbsolutePath().normalize();

    private final UserRepository userRepository;
    private final ProfilePictureRepository profilePictureRepository;

    public ProfilePictureMigration(UserRepository userRepository, ProfilePictureRepository profilePictureRepository) {
        this.userRepository = userRepository;
        this.profilePictureRepository = profilePictureRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!Files.isDirectory(UPLOAD_DIR)) {
            LOGGER.info("Profile picture migration skipped: uploads/profile does not exist.");
            return;
        }

        int migrated = 0;
        int missing = 0;
        int invalid = 0;

        for (User user : userRepository.findAll()) {
            String marker = user.getProfilePicture();
            if (marker == null || marker.isBlank() || "db".equals(marker)) {
                continue;
            }
            if (marker.contains("/") || marker.contains("\\")) {
                invalid++;
                LOGGER.warning("Profile picture migration skipped unsafe filename for user " + user.getUsername() + ".");
                continue;
            }

            Path file = UPLOAD_DIR.resolve(marker).normalize();
            if (!file.startsWith(UPLOAD_DIR) || !Files.isRegularFile(file)) {
                missing++;
                LOGGER.warning("Profile picture migration skipped missing file for user " + user.getUsername() + ".");
                continue;
            }

            byte[] imageData;
            try {
                imageData = Files.readAllBytes(file);
            } catch (IOException e) {
                missing++;
                LOGGER.warning("Profile picture migration could not read file for user " + user.getUsername() + ".");
                continue;
            }

            String contentType = detectImageContentType(imageData);
            if (contentType == null) {
                invalid++;
                LOGGER.warning("Profile picture migration skipped unsupported image for user " + user.getUsername() + ".");
                continue;
            }

            ProfilePicture picture = profilePictureRepository.findById(user.getUsername())
                    .orElseGet(ProfilePicture::new);
            picture.setUsername(user.getUsername());
            picture.setImageData(imageData);
            picture.setContentType(contentType);
            picture.setUpdatedAt(Instant.now());
            profilePictureRepository.save(picture);

            user.setProfilePicture("db");
            userRepository.save(user);
            migrated++;
            LOGGER.info("Profile picture migrated for user " + user.getUsername() + ".");
        }

        LOGGER.info("Profile picture migration complete: migrated=" + migrated
                + ", missing=" + missing + ", invalid=" + invalid + ".");
    }

    private String detectImageContentType(byte[] data) {
        if (data == null || data.length < 4) {
            return null;
        }
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if ((data[0] & 0xFF) == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return "image/png";
        }
        if (data.length >= 12
                && data[0] == 0x52 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x46
                && data[8] == 0x57 && data[9] == 0x45 && data[10] == 0x42 && data[11] == 0x50) {
            return "image/webp";
        }
        return null;
    }
}
