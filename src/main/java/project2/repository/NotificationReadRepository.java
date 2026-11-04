package project2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project2.entity.NotificationRead;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {

    Optional<NotificationRead> findByNotificationIdAndUsername(Long notificationId, String username);

    List<NotificationRead> findByUsernameAndNotificationIdIn(String username, Collection<Long> notificationIds);

    void deleteByNotificationIdIn(Collection<Long> notificationIds);
}
