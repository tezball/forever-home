# Geo Search Implementation Plan

## Overview

Enable users to find rescue organizations and their pets based on geographic proximity. This requires adding geospatial data to rescue profiles and implementing distance-based search queries.

## 1. Domain Model Changes

### 1.1 `Address` Value Object

Update `com.example.foreverhome.domain.profile.Address` to include coordinate data.

```java
public record Address(
    String street,
    String city,
    String state,
    String postalCode,
    String country,
    Double latitude,  // New
    Double longitude  // New
) {}
```

### 1.2 `RescueOrganization` Table

Add columns to `rescue_organizations` table:

- `address_latitude` (DOUBLE PRECISION)
- `address_longitude` (DOUBLE PRECISION)

## 2. Database & Repository

### 2.1 Schema Migration

Create a Flyway migration script to alter the table:

```sql
-- V20250130__add_geo_columns.sql
ALTER TABLE rescue_organizations
ADD COLUMN address_latitude DOUBLE PRECISION,
ADD COLUMN address_longitude DOUBLE PRECISION;

-- Create composite index for bounding box queries
CREATE INDEX idx_rescue_org_coordinates
ON rescue_organizations (address_latitude, address_longitude)
WHERE address_latitude IS NOT NULL AND address_longitude IS NOT NULL;
```

#### Rollback Strategy

```sql
-- V20250130__add_geo_columns_rollback.sql
DROP INDEX IF EXISTS idx_rescue_org_coordinates;
ALTER TABLE rescue_organizations
DROP COLUMN IF EXISTS address_latitude,
DROP COLUMN IF EXISTS address_longitude;
```

### 2.2 Repository Query

Update `RescueOrganizationRepository` with a Haversine formula query to find rescues within a radius. Uses a bounding box pre-filter for performance.

```java
@Query("""
    SELECT * FROM (
        SELECT r.*,
        (6371 * acos(
            cos(radians(:lat)) * cos(radians(r.address_latitude)) *
            cos(radians(r.address_longitude) - radians(:lon)) +
            sin(radians(:lat)) * sin(radians(r.address_latitude))
        )) AS distance
        FROM rescue_organizations r
        WHERE r.address_latitude IS NOT NULL
          AND r.address_longitude IS NOT NULL
          -- Bounding box pre-filter (rough approximation to reduce Haversine calculations)
          AND r.address_latitude BETWEEN :lat - (:radiusKm / 111.0) AND :lat + (:radiusKm / 111.0)
          AND r.address_longitude BETWEEN :lon - (:radiusKm / (111.0 * cos(radians(:lat))))
                                       AND :lon + (:radiusKm / (111.0 * cos(radians(:lat))))
    ) sub
    WHERE distance < :radiusKm
    ORDER BY distance ASC
    LIMIT :limit
""")
List<RescueOrganizationWithDistance> findNearby(
    @Param("lat") double lat,
    @Param("lon") double lon,
    @Param("radiusKm") double radiusKm,
    @Param("limit") int limit
);
```

### 2.3 Indexing Strategy

For the initial implementation, a composite B-tree index with bounding box pre-filtering is sufficient.

**Future Enhancement**: If geo queries become a primary use case, consider migrating to PostGIS:
- Handles edge cases (international dateline, polar regions) correctly
- GiST index on `geography` type for better performance
- Built-in functions like `ST_DWithin` for cleaner queries

## 3. Geocoding Service

Implement a `GeocodingService` to convert text addresses to coordinates.

### 3.1 Interface

```java
public interface GeocodingService {
    Optional<Coordinates> geocode(Address address);
    Optional<Coordinates> geocode(String zipCode);
}

public record Coordinates(double latitude, double longitude) {}
```

### 3.2 Implementation

- **Provider**: Use OpenStreetMap (Nominatim) for the free tier/dev environment, or a mock implementation for testing.
- **Trigger**: Call this service when a Rescue Organization creates or updates their profile.

### 3.3 Rate Limiting & Caching

Nominatim has strict rate limits (1 request/second). Implement the following safeguards:

```java
@Service
public class CachingGeocodingService implements GeocodingService {

    private final GeocodingClient client;
    private final GeocodeCacheRepository cache;
    private final RateLimiter rateLimiter = RateLimiter.create(1.0); // 1 req/sec

    @Override
    public Optional<Coordinates> geocode(Address address) {
        String cacheKey = buildCacheKey(address);

        // Check cache first
        return cache.findByKey(cacheKey)
            .map(cached -> new Coordinates(cached.latitude(), cached.longitude()))
            .or(() -> fetchAndCache(cacheKey, address));
    }

    private Optional<Coordinates> fetchAndCache(String key, Address address) {
        rateLimiter.acquire(); // Block until rate limit allows
        try {
            var coords = client.geocode(address);
            coords.ifPresent(c -> cache.save(new GeocodeCache(key, c.latitude(), c.longitude())));
            return coords;
        } catch (GeocodingException e) {
            log.warn("Geocoding failed for address: {}", address, e);
            return Optional.empty();
        }
    }
}
```

### 3.4 Async Geocoding for Bulk Operations

For data backfill or bulk imports, process geocoding asynchronously:

