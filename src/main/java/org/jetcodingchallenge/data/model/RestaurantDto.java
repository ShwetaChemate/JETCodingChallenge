package org.jetcodingchallenge.data.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Flattened Restaurant DTO for API response
 * Simplified structure with only required fields
 */
public record RestaurantDto(
        @JsonProperty("name") String name,
        @JsonProperty("cuisines") List<String> cuisines, // Flattened to just names
        @JsonProperty("rating") Double rating,           // Flattened to just the number
        @JsonProperty("address") String address,         // Joined address with distance context
        @JsonIgnore Double distanceMiles // Additional field for sorting/filtering
) {}
