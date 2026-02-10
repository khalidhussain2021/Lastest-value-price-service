package com.hussain.serviceImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import com.hussain.dto.BatchContext;
import com.hussain.dto.BatchStatus;
import com.hussain.dto.PriceRecord;
import com.hussain.service.PriceService;

@Service
public class PriceServiceImpl implements PriceService{

	private final Map<String, BatchContext> batches = new ConcurrentHashMap<>();
	private final AtomicReference<Map<String, PriceRecord>> latestPrices =new AtomicReference<>(new HashMap<>());
	
	public String startBatch() {
		String batchId = UUID.randomUUID().toString();
        batches.put(batchId, new BatchContext());
		return batchId;
	}

	public void uploadPrices(String batchId, List<PriceRecord> prices) {
		BatchContext context = batches.get(batchId);

        if (context == null || context.status != BatchStatus.STARTED) {
            throw new IllegalStateException("Invalid batch state");
        }

        for (PriceRecord record : prices) {
            context.batchPrices.merge(
                    record.getId(),
                    record,
                    (oldVal, newVal) ->
                            newVal.getAsOf().isAfter(oldVal.getAsOf()) ? newVal : oldVal
            );
        }
	}

	public void completeBatch(String batchId) {
		
		BatchContext context = batches.get(batchId);
        if (context == null || context.status != BatchStatus.STARTED) {
            return;
        }
        synchronized (this) {
            Map<String, PriceRecord> merged =
                    new HashMap<>(latestPrices.get());

            for (PriceRecord record : context.batchPrices.values()) {
                merged.merge(
                        record.getId(),
                        record,
                        (oldVal, newVal) ->
                                newVal.getAsOf().isAfter(oldVal.getAsOf()) ? newVal : oldVal
                );
            }

            latestPrices.set(Collections.unmodifiableMap(merged));
            context.status = BatchStatus.COMPLETED;
        }
	}

	public void cancelBatch(String batchId) {
		BatchContext context = batches.get(batchId);
        if (context != null) {
            context.status = BatchStatus.CANCELLED;
            context.batchPrices.clear();
        }
	}

	public Map<String, PriceRecord> getLastPrices(List<String> ids) {
		Map<String, PriceRecord> snapshot = latestPrices.get();
        Map<String, PriceRecord> result = new HashMap<>();

        for (String id : ids) {
            if (snapshot.containsKey(id)) {
                result.put(id, snapshot.get(id));
            }
        }
        return result;
    }
	
}
