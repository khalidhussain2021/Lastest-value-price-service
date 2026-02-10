package com.hussain.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceRecord {
	private String id;
    private LocalDateTime asOf;
    private Map<String, Object> payload;
}
