package org.example.isarsw.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class TimePickerForMinutesController {

    @FXML
    public ComboBox<String> cbMinutes;

    @FXML
    private void initialize() {
        // Установка стилей
        for (int m = 1; m <= 60; m++) {
            // ИЗМЕНЕНИЕ: Добавляем только числа, без текста "минут"
            cbMinutes.getItems().add(String.valueOf(m));
        }
        cbMinutes.setValue("30");

        // Установка стилей для выпадающего списка
        cbMinutes.setCellFactory(lv -> {
            return new javafx.scene.control.ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        // ИЗМЕНЕНИЕ: Отображаем число с добавлением " мин" только в отображении
                        setText(item + " мин");
                        setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-padding: 8px;");
                    }
                }
            };
        });

        // Установка стиля для кнопки ComboBox
        cbMinutes.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // ИЗМЕНЕНИЕ: Для кнопки тоже добавляем " мин"
                    setText(item + " мин");
                    setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50;");
                }
            }
        });
    }

    public int getValue() {
        try {
            String selected = cbMinutes.getValue();
            // ИЗМЕНЕНИЕ: Теперь значение уже содержит только число
            return Integer.parseInt(selected.replace(" мин", ""));
        } catch (Exception e) {
            return 30;
        }
    }

    public void setValue(int minutes) {
        if (minutes < 1) minutes = 1;
        if (minutes > 60) minutes = 60;
        // ИЗМЕНЕНИЕ: Устанавливаем только число
        cbMinutes.setValue(String.valueOf(minutes));
    }
}
