package org.example.isarsw.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.isarsw.model.Flight;
import org.example.isarsw.service.FlightService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ArrivalCheckController {

    @FXML private Label lblTimeInfo;
    @FXML private Label lblFlightNumber;
    @FXML private Label lblRoute;
    @FXML private Label lblPlatform;
    @FXML private Label lblScheduledTime;
    @FXML private Label lblCurrentStatus;

    @FXML private Button btnBoarding;
    @FXML private Button btnDelayed;
    @FXML private Button btnCancelled;
    @FXML private Button btnDeparted;

    private Flight flight;
    private FlightService flightService;
    private Stage stage;
    private long timeDelta;
    private String checkMode; // "arrival" или "departure"

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML
    private void initialize() {
        System.out.println("=== ОКНО ПРОВЕРКИ СТАТУСА ===");
        System.out.println("Контроллер инициализирован");

        Platform.runLater(() -> {
            System.out.println("Проверка элементов после загрузки:");
            System.out.println("lblFlightNumber: " + (lblFlightNumber != null ? "OK" : "NULL"));
            System.out.println("btnBoarding: " + (btnBoarding != null ? "OK" : "NULL"));

            // Убедимся, что кнопки имеют правильные стили
            btnBoarding.getStyleClass().addAll("status-button", "btn-boarding");
            btnDelayed.getStyleClass().addAll("status-button", "btn-delayed");
            btnCancelled.getStyleClass().addAll("status-button", "btn-cancelled");
            btnDeparted.getStyleClass().addAll("status-button", "btn-departed");
        });
    }

    public void setFlight(Flight flight) {
        this.flight = flight;
        updateFlightInfo();
    }

    public void setFlightService(FlightService flightService) {
        this.flightService = flightService;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setTimeDelta(long timeDelta) {
        this.timeDelta = timeDelta;
        updateTimeInfo();
    }

    public void setCheckMode(String checkMode) {
        this.checkMode = checkMode;
        configureForMode();
    }

    private void configureForMode() {
        if ("departure".equals(checkMode)) {
            // Настраиваем для проверки отправления
            Platform.runLater(() -> {
                if (stage != null) {
                    stage.setTitle("Проверка отправления поезда");
                }
            });

            // Обновляем информацию о времени отправления
            long departureTs = flight.getArriveTs() + (flight.getStandingTime() * 60L);
            String departureTime = dateTimeFormatter.format(
                    Instant.ofEpochSecond(departureTs).atZone(ZoneId.systemDefault()));
            lblScheduledTime.setText("Время отправления: " + departureTime);

            // Скрываем кнопку "Посадка" для режима отправления
            btnBoarding.setVisible(false);
            btnBoarding.setManaged(false);

            // Центрируем оставшиеся кнопки
            Platform.runLater(() -> {
                HBox parent = (HBox) btnDelayed.getParent();
                parent.getChildren().remove(btnBoarding);
                parent.setSpacing(15);
            });
        }
        // Для "arrival" режима ничего не меняем
    }

    private void updateTimeInfo() {
        if ("departure".equals(checkMode)) {
            // Логика для отправления
            if (timeDelta == 0) {
                lblTimeInfo.setText("Поезд должен был отбыть сейчас");
            } else {
                long hours = Math.abs(timeDelta) / 3600;
                long minutes = (Math.abs(timeDelta) % 3600) / 60;

                if (timeDelta > 0) {
                    lblTimeInfo.setText("Поезд должен отбыть через " + hours + " ч " + minutes + " мин");
                } else {
                    lblTimeInfo.setText("Поезд должен был отбыть " + hours + " ч " + minutes + " мин назад");
                }
            }
        } else {
            // Существующая логика для прибытия
            if (timeDelta < 0) {
                long absMinutes = Math.abs(timeDelta) / 60;
                lblTimeInfo.setText("Поезд должен прибыть через " + absMinutes + " минут");
            } else {
                long hours = timeDelta / 3600;
                long minutes = (timeDelta % 3600) / 60;

                if (hours < 24) {
                    lblTimeInfo.setText("Поезд должен был прибыть " + hours + " ч " + minutes + " мин назад");
                } else {
                    lblTimeInfo.setText("Поезд должен был прибыть более суток назад");
                }
            }
        }
    }

    private void updateFlightInfo() {
        if (flight != null) {
            lblFlightNumber.setText("Поезд: " + flight.getNumber());
            lblRoute.setText("Маршрут: " + flight.getRoute());
            lblPlatform.setText("Платформа: " + flight.getPlatform());

            if ("departure".equals(checkMode)) {
                // Показываем время отправления
                long departureTs = flight.getArriveTs() + (flight.getStandingTime() * 60L);
                String scheduledTime = dateTimeFormatter.format(
                        Instant.ofEpochSecond(departureTs).atZone(ZoneId.systemDefault()));
                lblScheduledTime.setText("Запланировано отправление: " + scheduledTime);
            } else {
                // Показываем время прибытия
                String scheduledTime = dateTimeFormatter.format(
                        Instant.ofEpochSecond(flight.getArriveTs()).atZone(ZoneId.systemDefault()));
                lblScheduledTime.setText("Запланировано прибытие: " + scheduledTime);
            }

            String currentStatus = flight.getStatus();
            lblCurrentStatus.setText("Текущий статус: " + currentStatus);

            // Устанавливаем CSS класс в зависимости от статуса
            lblCurrentStatus.getStyleClass().clear();
            lblCurrentStatus.getStyleClass().add("info-label-bold");

            switch (currentStatus) {
                case FlightService.STATUS_PLANNED:
                    lblCurrentStatus.getStyleClass().add("status-planned");
                    break;
                case FlightService.STATUS_EN_ROUTE:
                    lblCurrentStatus.getStyleClass().add("status-en-route");
                    break;
                case FlightService.STATUS_BOARDING:
                    lblCurrentStatus.getStyleClass().add("status-boarding");
                    break;
                case FlightService.STATUS_DELAYED:
                    lblCurrentStatus.getStyleClass().add("status-delayed");
                    break;
                case FlightService.STATUS_CANCELLED:
                    lblCurrentStatus.getStyleClass().add("status-cancelled");
                    break;
                case FlightService.STATUS_DEPARTED:
                    lblCurrentStatus.getStyleClass().add("status-departed");
                    break;
            }
        }
    }

    @FXML
    private void onBoarding() {
        try {
            flightService.changeStatus(flight.getId(), FlightService.STATUS_BOARDING,
                    "администратор", false);
            closeWindow();
        } catch (Exception e) {
            showAlert("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    private void onDelayed() {
        try {
            flightService.changeStatus(flight.getId(), FlightService.STATUS_DELAYED,
                    "администратор", false);
            closeWindow();
        } catch (Exception e) {
            showAlert("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelled() {
        try {
            flightService.changeStatus(flight.getId(), FlightService.STATUS_CANCELLED,
                    "администратор", false);
            closeWindow();
        } catch (Exception e) {
            showAlert("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    private void onDeparted() {
        try {
            flightService.changeStatus(flight.getId(), FlightService.STATUS_DEPARTED,
                    "администратор", false);
            closeWindow();
        } catch (Exception e) {
            showAlert("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    private void onLater() {
        closeWindow();
    }

    private void closeWindow() {
        if (stage != null) {
            stage.close();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}