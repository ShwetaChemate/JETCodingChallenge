package org.jetcodingchallenge.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Root response from Just Eat API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RestaurantResponseDto(
        @JsonProperty("restaurants") List<ApiRestaurantDto> restaurants
) {}
