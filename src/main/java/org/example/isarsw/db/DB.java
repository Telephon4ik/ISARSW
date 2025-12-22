package org.example.isarsw.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DB {
    private static final String DB_URL = "jdbc:sqlite:app.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void init() throws SQLException {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Проверяем существование таблицы flights
            boolean tableExists = false;
            try (var rs = conn.getMetaData().getTables(null, null, "flights", null)) {
                tableExists = rs.next();
            }

            if (tableExists) {
                // Проверяем наличие старых колонок и мигрируем если нужно
                migrateIfNeeded(conn, st);
            } else {
                // Создаем новую таблицу с новой схемой
                createNewTables(st);
            }

            // Создаем таблицу history (если не существует)
            createHistoryTable(st);

            // Создаем таблицу для хранения конфигурации
            createConfigTable(st);

            conn.commit();
            System.out.println("База данных инициализирована успешно");

        } catch (SQLException e) {
            System.err.println("Ошибка инициализации базы данных: " + e.getMessage());
            throw e;
        }
    }

    private static void migrateIfNeeded(Connection conn, Statement st) throws SQLException {
        // Проверяем наличие колонки standing_time
        boolean hasStandingTime = false;
        try (var rs = conn.getMetaData().getColumns(null, null, "flights", "standing_time")) {
            hasStandingTime = rs.next();
        }

        // Проверяем наличие колонки last_arrival_check
        boolean hasLastArrivalCheck = false;
        try (var rs = conn.getMetaData().getColumns(null, null, "flights", "last_arrival_check")) {
            hasLastArrivalCheck = rs.next();
        }

        if (!hasStandingTime || !hasLastArrivalCheck) {
            System.out.println("Обнаружена старая схема базы данных. Выполняем миграцию...");

            // Создаем временную таблицу с новой схемой
            st.executeUpdate("CREATE TABLE flights_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "number TEXT NOT NULL," +
                    "route TEXT NOT NULL," +
                    "arrive_ts INTEGER NOT NULL," +
                    "standing_time INTEGER NOT NULL DEFAULT 30," +
                    "platform TEXT NOT NULL," +
                    "status TEXT NOT NULL," +
                    "created_at INTEGER NOT NULL," +
                    "updated_at INTEGER NOT NULL," +
                    "last_arrival_check INTEGER DEFAULT 0" +
                    ");");

            // Копируем данные из старой таблицы
            boolean hasArriveTs = false;
            try (var rs = conn.getMetaData().getColumns(null, null, "flights", "arrive_ts")) {
                hasArriveTs = rs.next();
            }

            if (hasArriveTs) {
                // Копируем существующие данные
                if (hasLastArrivalCheck) {
                    st.executeUpdate("INSERT INTO flights_new " +
                            "(id, number, route, arrive_ts, standing_time, platform, status, created_at, updated_at, last_arrival_check) " +
                            "SELECT id, number, route, arrive_ts, standing_time, platform, status, created_at, updated_at, last_arrival_check " +
                            "FROM flights");
                } else {
                    st.executeUpdate("INSERT INTO flights_new " +
                            "(id, number, route, arrive_ts, standing_time, platform, status, created_at, updated_at, last_arrival_check) " +
                            "SELECT id, number, route, arrive_ts, COALESCE(standing_time, 30), platform, status, created_at, updated_at, 0 " +
                            "FROM flights");
                }
            } else {
                long now = System.currentTimeMillis() / 1000;
                st.executeUpdate("INSERT INTO flights_new " +
                        "(id, number, route, arrive_ts, standing_time, platform, status, created_at, updated_at, last_arrival_check) " +
                        "SELECT id, number, route, " + now + ", 30, platform, status, created_at, updated_at, 0 " +
                        "FROM flights");
            }

            // Удаляем старую таблицу и переименовываем новую
            st.executeUpdate("DROP TABLE flights");
            st.executeUpdate("ALTER TABLE flights_new RENAME TO flights");

            System.out.println("Миграция базы данных выполнена успешно");
        } else {
            System.out.println("База данных уже в новой схеме");
        }
    }

    private static void createNewTables(Statement st) throws SQLException {
        st.executeUpdate(
                "CREATE TABLE flights (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "number TEXT NOT NULL," +
                        "route TEXT NOT NULL," +
                        "arrive_ts INTEGER NOT NULL," +
                        "standing_time INTEGER NOT NULL DEFAULT 30," +
                        "platform TEXT NOT NULL," +
                        "status TEXT NOT NULL," +
                        "created_at INTEGER NOT NULL," +
                        "updated_at INTEGER NOT NULL," +
                        "last_arrival_check INTEGER DEFAULT 0" +
                        ");"
        );
        System.out.println("Создана таблица flights с новой схемой");
    }

    private static void createHistoryTable(Statement st) throws SQLException {
        st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS history (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "flight_id INTEGER," +
                        "action TEXT NOT NULL," +
                        "actor TEXT NOT NULL," +
                        "timestamp INTEGER NOT NULL," +
                        "payload_before TEXT," +
                        "payload_after TEXT," +
                        "FOREIGN KEY(flight_id) REFERENCES flights(id) ON DELETE CASCADE" +
                        ");"
        );
    }

    private static void createConfigTable(Statement st) throws SQLException {
        st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS config (" +
                        "key TEXT PRIMARY KEY," +
                        "value TEXT" +
                        ");"
        );
    }

    public static void recreateTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Удаляем таблицы
            st.executeUpdate("DROP TABLE IF EXISTS history");
            st.executeUpdate("DROP TABLE IF EXISTS config");
            st.executeUpdate("DROP TABLE IF EXISTS flights");

            // Создаем заново с новой схемой
            st.executeUpdate(
                    "CREATE TABLE flights (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "number TEXT NOT NULL," +
                            "route TEXT NOT NULL," +
                            "arrive_ts INTEGER NOT NULL," +
                            "standing_time INTEGER NOT NULL DEFAULT 30," +
                            "platform TEXT NOT NULL," +
                            "status TEXT NOT NULL," +
                            "created_at INTEGER NOT NULL," +
                            "updated_at INTEGER NOT NULL," +
                            "last_arrival_check INTEGER DEFAULT 0" +
                            ");"
            );

            st.executeUpdate(
                    "CREATE TABLE history (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "flight_id INTEGER," +
                            "action TEXT NOT NULL," +
                            "actor TEXT NOT NULL," +
                            "timestamp INTEGER NOT NULL," +
                            "payload_before TEXT," +
                            "payload_after TEXT," +
                            "FOREIGN KEY(flight_id) REFERENCES flights(id) ON DELETE CASCADE" +
                            ");"
            );

            st.executeUpdate(
                    "CREATE TABLE config (" +
                            "key TEXT PRIMARY KEY," +
                            "value TEXT" +
                            ");"
            );

            conn.commit();
            System.out.println("Таблицы пересозданы с новой схемой");
        }
    }

    public static void checkIntegrity() throws SQLException {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             var rs = st.executeQuery("PRAGMA integrity_check")) {

            if (rs.next()) {
                String result = rs.getString(1);
                if ("ok".equals(result)) {
                    System.out.println("Проверка целостности базы данных: OK");
                } else {
                    System.err.println("Проблемы с целостностью базы данных: " + result);
                }
            }
        }
    }

    // Метод для очистки всех данных (для тестирования)
    public static void clearAllData() throws SQLException {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {

            conn.setAutoCommit(false);
            st.executeUpdate("DELETE FROM history");
            st.executeUpdate("DELETE FROM flights");
            st.executeUpdate("DELETE FROM config");
            conn.commit();
            System.out.println("Все данные очищены");
        }
    }
}