```java
@Async
public CompletableFuture<Void> geocodeAllRescuesWithoutCoordinates() {
    var rescues = rescueRepository.findAllWithoutCoordinates();
    for (var rescue : rescues) {
        geocodingService.geocode(rescue.getAddress())
            .ifPresent(coords -> rescueRepository.updateCoordinates(
                rescue.getId(), coords.latitude(), coords.longitude()
            ));
        // Rate limiting handled by CachingGeocodingService
    }
    return CompletableFuture.completedFuture(null);
}
```

## 4. API Endpoints

### 4.1 Search API

`GET /api/rescues/search`

- **Params**:
  - `zip` (optional): Zip code to search near
  - `lat`/`lon` (optional): Direct coordinates
  - `radius` (optional, default 50km, max 200km)
  - `limit` (optional, default 20, max 100)
- **Response**: List of rescues with distance metadata.

```json
{
  "results": [
    {
      "id": "uuid",
      "name": "Happy Tails Rescue",
      "city": "Dublin",
      "state": "Co. Dublin",
      "distanceKm": 5.2
    }
  ],
  "searchCenter": {
    "latitude": 53.3498,
    "longitude": -6.2603
  },
  "radiusKm": 50
}
```

**Note**: Exact coordinates are NOT exposed in API responses for privacy. Only city/state and calculated distance are returned.

### 4.2 Error Handling

Rescues without coordinates are:
- **Excluded** from geo-search results
- **Included** in regular (non-geo) search results
- Flagged in admin dashboard for manual review

```java
@GetMapping("/api/rescues/search")
public ResponseEntity<SearchResponse> searchRescues(
    @RequestParam(required = false) String zip,
    @RequestParam(required = false) Double lat,
    @RequestParam(required = false) Double lon,
    @RequestParam(defaultValue = "50") double radius,
    @RequestParam(defaultValue = "20") int limit
) {
    if (zip == null && (lat == null || lon == null)) {
        // Fall back to non-geo search
        return ResponseEntity.ok(rescueService.searchAll(limit));
    }

    Coordinates center = resolveSearchCenter(zip, lat, lon);
    if (center == null) {
        return ResponseEntity.badRequest()
            .body(SearchResponse.error("Could not resolve location from provided parameters"));
    }

    return ResponseEntity.ok(rescueService.searchNearby(center, radius, limit));
}
```

### 4.3 Profile Update

Update `RescueOrgController.updateProfile` to automatically fetch coordinates if the address changes.

## 5. Frontend Implementation

### 5.1 Search Interface

- Add "Near Me" button using Browser Geolocation API
- Add "Zip Code" input as fallback
- Pass location data to the backend API

```typescript
// hooks/useGeolocation.ts
export function useGeolocation() {
  const [location, setLocation] = useState<{ lat: number; lon: number } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const requestLocation = useCallback(() => {
    if (!navigator.geolocation) {
      setError('Geolocation is not supported by your browser');
      return;
    }

    setLoading(true);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLocation({
          lat: position.coords.latitude,
          lon: position.coords.longitude,
        });
        setLoading(false);
      },
      (err) => {
        setError(err.message);
        setLoading(false);
      },
      { enableHighAccuracy: false, timeout: 10000 }
    );
  }, []);

  return { location, error, loading, requestLocation };
}
```

### 5.2 Results Display

- Show distance (e.g., "5.2 km away") on rescue cards.
- Sort results by distance when geo-search is active.
- Show "Location unavailable" badge for rescues without coordinates in admin views.

## 6. Data Migration

### 6.1 Backfill Existing Data

After deploying the schema changes, run a one-time backfill job:

```java
@Component
public class GeoBackfillRunner implements ApplicationRunner {

    @Value("${geo.backfill.enabled:false}")
    private boolean backfillEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (backfillEnabled) {
            log.info("Starting geocode backfill for existing rescues...");
            geocodingService.geocodeAllRescuesWithoutCoordinates()
                .thenRun(() -> log.info("Geocode backfill complete"));
        }
    }
}
```

Enable with: `GEO_BACKFILL_ENABLED=true` on first deployment after migration.

### 6.2 Monitoring

Track backfill progress and geocoding failures:

```sql
-- Rescues with coordinates
SELECT COUNT(*) FROM rescue_organizations WHERE address_latitude IS NOT NULL;

-- Rescues without coordinates (need geocoding or manual entry)
SELECT id, name, address_city, address_postal_code
FROM rescue_organizations
WHERE address_latitude IS NULL;
```

## 7. Testing Strategy

### 7.1 Unit Tests

- Test the Haversine distance calculation with known coordinates
- Test bounding box filter edge cases (equator, prime meridian)
- Test geocoding cache behavior

### 7.2 Integration Tests

- Verify the full flow from API -> Geocoding (Mocked) -> DB -> Response
- Test search with various radius values
- Test fallback behavior when geocoding fails

### 7.3 Test Data

```java
// Known distances for test assertions
// Dublin City Centre to Cork City Centre: ~220 km
// Dublin to Galway: ~210 km
// Dublin to Belfast: ~167 km

@Test
void shouldFindRescuesWithin50km() {
    // Given: rescues in Dublin (0km), Wicklow (40km), Cork (220km)
    // When: search from Dublin with 50km radius
    // Then: only Dublin and Wicklow rescues returned
}
```

## 8. Privacy Considerations

- **Do not expose** exact coordinates in public API responses
- Return only city/state and calculated distance
- Admin users may see full address for operational purposes
- Consider GDPR implications for EU rescue organizations
