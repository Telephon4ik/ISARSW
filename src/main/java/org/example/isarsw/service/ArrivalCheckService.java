package org.example.isarsw.service;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.isarsw.app.App;
import org.example.isarsw.controller.ArrivalCheckController;
import org.example.isarsw.model.Flight;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrivalCheckService {

    private final FlightService flightService = new FlightService();
    private final Map<String, Boolean> pendingChecks = new HashMap<>();

    public void checkArrivals() {
        long now = Instant.now().getEpochSecond();

        try {
            List<Flight> flights = flightService.listAll();

            for (Flight flight : flights) {
                checkSingleFlightForArrival(flight, now);
            }
        } catch (SQLException e) {
            System.err.println("Ошибка проверки прибытия: " + e.getMessage());
        }
    }

    private void checkSingleFlightForArrival(Flight flight, long now) {
        // Пропускаем отмененные или отбывшие поезда
        if (FlightService.STATUS_CANCELLED.equals(flight.getStatus()) ||
                FlightService.STATUS_DEPARTED.equals(flight.getStatus())) {
            return;
        }

        long arrivalTime = flight.getArriveTs();
        String currentStatus = flight.getStatus();
        String checkKey = "arrival_" + flight.getId();

        // Проверяем поезда со статусом "В ПУТИ", которые должны были уже прибыть
        if (FlightService.STATUS_EN_ROUTE.equals(currentStatus) &&
                arrivalTime <= now &&  // ← Время прибытия уже прошло!
                !pendingChecks.containsKey(checkKey)) {

            pendingChecks.put(checkKey, true);

            final Flight finalFlight = flight;
            final long timeDelta = now - arrivalTime;  // На сколько опоздал

            Platform.runLater(() -> {
                showArrivalCheckWindow(finalFlight, timeDelta, "arrival_late", "arrival");
            });
        }

        // Также проверяем поезда "В ПУТИ", которые должны прибыть в ближайшие 5 минут
        if (FlightService.STATUS_EN_ROUTE.equals(currentStatus) &&
                arrivalTime > now && arrivalTime <= (now + 300) &&  // В ближайшие 5 минут
                !pendingChecks.containsKey(checkKey + "_soon")) {

            pendingChecks.put(checkKey + "_soon", true);

            final Flight finalFlight = flight;
            final long timeDelta = arrivalTime - now;  // Через сколько прибывает

            Platform.runLater(() -> {
                showArrivalCheckWindow(finalFlight, -timeDelta, "arrival_soon", "arrival");
            });
        }
    }

    public void checkDepartures() {
        long now = Instant.now().getEpochSecond();

        try {
            List<Flight> flights = flightService.getFlightsReadyForDeparture();

            for (Flight flight : flights) {
                checkSingleFlightForDeparture(flight, now);
            }
        } catch (SQLException e) {
            System.err.println("Ошибка проверки отправления: " + e.getMessage());
        }
    }

    private void checkSingleFlightForDeparture(Flight flight, long now) {
        // Проверяем только поезда со статусом "ПОСАДКА"
        if (!FlightService.STATUS_BOARDING.equals(flight.getStatus())) {
            return;
        }

        long departureTime = flight.getArriveTs() + (flight.getStandingTime() * 60L);
        long timeUntilDeparture = departureTime - now;
        String checkKey = "departure_" + flight.getId();

        // Если время отправления наступило или прошло
        if (departureTime <= now && !pendingChecks.containsKey(checkKey)) {
            pendingChecks.put(checkKey, true);

            final Flight finalFlight = flight;
            final long timeDelta = now - departureTime;  // На сколько опоздал с отправлением

            Platform.runLater(() -> {
                showArrivalCheckWindow(finalFlight, timeDelta, "departure_late", "departure");
            });
        }
        // Или если отправление в ближайшие 5 минут
        else if (timeUntilDeparture > 0 && timeUntilDeparture <= 300 &&
                !pendingChecks.containsKey(checkKey + "_soon")) {

            pendingChecks.put(checkKey + "_soon", true);

            final Flight finalFlight = flight;
            final long timeDelta = timeUntilDeparture;  // Через сколько отправление

            Platform.runLater(() -> {
                showArrivalCheckWindow(finalFlight, -timeDelta, "departure_soon", "departure");
            });
        }
    }

    private void showArrivalCheckWindow(Flight flight, long timeDelta, String checkType, String mode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/isarsw/fxml/arrival_check.fxml"));
            Stage stage = new Stage();

            if ("departure".equals(mode)) {
                stage.setTitle("Проверка отправления поезда");
            } else {
                stage.setTitle("Проверка прибытия поезда");
            }

            stage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(loader.load());

            // Загружаем CSS стили
            String cssPath = "/org/example/isarsw/css/style.css";
            java.net.URL cssUrl = getClass().getResource(cssPath);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);

            // Устанавливаем иконку для окна проверки
            if (App.getAppIcon() != null) {
                stage.getIcons().clear();
                stage.getIcons().add(App.getAppIcon());
            }

            ArrivalCheckController controller = loader.getController();
            controller.setFlight(flight);
            controller.setFlightService(flightService);
            controller.setStage(stage);
            controller.setTimeDelta(timeDelta);
            controller.setCheckMode(mode);

            stage.sizeToScene();
            stage.setMinWidth(520);
            stage.setMinHeight(450);
            stage.setMaxWidth(700);
            stage.setMaxHeight(600);
            stage.setResizable(true);

            centerStage(stage);

            final String checkKey = mode + "_" + flight.getId() + "_" + checkType;
            stage.setOnHidden(e -> {
                pendingChecks.remove(checkKey);
            });

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            pendingChecks.remove(mode + "_" + flight.getId());
        }
    }

    private void centerStage(Stage stage) {
        Platform.runLater(() -> {
            Stage mainStage = (Stage) stage.getOwner();
            if (mainStage != null) {
                double centerX = mainStage.getX() + mainStage.getWidth() / 2;
                double centerY = mainStage.getY() + mainStage.getHeight() / 2;

                double x = centerX - stage.getWidth() / 2;
                double y = centerY - stage.getHeight() / 2;

                Screen screen = Screen.getPrimary();
                Rectangle2D bounds = screen.getVisualBounds();

                if (x < bounds.getMinX()) x = bounds.getMinX();
                if (y < bounds.getMinY()) y = bounds.getMinY();
                if (x + stage.getWidth() > bounds.getMaxX())
                    x = bounds.getMaxX() - stage.getWidth();
                if (y + stage.getHeight() > bounds.getMaxY())
                    y = bounds.getMaxY() - stage.getHeight();

                stage.setX(x);
                stage.setY(y);
            } else {
                stage.centerOnScreen();
            }
        });
    }

    public void startChecking() {
        Thread checkThread = new Thread(() -> {
            System.out.println("=== ЗАПУСК СЕРВИСА ПРОВЕРКИ СТАТУСОВ ===");

            while (true) {
                try {
                    System.out.println("--- Проверка в " + Instant.now() + " ---");
                    System.out.println("Проверка прибытия...");
                    checkArrivals();
                    System.out.println("Проверка отправления...");
                    checkDepartures();

                    Thread.sleep(30000);  // Проверяем каждые 30 секунд

                } catch (InterruptedException e) {
                    System.out.println("Сервис проверки остановлен");
                    break;
                } catch (Exception e) {
                    System.err.println("Ошибка в сервисе проверки: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        checkThread.setDaemon(true);
        checkThread.start();
    }

    public void stopChecking() {
        pendingChecks.clear();
    }
}