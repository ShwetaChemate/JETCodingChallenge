package org.jetcodingchallenge.data.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for mapping the raw API response (includes location coordinates for distance calculation)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RestaurantDto(
        @JsonProperty("name") String name,
        @JsonProperty("cuisines") List<CuisineDto> cuisines,
        @JsonProperty("rating") RatingDto rating,
        @JsonProperty("address") AddressDto address,
        @JsonProperty("driveDistanceMeters") Integer driveDistanceMeters // For distance calculation
) {}