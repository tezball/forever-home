package com.example.foreverhome.repository;

import com.example.foreverhome.domain.geo.GeocodeCache;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeocodeCacheRepository extends CrudRepository<GeocodeCache, UUID> {

    Optional<GeocodeCache> findByCacheKey(String cacheKey);

    @Modifying
    @Query("DELETE FROM geocode_cache WHERE expires_at < NOW()")
    int deleteExpiredEntries();
}
