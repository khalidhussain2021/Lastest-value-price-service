
package com.hussain.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages batch state and temporary storage for batch operations. Thread-safe
 * to allow parallel chunk uploads.
 */
public class BatchContext {
	private final String batchId;
	private final AtomicBoolean completed = new AtomicBoolean(false);
	private final AtomicBoolean cancelled = new AtomicBoolean(false);
	private final Map<String, PriceRecord> batchRecords = new ConcurrentHashMap<>();

	public BatchContext(String batchId) {
		this.batchId = Objects.requireNonNull(batchId);
	}

	public String getBatchId() {
		return batchId;
	}

	public boolean isCompleted() {
		return completed.get();
	}

	public boolean isCancelled() {
		return cancelled.get();
	}

	public void addRecord(PriceRecord record) {
		if (isCompleted() || isCancelled()) {
			throw new IllegalStateException("Batch " + batchId + " is already completed or cancelled");
		}

		// Only keep the latest record per id within the batch
		PriceRecord existing = batchRecords.get(record.getId());
		if (existing == null || record.getAsOf().isAfter(existing.getAsOf())) {
			batchRecords.put(record.getId(), record);
		}
	}

	public boolean complete() {
		if (cancelled.get()) {
			return false;
		}
		return completed.compareAndSet(false, true);
	}

	public boolean cancel() {
		if (completed.get()) {
			return false;
		}
		boolean success = cancelled.compareAndSet(false, true);
		if (success) {
			batchRecords.clear();
		}
		return success;
	}

	public Map<String, PriceRecord> getRecords() {
		if (!isCompleted()) {
			throw new IllegalStateException("Batch is not completed yet");
		}
		return new HashMap<>(batchRecords);
	}
}
