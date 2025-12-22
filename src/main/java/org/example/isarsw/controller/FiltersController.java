package org.example.isarsw.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.isarsw.model.Flight;
import org.example.isarsw.service.FlightService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.Predicate;

public class FiltersController {

    @FXML private ChoiceBox<String> cbStatus;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private TextField tfPlatform;

    private MainController mainController;

    public void setMainController(MainController mc) {
        this.mainController = mc;
    }

    @FXML
    private void initialize() {
        // Используем ИЗМЕНЕННЫЕ статусы
        cbStatus.getItems().addAll(
                "",
                FlightService.STATUS_PLANNED,
                FlightService.STATUS_EN_ROUTE,
                FlightService.STATUS_BOARDING,
                FlightService.STATUS_DELAYED,
                FlightService.STATUS_CANCELLED
        );
        cbStatus.setValue("");
    }

    @FXML
    private void onApply() {
        String status = cbStatus.getValue();
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        String platform = tfPlatform.getText().trim();

        Predicate<Flight> predicate = flight -> {
            // Фильтр по статусу
            if (status != null && !status.isEmpty()) {
                if (!status.equals(flight.getStatus())) return false;
            }

            // Фильтр по платформе
            if (platform != null && !platform.isEmpty()) {
                if (!flight.getPlatform().toLowerCase().contains(platform.toLowerCase())) return false;
            }

            // Фильтр по дате прибытия (от)
            if (from != null) {
                long fromEpoch = from
                        .atStartOfDay(ZoneId.systemDefault())
                        .toEpochSecond();
                if (flight.getArriveTs() < fromEpoch) return false;
            }

            // Фильтр по дате прибытия (до)
            if (to != null) {
                long toEpoch = to
                        .plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toEpochSecond() - 1;
                if (flight.getArriveTs() > toEpoch) return false;
            }

            return true;
        };

        if (mainController != null) {
            mainController.setFilterPredicate(predicate);
        }

        closeWindow();
    }

    @FXML
    private void onReset() {
        cbStatus.setValue("");
        dpFrom.setValue(null);
        dpTo.setValue(null);
        tfPlatform.clear();

        if (mainController != null) {
            mainController.setFilterPredicate(f -> true);
        }
    }

    @FXML
    private void onClose() {
        closeWindow();
    }

    private void closeWindow() {
        Stage st = (Stage) cbStatus.getScene().getWindow();
        st.close();
    }
}