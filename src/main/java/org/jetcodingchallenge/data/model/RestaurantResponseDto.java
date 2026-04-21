package org.jetcodingchallenge.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RestaurantResponseDto(
        @JsonProperty("restaurants") List<RestaurantDto> restaurants
) {}