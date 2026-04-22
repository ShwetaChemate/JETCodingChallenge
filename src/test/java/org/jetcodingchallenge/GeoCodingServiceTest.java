package org.jetcodingchallenge;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.jetcodingchallenge.service.GeocodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class GeocodingServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);
        geocodingService = new GeocodingService(restClientBuilder);
    }

    private void mockResponse(Object responseBody) {
        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), anyString());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn((Map) responseBody);
    }

    // ─── Happy Path ───────────────────────────────────────────────────────────

    @Test
    void getCoordinates_validPostcode_returnsLatLon() {
        Map<String, Object> apiResponse = Map.of(
            "status", 200,
            "result", Map.of("latitude", 51.280233, "longitude", 1.078151)
        );
        mockResponse(apiResponse);

        Map<String, Double> result = geocodingService.getCoordinates("CT1 2EH");

        assertThat(result).isNotNull();
        assertThat(result.get("latitude")).isEqualTo(51.280233);
        assertThat(result.get("longitude")).isEqualTo(1.078151);
    }

    @Test
    void getCoordinates_postcodeWithSpace_stripsSpaceBeforeCallingApi() {
        Map<String, Object> apiResponse = Map.of(
            "status", 200,
            "result", Map.of("latitude", 51.501009, "longitude", -0.141588)
        );
        mockResponse(apiResponse);

        Map<String, Double> result = geocodingService.getCoordinates("SW1A 1AA");

        assertThat(result).isNotNull();
        // verify space was stripped in the URI
        verify(requestHeadersUriSpec).uri(anyString(), eq("SW1A1AA"));
    }

    // ─── Null / Empty Response ────────────────────────────────────────────────

    @Test
    void getCoordinates_nullResponse_returnsNull() {
        mockResponse(null);

        Map<String, Double> result = geocodingService.getCoordinates("ZZ99ZZ");

        assertThat(result).isNull();
    }

    @Test
    void getCoordinates_resultIsNotMap_returnsNull() {
        mockResponse(Map.of("status", 200, "result", "unexpected_string"));

        Map<String, Double> result = geocodingService.getCoordinates("XX11XX");

        assertThat(result).isNull();
    }

    @Test
    void getCoordinates_emptyBody_returnsNull() {
        mockResponse(Map.of());

        Map<String, Double> result = geocodingService.getCoordinates("XX11XX");

        assertThat(result).isNull();
    }

    // ─── Error Scenarios ──────────────────────────────────────────────────────

    @Test
    void getCoordinates_apiThrowsException_returnsNull() {
        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), anyString());
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("Connection refused"));

        Map<String, Double> result = geocodingService.getCoordinates("TE1 1ST");

        // Must NOT throw — exception is swallowed internally
        assertThat(result).isNull();
    }

    @Test
    void getCoordinates_apiReturns404_returnsNull() {
        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), anyString());
        when(requestHeadersSpec.retrieve()).thenThrow(
            new org.springframework.web.client.HttpClientErrorException(
                org.springframework.http.HttpStatus.NOT_FOUND
            )
        );

        Map<String, Double> result = geocodingService.getCoordinates("ZZ11ZZ");

        assertThat(result).isNull();
    }

    // ─── Edge Cases ───────────────────────────────────────────────────────────

    @Test
    void getCoordinates_integerCoordinatesInResponse_parsedAsDouble() {
        Map<String, Object> apiResponse = Map.of(
            "status", 200,
            "result", Map.of("latitude", 53, "longitude", -2)  // integers, not doubles
        );
        mockResponse(apiResponse);

        Map<String, Double> result = geocodingService.getCoordinates("M1 1AE");

        assertThat(result).isNotNull();
        assertThat(result.get("latitude")).isEqualTo(53.0);
        assertThat(result.get("longitude")).isEqualTo(-2.0);
    }
}