package org.jetcodingchallenge.data.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Location coordinates from API
 * Format: [longitude, latitude] - GeoJSON standard
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LocationDto(
        @JsonProperty("type") String type,
        @JsonProperty("coordinates") List<Double> coordinates // [longitude, latitude]
) {
    public double getLongitude() {
        return coordinates != null && coordinates.size() >= 2 ? coordinates.get(0) : 0.0;
    }
    
    public double getLatitude() {
        return coordinates != null && coordinates.size() >= 2 ? coordinates.get(1) : 0.0;
    }
}