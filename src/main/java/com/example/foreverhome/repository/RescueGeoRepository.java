package com.example.foreverhome.repository;

import com.example.foreverhome.dto.RescueWithDistance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for geo-based rescue organization queries.
 * Uses JdbcTemplate for complex Haversine queries with distance calculation.
 */
@Repository
public class RescueGeoRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String FIND_NEARBY_QUERY = """
            SELECT * FROM (
                SELECT
                    r.id,
                    r.name,
                    r.description,
                    r.logo_url,
                    r.address_city,
                    r.address_state,
                    r.verified,
                    (6371 * acos(
                        LEAST(1.0, GREATEST(-1.0,
                            cos(radians(?)) * cos(radians(r.address_latitude)) *
                            cos(radians(r.address_longitude) - radians(?)) +
                            sin(radians(?)) * sin(radians(r.address_latitude))
                        ))
                    )) AS distance_km
                FROM rescue_organizations r
                WHERE r.address_latitude IS NOT NULL
                  AND r.address_longitude IS NOT NULL
                  AND r.verified = true
                  -- Bounding box pre-filter (rough approximation to reduce Haversine calculations)
                  AND r.address_latitude BETWEEN ? - (? / 111.0) AND ? + (? / 111.0)
                  AND r.address_longitude BETWEEN ? - (? / (111.0 * cos(radians(?))))
                                               AND ? + (? / (111.0 * cos(radians(?))))
            ) sub
            WHERE distance_km < ?
            ORDER BY distance_km ASC
            LIMIT ?
            """;

    private static final String COUNT_WITHOUT_COORDINATES_QUERY = """
            SELECT COUNT(*) FROM rescue_organizations
            WHERE address_latitude IS NULL OR address_longitude IS NULL
            """;

    private static final String FIND_WITHOUT_COORDINATES_QUERY = """
            SELECT id FROM rescue_organizations
            WHERE address_latitude IS NULL OR address_longitude IS NULL
            LIMIT ?
            """;

    private static final RowMapper<RescueWithDistance> RESCUE_WITH_DISTANCE_MAPPER = (rs, rowNum) ->
            new RescueWithDistance(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("logo_url"),
                    rs.getString("address_city"),
                    rs.getString("address_state"),
                    rs.getBoolean("verified"),
                    rs.getDouble("distance_km")
            );

    public RescueGeoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds rescue organizations within a given radius of the specified coordinates.
     * Uses Haversine formula with bounding box pre-filtering for performance.
     *
     * @param lat       latitude of search center
     * @param lon       longitude of search center
     * @param radiusKm  search radius in kilometers
     * @param limit     maximum number of results
     * @return list of rescues with distance, ordered by distance ascending
     */
    public List<RescueWithDistance> findNearby(double lat, double lon, double radiusKm, int limit) {
        return jdbcTemplate.query(
                FIND_NEARBY_QUERY,
                RESCUE_WITH_DISTANCE_MAPPER,
                // Haversine parameters
                lat, lon, lat,
                // Bounding box parameters for latitude
                lat, radiusKm, lat, radiusKm,
                // Bounding box parameters for longitude
                lon, radiusKm, lat, lon, radiusKm, lat,
                // Where clause and limit
                radiusKm, limit
        );
    }

    /**
     * Counts rescue organizations without coordinates.
     */
    public int countWithoutCoordinates() {
        Integer count = jdbcTemplate.queryForObject(COUNT_WITHOUT_COORDINATES_QUERY, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Finds IDs of rescue organizations without coordinates.
     */
    public List<UUID> findIdsWithoutCoordinates(int limit) {
        return jdbcTemplate.query(
                FIND_WITHOUT_COORDINATES_QUERY,
                (rs, rowNum) -> UUID.fromString(rs.getString("id")),
                limit
        );
    }

    /**
     * Updates coordinates for a rescue organization.
     */
    public int updateCoordinates(UUID rescueId, double latitude, double longitude) {
        return jdbcTemplate.update(
                "UPDATE rescue_organizations SET address_latitude = ?, address_longitude = ? WHERE id = ?",
                latitude, longitude, rescueId.toString()
        );
    }
}
