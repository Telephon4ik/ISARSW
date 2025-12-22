package org.example.isarsw.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class CommonUtils {

    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    public static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    // Общие методы для окон
    public static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static void closeWindow(javafx.scene.Node node) {
        Stage stage = (Stage) node.getScene().getWindow();
        stage.close();
    }

    // Форматирование времени
    public static String formatDateTime(long timestamp) {
        return DATE_TIME_FORMATTER.format(Instant.ofEpochSecond(timestamp));
    }

    public static String formatTime(long timestamp) {
        return TIME_FORMATTER.format(Instant.ofEpochSecond(timestamp));
    }
}