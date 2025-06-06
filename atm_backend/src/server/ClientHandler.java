package server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import handler.*;
import middleware.*;
import config.ConfigManager;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ClientHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private final UserRequestHandler userRequestHandler;
    private final AdminRequestHandler adminRequestHandler;
    private final MiddlewareChain middlewareChain;
    private final ConfigManager configManager;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static boolean middlewareEnabled;
    private static boolean authEnabled;

    public ClientHandler() {
        this.userRequestHandler = new UserRequestHandler();
        this.adminRequestHandler = new AdminRequestHandler();
        this.middlewareChain = new MiddlewareChain();
        this.configManager = ConfigManager.getInstance();
        middlewareEnabled = configManager.getBooleanProperty("middleware.enabled", false);
        boolean loggingEnabled = configManager.getBooleanProperty("middleware.logging.enabled", false);
        authEnabled = configManager.getBooleanProperty("middleware.auth.enabled", false);

        // 根据配置添加中间件
        if (middlewareEnabled && loggingEnabled) {
            middlewareChain.addMiddleware(new LoggingMiddleware());
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (middlewareEnabled && authEnabled) {
            middlewareChain.addMiddleware(new AuthenticationMiddleware());
        }
        // 处理OPTIONS请求（CORS）
        if (exchange.getRequestMethod().equals("OPTIONS")) {
            handleCORS(exchange);
            return;
        }

        // 添加CORS头
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        
        try {
            // 读取请求体
            String requestBody = "";
            if (exchange.getRequestMethod().equals("POST") || exchange.getRequestMethod().equals("PUT")) {
                InputStream requestBodyStream = exchange.getRequestBody();
                requestBody = new BufferedReader(new InputStreamReader(requestBodyStream))
                    .lines()
                    .collect(Collectors.joining("\n"));
            }

            // 将Headers转换为Map
            Map<String, String> headers = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    headers.put(entry.getKey(), entry.getValue().get(0));
                }
            }

            // 创建请求上下文
            RequestContext context = new RequestContext(exchange, headers);
            context.setRequestBody(requestBody);

            // 执行中间件链
            middlewareChain.next(context);

            // 如果中间件没有设置响应，则处理业务逻辑
            if (context.getStatusCode() == 0) {
                String path = exchange.getRequestURI().getPath();
                BaseRequestHandler.ResponseResult response;
                
                if (path.startsWith("/api/admin/")) {
                    response = adminRequestHandler.handleRequest(context);
                } else {
                    response = userRequestHandler.handleRequest(context);
                }
                
                context.setStatusCode(response.getStatusCode());
                context.setResponseBody(response.toJson());
            }

            // 发送响应
            sendResponse(exchange, context);
            
        } catch (Exception e) {
            String errorMessage = String.format("[%s] 处理请求失败: %s", 
                LocalDateTime.now().format(formatter), e.getMessage());
            logger.severe(errorMessage);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            String errorResponse = "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
            exchange.sendResponseHeaders(500, errorResponse.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes());
            }
        }
    }

    private void handleCORS(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(200, -1);
    }

    private void sendResponse(HttpExchange exchange, RequestContext context) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        String responseJson = context.getResponseBody();
        int statusCode = context.getStatusCode();
        
        // 记录响应日志
        String timestamp = LocalDateTime.now().format(formatter);
        String message = context.getMessage();
        String responseLog = String.format("[%s] 响应: %s %s - %d %s (耗时: %dms)",
            timestamp, method, path, statusCode, message == null ? responseJson : message, context.getProcessingTime());
        logger.info(responseLog);

        if (path.equals("/api/admin/export-logs")) {
            context.getExchange().getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
            context.getExchange().getResponseHeaders().set("Access-Control-Expose-Headers", "Content-Disposition");
            Gson gson = new Gson();
            context.getExchange().getResponseHeaders().set("Content-Disposition",
                    "attachment; filename=\"operation_logs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv\"");
            JsonObject jsonObject = gson.fromJson(responseJson, JsonObject.class);
            responseJson = String.valueOf(jsonObject.get("message"));
            responseJson = responseJson.substring(1, responseJson.length() - 1);
            responseJson = responseJson.replace("\\n", "\n");
        }else{
            exchange.getResponseHeaders().set("Content-Type", "application/json");
        }
        exchange.sendResponseHeaders(statusCode, responseJson.getBytes().length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseJson.getBytes());
        }
    }
} 