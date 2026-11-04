package project2.controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import project2.entity.Announcement;
import project2.entity.Notification;
import project2.entity.NotificationType;
import project2.entity.User;
import project2.repository.AnnouncementRepository;
import project2.repository.NotificationRepository;
import project2.repository.UserRepository;
import project2.service.NotificationService;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void anonymousCannotReadNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "member", roles = "USER")
    void notificationsAreReadPerUserAndActorIsNotNotified() throws Exception {
        notificationService.donationCreated("admin");

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("DONATION_CREATED"))
                .andExpect(jsonPath("$[0].read").value(false));

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        Long notificationId = notificationRepository.findAll().get(0).getId();
        mockMvc.perform(post("/api/notifications/" + notificationId + "/read").with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        notificationService.donationCreated("member");
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @WithMockUser(username = "first-user", roles = "USER")
    void markingAllReadDoesNotAffectAnotherUsersState() throws Exception {
        notificationService.donationCreated("admin");
        notificationService.announcementPublished(announcement("New campaign"), "admin");

        mockMvc.perform(post("/api/notifications/read-all").with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].read").value(true))
                .andExpect(jsonPath("$[1].read").value(true));

        mockMvc.perform(get("/api/notifications/unread-count").with(user("other-user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    @WithMockUser(username = "new-user", roles = "USER")
    void notificationsOlderThanTheAccountAreNotUnread() throws Exception {
        Notification old = notificationService.donationCreated("admin");
        old.setCreatedAt(Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS)));
        notificationRepository.save(old);

        User user = new User();
        user.setUsername("new-user");
        user.setEmail("new-user@example.org");
        user.setPassword("unused");
        user.setRole("USER");
        userRepository.save(user);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].read").value(true));
    }

    @Test
    @WithMockUser(username = "member", roles = "USER")
    void unknownNotificationReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/notifications/999999/read").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "member", roles = "USER")
    void creatingDonationAndHistoryCreatesOneNotificationEach() throws Exception {
        long before = notificationRepository.count();

        mockMvc.perform(post("/api/donation-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/donation-history")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyJson()))
                .andExpect(status().isOk());

        assertEquals(before + 2, notificationRepository.count());
        assertEquals(2, notificationRepository.findAll().stream()
                .filter(notification -> notification.getType() == NotificationType.DONATION_CREATED)
                .count());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void creatingPublishedAnnouncementCreatesNotification() throws Exception {
        long before = notificationRepository.count();

        mockMvc.perform(post("/api/announcements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Published immediately",
                                  "message": "Message",
                                  "category": "General",
                                  "status": "Published"
                                }
                                """))
                .andExpect(status().isOk());

        assertEquals(before + 1, notificationRepository.count());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void publishingAnAnnouncementCreatesOnlyOneNotification() throws Exception {
        Announcement draft = new Announcement();
        draft.setTitle("Draft announcement");
        draft.setMessage("Draft message");
        draft.setCategory("General");
        draft.setStatus("Draft");
        draft.setPublished(false);
        draft = announcementRepository.save(draft);

        long before = notificationRepository.count();
        mockMvc.perform(post("/api/announcements/" + draft.getId() + "/publish").with(csrf()))
                .andExpect(status().isOk());
        assertEquals(before + 1, notificationRepository.count());

        mockMvc.perform(post("/api/announcements/" + draft.getId() + "/publish").with(csrf()))
                .andExpect(status().isOk());
        assertEquals(before + 1, notificationRepository.count());

        Notification notification = notificationRepository.findAll().stream()
                .filter(item -> item.getType() == NotificationType.ANNOUNCEMENT_PUBLISHED)
                .findFirst()
                .orElseThrow();
        assertEquals("New announcement: Draft announcement", notification.getMessage());
    }

    @Test
    void cleanupRemovesOnlyExpiredNotifications() {
        Notification expired = new Notification();
        expired.setType(NotificationType.DONATION_CREATED);
        expired.setTitle("Old");
        expired.setMessage("Old message");
        expired.setLink("/donation-records");
        expired.setCreatedAt(Timestamp.from(Instant.now().minus(40, ChronoUnit.DAYS)));
        notificationRepository.save(expired);

        Notification recent = notificationService.donationCreated("admin");

        notificationService.cleanupOldNotifications();
        entityManager.clear();

        assertFalse(notificationRepository.findById(expired.getId()).isPresent());
        assertTrue(notificationRepository.findById(recent.getId()).isPresent());
    }

    private Announcement announcement(String title) {
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setMessage("Message");
        announcement.setCategory("General");
        announcement.setStatus("Published");
        announcement.setPublished(true);
        return announcement;
    }

    private String recordJson() {
        return """
                {
                  "donorName": "Notification Donor",
                  "dateDonated": "2026-01-01",
                  "donorAddress": "Address",
                  "donationType": "Cash donation",
                  "amount": 10.00,
                  "paymentMethod": "Cash"
                }
                """;
    }

    private String historyJson() {
        return """
                {
                  "username": "member",
                  "item": "Food",
                  "amount": 10.00,
                  "paymentMethod": "Cash"
                }
                """;
    }
}
