package project2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project2.entity.DonationHistory;

import java.util.List;

public interface DonationHistoryRepository extends JpaRepository<DonationHistory, Long> {
    List<DonationHistory> findAllByUsernameOrderByDateDonatedDesc(String username);
}
