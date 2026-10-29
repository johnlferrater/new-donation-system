package project2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project2.entity.Announcement;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findAllByPublishedTrueOrderByPublishedAtDescDatePostedDesc();
    List<Announcement> findAllByOrderByCreatedAtDescDatePostedDesc();
}
