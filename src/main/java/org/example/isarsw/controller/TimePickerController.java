package org.example.isarsw.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

import java.time.LocalTime;

public class TimePickerController {

    @FXML
    public ComboBox<String> cbHour;
    @FXML
    public ComboBox<String> cbMinute;

    @FXML
    private void initialize() {
        // Установка стилей для ComboBox
        for (int h = 0; h < 24; h++) {
            cbHour.getItems().add(String.format("%02d", h));
        }

        for (int m = 0; m < 60; m += 5) {
            cbMinute.getItems().add(String.format("%02d", m));
        }

        cbHour.setValue("12");
        cbMinute.setValue("00");

        // УСТАНОВИТЕ СТИЛЬ ДЛЯ ОТОБРАЖЕНИЯ В КНОПКЕ COMBOBOX
        cbHour.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50;");
        cbMinute.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        // (ОПЦИОНАЛЬНО) Можно также установить стиль для отображения в кнопке через cellFactory
        cbHour.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50;");
                }
            }
        });

        cbMinute.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50;");
                }
            }
        });

        for (int h = 0; h < 24; h++) {
            cbHour.getItems().add(String.format("%02d", h));
        }

        for (int m = 0; m < 60; m += 5) {
            cbMinute.getItems().add(String.format("%02d", m));
        }

        cbHour.setValue("12");
        cbMinute.setValue("00");
    }

    public LocalTime getValue() {
        try {
            return LocalTime.of(
                    Integer.parseInt(cbHour.getValue()),
                    Integer.parseInt(cbMinute.getValue())
            );
        } catch (Exception e) {
            return LocalTime.of(12, 0);
        }
    }

    public void setValue(LocalTime time) {
        cbHour.setValue(String.format("%02d", time.getHour()));
        cbMinute.setValue(String.format("%02d", time.getMinute()));
    }
}