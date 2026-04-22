package org.jetcodingchallenge.service;

import org.jetcodingchallenge.data.model.RestaurantDto;
import org.jetcodingchallenge.data.model.RestaurantResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantSvc {

    private final RestClient restClient;

    public RestaurantSvc(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://uk.api.just-eat.io/discovery/uk/")
                .build();
    }

    public List<RestaurantDto> getRestaurants(String postcode) {
        RestaurantResponseDto response =  restClient.get()
                .uri("/restaurants/enriched/bypostcode/{postcode}", postcode)
                .retrieve()
                .body(RestaurantResponseDto.class);

        return response.restaurants()
                .stream()
                .limit(10)
                .collect(Collectors.toList());
    }

}
