package project2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project2.entity.ProfilePicture;

public interface ProfilePictureRepository extends JpaRepository<ProfilePicture, String> {
}
