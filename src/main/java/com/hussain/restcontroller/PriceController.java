package com.hussain.restcontroller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hussain.dto.PriceRecord;
import com.hussain.service.PriceService;

@RestController
@RequestMapping("/prices")
public class PriceController {
	
	private final PriceService service;
    public PriceController(PriceService service) {
        this.service = service;
    }
    
    @PostMapping("/batch/start")
    public String startBatch() {
        return service.startBatch();
    }

    @PostMapping("/batch/{batchId}/upload")
    public void uploadPrices(
            @PathVariable String batchId,
            @RequestBody List<PriceRecord> prices) {
        service.uploadPrices(batchId, prices);
    }
    
    @PostMapping("/batch/{batchId}/complete")
    public void completeBatch(@PathVariable String batchId) {
        service.completeBatch(batchId);
    }

    @PostMapping("/batch/{batchId}/cancel")
    public void cancelBatch(@PathVariable String batchId) {
        service.cancelBatch(batchId);
    }

    @GetMapping
    public Map<String, PriceRecord> getLastPrices(
            @RequestParam List<String> ids) {
        return service.getLastPrices(ids);
    }
}
