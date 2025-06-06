package server;

import com.sun.net.httpserver.HttpServer;
import db.DatabaseManager;
import middleware.LoggingMiddleware;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.format.DateTimeFormatter;

public class Server {
    private final int port;
    private final ExecutorService threadPool;
    private HttpServer server;
    private boolean running;
    private final DatabaseManager dbManager;
    private final LoggingMiddleware loggingMiddleware;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Server(int port) {
        this.port = port;
        this.threadPool = Executors.newCachedThreadPool();
        this.dbManager = DatabaseManager.getInstance();
        this.loggingMiddleware = LoggingMiddleware.getInstance();
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(threadPool);
            
            // 注册路由处理器
            registerHandlers();
            
            server.start();
            running = true;
            System.out.println("HTTP服务器启动在端口: " + port);
        } catch (IOException e) {
            System.out.println("服务器启动失败: " + e.getMessage());
        }
    }

    private void registerHandlers() {
        // 用户相关接口
        server.createContext("/api/register", new ClientHandler());
        server.createContext("/api/login", new ClientHandler());
        server.createContext("/api/logout", new ClientHandler());
        server.createContext("/api/deposit", new ClientHandler());
        server.createContext("/api/withdraw", new ClientHandler());
        server.createContext("/api/transfer", new ClientHandler());
        server.createContext("/api/balance", new ClientHandler());
        server.createContext("/api/change-password", new ClientHandler());
        server.createContext("/api/profile", new ClientHandler());

        // 管理员相关接口
        server.createContext("/api/admin/login", new ClientHandler());
        server.createContext("/api/admin/logout", new ClientHandler());
        server.createContext("/api/admin/create-user", new ClientHandler());
        server.createContext("/api/admin/update-user", new ClientHandler());
        server.createContext("/api/admin/delete-user", new ClientHandler());
        server.createContext("/api/admin/disable-user", new ClientHandler());
        server.createContext("/api/admin/list-users", new ClientHandler());
        server.createContext("/api/admin/export-logs", new ClientHandler());
        server.createContext("/api/admin/transaction-total-amount", new ClientHandler());
        server.createContext("/api/admin/logs", new ClientHandler());
    }

    public void stop() {
        if (!running) {
            return;
        }
        
        System.out.println("正在关闭服务器...");
        running = false;
        
        // 停止HTTP服务器
        if (server != null) {
            server.stop(0);
            System.out.println("HTTP服务器已停止");
        }
        
        // 关闭线程池
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
        
        // 关闭数据库连接池
        dbManager.shutdown();
        
        // 关闭日志中间件
        loggingMiddleware.shutdown();
        
        System.out.println("服务器已完全关闭");
    }
} 