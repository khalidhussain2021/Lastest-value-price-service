package com.hussain.service;

import java.util.List;
import java.util.Map;

import com.hussain.dto.PriceRecord;

public interface PriceService {
	
	    String startBatch();
	    void uploadPrices(String batchId, List<PriceRecord> prices);
	    boolean completeBatch(String batchId);
	    boolean cancelBatch(String batchId);
	    Map<String, PriceRecord> getLastPrices(List<String> ids);
	    PriceRecord getLatestPrice(String id);
}
