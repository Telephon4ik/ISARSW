package org.example.isarsw.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FlightTest {

    @Test
    void getDepartureTs_CalculatesCorrectly() {
        long arriveTs = Instant.now().getEpochSecond();
        Flight flight = new Flight("SU-1234", "Route", arriveTs, 30, "A1", "STATUS");
        assertEquals(arriveTs + 1800, flight.getDepartureTs());
    }

    @Test
    void toPayload_ReturnsValidJson() {
        Flight flight = new Flight("SU-1234", "Москва-СПб", 1234567890L, 30, "A1", "PLANNED");
        String payload = flight.toPayload();
        assertTrue(payload.contains("\"number\":\"SU-1234\""));
        assertTrue(payload.contains("\"route\":\"Москва-СПб\""));
        assertTrue(payload.contains("\"platform\":\"A1\""));
    }

    @Test
    void needsArrivalCheck_WhenNotCheckedAndArrived_ReturnsTrue() {
        long pastTime = Instant.now().getEpochSecond() - 3600;
        Flight flight = new Flight("SU-1234", "Route", pastTime, 30, "A1", "STATUS");
        flight.setLastArrivalCheck(0);
        assertTrue(flight.needsArrivalCheck());
    }
}