package com.example.foreverhome.service.geo;

import com.example.foreverhome.domain.geo.Coordinates;
import com.example.foreverhome.domain.geo.GeocodeCache;
import com.example.foreverhome.domain.profile.Address;
import com.example.foreverhome.repository.GeocodeCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachingGeocodingServiceTest {

    @Mock
    private NominatimGeocodingClient client;

    @Mock
    private GeocodeCacheRepository cacheRepository;

    private CachingGeocodingService service;

    @BeforeEach
    void setUp() {
        service = new CachingGeocodingService(client, cacheRepository, 100.0, true);
    }

    @Test
    void shouldReturnCachedCoordinates() {
        Address address = new Address("123 Main St", "Dublin", "Dublin", "D01", "Ireland");
        GeocodeCache cached = new GeocodeCache("addr:123 main st|dublin|dublin|d01|ireland", 53.3498, -6.2603);

        when(cacheRepository.findByCacheKey(anyString())).thenReturn(Optional.of(cached));

        Optional<Coordinates> result = service.geocode(address);

        assertThat(result).isPresent();
        assertThat(result.get().latitude()).isEqualTo(53.3498);
        assertThat(result.get().longitude()).isEqualTo(-6.2603);
        verify(client, never()).geocode(any(Address.class));
    }

    @Test
    void shouldFetchAndCacheWhenNotCached() {
        Address address = new Address("123 Main St", "Dublin", "Dublin", "D01", "Ireland");
        Coordinates coords = new Coordinates(53.3498, -6.2603);

        when(cacheRepository.findByCacheKey(anyString())).thenReturn(Optional.empty());
        when(client.geocode(address)).thenReturn(Optional.of(coords));

        Optional<Coordinates> result = service.geocode(address);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(coords);
        verify(cacheRepository).save(any(GeocodeCache.class));
    }

    @Test
    void shouldReturnEmptyWhenGeocodingFails() {
        Address address = new Address("Invalid Address", "Nowhere", null, null, null);

        when(cacheRepository.findByCacheKey(anyString())).thenReturn(Optional.empty());
        when(client.geocode(address)).thenReturn(Optional.empty());

        Optional<Coordinates> result = service.geocode(address);

        assertThat(result).isEmpty();
        verify(cacheRepository, never()).save(any(GeocodeCache.class));
    }

    @Test
    void shouldReturnEmptyWhenDisabled() {
        service = new CachingGeocodingService(client, cacheRepository, 100.0, false);

        Address address = new Address("123 Main St", "Dublin", "Dublin", "D01", "Ireland");

        Optional<Coordinates> result = service.geocode(address);

        assertThat(result).isEmpty();
        verifyNoInteractions(client);
        verifyNoInteractions(cacheRepository);
    }

    @Test
    void shouldReturnEmptyForNullAddress() {
        Optional<Coordinates> result = service.geocode(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void shouldGeocodePostalCode() {
        Coordinates coords = new Coordinates(53.3498, -6.2603);

        when(cacheRepository.findByCacheKey(anyString())).thenReturn(Optional.empty());
        when(client.geocodePostalCode("D01", "IE")).thenReturn(Optional.of(coords));

        Optional<Coordinates> result = service.geocodePostalCode("D01", "IE");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(coords);
    }

    @Test
    void shouldNormalizeCacheKey() {
        // Two addresses that should have the same cache key
        Address address1 = new Address("123 Main St", "Dublin", "Dublin", "D01", "Ireland");
        Address address2 = new Address("123 MAIN ST", "DUBLIN", "Dublin", "d01", "IRELAND");

        GeocodeCache cached = new GeocodeCache("addr:123 main st|dublin|dublin|d01|ireland", 53.3498, -6.2603);
        when(cacheRepository.findByCacheKey(anyString())).thenReturn(Optional.of(cached));

        Optional<Coordinates> result1 = service.geocode(address1);
        Optional<Coordinates> result2 = service.geocode(address2);

        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result1.get()).isEqualTo(result2.get());
    }
}
