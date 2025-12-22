package org.example.isarsw.service;

import org.example.isarsw.dao.FlightDao;
import org.example.isarsw.dao.HistoryDao;
import org.example.isarsw.model.Flight;
import org.example.isarsw.model.HistoryEntry;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

public class StatusScheduler {
    private final FlightDao flightDao = new FlightDao();
    private final HistoryDao historyDao = new HistoryDao();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final long pollIntervalSeconds;

    public StatusScheduler(long pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::runOnce, 5, pollIntervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    // Убираем дублирование с FlightService.changeStatus
    private void changeStatus(long id, String newStatus, String actor, boolean automatic) throws SQLException {
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

    private void runOnce() {
        try {
            List<Flight> flights = flightDao.findFlightsNeedingStatusUpdate(Instant.now().getEpochSecond());

            for (Flight f : flights) {
                try {
                    String currentStatus = f.getStatus();

                    // Проверяем только активные рейсы
                    if (FlightService.STATUS_CANCELLED.equals(currentStatus) ||
                            FlightService.STATUS_DEPARTED.equals(currentStatus)) {
                        continue;
                    }

                    long arrivalTime = f.getArriveTs();
                    long standingTime = f.getStandingTime() * 60L;
                    long boardingEndTime = arrivalTime + standingTime;
                    long now = Instant.now().getEpochSecond();

                    // Перевод из В ПУТИ в ПОСАДКУ когда время прибытия наступило
                    if (FlightService.STATUS_EN_ROUTE.equals(currentStatus) &&
                            now >= arrivalTime && now < boardingEndTime) {
                        changeStatus(f.getId(), FlightService.STATUS_BOARDING,
                                FlightService.ACTOR_SYSTEM, true);
                    }

                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}