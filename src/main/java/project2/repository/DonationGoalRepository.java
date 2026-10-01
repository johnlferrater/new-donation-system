package project2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project2.entity.DonationGoal;

public interface DonationGoalRepository extends JpaRepository<DonationGoal, Long> {}
