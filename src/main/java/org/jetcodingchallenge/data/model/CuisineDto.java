package org.jetcodingchallenge.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Nested DTO for each cuisine entry
@JsonIgnoreProperties(ignoreUnknown = true)
public record CuisineDto(
        @JsonProperty("name") String name
) {}
