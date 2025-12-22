package org.example.isarsw.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.example.isarsw.db.DB;
import org.example.isarsw.service.ArrivalCheckService;

import java.net.URL;

public class App extends Application {

    private ArrivalCheckService arrivalCheckService;
    private static Image appIcon; // ← Сохраняем иконку статически

    @Override
    public void start(Stage stage) throws Exception {
        try {
            System.out.println("Инициализация базы данных...");
            DB.init();
            System.out.println("База данных успешно инициализирована");
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка базы данных");
            alert.setHeaderText("Не удалось инициализировать базу данных");
            alert.setContentText("Ошибка: " + e.getMessage() + "\n\nПопробуйте удалить файл app.db и перезапустить приложение.");
            alert.showAndWait();
            System.exit(1);
        }

        try {
            // Загружаем иконку приложения
            appIcon = loadAppIcon();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/isarsw/fxml/main.fxml"));
            Scene scene = new Scene(loader.load());

            // Загружаем CSS
            try {
                String cssPath = "/org/example/isarsw/css/style.css";
                java.net.URL cssUrl = getClass().getResource(cssPath);
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                    System.out.println("CSS загружен успешно");
                } else {
                    System.err.println("CSS файл не найден: " + cssPath);
                }
            } catch (Exception e) {
                System.err.println("Не удалось загрузить CSS: " + e.getMessage());
                e.printStackTrace();
            }

            // Устанавливаем иконку для главного окна
            if (appIcon != null) {
                stage.getIcons().add(appIcon);
            }

            stage.setTitle("ЖД Диспетчер - Управление поездами");
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(600);

            // Центрируем главное окно
            stage.centerOnScreen();
            stage.show();

            // Запускаем сервис проверки прибытия
            arrivalCheckService = new ArrivalCheckService();
            arrivalCheckService.startChecking();
            System.out.println("Сервис проверки прибытия запущен");

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка запуска");
            alert.setHeaderText("Не удалось загрузить интерфейс");
            alert.setContentText("Ошибка: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private Image loadAppIcon() {
        try {
            URL iconUrl = getClass().getResource("/org/example/isarsw/images/app_icon.png");
            if (iconUrl != null) {
                return new Image(iconUrl.toExternalForm());
            }
            System.err.println("Иконка приложения не найдена");
        } catch (Exception e) {
            System.err.println("Не удалось загрузить иконку: " + e.getMessage());
        }
        return null;
    }

    // Метод для получения иконки приложения из других классов
    public static Image getAppIcon() {
        return appIcon;
    }

    @Override
    public void stop() throws Exception {
        // Останавливаем сервис при закрытии приложения
        if (arrivalCheckService != null) {
            arrivalCheckService.stopChecking();
        }
        super.stop();
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--recreate-db")) {
            try {
                System.out.println("Принудительное пересоздание таблиц...");
                DB.recreateTables();
                System.out.println("Таблицы пересозданы");
            } catch (Exception e) {
                System.err.println("Ошибка при пересоздании таблиц: " + e.getMessage());
            }
        }
        launch(args);
    }
}