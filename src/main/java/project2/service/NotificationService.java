package project2.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project2.entity.Announcement;
import project2.entity.Notification;
import project2.entity.NotificationRead;
import project2.entity.NotificationType;
import project2.entity.User;
import project2.exception.ResourceNotFoundException;
import project2.repository.NotificationReadRepository;
import project2.repository.NotificationRepository;
import project2.repository.UserRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final int LATEST_LIMIT = 20;
    private static final String DONATION_LINK = "/donation-records";
    private static final String ANNOUNCEMENT_LINK = "/announcements";

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final UserRepository userRepository;

    @Value("${app.notifications.retention-days:30}")
    private long retentionDays;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationReadRepository notificationReadRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationReadRepository = notificationReadRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Notification donationCreated(String actorUsername) {
        return create(NotificationType.DONATION_CREATED, "New donation recorded",
                "A new donation was recorded.", DONATION_LINK, actorUsername);
    }

    @Transactional
    public Notification announcementPublished(Announcement announcement, String actorUsername) {
        String title = announcement.getTitle() == null || announcement.getTitle().isBlank()
                ? "Untitled announcement" : announcement.getTitle();
        return create(NotificationType.ANNOUNCEMENT_PUBLISHED, "New announcement",
                "New announcement: " + title, ANNOUNCEMENT_LINK, actorUsername);
    }

    @Transactional(readOnly = true)
    public List<NotificationView> latest(String username) {
        List<Notification> notifications = notificationRepository.findVisiblePage(
                username, PageRequest.of(0, LATEST_LIMIT));
        if (notifications.isEmpty()) {
            return List.of();
        }

        List<Long> ids = notifications.stream().map(Notification::getId).toList();
        Set<Long> readIds = notificationReadRepository
                .findByUsernameAndNotificationIdIn(username, ids)
                .stream()
                .map(NotificationRead::getNotificationId)
                .collect(Collectors.toSet());
        Timestamp accountCreatedAt = accountCreatedAt(username);

        return notifications.stream()
                .map(notification -> toView(notification, readIds.contains(notification.getId()),
                        accountCreatedAt))
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        return notificationRepository.countUnread(username, accountCreatedAt(username));
    }

    @Transactional
    public void markRead(String username, Long notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new ResourceNotFoundException("Notification", notificationId);
        }
        if (notificationReadRepository.findByNotificationIdAndUsername(notificationId, username).isEmpty()) {
            saveRead(notificationId, username);
        }
    }

    @Transactional
    public void markAllRead(String username) {
        List<Notification> visible = notificationRepository.findAllVisible(username);
        if (visible.isEmpty()) {
            return;
        }

        List<Long> ids = visible.stream().map(Notification::getId).toList();
        Set<Long> readIds = notificationReadRepository
                .findByUsernameAndNotificationIdIn(username, ids)
                .stream()
                .map(NotificationRead::getNotificationId)
                .collect(Collectors.toSet());

        List<NotificationRead> unread = ids.stream()
                .filter(id -> !readIds.contains(id))
                .map(id -> newRead(id, username))
                .toList();
        if (!unread.isEmpty()) {
            notificationReadRepository.saveAll(unread);
        }
    }

    @Scheduled(
            fixedDelayString = "${app.notifications.cleanup-interval-ms:86400000}",
            initialDelayString = "${app.notifications.cleanup-initial-delay-ms:60000}")
    @Transactional
    public void cleanupOldNotifications() {
        Timestamp cutoff = Timestamp.from(Instant.now().minus(retentionDays, ChronoUnit.DAYS));
        List<Notification> old = notificationRepository.findByCreatedAtBefore(cutoff);
        if (old.isEmpty()) {
            return;
        }

        List<Long> ids = old.stream().map(Notification::getId).toList();
        notificationReadRepository.deleteByNotificationIdIn(ids);
        notificationRepository.deleteAllInBatch(old);
    }

    private Notification create(NotificationType type, String title, String message,
                                String link, String actorUsername) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(normalizeLink(link));
        notification.setCreatedByUsername(actorUsername);
        return notificationRepository.save(notification);
    }

    private NotificationView toView(Notification notification, boolean read, Timestamp accountCreatedAt) {
        boolean effectivelyRead = read
                || notification.getCreatedAt() == null
                || notification.getCreatedAt().before(accountCreatedAt);
        return new NotificationView(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.getCreatedAt(),
                effectivelyRead);
    }

    private Timestamp accountCreatedAt(String username) {
        return userRepository.findById(username)
                .map(User::getDateCreated)
                .orElseGet(() -> new Timestamp(0));
    }

    private void saveRead(Long notificationId, String username) {
        notificationReadRepository.save(newRead(notificationId, username));
    }

    private NotificationRead newRead(Long notificationId, String username) {
        NotificationRead read = new NotificationRead();
        read.setNotificationId(notificationId);
        read.setUsername(username);
        return read;
    }

    private String normalizeLink(String link) {
        if (link == null || !link.startsWith("/") || link.startsWith("//") || link.contains("://")) {
            throw new IllegalArgumentException("Notification links must be internal paths");
        }
        return link;
    }

    public record NotificationView(
            Long id,
            NotificationType type,
            String title,
            String message,
            String link,
            Timestamp createdAt,
            boolean read) {
    }
}
