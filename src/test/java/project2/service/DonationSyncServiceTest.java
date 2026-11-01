package project2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project2.entity.DonationHistory;
import project2.entity.DonationRecord;
import project2.entity.PaymentMethod;
import project2.repository.DonationHistoryRepository;
import project2.repository.DonationRecordRepository;

import java.math.BigDecimal;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonationSyncServiceTest {

    @Mock
    private DonationRecordRepository recordRepository;

    @Mock
    private DonationHistoryRepository historyRepository;

    private DonationSyncService service;

    @BeforeEach
    void setUp() {
        service = new DonationSyncService(recordRepository, historyRepository);
    }

    @Test
    void recordCreatesMatchingHistoryWithSameAmountAndPaymentMethod() {
        DonationRecord record = new DonationRecord();
        record.setDonorName("Donor");
        record.setDonorAddress("Address");
        record.setDonationType("Cash donation");
        record.setDateDonated(Date.valueOf("2026-01-01"));
        record.setAmount(new BigDecimal("123.45"));
        record.setPaymentMethod(PaymentMethod.Maya);
        when(recordRepository.save(any(DonationRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DonationRecord saved = service.saveRecordWithHistory(record);

        ArgumentCaptor<DonationHistory> captor = ArgumentCaptor.forClass(DonationHistory.class);
        verify(historyRepository).save(captor.capture());
        DonationHistory history = captor.getValue();
        assertEquals(saved.getAmount(), history.getAmount());
        assertEquals(saved.getPaymentMethod(), history.getPaymentMethod());
        assertEquals(new BigDecimal("123.45"), history.getAmount());
        assertEquals(PaymentMethod.Maya, history.getPaymentMethod());
    }

    @Test
    void recordCanUseAuthenticatedUsernameForHistory() {
        DonationRecord record = new DonationRecord();
        record.setDonorName("Donor");
        record.setDonorAddress("Address");
        record.setDonationType("Cash donation");
        record.setDateDonated(Date.valueOf("2026-01-01"));
        record.setAmount(new BigDecimal("50.00"));
        record.setPaymentMethod(PaymentMethod.Cash);
        when(recordRepository.save(any(DonationRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.saveRecordWithHistory(record, "member");

        ArgumentCaptor<DonationHistory> captor = ArgumentCaptor.forClass(DonationHistory.class);
        verify(historyRepository).save(captor.capture());
        assertEquals("member", captor.getValue().getUsername());
    }
}
