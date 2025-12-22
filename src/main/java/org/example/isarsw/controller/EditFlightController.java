package org.example.isarsw.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.isarsw.model.Flight;
import org.example.isarsw.service.FlightService;
import org.example.isarsw.util.CommonUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.time.*;
import java.util.Optional;

public class EditFlightController {

    public static Long currentFlightId = null;

    @FXML private Label lblId;
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
    private Flight editing;

    @FXML
    private void initialize() {
        loadTimeControllers();
        cbStatus.getItems().addAll(FlightService.getAllStatuses());

        if (currentFlightId != null) {
            loadFlight(currentFlightId);
        } else {
            CommonUtils.showAlert("Ошибка", "Не задан ID рейса");
            closeWindow();
        }
    }

    private void loadTimeControllers() {
        try {
            FXMLLoader arriveLoader = new FXMLLoader(getClass().getResource("/org/example/isarsw/fxml/time_picker.fxml"));
            HBox arriveTimePicker = arriveLoader.load();
            arriveTimeController = arriveLoader.getController();

            FXMLLoader standingLoader = new FXMLLoader(getClass().getResource("/org/example/isarsw/fxml/time_picker_minutes.fxml"));
            HBox standingTimePicker = standingLoader.load();
            standingTimeController = standingLoader.getController();

            arriveTime.getChildren().setAll(arriveTimePicker);
            standingTimeBox.getChildren().setAll(standingTimePicker);

            setupChangeListeners();

        } catch (IOException e) {
            System.err.println("Ошибка загрузки контроллеров времени: " + e.getMessage());
        }
    }

    private void setupChangeListeners() {
        dpArrive.valueProperty().addListener((obs, oldVal, newVal) -> calculateDepartureTime());

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

    private void loadFlight(long flightId) {
        try {
            Optional<Flight> opt = flightService.listAll().stream()
                    .filter(f -> f.getId().equals(flightId)).findFirst();
            if (opt.isPresent()) {
                editing = opt.get();
                bindToForm(editing);
            } else {
                CommonUtils.showAlert("Ошибка", "Рейс не найден");
                closeWindow();
            }
        } catch (SQLException e) {
            CommonUtils.showAlert("Ошибка", "Ошибка загрузки: " + e.getMessage());
            closeWindow();
        }
    }

    private void bindToForm(Flight f) {
        lblId.setText("ID: " + f.getId());
        tfNumber.setText(f.getNumber());
        String[] parts = f.getRoute().split("-");
        tfFrom.setText(parts[0]);
        tfTo.setText(parts.length > 1 ? parts[1] : "");
        tfPlatform.setText(f.getPlatform());

        LocalDateTime arriveDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(f.getArriveTs()), ZoneId.systemDefault());
        dpArrive.setValue(arriveDateTime.toLocalDate());

        if (arriveTimeController != null) {
            arriveTimeController.setValue(arriveDateTime.toLocalTime());
        }

        if (standingTimeController != null) {
            standingTimeController.setValue(f.getStandingTime());
        }

        cbStatus.setValue(f.getStatus());
        calculateDepartureTime();
    }

    @FXML
    private void onSave() {
        try {
            String num = tfNumber.getText().trim();
            String from = tfFrom.getText().trim();
            String to = tfTo.getText().trim();
            String route = from + "-" + to;
            String platform = tfPlatform.getText().trim();
            LocalDate dArr = dpArrive.getValue();
            LocalTime ltArr = arriveTimeController.getValue();

            if (num.isEmpty() || from.isEmpty() || to.isEmpty() || platform.isEmpty()
                    || dArr == null || ltArr == null) {
                CommonUtils.showAlert("Ошибка", "Заполните все поля");
                return;
            }

            int standingTime = standingTimeController.getValue();
            long arriveTs = LocalDateTime.of(dArr, ltArr).atZone(ZoneId.systemDefault()).toEpochSecond();

            editing.setNumber(num);
            editing.setRoute(route);
            editing.setPlatform(platform);
            editing.setArriveTs(arriveTs);
            editing.setStandingTime(standingTime);
            editing.setStatus(cbStatus.getValue());

            try {
                flightService.updateFlight(editing, false);
                closeWindow();
            } catch (IllegalStateException conflictEx) {
                boolean ok = confirmOverride("Обнаружен конфликт по времени/платформе. Сохранить несмотря на это?");
                if (ok) {
                    flightService.updateFlight(editing, true);
                    closeWindow();
                }
            }

        } catch (Exception ex) {
            CommonUtils.showAlert("Ошибка", "Ошибка: " + ex.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) lblId.getScene().getWindow();
        stage.close();
    }

    private boolean confirmOverride(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setTitle("Конфликт");
        a.showAndWait();
        return a.getResult() == ButtonType.YES;
    }
}