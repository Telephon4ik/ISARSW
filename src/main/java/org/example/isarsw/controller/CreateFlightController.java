package org.example.isarsw.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.isarsw.model.Flight;
import org.example.isarsw.service.FlightService;

import java.io.IOException;
import java.time.*;

public class CreateFlightController {

    @FXML private TextField tfNumber;
    @FXML private TextField tfFrom;
    @FXML private TextField tfTo;
    @FXML private TextField tfPlatform;
    @FXML private DatePicker dpArrive;
    @FXML private ChoiceBox<String> cbStatus;
    @FXML private Label lblDepartureTime;
    @FXML private HBox arriveTime;
    @FXML private HBox standingTimeBox;

    private TimePickerController arriveTimeController;
    private TimePickerForMinutesController standingTimeController;
    private final FlightService flightService = new FlightService();

    @FXML
    private void initialize() {
        // Убираем избыточные проверки - элементы гарантированно есть в FXML
        cbStatus.getItems().addAll(FlightService.getAllStatuses());
        cbStatus.setValue(FlightService.STATUS_PLANNED);
        dpArrive.setValue(LocalDate.now());

        loadTimeControllers();
        setupChangeListeners();

        Platform.runLater(this::calculateDepartureTime);
    }

    private void loadTimeControllers() {
        try {
            FXMLLoader arriveLoader = new FXMLLoader(getClass().getResource("/org/example/isarsw/fxml/time_picker.fxml"));
            HBox arriveTimePicker = arriveLoader.load();
            arriveTimeController = arriveLoader.getController();

            FXMLLoader standingLoader = new FXMLLoader(getClass().getResource("/org/example/isarsw/fxml/time_picker_minutes.fxml"));
            HBox standingTimePicker = standingLoader.load();
            standingTimeController = standingLoader.getController();

            // Упрощаем установку контроллеров
            arriveTime.getChildren().setAll(arriveTimePicker);
            standingTimeBox.getChildren().setAll(standingTimePicker);

        } catch (IOException e) {
            showError("Ошибка загрузки компонентов времени", e.getMessage());
        }
    }

    private void setupChangeListeners() {
        dpArrive.valueProperty().addListener((obs, oldVal, newVal) -> calculateDepartureTime());

        // Более безопасная проверка
        if (arriveTimeController != null) {
            if (arriveTimeController.cbHour != null) {
                arriveTimeController.cbHour.valueProperty().addListener((obs, oldVal, newVal) -> calculateDepartureTime());
            }
            if (arriveTimeController.cbMinute != null) {
                arriveTimeController.cbMinute.valueProperty().addListener((obs, oldVal, newVal) -> calculateDepartureTime());
            }
        }

        if (standingTimeController != null && standingTimeController.cbMinutes != null) {
            standingTimeController.cbMinutes.valueProperty().addListener((obs, oldVal, newVal) -> calculateDepartureTime());
        }
    }

    private void calculateDepartureTime() {
        try {
            if (dpArrive.getValue() == null || arriveTimeController == null || standingTimeController == null) {
                lblDepartureTime.setText("--:--");
                return;
            }

            LocalTime arrivalTime = arriveTimeController.getValue();
            int standingMinutes = standingTimeController.getValue();
            LocalDateTime arrivalDateTime = LocalDateTime.of(dpArrive.getValue(), arrivalTime);
            LocalDateTime departureDateTime = arrivalDateTime.plusMinutes(standingMinutes);

            lblDepartureTime.setText(departureDateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        } catch (Exception e) {
            lblDepartureTime.setText("--:--");
        }
    }

    @FXML
    private void onSave() {
        try {
            // Валидация ввода
            String num = tfNumber.getText().trim();
            String from = tfFrom.getText().trim();
            String to = tfTo.getText().trim();
            String route = from + "-" + to;
            String platform = tfPlatform.getText().trim();

            if (num.isEmpty() || from.isEmpty() || to.isEmpty() || platform.isEmpty() ||
                    dpArrive.getValue() == null || arriveTimeController == null) {
                showAlert("Ошибка", "Заполните все обязательные поля");
                return;
            }

            if (!num.matches("[A-Za-z0-9\\-]+")) {
                showAlert("Ошибка", "Номер поезда должен содержать только буквы, цифры и дефисы");
                return;
            }

            int standingTime = standingTimeController.getValue();
            if (standingTime <= 0 || standingTime > 240) {
                showAlert("Ошибка", "Время стоянки должно быть от 1 до 240 минут");
                return;
            }

            LocalTime ltArr = arriveTimeController.getValue();
            long arriveTs = LocalDateTime.of(dpArrive.getValue(), ltArr)
                    .atZone(ZoneId.systemDefault()).toEpochSecond();

            Flight f = new Flight(num, route, arriveTs, standingTime, platform, cbStatus.getValue());

            try {
                flightService.addFlight(f, false);
                close();
            } catch (IllegalStateException e) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Конфликт расписания");
                alert.setHeaderText("Обнаружен конфликт по времени или платформе");
                alert.setContentText(e.getMessage() + "\n\nХотите сохранить несмотря на конфликт?");

                if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    flightService.addFlight(f, true);
                    close();
                }
            }

        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось создать рейс: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) tfNumber.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showError(String context, String message) {
        System.err.println(context + ": " + message);
        showAlert("Ошибка", context + ": " + message);
    }
}