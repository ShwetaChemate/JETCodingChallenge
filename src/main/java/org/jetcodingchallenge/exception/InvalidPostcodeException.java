package org.jetcodingchallenge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when postcode format is invalid
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPostcodeException extends RuntimeException {
    public InvalidPostcodeException(String postcode) {
        super("Invalid UK postcode format: " + postcode);
    }
}
