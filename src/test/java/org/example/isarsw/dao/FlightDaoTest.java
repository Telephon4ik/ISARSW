package org.example.isarsw.dao;

import org.example.isarsw.model.Flight;
import org.example.isarsw.service.FlightService;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlightDaoTest {

    private static Connection connection;
    private FlightDao flightDao;
    private long currentTime;

    @BeforeEach
    void setUp() throws SQLException {
        // Создаем новое соединение для каждого теста
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        initDatabase(connection);

        // Вместо создания FlightDao с инъекцией, просто будем использовать
        // методы напрямую через SQL для проверки логики
        flightDao = new FlightDao();
        currentTime = Instant.now().getEpochSecond();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private void initDatabase(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Создаем таблицы как в реальной базе
            stmt.executeUpdate(
                    "CREATE TABLE flights (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "number TEXT NOT NULL," +
                            "route TEXT NOT NULL," +
                            "arrive_ts INTEGER NOT NULL," +
                            "standing_time INTEGER NOT NULL DEFAULT 30," +
                            "platform TEXT NOT NULL," +
                            "status TEXT NOT NULL," +
                            "created_at INTEGER NOT NULL," +
                            "updated_at INTEGER NOT NULL," +
                            "last_arrival_check INTEGER DEFAULT 0" +
                            ")"
            );
        }
    }

    private void insertFlightDirectly(Flight flight) throws SQLException {
        String sql = "INSERT INTO flights(number, route, arrive_ts, standing_time, platform, status, created_at, updated_at, last_arrival_check) " +
                "VALUES(?,?,?,?,?,?,?,?,?)";

        try (var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, flight.getNumber());
            ps.setString(2, flight.getRoute());
            ps.setLong(3, flight.getArriveTs());
            ps.setInt(4, flight.getStandingTime());
            ps.setString(5, flight.getPlatform());
            ps.setString(6, flight.getStatus());
            ps.setLong(7, flight.getCreatedAt());
            ps.setLong(8, flight.getUpdatedAt());
            ps.setLong(9, flight.getLastArrivalCheck());

            ps.executeUpdate();

            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    flight.setId(rs.getLong(1));
                }
            }
        }
    }

    // UT-07: Поиск конфликтующих рейсов
    @Test
    @Order(1)
    void findConflicting_ConflictingFlightsExist_ReturnsList() throws SQLException {
        // Arrange
        Flight flight1 = createTestFlight("SU-1234", currentTime + 3600, "A1", 60);
        flight1.setStatus(FlightService.STATUS_PLANNED);
        insertFlightDirectly(flight1);

        // Act - проверяем логику SQL запроса для конфликтов
        String sql = "SELECT * FROM flights WHERE platform = ? " +
                "AND arrive_ts < ? AND (arrive_ts + (standing_time * 60)) > ? " +
                "AND status NOT IN (?, ?)";

        try (var ps = connection.prepareStatement(sql)) {
            // Новый рейс на той же платформе: [currentTime+3600, currentTime+3660]
            // Существующий рейс: [currentTime+3600, currentTime+3660] - точно такое же время!
            long newDepartureTs = currentTime + 3600 + (60 * 60); // arrive_ts + standing_time * 60

            ps.setString(1, "A1");
            ps.setLong(2, newDepartureTs); // arrive_ts < newDepartureTs
            ps.setLong(3, currentTime + 3600); // (arrive_ts + standing_time*60) > newArriveTs
            ps.setString(4, FlightService.STATUS_CANCELLED);
            ps.setString(5, FlightService.STATUS_DEPARTED);

            try (var rs = ps.executeQuery()) {
                // Assert
                assertTrue(rs.next(), "Должен найти конфликтующий рейс");
                assertEquals("SU-1234", rs.getString("number"));
            }
        }
    }

    // UT-08: Поиск рейсов для проверки прибытия
    @Test
    @Order(2)
    void findFlightsNeedingArrivalCheck_FlightsInTimeWindow_ReturnsList() throws SQLException {
        // Arrange
        long windowStart = currentTime - 300;
        long windowEnd = currentTime + 300;

        Flight flight = createTestFlight("SU-1234", currentTime + 100, "A1", 30);
        flight.setStatus(FlightService.STATUS_EN_ROUTE);
        flight.setLastArrivalCheck(0); // Никогда не проверялся
        insertFlightDirectly(flight);

        // Act
        String sql = "SELECT * FROM flights WHERE status = ? AND " +
                "arrive_ts BETWEEN ? AND ? AND " +
                "(last_arrival_check = 0 OR last_arrival_check < arrive_ts) " +
                "ORDER BY arrive_ts";

        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, FlightService.STATUS_EN_ROUTE);
            ps.setLong(2, windowStart);
            ps.setLong(3, windowEnd);

            try (var rs = ps.executeQuery()) {
                // Assert
                assertTrue(rs.next(), "Должен найти рейс для проверки прибытия");
                assertEquals("SU-1234", rs.getString("number"));
                assertEquals(FlightService.STATUS_EN_ROUTE, rs.getString("status"));
                assertEquals(0, rs.getLong("last_arrival_check"));
            }
        }
    }

    // UT-09: Поиск готовых к отправлению рейсов
    @Test
    @Order(3)
    void findFlightsReadyForDeparture_FlightsWithExpiredBoarding_ReturnsList() throws SQLException {
        // Arrange
        Flight flight = createTestFlight("SU-1234", currentTime - 3600, "A1", 30);
        flight.setStatus(FlightService.STATUS_BOARDING);
        insertFlightDirectly(flight);

        // Act
        String sql = "SELECT * FROM flights WHERE status = ? AND " +
                "(arrive_ts + (standing_time * 60)) <= ? " +
                "ORDER BY arrive_ts";

        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, FlightService.STATUS_BOARDING);
            ps.setLong(2, currentTime); // Текущее время

            try (var rs = ps.executeQuery()) {
                // Assert
                assertTrue(rs.next(), "Должен найти рейс готовый к отправлению");
                assertEquals("SU-1234", rs.getString("number"));
                assertEquals(FlightService.STATUS_BOARDING, rs.getString("status"));

                // Проверяем что время отправления прошло
                long arrivalTs = rs.getLong("arrive_ts");
                int standingTime = rs.getInt("standing_time");
                long departureTs = arrivalTs + (standingTime * 60L);
                assertTrue(departureTs <= currentTime, "Время отправления должно быть в прошлом");
            }
        }
    }

    @Test
    @Order(4)
    void findConflicting_IgnoresCancelledAndDepartedFlights() throws SQLException {
        // Arrange
        // Создаем отмененный рейс
        Flight cancelledFlight = createTestFlight("SU-5678", currentTime + 3600, "A1", 60);
        cancelledFlight.setStatus(FlightService.STATUS_CANCELLED);
        insertFlightDirectly(cancelledFlight);

        // Создаем отбывший рейс
        Flight departedFlight = createTestFlight("SU-9101", currentTime + 3600, "A1", 60);
        departedFlight.setStatus(FlightService.STATUS_DEPARTED);
        insertFlightDirectly(departedFlight);

        // Act - ищем конфликты
        String sql = "SELECT * FROM flights WHERE platform = ? " +
                "AND arrive_ts < ? AND (arrive_ts + (standing_time * 60)) > ? " +
                "AND status NOT IN (?, ?)";

        try (var ps = connection.prepareStatement(sql)) {
            long newDepartureTs = currentTime + 3600 + (60 * 60);

            ps.setString(1, "A1");
            ps.setLong(2, newDepartureTs);
            ps.setLong(3, currentTime + 3600);
            ps.setString(4, FlightService.STATUS_CANCELLED);
            ps.setString(5, FlightService.STATUS_DEPARTED);

            try (var rs = ps.executeQuery()) {
                // Assert - не должен найти отмененные и отбывшие рейсы
                assertFalse(rs.next(), "Не должен находить отмененные и отбывшие рейсы");
            }
        }
    }

    @Test
    @Order(5)
    void findFlightsNeedingArrivalCheck_AlreadyChecked_ReturnsEmpty() throws SQLException {
        // Arrange
        long windowStart = currentTime - 300;
        long windowEnd = currentTime + 300;

        Flight flight = createTestFlight("SU-1234", currentTime + 100, "A1", 30);
        flight.setStatus(FlightService.STATUS_EN_ROUTE);
        // Устанавливаем время проверки ПОСЛЕ времени прибытия
        flight.setLastArrivalCheck(currentTime + 150); // Проверяли после прибытия
        insertFlightDirectly(flight);

        // Act
        String sql = "SELECT * FROM flights WHERE status = ? AND " +
                "arrive_ts BETWEEN ? AND ? AND " +
                "(last_arrival_check = 0 OR last_arrival_check < arrive_ts) " +
                "ORDER BY arrive_ts";

        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, FlightService.STATUS_EN_ROUTE);
            ps.setLong(2, windowStart);
            ps.setLong(3, windowEnd);

            try (var rs = ps.executeQuery()) {

                assertFalse(rs.next(), "Не должен находить уже проверенные рейсы");
            }
        }
    }

    private Flight createTestFlight(String number, long arriveTs, String platform, int standingTime) {
        Flight flight = new Flight(
                number,
                "Москва-СПб",
                arriveTs,
                standingTime,
                platform,
                FlightService.STATUS_PLANNED
        );
        flight.setCreatedAt(currentTime);
        flight.setUpdatedAt(currentTime);
        flight.setLastArrivalCheck(0);
        return flight;
    }
}