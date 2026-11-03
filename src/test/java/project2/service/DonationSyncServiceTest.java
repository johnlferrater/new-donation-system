package project2.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import project2.entity.DonationHistory;
import project2.entity.DonationRecord;
import project2.entity.PaymentMethod;
import project2.repository.DonationHistoryRepository;
import project2.repository.DonationRecordRepository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DonationSyncServiceTest {

    @Autowired
    private DonationSyncService service;

    @Autowired
    private DonationRecordRepository recordRepository;

    @Autowired
    private DonationHistoryRepository historyRepository;

    @Test
    void recordCreateWritesExactlyOneMatchingHistory() {
        long recordsBefore = recordRepository.count();
        long historyBefore = historyRepository.count();
        DonationRecord record = record("Sync Donor", "Cash donation", "123.45", PaymentMethod.Maya,
                Date.valueOf("2026-01-01"));

        DonationRecord saved = service.saveRecordWithHistory(record);

        assertNotNull(saved);
        assertEquals(recordsBefore + 1, recordRepository.count());
        assertEquals(historyBefore + 1, historyRepository.count());

        DonationHistory history = historyRepository.findAll().stream()
                .filter(entry -> "Cash donation".equals(entry.getItem()))
                .findFirst()
                .orElseThrow();
        assertEquals("Sync Donor", history.getUsername());
        assertEquals("Cash donation", history.getItem());
        assertEquals(0, new BigDecimal("123.45").compareTo(history.getAmount()));
        assertEquals(PaymentMethod.Maya, history.getPaymentMethod());
        assertEquals(Date.valueOf("2026-01-01").getTime(), history.getDateDonated().getTime());
    }

    @Test
    void recordCreateUsesAuthenticatedUsernameForHistory() {
        DonationRecord record = record("Spoofed Donor", "Authenticated donation", "50.00", PaymentMethod.Cash,
                Date.valueOf("2026-01-02"));

        service.saveRecordWithHistory(record, "member");

        DonationHistory history = historyRepository.findAll().stream()
                .filter(entry -> "Authenticated donation".equals(entry.getItem()))
                .findFirst()
                .orElseThrow();
        assertEquals("member", history.getUsername());
        assertEquals("Spoofed Donor", record.getDonorName());
    }

    @Test
    void historyCreateWritesExactlyOneMatchingRecordWithDefaultAddress() {
        long recordsBefore = recordRepository.count();
        long historyBefore = historyRepository.count();
        DonationHistory history = new DonationHistory();
        history.setUsername("History User");
        history.setItem("History item");
        history.setAmount(new BigDecimal("75.50"));
        history.setPaymentMethod(PaymentMethod.GCash);
        history.setDateDonated(Timestamp.from(Instant.parse("2026-01-03T10:15:30Z")));

        DonationHistory saved = service.saveHistoryWithRecord(history);

        assertNotNull(saved);
        assertEquals(recordsBefore + 1, recordRepository.count());
        assertEquals(historyBefore + 1, historyRepository.count());

        List<DonationRecord> records = recordRepository.findAll().stream()
                .filter(entry -> "History item".equals(entry.getDonationType()))
                .toList();
        assertEquals(1, records.size());
        DonationRecord record = records.get(0);
        assertEquals("History User", record.getDonorName());
        assertEquals("N/A", record.getDonorAddress());
        assertEquals("History item", record.getDonationType());
        assertEquals(0, new BigDecimal("75.50").compareTo(record.getAmount()));
        assertEquals(PaymentMethod.GCash, record.getPaymentMethod());
        assertEquals(Date.valueOf("2026-01-03"), record.getDateDonated());
    }

    @Test
    void syncDoesNotLoopOrCreateDuplicateRows() {
        long recordsBefore = recordRepository.count();
        long historyBefore = historyRepository.count();

        service.saveRecordWithHistory(record("One Way", "One-way item", "10.00", PaymentMethod.Cash,
                Date.valueOf("2026-01-04")));
        assertEquals(recordsBefore + 1, recordRepository.count());
        assertEquals(historyBefore + 1, historyRepository.count());

        DonationHistory history = new DonationHistory();
        history.setUsername("Other Way");
        history.setItem("Other-way item");
        history.setAmount(new BigDecimal("20.00"));
        history.setPaymentMethod(PaymentMethod.CebuanaLhuillier);
        history.setDateDonated(Timestamp.from(Instant.parse("2026-01-05T08:00:00Z")));
        service.saveHistoryWithRecord(history);

        assertEquals(recordsBefore + 2, recordRepository.count());
        assertEquals(historyBefore + 2, historyRepository.count());
    }

    private DonationRecord record(String donorName, String donationType, String amount,
                                  PaymentMethod paymentMethod, Date dateDonated) {
        DonationRecord record = new DonationRecord();
        record.setDonorName(donorName);
        record.setDonorAddress("Address");
        record.setDonationType(donationType);
        record.setAmount(new BigDecimal(amount));
        record.setPaymentMethod(paymentMethod);
        record.setDateDonated(dateDonated);
        return record;
    }
}
