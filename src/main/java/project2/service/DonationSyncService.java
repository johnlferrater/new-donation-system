package project2.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project2.entity.DonationHistory;
import project2.entity.DonationRecord;
import project2.repository.DonationHistoryRepository;
import project2.repository.DonationRecordRepository;

import java.sql.Timestamp;

/**
 * Keeps {@link DonationRecord} and {@link DonationHistory} in sync.
 *
 * <p>Whenever a record is created a matching history entry is written, and
 * whenever a history entry is created a matching record is written. The internal
 * saves call the repositories directly (not the other controller/service method),
 * so the sync only runs one hop and cannot loop.
 */
@Service
public class DonationSyncService {

    /** Address used on records created from a history entry (history has no address). */
    private static final String DEFAULT_ADDRESS = "N/A";

    private final DonationRecordRepository recordRepo;
    private final DonationHistoryRepository historyRepo;

    public DonationSyncService(DonationRecordRepository recordRepo, DonationHistoryRepository historyRepo) {
        this.recordRepo = recordRepo;
        this.historyRepo = historyRepo;
    }

    /** Saves a record and writes the matching history entry. */
    @Transactional
    public DonationRecord saveRecordWithHistory(DonationRecord record) {
        record.setRecordId(0);
        DonationRecord saved = recordRepo.save(record);
        historyRepo.save(recordToHistory(saved));
        return saved;
    }

    /** Saves a history entry and writes the matching record. */
    @Transactional
    public DonationHistory saveHistoryWithRecord(DonationHistory history) {
        history.setId(null);
        DonationHistory saved = historyRepo.save(history);
        recordRepo.save(historyToRecord(saved));
        return saved;
    }

    private DonationHistory recordToHistory(DonationRecord record) {
        DonationHistory history = new DonationHistory();
        history.setUsername(record.getDonorName());
        history.setItem(record.getDonationType());
        history.setAmount(record.getAmount());
        history.setPaymentMethod(record.getPaymentMethod());
        if (record.getDateDonated() != null) {
            history.setDateDonated(new Timestamp(record.getDateDonated().getTime()));
        }
        return history;
    }

    private DonationRecord historyToRecord(DonationHistory history) {
        DonationRecord record = new DonationRecord();
        record.setDonorName(history.getUsername());
        record.setDonorAddress(DEFAULT_ADDRESS);
        record.setDonationType(history.getItem());
        record.setAmount(history.getAmount());
        record.setPaymentMethod(history.getPaymentMethod());
        if (history.getDateDonated() != null) {
            record.setDateDonated(new java.sql.Date(history.getDateDonated().getTime()));
        }
        return record;
    }
}
