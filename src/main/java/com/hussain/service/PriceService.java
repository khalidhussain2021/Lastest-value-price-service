package com.hussain.service;

import java.util.List;
import java.util.Map;

import com.hussain.dto.PriceRecord;

public interface PriceService {
	
	    String startBatch();
	    void uploadPrices(String batchId, List<PriceRecord> prices);
	    void completeBatch(String batchId);
	    void cancelBatch(String batchId);
	    Map<String, PriceRecord> getLastPrices(List<String> ids);
}
