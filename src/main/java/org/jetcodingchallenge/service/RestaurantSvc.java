package org.jetcodingchallenge.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetcodingchallenge.data.model.AddressDto;
import org.jetcodingchallenge.data.model.ApiRestaurantDto;
import org.jetcodingchallenge.data.model.CuisineDto;
import org.jetcodingchallenge.data.model.RestaurantDto;
import org.jetcodingchallenge.data.model.RestaurantResponseDto;
import org.jetcodingchallenge.data.model.SortBy;
import org.jetcodingchallenge.exception.ExternalApiException;
import org.jetcodingchallenge.exception.InvalidPostcodeException;
import org.jetcodingchallenge.exception.RestaurantNotFoundException;
import org.jetcodingchallenge.util.DistanceCalculator;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RestaurantSvc {

    private final RestClient restClient;
    private final GeocodingService geocodingService;
    
    // UK postcode regex pattern
    private static final Pattern UK_POSTCODE = 
            Pattern.compile("^[A-Z]{1,2}\\d{1,2}[A-Z]?\\s?\\d[A-Z]{2}$", Pattern.CASE_INSENSITIVE);

    public RestaurantSvc(RestClient.Builder builder, GeocodingService geocodingService) {
        this.restClient = builder
                .baseUrl("https://uk.api.just-eat.io/discovery/uk/")
                .build();
        this.geocodingService = geocodingService;
    }

    /**
     * Get restaurants by postcode with distance calculation
     * 
     * @param postcode UK postcode to search
     * @param sortBy Sorting option (distance, rating, or name)
     * @return List of up to 10 restaurants with flattened data structure
     */
    public List<RestaurantDto> getRestaurants(String postcode, SortBy sortBy) {
        // Validate postcode format
        if (!UK_POSTCODE.matcher(postcode).matches()) {
            throw new InvalidPostcodeException(postcode);
        }

        // Get coordinates for the search postcode (for distance calculation)
        Map<String, Double> searchCoords = geocodingService.getCoordinates(postcode);
        Double searchLat = searchCoords != null ? searchCoords.get("latitude") : null;
        Double searchLon = searchCoords != null ? searchCoords.get("longitude") : null;

        try {
            RestaurantResponseDto response = restClient.get()
                    .uri("/restaurants/enriched/bypostcode/{postcode}", postcode)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                        if (resp.getStatusCode().value() == 404) {
                            throw new RestaurantNotFoundException(postcode);
                        }
                        throw new InvalidPostcodeException(postcode);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, resp) -> {
                        throw new ExternalApiException("Just Eat API is currently unavailable");
                    })
                    .body(RestaurantResponseDto.class);

            // Handle empty response
            if (response == null || response.restaurants() == null || response.restaurants().isEmpty()) {
                return Collections.emptyList();
            }

            // Map API response to flattened DTO
            List<RestaurantDto> restaurants = response.restaurants()
                    .stream()
                    .map(apiRestaurant -> mapToDto(apiRestaurant, postcode, searchLat, searchLon))
                    .collect(Collectors.toList());

            // Sort based on user preference
            sortRestaurants(restaurants, sortBy);

            // Return first 10 results
            return restaurants.stream()
                    .limit(10)
                    .collect(Collectors.toList());

        } catch (RestaurantNotFoundException | InvalidPostcodeException | ExternalApiException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            throw new ExternalApiException("Error fetching restaurants: " + e.getMessage(), e);
        }
    }

    /**
     * Map API restaurant to flattened DTO
     */
    private RestaurantDto mapToDto(ApiRestaurantDto apiRestaurant, String searchPostcode, 
                                    Double searchLat, Double searchLon) {
        // Flatten cuisines to just names
        List<String> cuisineNames = apiRestaurant.cuisines() != null
                ? apiRestaurant.cuisines().stream()
                    .map(CuisineDto::name)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        // Flatten rating to just the number
        Double rating = apiRestaurant.rating() != null 
                ? apiRestaurant.rating().starRating() 
                : 0.0;

        // Calculate distance
        Double distanceMiles = calculateDistance(apiRestaurant, searchLat, searchLon);

        // Build address with distance context
        String address = buildAddress(apiRestaurant.address(), searchPostcode, distanceMiles);

        return new RestaurantDto(
                apiRestaurant.name(),
                cuisineNames,
                rating,
                address,
                distanceMiles
        );
    }

    /**
     * Calculate distance between search postcode and restaurant

     */
    private Double calculateDistance(ApiRestaurantDto restaurant, Double searchLat, Double searchLon) {
        // Try to calculate using coordinates
        if (searchLat != null && searchLon != null 
                && restaurant.address() != null 
                && restaurant.address().location() != null) {
            
            double restaurantLat = restaurant.address().location().getLatitude();
            double restaurantLon = restaurant.address().location().getLongitude();
            
            return DistanceCalculator.calculateDistanceMiles(
                    searchLat, searchLon, 
                    restaurantLat, restaurantLon
            );
        }
        
        return null;
    }

    /**
     * Build address string with distance context
     * Format: "71 Sturry Road, Canterbury, CT1 1BU (0.6 mi from CT1 2EH)"
     */
    private String buildAddress(AddressDto address, String searchPostcode, Double distanceMiles) {
        if (address == null) {
            return "Address not available";
        }

        StringBuilder sb = new StringBuilder();
        
        // Add street address
        if (address.firstLine() != null && !address.firstLine().isEmpty()) {
            sb.append(address.firstLine());
        }
        
        // Add city
        if (address.city() != null && !address.city().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.city());
        }
        
        // Add postcode
        if (address.postalCode() != null && !address.postalCode().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.postalCode());
        }
        
        // Add distance context if available and restaurant postcode differs from search
        if (distanceMiles != null && distanceMiles > 0.0) {
            boolean differentPostcode = address.postalCode() == null 
                    || !address.postalCode().equalsIgnoreCase(searchPostcode);
            
            if (differentPostcode) {
                sb.append(String.format(" (%.1f mi from %s)", distanceMiles, searchPostcode));
            }
        }
        
        return sb.toString();
    }

    /**
     * Sort restaurants based on user preference
     * Default sort is by distance (nearest first)
     */
    private void sortRestaurants(List<RestaurantDto> restaurants, SortBy sortBy) {
        if (sortBy == null) {
            sortBy = SortBy.DISTANCE; // Default to distance
        }

        switch (sortBy) {
            case DISTANCE:
                // Sort by distance (nearest first)
                // Null distances go to the end
                restaurants.sort(Comparator.comparing(
                    RestaurantDto::distanceMiles,
                    Comparator.nullsLast(Comparator.naturalOrder())
                ));
                break;

            case RATING:
                // Sort by rating (highest first)
                // Null ratings go to the end
                restaurants.sort(Comparator.comparing(
                    RestaurantDto::rating,
                    Comparator.nullsLast(Comparator.reverseOrder())
                ));
                break;

            case NAME:
                // Sort alphabetically by name
                restaurants.sort(Comparator.comparing(
                    RestaurantDto::name,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ));
                break;
        }
    }
}
