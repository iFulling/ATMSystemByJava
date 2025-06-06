package util;

import config.ConfigManager;
import dao.AdminDAO;
import dao.UserDAO;
import model.Admin;
import model.User;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Token管理器，处理token的生成、验证和管理
 */
public class TokenManager {
    private static final Logger logger = Logger.getLogger(TokenManager.class.getName());
    private static final ConfigManager configManager = ConfigManager.getInstance();
    private static TokenManager instance;
    
    // 用户token存储，key为token，value为TokenInfo对象
    private final Map<String, TokenInfo> userTokens = new ConcurrentHashMap<>();
    private final Map<String, TokenInfo> adminTokens = new ConcurrentHashMap<>();
    // 用户设备token映射，key为用户ID，value为该用户的所有token
    private final Map<Long, Set<String>> userDeviceTokens = new ConcurrentHashMap<>();
    // 每个用户最大允许的设备数
    private static final int MAX_DEVICES_PER_USER = configManager.getIntProperty("user.devices.max", 3);
    // token过期时间（毫秒）
    private static final long TOKEN_EXPIRATION = configManager.getLongProperty("user.token.expiration", 86400000); // 24小时
    // token清理间隔（毫秒）
    private static final long TOKEN_CLEANUP_INTERVAL = configManager.getLongProperty("middleware.token.cleanup.interval", 300000); // 5分钟

    private TokenManager() {
        // 启动定时清理过期token的任务
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
            this::cleanupExpiredTokens, 
            TOKEN_CLEANUP_INTERVAL, 
            TOKEN_CLEANUP_INTERVAL, 
            TimeUnit.MILLISECONDS
        );
    }
    
    public static synchronized TokenManager getInstance() {
        if (instance == null) {
            instance = new TokenManager();
        }
        return instance;
    }
    
    /**
     * 生成新的token
     */
    private String generateToken() {
        return java.util.UUID.randomUUID().toString();
    }
    
    /**
     * 添加token
     */
    public String addToken(Long userId, String deviceInfo) {
        String token = generateToken();
        TokenInfo tokenInfo = new TokenInfo(userId, deviceInfo);
        userTokens.put(token, tokenInfo);
        userDeviceTokens.computeIfAbsent(userId, k -> new HashSet<>()).add(token);
        Set<String> userTokens = userDeviceTokens.get(userId);
        if (userTokens.size() > MAX_DEVICES_PER_USER) {
            String oldestToken = userTokens.iterator().next();
            removeToken(oldestToken);
        }
        return token;
    }
    
    public String addAdminToken(Long adminId) {
        String token = generateToken();
        TokenInfo tokenInfo = new TokenInfo(adminId, null); // 管理员不需要设备信息
        adminTokens.put(token, tokenInfo);
        return token;
    }
    
    /**
     * 移除token
     */
    public void removeToken(String token) {
        userTokens.remove(token);
        adminTokens.remove(token);
        TokenInfo tokenInfo = userTokens.get(token);
        if (tokenInfo != null) {
            Set<String> userTokens = userDeviceTokens.get(tokenInfo.getUserId());
            if (userTokens != null) {
                userTokens.remove(token);
                if (userTokens.isEmpty()) {
                    userDeviceTokens.remove(tokenInfo.getUserId());
                }
            }
        }
    }
    
    /**
     * 验证token并返回用户信息
     */
    public User validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        TokenInfo tokenInfo = userTokens.get(token);
        if (tokenInfo == null || tokenInfo.isExpired()) {
            if (tokenInfo != null) {
                removeToken(token);
            }
            return null;
        }
        try {
            return new UserDAO().findById(tokenInfo.getUserId());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "获取用户信息失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    public Admin validateAdminToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        TokenInfo tokenInfo = adminTokens.get(token);
        if (tokenInfo == null || tokenInfo.isExpired()) {
            if (tokenInfo != null) {
                removeToken(token);
            }
            return null;
        }
        try {
            return new AdminDAO().findById(tokenInfo.getUserId());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "获取管理员信息失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 清理过期token
     */
    public void cleanupExpiredTokens() {
        userTokens.entrySet().removeIf(entry -> entry.getValue().isExpired());
        adminTokens.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * Token信息类
     */
    private static class TokenInfo {
        private final Long userId;
        private final long expireTime;
        private final String deviceInfo;
        
        public TokenInfo(Long userId, String deviceInfo) {
            this.userId = userId;
            this.expireTime = System.currentTimeMillis() + TOKEN_EXPIRATION;
            this.deviceInfo = deviceInfo;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public String getDeviceInfo() {
            return deviceInfo;
        }
    }
} 