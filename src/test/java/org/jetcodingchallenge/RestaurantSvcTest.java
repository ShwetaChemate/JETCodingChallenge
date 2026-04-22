package org.jetcodingchallenge;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.jetcodingchallenge.data.model.AddressDto;
import org.jetcodingchallenge.data.model.ApiRestaurantDto;
import org.jetcodingchallenge.data.model.CuisineDto;
import org.jetcodingchallenge.data.model.LocationDto;
import org.jetcodingchallenge.data.model.RatingDto;
import org.jetcodingchallenge.data.model.RestaurantDto;
import org.jetcodingchallenge.data.model.RestaurantResponseDto;
import org.jetcodingchallenge.data.model.SortBy;
import org.jetcodingchallenge.exception.ExternalApiException;
import org.jetcodingchallenge.exception.InvalidPostcodeException;
import org.jetcodingchallenge.exception.RestaurantNotFoundException;
import org.jetcodingchallenge.service.GeocodingService;
import org.jetcodingchallenge.service.RestaurantSvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class RestaurantSvcTest {

    @Mock private RestClient restClient;
    @Mock private RestClient.Builder restClientBuilder;
    @Mock private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
    @Mock private RestClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock private RestClient.ResponseSpec responseSpec;
    @Mock private GeocodingService geocodingService;

    private RestaurantSvc restaurantSvc;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);
        restaurantSvc = new RestaurantSvc(restClientBuilder, geocodingService);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void mockApiResponse(RestaurantResponseDto responseDto) {
        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), anyString());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(RestaurantResponseDto.class)).thenReturn(responseDto);
    }

    private void mockApiException(Exception ex) {
        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), anyString());
        when(requestHeadersSpec.retrieve()).thenThrow(ex);
    }

    private ApiRestaurantDto buildApiRestaurant(String name, double rating,
                                                double lat, double lon,
                                                String... cuisines) {
        LocationDto location = new LocationDto("Point", List.of(lon, lat));
        AddressDto address = new AddressDto("1 High St", "London", "EC1A 1BB", location);
        RatingDto ratingDto = new RatingDto(rating);
        List<CuisineDto> cuisineDtos = java.util.Arrays.stream(cuisines)
                .map(CuisineDto::new)
                .toList();
        return new ApiRestaurantDto(name, cuisineDtos, ratingDto, address);
    }

    // ── Postcode Validation ───────────────────────────────────────────────────

    @Test
    void getRestaurants_invalidPostcode_throwsInvalidPostcodeException() {
        assertThatThrownBy(() -> restaurantSvc.getRestaurants("INVALID", SortBy.DISTANCE))
                .isInstanceOf(InvalidPostcodeException.class);
    }

    @Test
    void getRestaurants_emptyPostcode_throwsInvalidPostcodeException() {
        assertThatThrownBy(() -> restaurantSvc.getRestaurants("", SortBy.DISTANCE))
                .isInstanceOf(InvalidPostcodeException.class);
    }

    @Test
    void getRestaurants_validPostcode_doesNotThrowValidationError() {
        mockApiResponse(new RestaurantResponseDto(List.of()));
        when(geocodingService.getCoordinates(anyString())).thenReturn(null);

        assertThatNoException().isThrownBy(() ->
                restaurantSvc.getRestaurants("EC1A 1BB", SortBy.DISTANCE));
    }

    // ── Empty / Null Response ─────────────────────────────────────────────────

    @Test
    void getRestaurants_nullResponse_returnsEmptyList() {
        mockApiResponse(null);
        when(geocodingService.getCoordinates(anyString())).thenReturn(null);

        List<RestaurantDto> result = restaurantSvc.getRestaurants("EC1A 1BB", SortBy.DISTANCE);

        assertThat(result).isEmpty();
    }

    @Test
    void getRestaurants_emptyRestaurantList_returnsEmptyList() {
        mockApiResponse(new RestaurantResponseDto(List.of()));
        when(geocodingService.getCoordinates(anyString())).thenReturn(null);

        List<RestaurantDto> result = restaurantSvc.getRestaurants("EC1A 1BB", SortBy.DISTANCE);

        assertThat(result).isEmpty();
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    @Test
    void getRestaurants_validResponse_mapsNameCorrectly() {
        ApiRestaurantDto api = buildApiRestaurant("Pizza Palace", 4.5, 51.5, -0.1, "Italian");
        mockApiResponse(new RestaurantResponseDto(List.of(api)));
        when(geocodingService.getCoordinates(anyString())).thenReturn(null);

        List<RestaurantDto> result = restaurantSvc.getRestaurants("EC1A 1BB", SortBy.NAME);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Pizza Palace");
    }

    @Test
    void getRestaurants_validResponse_flattensCuisinesToStringList() {
        ApiRestaurantDto api = buildApiRestaurant("Burger Bar", 4.0, 51.5, -0.1, "American", "Burgers");
        mockApiResponse(new RestaurantResponseDto(List.of(api)));
        when(geocodingService.getCoordinates(anyString())).thenReturn(null);

        List<RestaurantDto> result = restaurantSvc.getRestaurants("EC1A 1BB", SortBy.NAME);

        assertThat(result.get(0).cuisines()).containsExactlyInAnyOrder("American", "Burgers");
    }

    @Test
    void getRestaurants_validResponse_flattensRatingToDouble() {
        ApiRestaurantDto api = buildApiRestaurant("Sushi Spot", 4.8, 51.5, -0.1, "Japanese");
        mockApiResponse(new RestaurantResponseDto(List.of(api)));
        when(geocodingService.getCoordinates(anyString())).thenReturn(null);

        List<RestaurantDto> result = restaurantSvc.getRestaurants("EC1A 1BB", SortBy.RATING);

        assertThat(result.get(0).rating()).isEqualTo(4.8);
    }

    @Test
    void getRestaurants_nullRating_defaultsToZero() {
        LocationDto location = new LocationDto("Point", List.of(-0.1, 51.5));
        AddressDto address = new AddressDto("1 High St", "London", "EC1A 1BB", location);
        ApiRestaurantDto api = new ApiRestaurantDto("No Rating Place", List.of(), null, address);
        mockApiResponse(new RestaurantResponseDto(List.of(api)));
        when(geocodingService.getCoordinates(anyString())).thenReturn(null);

        List<RestaurantDto> result = restaurantSvc.getRestaurants("EC1A 1BB", SortBy.RATING);

        assertThat(result.get(0).rating()).isEqualTo(0.0);
    }

    @Test
    void getRestaurants_nullCuisines_returnsEmptyCuisineList() {
        LocationDto location = new LocationDto("Point", List.of(-0.1, 51.5));
        AddressDto address = new AddressDto("1 High St", "London", "EC1A 1BB", location);
        ApiRestaurantDto api = new ApiRestaurantDto("No Cuisine", null, new RatingDto(3.0), address);
        mockApiResponse(new RestaurantResponseDto(List.of(api)));
        when(geocodingService.getCoordinates(anyString())).thenReturn(null);

        List<RestaurantDto> result = restaurantSvc.getRestaurants("EC1A 1BB", SortBy.NAME);

        assertThat(result.get(0).cuisines()).isEmpty();
    }

    // ── Limit to 10 ───────────────────────────────────────────────────────────

    @Test
    void getRestaurants_moreThan10Results_returnsOnly10() {
        List<ApiRestaurantDto> apiList = java.util.stream.IntStream.rangeClosed(1, 15)
                .mapToObj(i -> buildApiRestaurant("Restaurant " + i, 4.0, 51.5 + i * 0.01, -0.1, "Italian"))
                .toList();
        mockApiResponse(new RestaurantResponseDto(apiList));
        when(geocodingService.getCoordinates(anyString()))
                .thenReturn(Map.of("latitude", 51.5, "longitude", -0.1));

        List<RestaurantDto> result = restaurantSvc.getRestaurants("EC1A 1BB", SortBy.DISTANCE);

        assertThat(result).hasSize(10);
    }

    // ── Sorting ───────────────────────────────────────────────────────────────

    @Test
    void getRestaurants_sortByName_returnsSortedAlphabetically() {
        List<ApiRestaurantDto> apiList = List.of(
                buildApiRestaurant("Zara's", 4.0, 51.51, -0.1, "Italian"),
                buildApiRestaurant("Apollo", 3.5, 51.52, -0.1, "Greek"),
                buildApiRestaurant("Mango", 4.5, 51.53, -0.1, "Indian")
        );
        mockApiResponse(new RestaurantResponseDto(apiList));
        when(geocodingService.getCoordinates(anyString())).thenReturn(null);

        List<RestaurantDto> result = restaurantSvc.getRestaurants("EC1A 1BB", SortBy.NAME);

        assertThat(result).extracting(RestaurantDto::name)
                .containsExactly("Apollo", "Mango", "Zara's");
    }

    @Test
    void getRestaurants_sortByRating_returnsHighestFirst() {
        List<ApiRestaurantDto> apiList = List.of(
                buildApiRestaurant("Good Place", 3.5, 51.51, -0.1, "Italian"),
                buildApiRestaurant("Best Place", 5.0, 51.52, -0.1, "French"),
                buildApiRestaurant("Okay Place", 2.0, 51.53, -0.1, "Thai")
        );
        mockApiResponse(new RestaurantResponseDto(apiList));
        when(geocodingService.getCoordinates(anyString())).thenReturn(null);

        List<RestaurantDto> result = restaurantSvc.getRestaurants("EC1A 1BB", SortBy.RATING);

        assertThat(result).extracting(RestaurantDto::rating)
                .containsExactly(5.0, 3.5, 2.0);
    }

    @Test
    void getRestaurants_sortByDistance_returnsNearestFirst() {
        List<ApiRestaurantDto> apiList = List.of(
                buildApiRestaurant("Far Away",  4.0, 51.6, -0.1, "Italian"),  // far
                buildApiRestaurant("Very Near", 4.0, 51.501, -0.1, "Greek"),  // nearest
                buildApiRestaurant("Midway",    4.0, 51.55, -0.1, "Thai")     // middle
        );
        mockApiResponse(new RestaurantResponseDto(apiList));
        when(geocodingService.getCoordinates(anyString()))
                .thenReturn(Map.of("latitude", 51.5, "longitude", -0.1));

        List<RestaurantDto> result = restaurantSvc.getRestaurants("EC1A 1BB", SortBy.DISTANCE);

        assertThat(result.get(0).name()).isEqualTo("Very Near");
        assertThat(result.get(2).name()).isEqualTo("Far Away");
    }

    @Test
    void getRestaurants_nullSortBy_defaultsToDistanceSort() {
        List<ApiRestaurantDto> apiList = List.of(
                buildApiRestaurant("Far",  4.0, 51.6,   -0.1, "Italian"),
                buildApiRestaurant("Near", 4.0, 51.501, -0.1, "Greek")
        );
        mockApiResponse(new RestaurantResponseDto(apiList));
        when(geocodingService.getCoordinates(anyString()))
                .thenReturn(Map.of("latitude", 51.5, "longitude", -0.1));

        List<RestaurantDto> result = restaurantSvc.getRestaurants("EC1A 1BB", null);

        assertThat(result.get(0).name()).isEqualTo("Near");
    }

    // ── Exception Handling ────────────────────────────────────────────────────

    @Test
    void getRestaurants_apiThrows404_throwsRestaurantNotFoundException() {
        mockApiException(new RestaurantNotFoundException("EC1A 1BB"));
        when(geocodingService.getCoordinates(anyString())).thenReturn(null);

        assertThatThrownBy(() -> restaurantSvc.getRestaurants("EC1A 1BB", SortBy.DISTANCE))
                .isInstanceOf(RestaurantNotFoundException.class);
    }

    @Test
    void getRestaurants_apiThrows5xx_throwsExternalApiException() {
        mockApiException(new ExternalApiException("Just Eat API is currently unavailable"));
        when(geocodingService.getCoordinates(anyString())).thenReturn(null);

        assertThatThrownBy(() -> restaurantSvc.getRestaurants("EC1A 1BB", SortBy.DISTANCE))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    void getRestaurants_unexpectedException_wrapsInExternalApiException() {
        mockApiException(new RuntimeException("Unexpected error"));
        when(geocodingService.getCoordinates(anyString())).thenReturn(null);

        assertThatThrownBy(() -> restaurantSvc.getRestaurants("EC1A 1BB", SortBy.DISTANCE))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Error fetching restaurants");
    }

    // ── Distance Calculation ──────────────────────────────────────────────────

    @Test
    void getRestaurants_geocodingFails_distanceIsNull() {
        ApiRestaurantDto api = buildApiRestaurant("Test Place", 4.0, 51.5, -0.1, "Italian");
        mockApiResponse(new RestaurantResponseDto(List.of(api)));
        when(geocodingService.getCoordinates(anyString())).thenReturn(null); // geocoding fails

        List<RestaurantDto> result = restaurantSvc.getRestaurants("EC1A 1BB", SortBy.NAME);

        assertThat(result.get(0).distanceMiles()).isNull();
    }

    @Test
    void getRestaurants_geocodingSucceeds_distanceIsCalculated() {
        ApiRestaurantDto api = buildApiRestaurant("Near Place", 4.0, 51.501, -0.099, "Italian");
        mockApiResponse(new RestaurantResponseDto(List.of(api)));
        when(geocodingService.getCoordinates(anyString()))
                .thenReturn(Map.of("latitude", 51.5, "longitude", -0.1));

        List<RestaurantDto> result = restaurantSvc.getRestaurants("EC1A 1BB", SortBy.DISTANCE);

        assertThat(result.get(0).distanceMiles()).isNotNull().isGreaterThan(0.0);
    }
}
