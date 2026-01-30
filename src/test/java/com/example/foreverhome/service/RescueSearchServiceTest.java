package com.example.foreverhome.service;

import com.example.foreverhome.domain.geo.Coordinates;
import com.example.foreverhome.dto.RescueSearchResponse;
import com.example.foreverhome.dto.RescueWithDistance;
import com.example.foreverhome.repository.RescueGeoRepository;
import com.example.foreverhome.service.geo.GeocodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RescueSearchServiceTest {

    @Mock
    private RescueGeoRepository rescueGeoRepository;

    @Mock
    private GeocodingService geocodingService;

    private RescueSearchService service;

    @BeforeEach
    void setUp() {
        service = new RescueSearchService(rescueGeoRepository, geocodingService);
    }

    @Test
    void shouldSearchNearbyWithCoordinates() {
        double lat = 53.3498;
        double lon = -6.2603;

        List<RescueWithDistance> mockResults = List.of(
                new RescueWithDistance(UUID.randomUUID(), "Dublin Rescue", "Helping pets", null, "Dublin", "Dublin", true, 5.2),
                new RescueWithDistance(UUID.randomUUID(), "Wicklow Rescue", "Rural pets", null, "Wicklow", "Wicklow", true, 35.8)
        );

        when(rescueGeoRepository.findNearby(eq(lat), eq(lon), eq(50.0), eq(20))).thenReturn(mockResults);

        RescueSearchResponse response = service.searchNearby(lat, lon, null, null);

        assertThat(response.results()).hasSize(2);
        assertThat(response.searchCenter().latitude()).isEqualTo(lat);
        assertThat(response.searchCenter().longitude()).isEqualTo(lon);
        assertThat(response.radiusKm()).isEqualTo(50.0);
        assertThat(response.totalResults()).isEqualTo(2);
    }

    @Test
    void shouldSearchWithCustomRadiusAndLimit() {
        double lat = 53.3498;
        double lon = -6.2603;
        double radius = 100.0;
        int limit = 50;

        when(rescueGeoRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyInt())).thenReturn(List.of());

        service.searchNearby(lat, lon, radius, limit);

        verify(rescueGeoRepository).findNearby(lat, lon, 100.0, 50);
    }

    @Test
    void shouldClampRadiusToMax() {
        double lat = 53.3498;
        double lon = -6.2603;
        double radius = 500.0; // Over max of 200

        when(rescueGeoRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyInt())).thenReturn(List.of());

        RescueSearchResponse response = service.searchNearby(lat, lon, radius, null);

        assertThat(response.radiusKm()).isEqualTo(200.0);
        verify(rescueGeoRepository).findNearby(lat, lon, 200.0, 20);
    }

    @Test
    void shouldClampLimitToMax() {
        double lat = 53.3498;
        double lon = -6.2603;
        int limit = 500; // Over max of 100

        when(rescueGeoRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyInt())).thenReturn(List.of());

        service.searchNearby(lat, lon, null, limit);

        verify(rescueGeoRepository).findNearby(lat, lon, 50.0, 100);
    }

    @Test
    void shouldSearchByPostalCode() {
        String postalCode = "D01";
        String country = "IE";
        Coordinates coords = new Coordinates(53.3498, -6.2603);

        when(geocodingService.geocodePostalCode(postalCode, country)).thenReturn(Optional.of(coords));
        when(rescueGeoRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyInt())).thenReturn(List.of());

        Optional<RescueSearchResponse> response = service.searchByPostalCode(postalCode, country, null, null);

        assertThat(response).isPresent();
        assertThat(response.get().searchCenter().latitude()).isEqualTo(53.3498);
        assertThat(response.get().searchCenter().longitude()).isEqualTo(-6.2603);
    }

    @Test
    void shouldReturnEmptyWhenPostalCodeCannotBeGeocoded() {
        String postalCode = "INVALID";
        String country = "XX";

        when(geocodingService.geocodePostalCode(postalCode, country)).thenReturn(Optional.empty());

        Optional<RescueSearchResponse> response = service.searchByPostalCode(postalCode, country, null, null);

        assertThat(response).isEmpty();
        verifyNoInteractions(rescueGeoRepository);
    }

    @Test
    void shouldPreferCoordinatesOverPostalCode() {
        Double lat = 53.3498;
        Double lon = -6.2603;
        String postalCode = "D01";

        when(rescueGeoRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyInt())).thenReturn(List.of());

        Optional<RescueSearchResponse> response = service.search(lat, lon, postalCode, null, null, null);

        assertThat(response).isPresent();
        verifyNoInteractions(geocodingService);
    }

    @Test
    void shouldFallBackToPostalCodeWhenNoCoordinates() {
        Coordinates coords = new Coordinates(53.3498, -6.2603);
        String postalCode = "D01";

        when(geocodingService.geocodePostalCode(postalCode, null)).thenReturn(Optional.of(coords));
        when(rescueGeoRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyInt())).thenReturn(List.of());

        Optional<RescueSearchResponse> response = service.search(null, null, postalCode, null, null, null);

        assertThat(response).isPresent();
        verify(geocodingService).geocodePostalCode(postalCode, null);
    }

    @Test
    void shouldReturnEmptyWhenNoLocationProvided() {
        Optional<RescueSearchResponse> response = service.search(null, null, null, null, null, null);

        assertThat(response).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenOnlyLatProvided() {
        Optional<RescueSearchResponse> response = service.search(53.3498, null, null, null, null, null);

        assertThat(response).isEmpty();
    }
}
