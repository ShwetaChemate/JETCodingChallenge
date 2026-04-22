package org.jetcodingchallenge.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service to get coordinates for a given UK postcode
 * Uses postcodes.io free API
 */
@Service
public class GeocodingService {

    private final RestClient restClient;

    public GeocodingService(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://api.postcodes.io")
                .build();
    }

    /**
     * Get latitude and longitude for a UK postcode
     * 
     * @param postcode UK postcode (e.g., "CT1 2EH")
     * @return Map with "latitude" and "longitude" keys, or null if not found
     * 
     * Example response for CT1 2EH:
     * {
     *   "latitude": 51.280233,
     *   "longitude": 1.078151
     * }
     */
    public Map<String, Double> getCoordinates(String postcode) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/postcodes/{postcode}", postcode.replace(" ", ""))
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.get("result") instanceof Map) {
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                Double latitude = ((Number) result.get("latitude")).doubleValue();
                Double longitude = ((Number) result.get("longitude")).doubleValue();
                
                return Map.of(
                    "latitude", latitude,
                    "longitude", longitude
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to geocode postcode " + postcode + ": " + e.getMessage());
        }
        
        return null;
    }
}