package dao;

import db.DatabaseManager;
import model.User;
import model.TransferRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final DatabaseManager dbManager;

    public UserDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public User findById(Long id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ? FOR UPDATE";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }
        }
        return users;
    }

    public void create(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, enabled, balance, permissions_flags) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(false);
            try {
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getPassword());
                stmt.setBoolean(3, user.isEnabled());
                stmt.setDouble(4, user.getBalance());
                stmt.setInt(5, user.getPermissionsFlags());
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getLong(1));
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

    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, password = ?, enabled = ?, balance = ?, permissions_flags = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getPassword());
                stmt.setBoolean(3, user.isEnabled());
                stmt.setDouble(4, user.getBalance());
                stmt.setInt(5, user.getPermissionsFlags());
                stmt.setLong(6, user.getId());
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

    public void updateBalance(Long userId, double newBalance) throws SQLException {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                stmt.setDouble(1, newBalance);
                stmt.setLong(2, userId);
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

    public boolean updateBalanceWithVersion(Long userId, double oldBalance, double newBalance) throws SQLException {
        String sql = "UPDATE users SET balance = ? WHERE id = ? AND balance = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                stmt.setDouble(1, newBalance);
                stmt.setLong(2, userId);
                stmt.setDouble(3, oldBalance);
                int rowsAffected = stmt.executeUpdate();
                conn.commit();
                return rowsAffected > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                stmt.setLong(1, id);
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

    public boolean transfer(Long fromUserId, Long toUserId, double amount, TransferRecord record) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            // 查询并锁定转出用户
            String fromSql = "SELECT * FROM users WHERE id = ? FOR UPDATE";
            User fromUser;
            try (PreparedStatement stmt = conn.prepareStatement(fromSql)) {
                stmt.setLong(1, fromUserId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    fromUser = extractUserFromResultSet(rs);
                }
            }

            // 检查余额
            if (fromUser.getBalance() < amount) {
                conn.rollback();
                return false;
            }

            // 更新转出用户余额
            String updateFromSql = "UPDATE users SET balance = balance - ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateFromSql)) {
                stmt.setDouble(1, amount);
                stmt.setLong(2, fromUserId);
                stmt.executeUpdate();
            }

            // 更新转入用户余额
            String updateToSql = "UPDATE users SET balance = balance + ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateToSql)) {
                stmt.setDouble(1, amount);
                stmt.setLong(2, toUserId);
                stmt.executeUpdate();
            }

            // 记录操作日志
            String logSql = "INSERT INTO operation_logs (user_id, operation) VALUES (?, ?)";
            Long logId;
            try (PreparedStatement stmt = conn.prepareStatement(logSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, fromUserId);
                stmt.setString(2, "转账: " + amount + " 到用户 " + toUserId);
                stmt.executeUpdate();
                
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    logId = rs.getLong(1);
                }
            }

            // 创建转账记录
            String recordSql = "INSERT INTO transfer_record (from_user_id, to_user_id, amount, timestamp, remark, status, permissions_snapshot, log_id) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(recordSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, record.getFromUserId());
                stmt.setLong(2, record.getToUserId());
                stmt.setBigDecimal(3, record.getAmount());
                stmt.setTimestamp(4, Timestamp.valueOf(record.getTimestamp()));
                stmt.setString(5, record.getRemark());
                stmt.setString(6, record.getStatus());
                stmt.setInt(7, record.getPermissionsSnapshot());
                stmt.setLong(8, logId);
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        record.setId(rs.getLong(1));
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    // 记录回滚失败的错误
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    // 记录恢复自动提交失败的错误
                }
            }
        }
    }

    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEnabled(rs.getBoolean("enabled"));
        user.setBalance(rs.getDouble("balance"));
        user.setPermissionsFlags(rs.getInt("permissions_flags"));
        return user;
    }
} 