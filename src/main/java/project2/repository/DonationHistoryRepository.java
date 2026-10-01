package project2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project2.entity.DonationHistory;

public interface DonationHistoryRepository extends JpaRepository<DonationHistory, Long> {}
