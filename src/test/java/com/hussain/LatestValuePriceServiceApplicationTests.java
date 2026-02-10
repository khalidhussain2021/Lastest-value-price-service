package com.hussain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.hussain.dto.PriceRecord;
import com.hussain.service.PriceService;
import com.hussain.serviceImpl.PriceServiceImpl;

@SpringBootTest
class LatestValuePriceServiceApplicationTests {

	private final PriceService service = new PriceServiceImpl();

    @Test
    void testBatchCompletion() {
        String batchId = service.startBatch();

        PriceRecord record = new PriceRecord(
                "AAPL",
                LocalDateTime.now(),
                Map.of("price", 150)
        );

        service.uploadPrices(batchId, List.of(record));
        service.completeBatch(batchId);

        Map<String, PriceRecord> result =
                service.getLastPrices(List.of("AAPL"));

        assertEquals(150, result.get("AAPL").getPayload().get("price"));
    }

    @Test
    void testCancelledBatchNotVisible() {
        String batchId = service.startBatch();

        PriceRecord record = new PriceRecord(
                "GOOG",
                LocalDateTime.now(),
                Map.of("price", 200)
        );

        service.uploadPrices(batchId, List.of(record));
        service.cancelBatch(batchId);

        assertTrue(service.getLastPrices(List.of("GOOG")).isEmpty());
    }

    @Test
    void testLatestAsOfWins() {
        String batchId = service.startBatch();

        PriceRecord oldPrice = new PriceRecord(
                "MSFT",
                LocalDateTime.now().minusMinutes(10),
                Map.of("price", 300)
        );

        PriceRecord newPrice = new PriceRecord(
                "MSFT",
                LocalDateTime.now(),
                Map.of("price", 320)
        );

        service.uploadPrices(batchId, List.of(oldPrice, newPrice));
        service.completeBatch(batchId);

        assertEquals(
                320,
                service.getLastPrices(List.of("MSFT"))
                        .get("MSFT")
                        .getPayload()
                        .get("price")
        );
    }

}
