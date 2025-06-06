package handler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dao.*;
import middleware.RequestContext;
import model.*;
import util.TokenManager;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.logging.Logger;

public abstract class BaseRequestHandler {
    protected static final Logger logger = Logger.getLogger(BaseRequestHandler.class.getName());
    protected static final Gson gson = new Gson();
    protected static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    protected final UserDAO userDAO;
    protected final AdminDAO adminDAO;
    protected final OperationLogDAO operationLogDAO;
    protected final TokenManager tokenManager;

    // HTTP状态码常量
    public static final int OK = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int INTERNAL_ERROR = 500;

    public BaseRequestHandler() {
        this.userDAO = new UserDAO();
        this.adminDAO = new AdminDAO();
        this.operationLogDAO = new OperationLogDAO();
        this.tokenManager = TokenManager.getInstance();
    }

    protected String extractPath(RequestContext context) {
        return context.getExchange().getRequestURI().getPath();
    }

    protected String extractMethod(RequestContext context) {
        return context.getExchange().getRequestMethod();
    }

    protected String extractBody(RequestContext context) {
        String body = context.getRequestBody();
        return body.isEmpty() ? "{}" : body;
    }

    protected void logOperation(Long userId, String operation) throws SQLException {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperation(operation);
        log.setTimestamp(LocalDateTime.now());
        operationLogDAO.create(log);
    }

    public abstract ResponseResult handleRequest(RequestContext context);

    public static class ResponseResult {
        private final int statusCode;
        private final String message;
        private final boolean isJson;

        public ResponseResult(int statusCode, String message) {
            this.statusCode = statusCode;
            this.message = message;
            this.isJson = (message.startsWith("{") && message.endsWith("}")) || 
                         (message.startsWith("[") && message.endsWith("]"));
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getMessage() {
            return message;
        }

        public String toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("status", statusCode >= 200 && statusCode < 300 ? "success" : "error");
            if (isJson) {
                if (message.startsWith("[")) {
                    JsonArray messageArray = gson.fromJson(message, JsonArray.class);
                    json.add("message", messageArray);
                } else {
                    JsonObject messageJson = gson.fromJson(message, JsonObject.class);
                    json.add("message", messageJson);
                }
            } else {
                json.addProperty("message", message);
            }
            return gson.toJson(json);
        }
    }

    // 规范化 HTTP 头部字段名称
    protected String normalizeHeaderName(String name) {
        if (name == null) return null;
        return name.toLowerCase();
    }

    // 从头部 Map 中获取值（不区分大小写）
    protected String getHeaderValue(Map<String, String> headers, String name) {
        if (headers == null || name == null) return null;
        String normalizedName = normalizeHeaderName(name);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (normalizeHeaderName(entry.getKey()).equals(normalizedName)) {
                return entry.getValue();
            }
        }
        return null;
    }
} 