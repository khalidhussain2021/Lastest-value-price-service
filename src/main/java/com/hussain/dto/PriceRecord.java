package com.hussain.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PriceRecord {
	private String id;
    private LocalDateTime asOf;
    private Map<String, Object> payload;
    
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PriceRecord other = (PriceRecord) obj;
		return Objects.equals(asOf, other.asOf) && Objects.equals(id, other.id)
				&& Objects.equals(payload, other.payload);
	}
	@Override
	public int hashCode() {
		return Objects.hash(asOf, id, payload);
	}
    
}
