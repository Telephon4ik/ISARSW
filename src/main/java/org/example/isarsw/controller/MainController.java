package org.example.isarsw.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.example.isarsw.app.App;
import org.example.isarsw.model.Flight;
import org.example.isarsw.service.FlightService;
import org.example.isarsw.service.StatusScheduler;
import org.example.isarsw.util.CommonUtils;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.example.isarsw.util.CommonUtils.showAlert;

public class MainController {

    // ---------- TABLE ----------
    @FXML private TableView<Flight> flightsTable;
    @FXML private TableColumn<Flight, Long> colId;
    @FXML private TableColumn<Flight, String> colNumber;
    @FXML private TableColumn<Flight, String> colRoute;
    @FXML private TableColumn<Flight, String> colPlatform;
    @FXML private TableColumn<Flight, String> colArrive;
    @FXML private TableColumn<Flight, Integer> colStandingTime;
    @FXML private TableColumn<Flight, String> colDeparture;
    @FXML private TableColumn<Flight, String> colStatus;
    @FXML private Label lblStatus;
    @FXML private TextField searchField;
    @FXML private BorderPane mainPane;

    // ---------- DATA ----------
    private final FlightService flightService = new FlightService();
    private final StatusScheduler statusScheduler = new StatusScheduler(5);
    private final ObservableList<Flight> masterData = FXCollections.observableArrayList();
    private FilteredList<Flight> filteredData;
    private SortedList<Flight> sortedData;
    private Predicate<Flight> externalFilter = f -> true;

    // ---------- INIT ----------
    @FXML
    private void initialize() {
        setupTableColumns();
        setupDataFiltering();
        loadFromDb();
        startAutoRefresh();
        statusScheduler.start();

        // Обновляем статус при изменении данных
        masterData.addListener((javafx.collections.ListChangeListener.Change<? extends Flight> change) ->
                updateStatusLabel());
        filteredData.addListener((javafx.collections.ListChangeListener.Change<? extends Flight> change) ->
                updateStatusLabel());

        // Назначаем контекстное меню
        flightsTable.setOnContextMenuRequested(event -> onTableRightClick());
    }

