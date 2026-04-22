package org.jetcodingchallenge.data.model;

/**
 * Sorting options for restaurant results
 */
public enum SortBy {
    /**
     * Sort by distance (nearest first) - default
     */
    DISTANCE,
    
    /**
     * Sort by rating (highest first)
     */
    RATING,
    
    /**
     * Sort by name (alphabetically)
     */
    NAME
}