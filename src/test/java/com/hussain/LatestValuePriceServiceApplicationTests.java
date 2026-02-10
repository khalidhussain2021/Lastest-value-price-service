package com.hussain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;

import com.hussain.dto.PriceRecord;
import com.hussain.service.PriceService;
import com.hussain.serviceImpl.PriceServiceImpl;

@SpringBootTest
class LatestValuePriceServiceApplicationTests {

	private PriceService priceService;

	@BeforeEach
	void setUp() {
		priceService = new PriceServiceImpl();
	}

	@Test
	void testStartBatch() {
		String batchId = priceService.startBatch();
		assertNotNull(batchId);
		assertTrue(batchId.startsWith("BATCH-"));
	}

	@Test
	void testUploadChunkAndComplete() {
		String batchId = priceService.startBatch();

		List<PriceRecord> records = Arrays.asList(new PriceRecord("AAPL", Instant.now(), 150.25),
				new PriceRecord("GOOGL", Instant.now(), 2750.50));

		priceService.uploadChunk(batchId, records);
		assertTrue(priceService.completeBatch(batchId));

		PriceRecord applePrice = priceService.getLatestPrice("AAPL");
		assertNotNull(applePrice);
		assertEquals(150.25, applePrice.getPayload());
	}

	@Test
	void testChunkSizeValidation() {
		String batchId = priceService.startBatch();
		List<PriceRecord> largeChunk = new ArrayList<>();
		for (int i = 0; i < 1001; i++) {
			largeChunk.add(new PriceRecord("ID" + i, Instant.now(), i));
		}

		assertThrows(IllegalArgumentException.class, () -> priceService.uploadChunk(batchId, largeChunk));
	}

	@Test
	void testLatestPriceByAsOfTime() throws InterruptedException {
		String batchId = priceService.startBatch();

		Instant olderTime = Instant.now();
		Thread.sleep(10); // Ensure time difference
		Instant newerTime = Instant.now();

		// Older record first
		priceService.uploadChunk(batchId, Arrays.asList(new PriceRecord("MSFT", olderTime, 300.0)));
		priceService.uploadChunk(batchId, Arrays.asList(new PriceRecord("MSFT", newerTime, 305.0)));

		priceService.completeBatch(batchId);

		PriceRecord latest = priceService.getLatestPrice("MSFT");
		assertEquals(305.0, latest.getPayload());
		assertEquals(newerTime, latest.getAsOf());
	}

	@Test
	void testBatchCancellation() {
		String batchId = priceService.startBatch();

		priceService.uploadChunk(batchId, Arrays.asList(new PriceRecord("TSLA", Instant.now(), 700.0)));

		assertTrue(priceService.cancelBatch(batchId));
		assertNull(priceService.getLatestPrice("TSLA"));
	}

	@Test
	void testNoPartialDataVisibility() {
		String batchId = priceService.startBatch();

		priceService.uploadChunk(batchId, Arrays.asList(new PriceRecord("AMZN", Instant.now(), 3500.0)));

		// Data should not be visible before completion
		assertNull(priceService.getLatestPrice("AMZN"));

		priceService.completeBatch(batchId);

		// Data should be visible after completion
		assertNotNull(priceService.getLatestPrice("AMZN"));
	}

}

	@Test
	void testMultipleBatches() {
		// First batch
		String batch1 = priceService.startBatch();
		priceService.uploadChunk(batch1, Arrays.asList(new PriceRecord("NVDA", Instant.now().minusSeconds(60), 500.0)));
		priceService.completeBatch(batch1);

		// Second batch with newer price
		String batch2 = priceService.startBatch();
		priceService.uploadChunk(batch2, Arrays.asList(new PriceRecord("NVDA", Instant.now(), 510.0)));
		priceService.completeBatch(batch2);

		// Should have latest price from second batch
		PriceRecord latest = priceService.getLatestPrice("NVDA");
		assertEquals(510.0, latest.getPayload());
	}

	@Test
	@Timeout(5)
	void testConcurrentChunkUploads() throws InterruptedException {
		String batchId = priceService.startBatch();
		int threadCount = 10;
		int recordsPerThread = 100;

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		AtomicInteger recordCount = new AtomicInteger();

		// Create concurrent chunk uploads
		List<Callable<Void>> tasks = new ArrayList<>();
		for (int i = 0; i < threadCount; i++) {
			final int threadId = i;
			tasks.add(() -> {
				List<PriceRecord> records = new ArrayList<>();
				for (int j = 0; j < recordsPerThread; j++) {
					int recordId = threadId * recordsPerThread + j;
					records.add(new PriceRecord("ID-" + recordId, Instant.now(), recordId));
					recordCount.incrementAndGet();
				}
				priceService.uploadChunk(batchId, records);
				return null;

			});
		}

	}executor.invokeAll(tasks);executor.shutdown();

	assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
    
    priceService.completeBatch(batchId);
    
    // Verify all records are accessible
    List<String> allIds = new ArrayList<>();
    for (int i = 0; i < threadCount * recordsPerThread; i++) {
        allIds.add("ID-" + i);
    }
    
    Map<String, PriceRecord> latest = priceService.getLatestPrices(allIds);
    assertEquals(threadCount * recordsPerThread, latest.size());
}

	@Test
	void testGetLatestPricesForMultipleIds() {
		String batchId = priceService.startBatch();

		List<PriceRecord> records = Arrays.asList(new PriceRecord("AAPL", Instant.now(), 150.0),
				new PriceRecord("GOOGL", Instant.now(), 2750.0), new PriceRecord("MSFT", Instant.now(), 300.0));

		priceService.uploadChunk(batchId, records);
		priceService.completeBatch(batchId);
		Map<String, PriceRecord> latest = priceService
				.getLatestPrices(Arrays.asList("AAPL", "GOOGL", "MSFT", "UNKNOWN"));

		assertEquals(3, latest.size());
		assertTrue(latest.containsKey("AAPL"));
		assertTrue(latest.containsKey("GOOGL"));
		assertTrue(latest.containsKey("MSFT"));
		assertFalse(latest.containsKey("UNKNOWN"));
	}

	@Test
	void testResilienceToInvalidOperations() {
		// Try to upload to non-existent batch
		assertThrows(IllegalArgumentException.class, () -> {
			priceService.uploadChunk("NON_EXISTENT", Collections.emptyList());
		});

		// Try to complete non-existent batch
		assertFalse(priceService.completeBatch("NON_EXISTENT"));

		// Try to cancel non-existent batch
		assertFalse(priceService.cancelBatch("NON_EXISTENT"));
	}

	@Test
	void testBatchIdempotency() {
		String batchId = priceService.startBatch();

		// Complete twice
		assertTrue(priceService.completeBatch(batchId));
		assertFalse(priceService.completeBatch(batchId));

		// Try to upload after completion
		assertThrows(IllegalArgumentException.class, () -> {
			priceService.uploadChunk(batchId, Collections.singletonList(new PriceRecord("TEST", Instant.now(), 100.0)));

		});
	}
}
