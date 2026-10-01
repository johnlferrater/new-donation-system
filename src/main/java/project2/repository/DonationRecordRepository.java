package project2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project2.entity.DonationRecord;

public interface DonationRecordRepository extends JpaRepository<DonationRecord, Integer> {}
