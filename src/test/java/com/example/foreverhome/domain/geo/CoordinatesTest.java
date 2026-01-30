package com.example.foreverhome.domain.geo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CoordinatesTest {

    @Test
    void shouldCreateValidCoordinates() {
        Coordinates coords = new Coordinates(53.3498, -6.2603);

        assertThat(coords.latitude()).isEqualTo(53.3498);
        assertThat(coords.longitude()).isEqualTo(-6.2603);
    }

    @Test
    void shouldRejectInvalidLatitude() {
        assertThatThrownBy(() -> new Coordinates(91.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude");

        assertThatThrownBy(() -> new Coordinates(-91.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude");
    }

    @Test
    void shouldRejectInvalidLongitude() {
        assertThatThrownBy(() -> new Coordinates(0.0, 181.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Longitude");

        assertThatThrownBy(() -> new Coordinates(0.0, -181.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Longitude");
    }

    @Test
    void shouldCalculateDistanceDublinToCork() {
        // Dublin City Centre
        Coordinates dublin = new Coordinates(53.3498, -6.2603);
        // Cork City Centre
        Coordinates cork = new Coordinates(51.8985, -8.4756);

        double distance = dublin.distanceTo(cork);

        // Dublin to Cork is approximately 220 km
        assertThat(distance).isBetween(215.0, 225.0);
    }

    @Test
    void shouldCalculateDistanceDublinToGalway() {
        // Dublin City Centre
        Coordinates dublin = new Coordinates(53.3498, -6.2603);
        // Galway City Centre
        Coordinates galway = new Coordinates(53.2707, -9.0568);

        double distance = dublin.distanceTo(galway);

        // Dublin to Galway is approximately 186 km (as-the-crow-flies)
        assertThat(distance).isBetween(180.0, 195.0);
    }

    @Test
    void shouldCalculateDistanceDublinToBelfast() {
        // Dublin City Centre
        Coordinates dublin = new Coordinates(53.3498, -6.2603);
        // Belfast City Centre
        Coordinates belfast = new Coordinates(54.5973, -5.9301);

        double distance = dublin.distanceTo(belfast);

        // Dublin to Belfast is approximately 140 km (as-the-crow-flies)
        assertThat(distance).isBetween(135.0, 150.0);
    }

    @Test
    void shouldCalculateZeroDistanceForSamePoint() {
        Coordinates point = new Coordinates(53.3498, -6.2603);

        double distance = point.distanceTo(point);

        assertThat(distance).isEqualTo(0.0);
    }

    @Test
    void shouldHandleAntipodes() {
        // North Pole
        Coordinates northPole = new Coordinates(90.0, 0.0);
        // South Pole
        Coordinates southPole = new Coordinates(-90.0, 0.0);

        double distance = northPole.distanceTo(southPole);

        // Half of Earth's circumference is approximately 20,000 km
        assertThat(distance).isBetween(19900.0, 20100.0);
    }
}
