package org.jetcodingchallenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jetcodingchallenge.data.model.RestaurantDto;
import org.jetcodingchallenge.data.model.RestaurantResponseDto;
import org.jetcodingchallenge.service.RestaurantSvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RestaurantController {

    @Autowired
    private RestaurantSvc restaurantSvc;

    @Operation(summary = "Get restaurants by postcode", description = "Returns first 10 restaurants")
    @ApiResponse(responseCode = "200", description = "List of restaurants")
    @GetMapping("/restaurants")
    public List<RestaurantDto> getRestaurants(@RequestParam String postcode){

        List<RestaurantDto> response = restaurantSvc.getRestaurants(postcode);
        return response;

    }

}
