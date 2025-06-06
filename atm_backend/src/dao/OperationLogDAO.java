package dao;

import db.DatabaseManager;
import model.OperationLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OperationLogDAO {
    private final DatabaseManager dbManager;

    public OperationLogDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public void create(OperationLog log) throws SQLException {
        String sql = "INSERT INTO operation_logs (user_id, operation, timestamp) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, log.getUserId());
            stmt.setString(2, log.getOperation());
            stmt.setTimestamp(3, Timestamp.valueOf(log.getTimestamp()));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setId(rs.getLong(1));
                }
            }
        }
    }

    public List<OperationLog> findByUserId(Long userId) throws SQLException {
        List<OperationLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM operation_logs WHERE user_id = ? ORDER BY timestamp DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(extractLogFromResultSet(rs));
                }
            }
        }
        return logs;
    }

    public List<OperationLog> findAll() throws SQLException {
        List<OperationLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM operation_logs ORDER BY timestamp DESC";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(extractLogFromResultSet(rs));
            }
        }
        return logs;
    }

    public List<OperationLog> findByDateRange(LocalDateTime start, LocalDateTime end) throws SQLException {
        List<OperationLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM operation_logs WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(extractLogFromResultSet(rs));
                }
            }
        }
        return logs;
    }

    public List<OperationLog> findAllPaginated(int page, int pageSize) throws SQLException {
        List<OperationLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM operation_logs ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pageSize);
            stmt.setInt(2, (page - 1) * pageSize);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(extractLogFromResultSet(rs));
                }
            }
        }
        return logs;
    }

    public List<OperationLog> findByUserIdPaginated(Long userId, int page, int pageSize) throws SQLException {
        List<OperationLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM operation_logs WHERE user_id = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setInt(2, pageSize);
            stmt.setInt(3, (page - 1) * pageSize);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(extractLogFromResultSet(rs));
                }
            }
        }
        return logs;
    }

    public int getTotalCount() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM operation_logs";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
            return 0;
        }
    }

    public int getTotalCountByUserId(Long userId) throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM operation_logs WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
                return 0;
            }
        }
    }

    private OperationLog extractLogFromResultSet(ResultSet rs) throws SQLException {
        OperationLog log = new OperationLog();
        log.setId(rs.getLong("id"));
        log.setUserId(rs.getLong("user_id"));
        log.setOperation(rs.getString("operation"));
        log.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        return log;
    }
} 