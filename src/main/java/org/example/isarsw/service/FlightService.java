package org.example.isarsw.service;

import org.example.isarsw.dao.FlightDao;
import org.example.isarsw.dao.HistoryDao;
import org.example.isarsw.model.Flight;
import org.example.isarsw.model.HistoryEntry;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class FlightService {
    private final FlightDao flightDao = new FlightDao();
    private final HistoryDao historyDao = new HistoryDao();

    public static final String STATUS_PLANNED = "ПЛАНИРУЕТСЯ";
    public static final String STATUS_EN_ROUTE = "В ПУТИ";
    public static final String STATUS_BOARDING = "ПОСАДКА";
    public static final String STATUS_DELAYED = "ЗАДЕРЖАН";
    public static final String STATUS_DEPARTED = "ОТБЫЛ";
    public static final String STATUS_CANCELLED = "ОТМЕНЁН";

    public static final long TWO_DAYS_SECONDS = 48 * 3600L;
    public static final long ONE_HOUR_SECONDS = 3600L;
    public static final long DELAY_LIMIT_SECONDS = 24 * 3600L;

    public static final String ACTOR_ADMIN = "администратор";
    public static final String ACTOR_SYSTEM = "система";

    public Flight addFlight(Flight f, boolean overrideConflicts) throws SQLException {
        validateFlight(f);

        List<Flight> conflicts = flightDao.findConflicting(
                f.getPlatform(),
                f.getArriveTs(),
                f.getStandingTime(),
                null
        );

        if (!conflicts.isEmpty() && !overrideConflicts) {
            StringBuilder conflictMsg = new StringBuilder("Обнаружен конфликт по платформе " + f.getPlatform() + ":\n");
            for (Flight conflict : conflicts) {
                conflictMsg.append("• Рейс ").append(conflict.getNumber())
                        .append(" (прибытие: ").append(formatTime(conflict.getArriveTs()))
                        .append(", стоянка: ").append(conflict.getStandingTime()).append(" мин)\n");
            }
            throw new IllegalStateException(conflictMsg.toString());
        }

        long now = Instant.now().getEpochSecond();
        f.setCreatedAt(now);
        f.setUpdatedAt(now);

        Flight saved = flightDao.create(f);

        HistoryEntry h = new HistoryEntry(saved.getId(), "CREATE", ACTOR_ADMIN, now, null, saved.toPayload());
        historyDao.insert(h);
        return saved;
    }

    public void updateFlight(Flight f, boolean overrideConflicts) throws SQLException {
        validateFlight(f);
        Optional<Flight> existingOpt = flightDao.findById(f.getId());
        if (existingOpt.isEmpty()) throw new IllegalArgumentException("Рейс не найден: " + f.getId());
        Flight before = existingOpt.get();

        List<Flight> conflicts = flightDao.findConflicting(
                f.getPlatform(),
                f.getArriveTs(),
                f.getStandingTime(),
                f.getId()
        );

        if (!conflicts.isEmpty() && !overrideConflicts) {
            StringBuilder conflictMsg = new StringBuilder("Обнаружен конфликт по платформе " + f.getPlatform() + ":\n");
            for (Flight conflict : conflicts) {
                conflictMsg.append("• Рейс ").append(conflict.getNumber())
                        .append(" (прибытие: ").append(formatTime(conflict.getArriveTs()))
                        .append(", стоянка: ").append(conflict.getStandingTime()).append(" мин)\n");
            }
            throw new IllegalStateException(conflictMsg.toString());
        }

        f.setUpdatedAt(Instant.now().getEpochSecond());
        flightDao.update(f);

        HistoryEntry h = new HistoryEntry(f.getId(), "UPDATE", ACTOR_ADMIN, Instant.now().getEpochSecond(), before.toPayload(), f.toPayload());
        historyDao.insert(h);
    }

    public void deleteFlight(long id) throws SQLException {
        Optional<Flight> existingOpt = flightDao.findById(id);
        if (existingOpt.isEmpty()) throw new IllegalArgumentException("Рейс не найден: " + id);
        Flight before = existingOpt.get();
        flightDao.delete(id);
        HistoryEntry h = new HistoryEntry(id, "DELETE", ACTOR_ADMIN, Instant.now().getEpochSecond(), before.toPayload(), null);
        historyDao.insert(h);
    }

    public void changeStatus(long id, String newStatus, String actor, boolean automatic) throws SQLException {
        Optional<Flight> existingOpt = flightDao.findById(id);
        if (existingOpt.isEmpty()) return;
        Flight f = existingOpt.get();
        String prev = f.getStatus();
        f.setStatus(newStatus);
        f.setUpdatedAt(Instant.now().getEpochSecond());
        flightDao.update(f);
        String action = automatic ? "STATUS_AUTO" : "STATUS_MANUAL";
        HistoryEntry h = new HistoryEntry(f.getId(), action, actor, Instant.now().getEpochSecond(),
                "{\"status\":\""+prev+"\"}", "{\"status\":\""+newStatus+"\"}");
        historyDao.insert(h);
    }

    public void changeStatusWithValidation(long id, String newStatus, String actor, boolean automatic) throws SQLException {
        Optional<Flight> existingOpt = flightDao.findById(id);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Рейс не найден: " + id);
        }

        Flight flight = existingOpt.get();
        String currentStatus = flight.getStatus();

        if (!canChangeStatus(currentStatus, newStatus)) {
            throw new IllegalStateException("Невозможно изменить статус с '" + currentStatus +
                    "' на '" + newStatus + "'");
        }

        changeStatus(id, newStatus, actor, automatic);
    }

    public static boolean canChangeStatus(String fromStatus, String toStatus) {
        // Нельзя менять статус у отменённых или отбывших поездов
        if (fromStatus.equals(STATUS_CANCELLED) || fromStatus.equals(STATUS_DEPARTED)) {
            return false;
        }

        // Любой поезд можно отменить (кроме уже отменённых или отбывших)
        if (toStatus.equals(STATUS_CANCELLED)) {
            return true;
        }

        // Поезд может отбыть только если он был на посадке или задержан
        if (toStatus.equals(STATUS_DEPARTED)) {
            return fromStatus.equals(STATUS_BOARDING) || fromStatus.equals(STATUS_DELAYED);
        }

        // Проверка других переходов
        switch (fromStatus) {
            case STATUS_PLANNED:
                return toStatus.equals(STATUS_EN_ROUTE) ||
                        toStatus.equals(STATUS_DELAYED) ||
                        toStatus.equals(STATUS_BOARDING);

            case STATUS_EN_ROUTE:
                return toStatus.equals(STATUS_BOARDING) ||
                        toStatus.equals(STATUS_DELAYED);

            case STATUS_BOARDING:
                return toStatus.equals(STATUS_DELAYED) ||
                        toStatus.equals(STATUS_DEPARTED); // Теперь может отбыть через окно

            case STATUS_DELAYED:
                return toStatus.equals(STATUS_BOARDING) ||
                        toStatus.equals(STATUS_EN_ROUTE) ||
                        toStatus.equals(STATUS_DEPARTED); // И задержанный может отбыть

            default:
                return false;
        }
    }

    public List<Flight> listAll() throws SQLException {
        return flightDao.findAll();
    }

    public List<Flight> search(String query) throws SQLException {
        return flightDao.search(query);
    }

    public Optional<Flight> findById(long id) throws SQLException {
        return flightDao.findById(id);
    }

    void validateFlight(Flight f) {
        if (f == null) {
            throw new IllegalArgumentException("Рейс равен null");
        }

        if (f.getNumber() == null || f.getNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Номер рейса обязателен");
        }

        if (f.getRoute() == null || f.getRoute().trim().isEmpty()) {
            throw new IllegalArgumentException("Маршрут обязателен");
        }

        if (f.getPlatform() == null || f.getPlatform().trim().isEmpty()) {
            throw new IllegalArgumentException("Платформа обязательна");
        }

        if (f.getArriveTs() <= 0) {
            throw new IllegalArgumentException("Время прибытия должно быть указано");
        }

        if (f.getStandingTime() <= 0) {
            throw new IllegalArgumentException("Время стоянки должно быть положительным числом");
        }
    }

    private String formatTime(long timestamp) {
        return java.time.Instant.ofEpochSecond(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    public static String[] getAllStatuses() {
        return new String[] {
                STATUS_PLANNED,
                STATUS_EN_ROUTE,
                STATUS_BOARDING,
                STATUS_DELAYED,
                STATUS_DEPARTED,
                STATUS_CANCELLED
        };
    }

    public static String getStatusDescription(String status) {
        switch (status) {
            case STATUS_PLANNED:
                return "Рейс планируется (более 48 часов до прибытия)";
            case STATUS_EN_ROUTE:
                return "Рейс в пути (от 48 часов до 1 часа до прибытия)";
            case STATUS_BOARDING:
                return "Идет посадка пассажиров";
            case STATUS_DELAYED:
                return "Рейс задержан (максимум 24 часа)";
            case STATUS_DEPARTED:
                return "Поезд отбыл со станции";
            case STATUS_CANCELLED:
                return "Рейс отменен";
            default:
                return "Неизвестный статус";
        }
    }

    public List<Flight> getFlightsReadyForDeparture() throws SQLException {
        long currentTime = Instant.now().getEpochSecond();
        return flightDao.findFlightsReadyForDeparture(currentTime);
    }

    public void markAsDeparted(long flightId) throws SQLException {
        changeStatus(flightId, STATUS_DEPARTED, ACTOR_SYSTEM, true);
    }
}