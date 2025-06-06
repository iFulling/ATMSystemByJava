package db;

import config.ConfigManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final BlockingQueue<Connection> connectionPool;
    private final ConfigManager configManager;

    private DatabaseManager() {
        configManager = ConfigManager.getInstance();
        int poolSize = configManager.getIntProperty("db.pool.size", 10);
        connectionPool = new ArrayBlockingQueue<>(poolSize);
        initializePool();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializePool() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            for (int i = 0; i < connectionPool.remainingCapacity(); i++) {
                Connection conn = createConnection();
                if (conn != null) {
                    connectionPool.offer(conn);
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL驱动加载失败: " + e.getMessage());
        }
    }

    private Connection createConnection() {
        try {
            String url = configManager.getProperty("db.url");
            String username = configManager.getProperty("db.username");
            String password = configManager.getProperty("db.password");
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            System.out.println("创建数据库连接失败: " + e.getMessage());
            return null;
        }
    }

    public Connection getConnection() throws SQLException {
        try {
            Connection conn = connectionPool.poll();
            if (conn == null || conn.isClosed()) {
                conn = createConnection();
            }
            return conn;
        } catch (SQLException e) {
            System.out.println("获取数据库连接失败: " + e.getMessage());
            throw e;
        }
    }

    public void releaseConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed() && connectionPool.size() < connectionPool.remainingCapacity()) {
                    connectionPool.offer(conn);
                } else {
                    conn.close();
                }
            } catch (SQLException e) {
                System.out.println("释放数据库连接失败: " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        System.out.println("正在关闭数据库连接池...");
        Connection conn;
        while ((conn = connectionPool.poll()) != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.out.println("关闭数据库连接失败: " + e.getMessage());
            }
        }
        System.out.println("数据库连接池已关闭");
    }
} 