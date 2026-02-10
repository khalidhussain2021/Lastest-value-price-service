package com.hussain.serviceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.hussain.dto.BatchContext;
import com.hussain.dto.PriceRecord;
import com.hussain.service.PriceService;

@Service
public class PriceServiceImpl implements PriceService {

	private final Map<String, BatchContext> activeBatches = new ConcurrentHashMap<>();

	// Latest prices by instrument ID (only from completed batches)
	private final Map<String, PriceRecord> latestPrices = new ConcurrentHashMap<>();

	// For generating unique batch IDs
	private final AtomicLong batchCounter = new AtomicLong(0);

	// Maximum chunk size as per requirements
	private static final int MAX_CHUNK_SIZE = 1000;

	@Override
	public String startBatch() {
		String batchId = "BATCH-" + batchCounter.incrementAndGet();
		activeBatches.put(batchId, new BatchContext(batchId));
		return batchId;
	}

	@Override
	public void uploadPrices(String batchId, List<PriceRecord> prices) {
		validateChunkSize(prices);
		BatchContext batch = activeBatches.get(batchId);
		if (batch == null) {
			throw new IllegalArgumentException("Batch not found: " + batchId);
		}
		// Add all records to the batch (handles concurrency internally)
		for (PriceRecord record : prices) {
			batch.addRecord(record);
		}
	}

	@Override
	public boolean completeBatch(String batchId) {
		BatchContext batch = activeBatches.remove(batchId);
		if (batch == null) {
			return false; // Batch not found or already removed
		}

		if (batch.complete()) {
			// Atomically update latest prices with batch records
			updateLatestPrices(batch.getRecords());
			return true;
		}
		return false;
	}

	@Override
	public boolean cancelBatch(String batchId) {
		BatchContext batch = activeBatches.remove(batchId);
		if (batch == null) {
			return false;
		}
		return batch.cancel();
	}

	@Override
	public Map<String, PriceRecord> getLastPrices(List<String> ids) {
		Map<String, PriceRecord> snapshot = new HashMap<>(latestPrices);

		return ids.stream().distinct().filter(snapshot::containsKey).collect(Collectors.toMap(id -> id, snapshot::get));
	}

	@Override
	public PriceRecord getLatestPrice(String id) {
		return latestPrices.get(id);
	}

	private void updateLatestPrices(Map<String, PriceRecord> batchRecords) {
		for (Map.Entry<String, PriceRecord> entry : batchRecords.entrySet()) {
			latestPrices.merge(entry.getKey(), entry.getValue(),
					(existing, incoming) -> incoming.getAsOf().isAfter(existing.getAsOf()) ? incoming : existing);
		}
	}

	private void validateChunkSize(List<PriceRecord> records) {
		if (records == null) {
			throw new IllegalArgumentException("Records list cannot be null");
		}
		if (records.size() > MAX_CHUNK_SIZE) {
			throw new IllegalArgumentException(
					String.format("Chunk size %d exceeds maximum of %d", records.size(), MAX_CHUNK_SIZE));
		}
	}

	Map<String, BatchContext> getActiveBatches() {
		return new HashMap<>(activeBatches);
	}
}
