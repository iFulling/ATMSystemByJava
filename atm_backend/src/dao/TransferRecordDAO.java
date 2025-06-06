package dao;

import db.DatabaseManager;
import model.TransferRecord;

import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransferRecordDAO {
    private final DatabaseManager dbManager;

    public TransferRecordDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public void create(TransferRecord record) throws SQLException {
        String sql = "INSERT INTO transfer_record (from_user_id, to_user_id, amount, timestamp, remark, status, permissions_snapshot) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(false);
            try {
                stmt.setLong(1, record.getFromUserId());
                stmt.setLong(2, record.getToUserId());
                stmt.setBigDecimal(3, record.getAmount());
                stmt.setTimestamp(4, Timestamp.valueOf(record.getTimestamp()));
                stmt.setString(5, record.getRemark());
                stmt.setString(6, record.getStatus());
                stmt.setInt(7, record.getPermissionsSnapshot());
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        record.setId(rs.getLong(1));
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<TransferRecord> findByFromUserId(Long userId) throws SQLException {
        List<TransferRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM transfer_record WHERE from_user_id = ? ORDER BY timestamp DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(extractRecordFromResultSet(rs));
                }
            }
        }
        return records;
    }

    public List<TransferRecord> findByToUserId(Long userId) throws SQLException {
        List<TransferRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM transfer_record WHERE to_user_id = ? ORDER BY timestamp DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(extractRecordFromResultSet(rs));
                }
            }
        }
        return records;
    }

    public List<TransferRecord> findByDateRange(LocalDateTime start, LocalDateTime end) throws SQLException {
        List<TransferRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM transfer_record WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(extractRecordFromResultSet(rs));
                }
            }
        }
        return records;
    }

    public void updateStatus(Long id, String status) throws SQLException {
        String sql = "UPDATE transfer_record SET status = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                stmt.setString(1, status);
                stmt.setLong(2, id);
                stmt.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }


    public BigDecimal getTotalAmount() throws SQLException {
        String sql = "SELECT SUM(amount) as total FROM transfer_record";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total");
                return total != null ? total : BigDecimal.ZERO;
            }
            return BigDecimal.ZERO;
        }
    }


    public BigDecimal getTotalAmountByDateRange(LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = "SELECT SUM(amount) as total FROM transfer_record WHERE timestamp BETWEEN ? AND ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("total");
                    return total != null ? total : BigDecimal.ZERO;
                }
                return BigDecimal.ZERO;
            }
        }
    }

    private TransferRecord extractRecordFromResultSet(ResultSet rs) throws SQLException {
        TransferRecord record = new TransferRecord();
        record.setId(rs.getLong("id"));
        record.setFromUserId(rs.getLong("from_user_id"));
        record.setToUserId(rs.getLong("to_user_id"));
        record.setAmount(rs.getBigDecimal("amount"));
        record.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        record.setRemark(rs.getString("remark"));
        record.setStatus(rs.getString("status"));
        record.setPermissionsSnapshot(rs.getInt("permissions_snapshot"));
        return record;
    }
} 