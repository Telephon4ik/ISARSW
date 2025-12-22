package org.example.isarsw.dao;

import org.example.isarsw.db.DB;
import org.example.isarsw.model.HistoryEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HistoryDao {

    public void insert(HistoryEntry e) throws SQLException {
        String sql = "INSERT INTO history(flight_id, action, actor, timestamp, payload_before, payload_after) VALUES(?,?,?,?,?,?)";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (e.getFlightId() == null) ps.setNull(1, Types.BIGINT);
            else ps.setLong(1, e.getFlightId());
            ps.setString(2, e.getAction());
            ps.setString(3, e.getActor());
            ps.setLong(4, e.getTimestamp());
            ps.setString(5, e.getPayloadBefore());
            ps.setString(6, e.getPayloadAfter());
            ps.executeUpdate();
        }
    }

    public List<HistoryEntry> listByFlight(Long flightId) throws SQLException {
        String sql = "SELECT * FROM history WHERE flight_id = ? ORDER BY timestamp DESC";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, flightId);
            try (ResultSet rs = ps.executeQuery()) {
                List<HistoryEntry> out = new ArrayList<>();
                while (rs.next()) {
                    HistoryEntry e = new HistoryEntry();
                    e.setId(rs.getLong("id"));
                    e.setFlightId(rs.getLong("flight_id"));
                    e.setAction(rs.getString("action"));
                    e.setActor(rs.getString("actor"));
                    e.setTimestamp(rs.getLong("timestamp"));
                    e.setPayloadBefore(rs.getString("payload_before"));
                    e.setPayloadAfter(rs.getString("payload_after"));
                    out.add(e);
                }
                return out;
            }
        }
    }
}