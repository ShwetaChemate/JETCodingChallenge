package org.jetcodingchallenge;

import org.jetcodingchallenge.util.DistanceCalculator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * Test cases for DistanceCalculator demonstrating the Haversine formula
 */
class DistanceCalculatorTest {

    private static final double DELTA = 0.01; // Tolerance for floating point comparison

    @Test
    void testCanterburyFishbar_RealWorldExample() {
        // Real coordinates from the API
        // Search postcode: CT1 2EH
        double searchLat = 51.280233;
        double searchLon = 1.078151;
        
        // Restaurant: Canterbury Fishbar at 71 Sturry Road, CT1 1BU
        double restaurantLat = 51.286542;
        double restaurantLon = 1.091004;
        
        double distance = DistanceCalculator.calculateDistanceMiles(
            searchLat, searchLon, 
            restaurantLat, restaurantLon
        );
        
        // Actual haversine distance for these coords is ~0.71 miles
        assertEquals(0.71, distance, 0.02,
            "Canterbury Fishbar should be ~0.71 miles from CT1 2EH");
    }

    @Test
    void testSameLocation_ZeroDistance() {
        double lat = 51.5074;  // London
        double lon = -0.1278;
        
        double distance = DistanceCalculator.calculateDistanceMiles(lat, lon, lat, lon);
        
        assertEquals(0.0, distance, DELTA, 
            "Same location should have zero distance");
    }

    @Test
    void testLondonToManchester() {
        // London coordinates
        double londonLat = 51.5074;
        double londonLon = -0.1278;
        
        // Manchester coordinates
        double manchesterLat = 53.4808;
        double manchesterLon = -2.2426;
        
        double distance = DistanceCalculator.calculateDistanceMiles(
            londonLat, londonLon, 
            manchesterLat, manchesterLon
        );
        
        // Actual distance is approximately 163 miles
        assertEquals(163.0, distance, 5.0, 
            "London to Manchester should be ~163 miles");
    }

    @Test
    void testMetersToMiles() {
        // 1007 meters = 0.626 miles (Canterbury Fishbar example)
        assertEquals(0.626, DistanceCalculator.metersToMiles(1007), 0.01);
        
        // 1609 meters = 1 mile (exact)
        assertEquals(1.0, DistanceCalculator.metersToMiles(1609), 0.01);
        
        // 5000 meters = 3.11 miles
        assertEquals(3.11, DistanceCalculator.metersToMiles(5000), 0.01);
    }

    @Test
    void testFormatDistance() {
        assertEquals("0.6 mi", DistanceCalculator.formatDistance(0.62));
        assertEquals("1.2 mi", DistanceCalculator.formatDistance(1.23));
        assertEquals("163.5 mi", DistanceCalculator.formatDistance(163.48));
    }

    @Test
    void testKilometersCalculation() {
        // Canterbury Fishbar example in kilometers
        double searchLat = 51.280233;
        double searchLon = 1.078151;
        double restaurantLat = 51.286542;
        double restaurantLon = 1.091004;
        
        double distanceKm = DistanceCalculator.calculateDistanceKm(
            searchLat, searchLon, 
            restaurantLat, restaurantLon
        );
        
        // 0.62 miles ≈ 1.0 km
        assertEquals(1.13, distanceKm, 0.1, 
            "Should be approximately 1 kilometer");
    }

    /**
     * Test demonstrating the step-by-step Haversine calculation
     */
    @Test
    void testHaversineFormula_StepByStep() {
        // Canterbury example
        double lat1 = 51.280233;  // Search postcode
        double lon1 = 1.078151;
        double lat2 = 51.286542;  // Restaurant
        double lon2 = 1.091004;
        
        // Step 1: Convert to radians
        double lat1Rad = Math.toRadians(lat1);  // 0.8949 radians
        double lon1Rad = Math.toRadians(lon1);  // 0.0188 radians
        double lat2Rad = Math.toRadians(lat2);  // 0.8950 radians
        double lon2Rad = Math.toRadians(lon2);  // 0.0190 radians
        
        // Step 2: Calculate differences
        double dLat = lat2Rad - lat1Rad;  // 0.00011 radians
        double dLon = lon2Rad - lon1Rad;  // 0.00022 radians
        
        // Step 3: Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        // Step 4: Multiply by Earth's radius
        double distance = 3958.8 * c;
        
        // Verify it matches our utility method
        double utilityDistance = DistanceCalculator.calculateDistanceMiles(
            lat1, lon1, lat2, lon2
        );
        
        assertEquals(utilityDistance, distance, 0.0001, 
            "Step-by-step calculation should match utility method");
        assertEquals(0.71, distance, 0.02,
            "Should be approximately 0.71 miles");
    }
}
