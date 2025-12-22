package org.example.isarsw.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.isarsw.dao.HistoryDao;
import org.example.isarsw.model.HistoryEntry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HistoryController {

    public static Long currentFlightId = null;

    @FXML private TableView<HistoryEntry> historyTable;
    @FXML private TableColumn<HistoryEntry, String> colTimestamp;
    @FXML private TableColumn<HistoryEntry, String> colActor;
    @FXML private TableColumn<HistoryEntry, String> colAction;
    @FXML private TableColumn<HistoryEntry, String> colBefore;
    @FXML private TableColumn<HistoryEntry, String> colAfter;
    @FXML private Label lblFlightInfo;

    private final HistoryDao historyDao = new HistoryDao();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    @FXML
    private void initialize() {
        colTimestamp.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(fmt.format(Instant.ofEpochSecond(c.getValue().getTimestamp()))));
        colActor.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getActor()));
        colAction.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getAction()));
        colBefore.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getPayloadBefore() == null ? "" : c.getValue().getPayloadBefore()));
        colAfter.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getPayloadAfter() == null ? "" : c.getValue().getPayloadAfter()));

        loadHistory();
    }

    private void loadHistory() {
        if (currentFlightId == null) return;
        try {
            List<HistoryEntry> list = historyDao.listByFlight(currentFlightId);
            historyTable.getItems().setAll(list);

            if (!list.isEmpty()) {
                lblFlightInfo.setText("Записи для рейса ID: " + currentFlightId);
            } else {
                lblFlightInfo.setText("История изменений отсутствует");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onClose() {
        Stage st = (Stage) historyTable.getScene().getWindow();
        st.close();
    }
}