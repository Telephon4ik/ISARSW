package org.example.isarsw.service;

import org.example.isarsw.model.Flight;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FlightServiceTest {

    private FlightService flightService = new FlightService();

    @Test
    void validateFlight_ValidFlight_DoesNotThrow() {
        Flight flight = new Flight("SU-1234", "Москва-СПб",
                Instant.now().getEpochSecond() + 3600, 30, "A1", "ПЛАНИРУЕТСЯ");

        // Проверяем что конструктор Flight не выбрасывает исключений
        assertDoesNotThrow(() -> new Flight("SU-1234", "Москва-СПб",
                Instant.now().getEpochSecond() + 3600, 30, "A1", "ПЛАНИРУЕТСЯ"));
    }

    @Test
    void flightConstructor_InvalidNumber_DoesNotValidate() {
        // Конструктор Flight не валидирует номер, только создает объект
        // Валидация происходит в FlightService.addFlight()
        assertDoesNotThrow(() -> new Flight("", "Route",
                Instant.now().getEpochSecond(), 30, "A1", "STATUS"));
    }

    // Проверка констант и статических методов
    @Test
    void testStatusConstants() {
        assertEquals("ПЛАНИРУЕТСЯ", FlightService.STATUS_PLANNED);
        assertEquals("В ПУТИ", FlightService.STATUS_EN_ROUTE);
        assertEquals("ПОСАДКА", FlightService.STATUS_BOARDING);
        assertEquals("ЗАДЕРЖАН", FlightService.STATUS_DELAYED);
        assertEquals("ОТБЫЛ", FlightService.STATUS_DEPARTED);
        assertEquals("ОТМЕНЁН", FlightService.STATUS_CANCELLED);
    }

    @Test
    void testStaticMethods() {
        // Проверяем что статические методы работают
        String[] statuses = FlightService.getAllStatuses();
        assertNotNull(statuses);
        assertEquals(6, statuses.length);

        for (String status : statuses) {
            assertNotNull(FlightService.getStatusDescription(status));
        }
    }
}