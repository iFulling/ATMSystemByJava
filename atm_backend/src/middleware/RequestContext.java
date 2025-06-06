package middleware;

import com.sun.net.httpserver.HttpExchange;
import model.Admin;
import model.User;
import java.util.Map;

/**
 * 请求上下文，用于在中间件之间传递数据
 */
public class RequestContext {
    private static final ThreadLocal<RequestContext> contextHolder = new ThreadLocal<>();
    
    private final HttpExchange exchange;
    private final Map<String, String> headers;
    private String requestBody;
    private User currentUser;
    private Admin currentAdmin;
    private final long startTime;
    private int statusCode;
    private String responseBody;
    private String message;

    public RequestContext(HttpExchange exchange, Map<String, String> headers) {
        this.exchange = exchange;
        this.headers = headers;
        this.startTime = System.currentTimeMillis();
        contextHolder.set(this);
    }

    public static void setContext(RequestContext context) {
        contextHolder.set(context);
    }

    public static RequestContext getContext() {
        return contextHolder.get();
    }

    public static void clearContext() {
        contextHolder.remove();
    }

    public HttpExchange getExchange() {
        return exchange;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public Admin getCurrentAdmin() {
        return currentAdmin;
    }

    public void setCurrentAdmin(Admin currentAdmin) {
        this.currentAdmin = currentAdmin;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public long getProcessingTime() {
        return System.currentTimeMillis() - startTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}