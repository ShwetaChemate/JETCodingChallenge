# JET Coding Challenge — Restaurant Search API

A Spring Boot REST API that retrieves and displays restaurants near a given UK postcode
using the Just Eat public API. Results include the restaurant name, cuisines, star rating,
and address — sorted by distance, rating, or name.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Data Displayed](#data-displayed)
- [Prerequisites](#prerequisites)
- [How to Build & Run](#how-to-build--run)
- [Running the Tests](#running-the-tests)
- [API Usage](#api-usage)
- [Swagger UI](#swagger-ui)
- [Assumptions](#assumptions)
- [Improvements I Would Make](#improvements-i-would-make)

---

## Architecture Overview

The application follows a clean layered architecture:

HTTP Request - >

RestaurantController (validates input, parses sortBy param) - >

RestaurantSvc (orchestrates: validates postcode, calls API, maps & sorts results)

Just Eat API GeocodingService (resolves postcode to lat/lon for distance calculation) - >

DistanceCalculator (Haversine formula to calculate miles between coordinates)


**Key design decisions:**
- **`RestaurantSvc`** is the single source of truth — it validates the UK postcode via regex,
  fetches from the Just Eat enriched endpoint, maps the raw API response into a flat
  `RestaurantDto`, calculates distances, and sorts the result.

- **`GeocodingService`** uses the `api.postcodes.io` public API to convert a postcode into
  latitude/longitude so real distances can be computed.

- **`RestaurantDto`** is a flat Java record intentionally designed to expose only the four
  required data points plus `distanceMiles` for sorting.
  
- **Custom exceptions** (`InvalidPostcodeException`, `RestaurantNotFoundException`,
  `ExternalApiException`) map to appropriate HTTP status codes (400, 404, 503).

- **Swagger UI** is included via SpringDoc OpenAPI for easy API exploration.

---

## Data Displayed

Each restaurant in the response includes all four required data points:

| Field           | Description                                          | Example                                                      |
|-----------------|------------------------------------------------------|--------------------------------------------------------------|
| `name`          | Restaurant name                                      | `"Canterbury Fishbar"`                                       |
| `cuisines`      | List of cuisine names (flattened from API objects)   | `["Fish & Chips", "Chicken"]`                                |
| `rating`        | Star rating as a plain number                        | `4.5`                                                        |
| `address`       | Formatted address with distance from search postcode | `"71 Sturry Rd, Canterbury, CT1 1BU (0.6 mi from CT1 2EH)"` |


---

## Prerequisites

- **Java 21**
- **Maven** 

Verify your Java version:
```bash
java -version
```

---

## How to Build & Run

### 1. Clone the repository

```bash
git clone https://github.com/ShwetaChemate/JETCodingChallenge.git
cd JETCodingChallenge
```

### 2. Build the project

```bash
./mvnw clean package
```

### 3. Run the application

```bash
./mvnw spring-boot:run
```

The server starts at: **`http://localhost:8080`**

---

## Running the Tests

### Run all tests

```bash
./mvnw clean test
```

---

## API Usage

### Endpoint
**`GET /api/restaurants?postcode={postcode}&sortBy={sortBy}`**

### Parameters

| Parameter  | Required | Default    | Options                       |
|------------|----------|------------|-------------------------------|
| `postcode` | ✅ Yes   | —          | Any valid UK postcode         |
| `sortBy`   | ❌ No    | `distance` | `distance`, `rating`, `name`  |

### Example Requests

```bash
# Sort by distance (default)
curl "http://localhost:8080/api/restaurants?postcode=CT1%202EH"

# Sort by rating
curl "http://localhost:8080/api/restaurants?postcode=CT1%202EH&sortBy=rating"

# Sort by name
curl "http://localhost:8080/api/restaurants?postcode=SW1A%201AA&sortBy=name"
```

### Example Response

```json
[
  {
    "name": "Canterbury Fishbar",
    "cuisines": ["Fish & Chips", "Chicken"],
    "rating": 5.0,
    "address": "71 Sturry Road, Canterbury, CT1 1BU (0.6 mi from CT1 2EH)"
  }
]
```

### Error Responses

| Status | Scenario                   | Response body                                               |
|--------|----------------------------|-------------------------------------------------------------|
| `400`  | Invalid postcode format    | `{"error": "Invalid UK postcode format: INVALID"}`          |
| `404`  | No restaurants at postcode | `{"error": "No restaurants found for postcode: ZZ99 9ZZ"}`  |
| `503`  | Just Eat API unavailable   | `{"error": "Just Eat API is currently unavailable"}`        |

---

## Swagger UI

Interactive API documentation is available once the application is running:

**`http://localhost:8080/swagger-ui.html`**

<img width="1428" height="778" alt="Screenshot 2026-04-22 at 7 08 42 PM" src="https://github.com/user-attachments/assets/3f49f9cb-1e70-4a56-a6f8-fa1a5c993480" />

---

## Assumptions

- **UK postcodes only** — the API and validation regex are scoped to valid UK postcode formats
  (e.g. `CT1 2EH`, `SW1A 1AA`, `M1 1AE`). Spaces within postcodes are handled.
- **Top 10 results** 
- **Distance calculation** — `postcodes.io` is used to geocode the user's search postcode.
  Restaurant coordinates are taken from the Just Eat API response. If geocoding fails,
  distance is omitted and those restaurants sort last.
- **Interface type** — the challenge was interpreted as a REST API (JSON response) rather
  than a frontend UI
- **Default sort** — `DISTANCE` (nearest first) was chosen as the default as it most closely
  mirrors how a real user would search for food.
- **Invalid `sortBy` values** silently fall back to `DISTANCE` rather than returning a 400,
  to keep the consumer experience forgiving.

---

## Improvements I Would Make

1. **Caching** — Cache geocoding responses per postcode using Spring Cache.
   Postcode coordinates don't change and this would reduce latency and rate-limit risk
   on `postcodes.io`.

2. **Pagination** — Allow callers to request more than 10 results via `limit` and `offset`
   query parameters for clients that want to show more results.

3. **Integration tests** 

4. **Configuration externalisation** — Move the Just Eat base URL and the result limit (10)
   into `application.properties` so they can be changed per environment without a code change.

5. **Structured error responses** — Replace the plain `{"error": "..."}` string with a proper
   `ErrorResponseDto` record that includes a machine-readable `code`, `message`, and
   `timestamp` for easier client-side handling.

6. **Structured logging** — Add structured logging for request tracing across the
   service and controller layers for production observability.

