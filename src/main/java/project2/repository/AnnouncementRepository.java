package project2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project2.entity.Announcement;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {}