    private void setupTableColumns() {
        flightsTable.setStyle("-fx-font-size: 14px;");

        colId.setCellValueFactory(c -> c.getValue().idProperty().asObject());
        colNumber.setCellValueFactory(c -> c.getValue().numberProperty());
        colRoute.setCellValueFactory(c -> c.getValue().routeProperty());
        colPlatform.setCellValueFactory(c -> c.getValue().platformProperty());
        colStatus.setCellValueFactory(c -> c.getValue().statusProperty());

        if (colStandingTime != null) {
            colStandingTime.setCellValueFactory(c -> c.getValue().standingTimeProperty().asObject());
        }

        // Колонка прибытия
        if (colArrive != null) {
            colArrive.setCellValueFactory(c -> new SimpleObjectProperty<>(formatTime(c.getValue().getArriveTs())));
            colArrive.setCellFactory(col -> createStyledTableCell());
        }

        // Колонка отправления
        if (colDeparture != null) {
            colDeparture.setCellValueFactory(c -> new SimpleObjectProperty<>(formatDepartureTime(c.getValue())));
            colDeparture.setCellFactory(col -> createStyledTableCell());
        }

        // Колонка статуса
        if (colStatus != null) {
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);

                    if (empty || status == null) {
                        setText(null);
                        setStyle("");
                        setTooltip(null);
                    } else {
                        setText(status);
                        setStyle(getStatusStyle(status));
                        setTooltip(new Tooltip(FlightService.getStatusDescription(status)));
                    }
                }
            });
        }
    }

    private TableCell<Flight, String> createStyledTableCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String timeStr, boolean empty) {
                super.updateItem(timeStr, empty);
                setText(empty || timeStr == null ? "" : timeStr);
                setStyle("-fx-font-size: 14px;");
            }
        };
    }

    private String getStatusStyle(String status) {
        String baseStyle = "-fx-font-size: 14px; -fx-font-weight: bold;";

        switch (status) {
            case FlightService.STATUS_PLANNED:
                return baseStyle + " -fx-text-fill: #2196F3;";
            case FlightService.STATUS_EN_ROUTE:
                return baseStyle + " -fx-text-fill: #673AB7;";
            case FlightService.STATUS_BOARDING:
                return baseStyle + " -fx-text-fill: #4CAF50;";
            case FlightService.STATUS_DELAYED:
                return baseStyle + " -fx-text-fill: #FF5722;";
            case FlightService.STATUS_CANCELLED:
                return baseStyle + " -fx-text-fill: #F44336; -fx-strikethrough: true;";
            case FlightService.STATUS_DEPARTED:
                return baseStyle + " -fx-text-fill: #34495e;";
            default:
                return baseStyle;
        }
    }

    private void setupDataFiltering() {
        filteredData = new FilteredList<>(masterData, f -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(flightsTable.comparatorProperty());
        flightsTable.setItems(sortedData);
    }

    private void updateStatusLabel() {
        if (lblStatus != null) {
            lblStatus.setText(String.valueOf(filteredData.size()));
        }
    }

    // ---------- FORMATTING METHODS ----------
    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "";
        return CommonUtils.formatDateTime(timestamp);
    }

    private String formatDepartureTime(Flight flight) {
        if (flight == null || flight.getArriveTs() <= 0) return "";
        long departureTs = flight.getArriveTs() + (flight.getStandingTime() * 60L);
        return CommonUtils.formatDateTime(departureTs);
    }

    // ---------- AUTO REFRESH ----------
    private void startAutoRefresh() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    List<Flight> fresh = flightService.listAll();
                    Platform.runLater(() -> {
                        sync(masterData, fresh);
                        updateStatusLabel();
                    });
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void sync(ObservableList<Flight> target, List<Flight> fresh) {
        target.removeIf(old -> fresh.stream().noneMatch(f -> f.getId().equals(old.getId())));
        for (Flight f : fresh) {
            int idx = -1;
            for (int i = 0; i < target.size(); i++) {
                if (target.get(i).getId().equals(f.getId())) {
                    idx = i;
                    break;
                }
            }
            if (idx == -1) target.add(f);
            else target.set(idx, f);
        }
    }

    private void loadFromDb() {
        try {
            masterData.setAll(flightService.listAll());
            updateStatusLabel();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Ошибка загрузки данных", "Не удалось загрузить поезда из базы данных: " + e.getMessage());
        }
    }

    // ---------- SEARCH ----------
    @FXML
    public void onSearch() {
        String q = searchField.getText().trim().toLowerCase();
        filteredData.setPredicate(f ->
                externalFilter.test(f) &&
                        (q.isEmpty() ||
                                f.getNumber().toLowerCase().contains(q) ||
                                f.getRoute().toLowerCase().contains(q) ||
                                f.getPlatform().toLowerCase().contains(q))
        );
        updateStatusLabel();
    }

    // ---------- FILTER API ----------
    public void setFilterPredicate(Predicate<Flight> p) {
        this.externalFilter = p;
        onSearch();
    }

    // ---------- ACTIONS ----------
    @FXML
    public void onRefresh() {
        loadFromDb();
    }

    @FXML
    public void onCreateFlight() {
        openWindow("/org/example/isarsw/fxml/flight_create.fxml", "Новый поезд");
    }

    @FXML
    public void onEdit() {
        Flight selected = flightsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Не выбран поезд", "Пожалуйста, выберите поезд для редактирования");
            return;
        }

        EditFlightController.currentFlightId = selected.getId();
        openWindow("/org/example/isarsw/fxml/flight_edit.fxml", "Редактирование поезда");
        loadFromDb();
    }

    @FXML
    public void onDelete() {
        Flight selected = flightsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Не выбран поезд", "Пожалуйста, выберите поезд для удаления");
            return;
        }

        if (!CommonUtils.confirm("Подтверждение удаления",
                "Вы уверены, что хотите удалить поезд " + selected.getNumber() + "?")) {
            return;
        }

        try {
            flightService.deleteFlight(selected.getId());
            masterData.remove(selected);
            updateStatusLabel();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Ошибка удаления", "Не удалось удалить поезд: " + e.getMessage());
        }
    }

    @FXML
    public void onHistory() {
        Flight selected = flightsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Не выбран поезд", "Пожалуйста, выберите поезд для просмотра истории");
            return;
        }

        HistoryController.currentFlightId = selected.getId();
        openWindow("/org/example/isarsw/fxml/history.fxml", "История изменений поезда " + selected.getNumber());
    }

    @FXML
    public void onFilters() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/isarsw/fxml/filters.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(mainPane.getScene().getWindow());
            stage.setTitle("Фильтры");

            Pane root = loader.load();
            Scene scene = new Scene(root);

            URL cssUrl = getClass().getResource("/org/example/isarsw/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);
            stage.sizeToScene();

            // Устанавливаем иконку
            setWindowIcon(stage);

            centerStage(stage);

            FiltersController fc = loader.getController();
            fc.setMainController(this);

            stage.showAndWait();
            updateStatusLabel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------- CONTEXT MENU ----------
    @FXML
    private void onTableRightClick() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("Редактировать");
        editItem.setOnAction(e -> onEdit());

        MenuItem deleteItem = new MenuItem("Удалить");
        deleteItem.setOnAction(e -> onDelete());

        MenuItem historyItem = new MenuItem("История изменений");
        historyItem.setOnAction(e -> onHistory());

        MenuItem refreshItem = new MenuItem("Обновить");
        refreshItem.setOnAction(e -> onRefresh());

        contextMenu.getItems().addAll(editItem, deleteItem, historyItem, new SeparatorMenuItem(), refreshItem);
        flightsTable.setContextMenu(contextMenu);
    }

    // ---------- ОТКРЫТИЕ ОКОН ----------
    private void openWindow(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(mainPane.getScene().getWindow());
            stage.setTitle(title);

            Pane root = loader.load();
            Scene scene = new Scene(root);

            URL cssUrl = getClass().getResource("/org/example/isarsw/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);
            stage.sizeToScene();

            // ВАЖНО: Устанавливаем иконку для дочернего окна
            setWindowIcon(stage);

            centerStage(stage);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка открытия окна", "Не удалось открыть " + title + ": " + e.getMessage());
        }
    }

    // Метод для установки иконки окна
    private void setWindowIcon(Stage stage) {
        if (App.getAppIcon() != null) {
            stage.getIcons().clear();
            stage.getIcons().add(App.getAppIcon());
        }
    }

    private void centerStage(Stage stage) {
        Stage mainStage = (Stage) mainPane.getScene().getWindow();

        double centerX = mainStage.getX() + mainStage.getWidth() / 2;
        double centerY = mainStage.getY() + mainStage.getHeight() / 2;

        double x = centerX - stage.getWidth() / 2;
        double y = centerY - stage.getHeight() / 2;

        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        if (x < bounds.getMinX()) x = bounds.getMinX();
        if (y < bounds.getMinY()) y = bounds.getMinY();
        if (x + stage.getWidth() > bounds.getMaxX()) x = bounds.getMaxX() - stage.getWidth();
        if (y + stage.getHeight() > bounds.getMaxY()) y = bounds.getMaxY() - stage.getHeight();

        stage.setX(x);
        stage.setY(y);
    }
}