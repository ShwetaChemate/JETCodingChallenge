package org.jetcodingchallenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetcodingchallenge.data.model.RestaurantDto;
import org.jetcodingchallenge.data.model.SortBy;
import org.jetcodingchallenge.service.RestaurantSvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Restaurant API", description = "Search for restaurants by UK postcode")
public class RestaurantController {

    @Autowired
    private RestaurantSvc restaurantSvc;

    @Operation(
        summary = "Get restaurants by UK postcode",
        description = """
            Returns up to 10 restaurants near the provided UK postcode.
            Each restaurant includes:
            - Name
            - Cuisines (flattened list of cuisine names)
            - Rating (as a number, e.g., 5.0)
            - Address (formatted with distance from search postcode)
            - Distance in miles
            
            Results are sorted by distance (nearest first) by default.
            You can change sorting using the sortBy parameter.
            
            Example: GET /api/restaurants?postcode=CT1 2EH&sortBy=rating
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved restaurants",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestaurantDto.class),
                examples = @ExampleObject(
                    value = """
                        [
                          {
                            "name": "Canterbury Fishbar",
                            "cuisines": ["Fish & Chips", "Chicken"],
                            "rating": 5.0,
                            "address": "71 Sturry Road, Canterbury, CT1 1BU (0.6 mi from CT1 2EH)",
                            "distanceMiles": 0.62
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid postcode format",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"error\": \"Invalid UK postcode format: INVALID\"}")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No restaurants found for postcode",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"error\": \"No restaurants found for postcode: ZZ99 9ZZ\"}")
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "External API unavailable",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"error\": \"Just Eat API is currently unavailable\"}")
            )
        )
    })
    @GetMapping("/restaurants")
    public ResponseEntity<List<RestaurantDto>> getRestaurants(
            @Parameter(
                description = "UK postcode (e.g., 'CT1 2EH', 'SW1A 1AA', 'M1 1AE')",
                example = "CT1 2EH",
                required = true
            )
            @RequestParam String postcode,
            
            @Parameter(
                description = "Sort order: DISTANCE (nearest first - default), RATING (highest first), or NAME (alphabetical)",
                example = "distance"
            )
            @RequestParam(required = false, defaultValue = "distance") String sortBy
    ) {
        // Convert string to enum, default to DISTANCE if invalid
        SortBy sort;
        try {
            sort = SortBy.valueOf(sortBy.toUpperCase());
        } catch (IllegalArgumentException e) {
            sort = SortBy.DISTANCE;
        }
        
        List<RestaurantDto> restaurants = restaurantSvc.getRestaurants(postcode, sort);
        return ResponseEntity.ok(restaurants);
    }
}
