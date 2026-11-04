package project2.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project2.entity.Notification;

import java.sql.Timestamp;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            select n from Notification n
            where n.createdByUsername is null or n.createdByUsername <> :username
            order by n.createdAt desc
            """)
    List<Notification> findVisiblePage(@Param("username") String username, Pageable pageable);

    @Query("""
            select count(n) from Notification n
            where n.createdAt > :since
              and (n.createdByUsername is null or n.createdByUsername <> :username)
              and not exists (
                  select r.id from NotificationRead r
                  where r.notificationId = n.id and r.username = :username
              )
            """)
    long countUnread(@Param("username") String username, @Param("since") Timestamp since);

    @Query("""
            select n from Notification n
            where n.createdByUsername is null or n.createdByUsername <> :username
            """)
    List<Notification> findAllVisible(@Param("username") String username);

    List<Notification> findByCreatedAtBefore(Timestamp cutoff);
}
