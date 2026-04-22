package org.jetcodingchallenge.util;

/**
 * Utility class for calculating distance between two geographic coordinates
 * using the Haversine formula
 */
public class DistanceCalculator {

    private static final double EARTH_RADIUS_MILES = 3958.8; // Earth's radius in miles
    private static final double EARTH_RADIUS_KM = 6371.0;    // Earth's radius in kilometers

    /**
     * Calculate distance between two points using Haversine formula
     * 
     * @param lat1 Latitude of first point (e.g., search postcode)
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point (e.g., restaurant)
     * @param lon2 Longitude of second point
     * @return Distance in miles
     * 
     * Example:
     * Input postcode CT1 2EH coordinates: (51.280233, 1.078151)
     * Restaurant at 71 Sturry Road: (51.286542, 1.091004)
     * Result: 0.62 miles (approximately 0.6 mi)
     */
    public static double calculateDistanceMiles(double lat1, double lon1, double lat2, double lon2) {
        return calculateDistance(lat1, lon1, lat2, lon2, EARTH_RADIUS_MILES);
    }

    /**
     * Calculate distance in kilometers
     */
    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        return calculateDistance(lat1, lon1, lat2, lon2, EARTH_RADIUS_KM);
    }

    /**
     * Haversine formula implementation
     * 
     * Formula:
     * a = sin²(Δφ/2) + cos φ1 ⋅ cos φ2 ⋅ sin²(Δλ/2)
     * c = 2 ⋅ atan2(√a, √(1−a))
     * d = R ⋅ c
     * 
     * where φ is latitude, λ is longitude, R is earth's radius
     */
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2, double radius) {
        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Haversine formula
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return radius * c;
    }

    /**
     * Convert meters to miles
     */
    public static double metersToMiles(int meters) {
        return meters / 1609.344;
    }

    /**
     * Format distance for display
     */
    public static String formatDistance(double miles) {
        return String.format("%.1f mi", miles);
    }
}