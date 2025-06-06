package middleware;

import config.ConfigManager;
import model.Admin;
import model.User;
import util.TokenManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 认证中间件，处理用户认证
 */
public class AuthenticationMiddleware implements Middleware {
    private static final Logger logger = Logger.getLogger(AuthenticationMiddleware.class.getName());
    private final TokenManager tokenManager;
    private final Set<String> publicPaths;
    private final Set<String> adminPaths;
    private static final ConfigManager configManager = ConfigManager.getInstance();
    private static final String ADMIN_BASE_PATH = "/api/admin/";

    public AuthenticationMiddleware() {
        this.tokenManager = TokenManager.getInstance();
        
        // 从配置文件读取公共路径
        String publicPathsStr = configManager.getProperty("middleware.auth.public.paths", "");
        this.publicPaths = new HashSet<>(Arrays.asList(publicPathsStr.split(",")));
        
        // 从配置文件读取管理员路径
        String adminPathsStr = configManager.getProperty("middleware.auth.admin.paths", "");
        this.adminPaths = new HashSet<>();
        for (String path : adminPathsStr.split(",")) {
            this.adminPaths.add(ADMIN_BASE_PATH + path.trim());
        }
    }

    @Override
    public void process(RequestContext context, MiddlewareChain chain) {
        String path = context.getExchange().getRequestURI().getPath();
        
        // 如果是公开路径，直接放行
        if (publicPaths.contains(path)) {
            chain.next(context);
            return;
        }

        // 获取token
        String token = context.getHeaders().get("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            context.setStatusCode(401);
            context.setResponseBody("{\"status\":\"error\",\"message\":\"未提供认证令牌\"}");
            return;
        }

        token = token.substring(7); // 移除"Bearer "前缀

        try {
            if (adminPaths.contains(path)) {
                Admin admin = tokenManager.validateAdminToken(token);
                if (admin == null) {
                    context.setStatusCode(401);
                    context.setResponseBody("{\"status\":\"error\",\"message\":\"请先登录\"}");
                    return;
                }
                // 检查用户状态
                if (!admin.isEnabled()) {
                    context.setStatusCode(403);
                    context.setResponseBody("{\"status\":\"error\",\"message\":\"账户已被禁用\"}");
                    return;
                }
                // 将用户信息存入上下文
                context.setCurrentAdmin(admin);
            } else {
                // 验证token并获取用户信息
                User user = tokenManager.validateToken(token);
                if (user == null) {
                    context.setStatusCode(401);
                    context.setResponseBody("{\"status\":\"error\",\"message\":\"请先登录\"}");
                    return;
                }
                // 检查用户状态
                if (!user.isEnabled()) {
                    context.setStatusCode(403);
                    context.setResponseBody("{\"status\":\"error\",\"message\":\"账户已被禁用\"}");
                    return;
                }
                // 将用户信息存入上下文
                context.setCurrentUser(user);
            }
            // 继续处理链
            chain.next(context);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "认证过程发生错误", e);
            context.setStatusCode(500);
            context.setResponseBody("{\"status\":\"error\",\"message\":\"认证过程发生错误\"}");
        }
    }
} 