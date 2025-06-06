package middleware;

import config.ConfigManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日志中间件，记录请求和响应信息
 */
public class LoggingMiddleware implements Middleware {
    private static final Logger logger = Logger.getLogger(LoggingMiddleware.class.getName());
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ConfigManager configManager = ConfigManager.getInstance();
    private static LoggingMiddleware instance;
    private static FileHandler fileHandler;
    private static ConsoleHandler consoleHandler;
    private static boolean initialized = false;

    public LoggingMiddleware() {
        initializeLogger();
    }

    public static synchronized LoggingMiddleware getInstance() {
        if (instance == null) {
            instance = new LoggingMiddleware();
        }
        return instance;
    }

    private synchronized void initializeLogger() {
        if (initialized) {
            return;
        }

        try {
            // 获取根日志记录器
            Logger rootLogger = Logger.getLogger("");
            
            // 移除所有现有的处理器
            for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }

            // 创建日志目录
            String logPath = configManager.getProperty("logging.file.path", "logs");
            File logDir = new File(logPath);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            // 获取日志配置
            String logFileName = configManager.getProperty("logging.file.name", "server-%d{yyyy-MM-dd}.log");
            int maxFileSize = configManager.getIntProperty("logging.max.file.size", 10485760); // 默认10MB
            int maxFiles = configManager.getIntProperty("logging.max.files", 30);
            boolean append = configManager.getBooleanProperty("logging.append", true);
            String logLevel = configManager.getProperty("logging.level", "INFO");
            
            // 处理日志文件名中的日期格式
            String formattedLogFileName = logFileName.replace("%d{yyyy-MM-dd}", 
                new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            
            // 创建文件处理器
            String logFilePath = logPath + File.separator + formattedLogFileName;
            fileHandler = new FileHandler(logFilePath, maxFileSize, maxFiles, append);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.parse(logLevel));
            
            // 创建控制台处理器
            consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter());
            consoleHandler.setLevel(Level.parse(logLevel));
            
            // 设置根日志记录器的级别
            rootLogger.setLevel(Level.parse(logLevel));
            
            // 添加处理器到根日志记录器
            rootLogger.addHandler(fileHandler);
            rootLogger.addHandler(consoleHandler);
            
            initialized = true;
            System.out.println("日志中间件初始化成功");
        } catch (Exception e) {
            System.err.println("初始化日志中间件失败: " + e.getMessage());
        }
    }

    @Override
    public void process(RequestContext context, MiddlewareChain chain) {
        String path = context.getExchange().getRequestURI().getPath();
        String method = context.getExchange().getRequestMethod();
        String clientIP = context.getExchange().getRemoteAddress().getAddress().getHostAddress();
        
        // 记录请求开始
        String timestamp;
        // String timestamp = LocalDateTime.now().format(formatter);
        // String requestLog = String.format("[%s] 收到请求: %s %s %s",
        //     timestamp, clientIP, method, path);
        // logger.info(requestLog);

        try {
            // 继续处理链
            chain.next(context);

            // 记录响应信息
            // timestamp = LocalDateTime.now().format(formatter);
            // String responseLog = String.format("[%s] 请求处理完成: %s %s %s (耗时: %dms)",
            //     timestamp, clientIP, method, path, context.getProcessingTime());
            // logger.info(responseLog);

        } catch (Exception e) {
            // 记录错误信息
            timestamp = LocalDateTime.now().format(formatter);
            String errorLog = String.format("[%s] 处理请求失败: %s %s %s - %s",
                timestamp, clientIP, method, path, e.getMessage());
            logger.log(Level.SEVERE, errorLog, e);

            // 设置错误响应
            context.setStatusCode(500);
            context.setResponseBody("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    public void shutdown() {
        if (fileHandler != null) {
            fileHandler.close();
        }
        if (consoleHandler != null) {
            consoleHandler.close();
        }
        initialized = false;
    }
} 