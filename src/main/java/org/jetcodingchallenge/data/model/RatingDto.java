package org.jetcodingchallenge.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Nested DTO for rating
@JsonIgnoreProperties(ignoreUnknown = true)
public record RatingDto(
        @JsonProperty("starRating") double starRating
) {}
