package org.example.isarsw.dao;

import org.example.isarsw.db.DB;
import org.example.isarsw.model.Flight;
import org.example.isarsw.service.FlightService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlightDao {

    // ---------- ОСНОВНЫЕ CRUD МЕТОДЫ ----------

    public Flight create(Flight f) throws SQLException {
        String sql = "INSERT INTO flights(number, route, arrive_ts, standing_time, platform, status, created_at, updated_at, last_arrival_check) " +
                "VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setFlightParameters(ps, f);
            ps.setLong(7, f.getCreatedAt());
            ps.setLong(8, f.getUpdatedAt());
            ps.setLong(9, f.getLastArrivalCheck());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    f.setId(rs.getLong(1));
                    return f;
                } else {
                    throw new SQLException("Creating flight failed, no ID obtained.");
                }
            }
        }
    }

    public void update(Flight f) throws SQLException {
        String sql = "UPDATE flights SET number=?, route=?, arrive_ts=?, standing_time=?, platform=?, status=?, updated_at=?, last_arrival_check=? WHERE id=?";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            setFlightParameters(ps, f);
            ps.setLong(7, f.getUpdatedAt());
            ps.setLong(8, f.getLastArrivalCheck());
            ps.setLong(9, f.getId());

            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM flights WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<Flight> findById(long id) throws SQLException {
        String sql = "SELECT * FROM flights WHERE id = ?";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        }
    }

    public List<Flight> findAll() throws SQLException {
        String sql = "SELECT * FROM flights ORDER BY arrive_ts";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Flight> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public List<Flight> search(String query) throws SQLException {
        String sql = "SELECT * FROM flights WHERE number LIKE ? OR route LIKE ? OR platform LIKE ? ORDER BY arrive_ts";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String q = "%" + query + "%";
            ps.setString(1, q);
            ps.setString(2, q);
            ps.setString(3, q);

            try (ResultSet rs = ps.executeQuery()) {
                List<Flight> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    // ---------- ВАЖНЫЕ БИЗНЕС-МЕТОДЫ ----------

    public List<Flight> findConflicting(String platform, long newArriveTs, int newStandingTime, Long excludeId) throws SQLException {
        long newDepartureTs = newArriveTs + (newStandingTime * 60L);

        String sql = "SELECT * FROM flights WHERE platform = ?"
                + (excludeId != null ? " AND id != ? " : " ")
                + " AND arrive_ts < ? AND (arrive_ts + (standing_time * 60)) > ?"
                + " AND status NOT IN (?, ?)";

        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, platform);
            if (excludeId != null) ps.setLong(idx++, excludeId);
            ps.setLong(idx++, newDepartureTs);
            ps.setLong(idx++, newArriveTs);
            ps.setString(idx++, FlightService.STATUS_CANCELLED);
            ps.setString(idx, FlightService.STATUS_DEPARTED);

            try (ResultSet rs = ps.executeQuery()) {
                List<Flight> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public List<Flight> findFlightsNeedingArrivalCheck(long timeWindowStart, long timeWindowEnd) throws SQLException {
        String sql = "SELECT * FROM flights WHERE status = ? AND " +
                "arrive_ts BETWEEN ? AND ? AND " +
                "(last_arrival_check = 0 OR last_arrival_check < arrive_ts) " +
                "ORDER BY arrive_ts";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, FlightService.STATUS_EN_ROUTE);
            ps.setLong(2, timeWindowStart);
            ps.setLong(3, timeWindowEnd);

            try (ResultSet rs = ps.executeQuery()) {
                List<Flight> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public List<Flight> findFlightsReadyForDeparture(long currentTime) throws SQLException {
        String sql = "SELECT * FROM flights WHERE status = ? AND " +
                "(arrive_ts + (standing_time * 60)) <= ? " +
                "ORDER BY arrive_ts";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, FlightService.STATUS_BOARDING);
            ps.setLong(2, currentTime);

            try (ResultSet rs = ps.executeQuery()) {
                List<Flight> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    // ---------- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ----------

    private void setFlightParameters(PreparedStatement ps, Flight f) throws SQLException {
        ps.setString(1, f.getNumber());
        ps.setString(2, f.getRoute());
        ps.setLong(3, f.getArriveTs());
        ps.setInt(4, f.getStandingTime());
        ps.setString(5, f.getPlatform());
        ps.setString(6, f.getStatus());
    }

    private Flight map(ResultSet rs) throws SQLException {
        Flight f = new Flight();
        f.setId(rs.getLong("id"));
        f.setNumber(rs.getString("number"));
        f.setRoute(rs.getString("route"));
        f.setArriveTs(rs.getLong("arrive_ts"));
        f.setStandingTime(rs.getInt("standing_time"));
        f.setPlatform(rs.getString("platform"));
        f.setStatus(rs.getString("status"));
        f.setCreatedAt(rs.getLong("created_at"));
        f.setUpdatedAt(rs.getLong("updated_at"));
        f.setLastArrivalCheck(rs.getLong("last_arrival_check"));
        return f;
    }

    // УДАЛЯЕМ ТОЛЬКО ТЕ МЕТОДЫ, КОТОРЫЕ ТОЧНО НЕ ИСПОЛЬЗУЮТСЯ:

    // 1. findFlightsNeedingStatusUpdate - используется в StatusScheduler
    public List<Flight> findFlightsNeedingStatusUpdate(long currentTime) throws SQLException {
        String sql = "SELECT * FROM flights WHERE " +
                "status NOT IN (?, ?) AND " +
                "arrive_ts <= ? " +
                "ORDER BY arrive_ts";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, FlightService.STATUS_CANCELLED);
            ps.setString(2, FlightService.STATUS_DEPARTED);
            ps.setLong(3, currentTime);

            try (ResultSet rs = ps.executeQuery()) {
                List<Flight> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    // 2. updateLastArrivalCheck - используется в ArrivalCheckService
    public void updateLastArrivalCheck(long flightId, long timestamp) throws SQLException {
        String sql = "UPDATE flights SET last_arrival_check = ? WHERE id = ?";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, timestamp);
            ps.setLong(2, flightId);
            ps.executeUpdate();
        }
    }

    // 3. findFlightsDelayedOver24Hours - используется в ArrivalCheckService
    public List<Flight> findFlightsDelayedOver24Hours(long twentyFourHoursAgo) throws SQLException {
        String sql = "SELECT * FROM flights WHERE status = ? AND arrive_ts < ? " +
                "ORDER BY arrive_ts";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, FlightService.STATUS_DELAYED);
            ps.setLong(2, twentyFourHoursAgo);

            try (ResultSet rs = ps.executeQuery()) {
                List<Flight> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }
}