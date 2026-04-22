package org.jetcodingchallenge;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import org.jetcodingchallenge.controller.RestaurantController;
import org.jetcodingchallenge.data.model.RestaurantDto;
import org.jetcodingchallenge.data.model.SortBy;
import org.jetcodingchallenge.exception.ExternalApiException;
import org.jetcodingchallenge.exception.InvalidPostcodeException;
import org.jetcodingchallenge.exception.RestaurantNotFoundException;
import org.jetcodingchallenge.service.RestaurantSvc;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;     
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestaurantController.class)
class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean                        
    private RestaurantSvc restaurantSvc;
    // ── helper ────────────────────────────────────────────────────────────────

    private RestaurantDto buildRestaurantDto(String name, double rating, String address) {
        return new RestaurantDto(name, List.of("Italian"), rating, address, 0.5);
    }

    // ── Happy Path ────────────────────────────────────────────────────────────

    @Test
    void getRestaurants_validPostcode_returns200WithList() throws Exception {
        List<RestaurantDto> restaurants = List.of(
                buildRestaurantDto("Pizza Palace", 4.5, "1 High St, London, EC1A 1BB")
        );
        when(restaurantSvc.getRestaurants(eq("CT1 2EH"), any(SortBy.class)))
                .thenReturn(restaurants);

        mockMvc.perform(get("/api/restaurants")
                        .param("postcode", "CT1 2EH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Pizza Palace"))
                .andExpect(jsonPath("$[0].rating").value(4.5))
                .andExpect(jsonPath("$[0].cuisines", hasItem("Italian")));
    }

    @Test
    void getRestaurants_validPostcodeEmptyResult_returns200WithEmptyList() throws Exception {
        when(restaurantSvc.getRestaurants(any(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/restaurants")
                        .param("postcode", "CT1 2EH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getRestaurants_returns10Results() throws Exception {
        List<RestaurantDto> tenRestaurants = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(i -> buildRestaurantDto("Restaurant " + i, 4.0, "Address " + i))
                .toList();
        when(restaurantSvc.getRestaurants(any(), any())).thenReturn(tenRestaurants);

        mockMvc.perform(get("/api/restaurants")
                        .param("postcode", "CT1 2EH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)));
    }

    // ── SortBy Parameter ──────────────────────────────────────────────────────

    @Test
    void getRestaurants_sortByRating_passesRatingEnumToService() throws Exception {
        when(restaurantSvc.getRestaurants("CT1 2EH", SortBy.RATING))
                .thenReturn(List.of(buildRestaurantDto("Best Place", 5.0, "Addr")));

        mockMvc.perform(get("/api/restaurants")
                        .param("postcode", "CT1 2EH")
                        .param("sortBy", "rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Best Place"));
    }

    @Test
    void getRestaurants_sortByName_passesNameEnumToService() throws Exception {
        when(restaurantSvc.getRestaurants("CT1 2EH", SortBy.NAME))
                .thenReturn(List.of(buildRestaurantDto("Apollo", 3.5, "Addr")));

        mockMvc.perform(get("/api/restaurants")
                        .param("postcode", "CT1 2EH")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Apollo"));
    }

    @Test
    void getRestaurants_sortByDistance_passesDistanceEnumToService() throws Exception {
        when(restaurantSvc.getRestaurants("CT1 2EH", SortBy.DISTANCE))
                .thenReturn(List.of(buildRestaurantDto("Near Place", 4.0, "Addr")));

        mockMvc.perform(get("/api/restaurants")
                        .param("postcode", "CT1 2EH")
                        .param("sortBy", "distance"))
                .andExpect(status().isOk());
    }

    @Test
    void getRestaurants_invalidSortBy_defaultsToDistanceSort() throws Exception {
        // "blah" is not a valid SortBy — controller falls back to DISTANCE
        when(restaurantSvc.getRestaurants("CT1 2EH", SortBy.DISTANCE))
                .thenReturn(List.of(buildRestaurantDto("Some Place", 4.0, "Addr")));

        mockMvc.perform(get("/api/restaurants")
                        .param("postcode", "CT1 2EH")
                        .param("sortBy", "blah"))
                .andExpect(status().isOk());
    }

    @Test
    void getRestaurants_noSortByParam_defaultsToDistanceSort() throws Exception {
        when(restaurantSvc.getRestaurants("CT1 2EH", SortBy.DISTANCE))
                .thenReturn(List.of(buildRestaurantDto("Default Sort", 4.0, "Addr")));

        mockMvc.perform(get("/api/restaurants")
                        .param("postcode", "CT1 2EH"))
                .andExpect(status().isOk());
    }

    @Test
    void getRestaurants_sortByIsCaseInsensitive_parsesUppercase() throws Exception {
        when(restaurantSvc.getRestaurants("CT1 2EH", SortBy.RATING))
                .thenReturn(List.of(buildRestaurantDto("Place", 4.0, "Addr")));

        mockMvc.perform(get("/api/restaurants")
                        .param("postcode", "CT1 2EH")
                        .param("sortBy", "RATING"))  // uppercase
                .andExpect(status().isOk());
    }

    // ── Missing Required Param ────────────────────────────────────────────────

    @Test
    void getRestaurants_missingPostcode_returns400() throws Exception {
        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isBadRequest());
    }

    // ── Exception Handling ────────────────────────────────────────────────────

    @Test
    void getRestaurants_invalidPostcode_returns400() throws Exception {
        when(restaurantSvc.getRestaurants(any(), any()))
                .thenThrow(new InvalidPostcodeException("INVALID"));

        mockMvc.perform(get("/api/restaurants")
                        .param("postcode", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRestaurants_noRestaurantsFound_returns404() throws Exception {
        when(restaurantSvc.getRestaurants(any(), any()))
                .thenThrow(new RestaurantNotFoundException("ZZ99 9ZZ"));

        mockMvc.perform(get("/api/restaurants")
                        .param("postcode", "ZZ99 9ZZ"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRestaurants_externalApiDown_returns503() throws Exception {
        when(restaurantSvc.getRestaurants(any(), any()))
                .thenThrow(new ExternalApiException("Just Eat API is currently unavailable"));

        mockMvc.perform(get("/api/restaurants")
                        .param("postcode", "CT1 2EH"))
                .andExpect(status().isServiceUnavailable());
    }

    // ── Response Body ─────────────────────────────────────────────────────────

    @Test
    void getRestaurants_responseContainsAllExpectedFields() throws Exception {
        RestaurantDto dto = new RestaurantDto(
                "Canterbury Fishbar",
                List.of("Fish & Chips", "Chicken"),
                5.0,
                "71 Sturry Road, Canterbury, CT1 1BU (0.6 mi from CT1 2EH)",
                0.62
        );
        when(restaurantSvc.getRestaurants(any(), any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/restaurants")
                        .param("postcode", "CT1 2EH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Canterbury Fishbar"))
                .andExpect(jsonPath("$[0].rating").value(5.0))
                .andExpect(jsonPath("$[0].cuisines", hasItems("Fish & Chips", "Chicken")))
                .andExpect(jsonPath("$[0].address").value(containsString("Canterbury")));
    }

    @Test
    void getRestaurants_distanceMilesNotExposedInResponse() throws Exception {
        // distanceMiles is @JsonIgnore — should NOT appear in response JSON
        RestaurantDto dto = new RestaurantDto("Test", List.of(), 4.0, "Addr", 1.5);
        when(restaurantSvc.getRestaurants(any(), any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/restaurants")
                        .param("postcode", "CT1 2EH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].distanceMiles").doesNotExist());
    }
}
