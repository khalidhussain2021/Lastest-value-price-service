package com.hussain.dto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BatchContext {
	public volatile BatchStatus status = BatchStatus.STARTED;
    public Map<String, PriceRecord> batchPrices = new ConcurrentHashMap<>();
}
