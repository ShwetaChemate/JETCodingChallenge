package org.jetcodingchallenge.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Nested DTO for address
@JsonIgnoreProperties(ignoreUnknown = true)
public record AddressDto(
        @JsonProperty("firstLine") String firstLine,
        @JsonProperty("city") String city,
        @JsonProperty("postalCode") String postalCode,
        @JsonProperty("location") LocationDto location
) {}
