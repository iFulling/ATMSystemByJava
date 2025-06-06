package util;

import com.google.gson.JsonObject;
import config.ConfigManager;

public class ValidationUtil {
    private static final ConfigManager configManager = ConfigManager.getInstance();
    
    private static final int USERNAME_MIN_LENGTH = configManager.getIntProperty("user.username.min.length", 4);
    private static final int USERNAME_MAX_LENGTH = configManager.getIntProperty("user.username.max.length", 20);
    private static final int PASSWORD_MIN_LENGTH = configManager.getIntProperty("user.password.min.length", 6);
    private static final int PASSWORD_MAX_LENGTH = configManager.getIntProperty("user.password.max.length", 20);
    private static final double MAX_TRANSFER_AMOUNT = configManager.getDoubleProperty("user.transfer.max.amount", 1000000);

    /**
     * 验证必填参数
     */
    public static String validateRequired(JsonObject params, String... fields) {
        for (String field : fields) {
            if (!params.has(field) || params.get(field).isJsonNull()) {
                return "缺少必填参数: " + field;
            }
        }
        return null;
    }

    /**
     * 验证字符串参数
     */
    public static String validateString(JsonObject params, String field, int minLength, int maxLength) {
        if (!params.has(field)) {
            return null; // 非必填参数
        }
        String value = params.get(field).getAsString();
        if (value.length() < minLength) {
            return field + "长度不能小于" + minLength + "个字符";
        }
        if (value.length() > maxLength) {
            return field + "长度不能超过" + maxLength + "个字符";
        }
        return null;
    }

    /**
     * 验证数字参数
     */
    public static String validateNumber(JsonObject params, String field, double min, double max) {
        if (!params.has(field)) {
            return null; // 非必填参数
        }
        try {
            double value = params.get(field).getAsDouble();
            if (value < min) {
                return field + "不能小于" + min;
            }
            if (value > max) {
                return field + "不能大于" + max;
            }
        } catch (Exception e) {
            return field + "必须是数字";
        }
        return null;
    }

    /**
     * 验证布尔参数
     */
    public static String validateBoolean(JsonObject params, String field) {
        if (!params.has(field)) {
            return null; // 非必填参数
        }
        try {
            params.get(field).getAsBoolean();
        } catch (Exception e) {
            return field + "必须是布尔值";
        }
        return null;
    }

    /**
     * 验证用户名格式
     */
    public static String validateUsername(String username) {
        if (username == null || username.length() < USERNAME_MIN_LENGTH || username.length() > USERNAME_MAX_LENGTH) {
            return "用户名长度必须在" + USERNAME_MIN_LENGTH + "-" + USERNAME_MAX_LENGTH + "个字符之间";
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return "用户名只能包含字母、数字和下划线";
        }
        return null;
    }

    /**
     * 验证密码强度
     */
    public static String validatePassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            return "密码长度必须在" + PASSWORD_MIN_LENGTH + "-" + PASSWORD_MAX_LENGTH + "个字符之间";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "密码必须包含至少一个大写字母";
        }
        if (!password.matches(".*[a-z].*")) {
            return "密码必须包含至少一个小写字母";
        }
        if (!password.matches(".*[0-9].*")) {
            return "密码必须包含至少一个数字";
        }
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            return "密码必须包含至少一个特殊字符";
        }
        return null;
    }

    /**
     * 验证金额
     */
    public static String validateAmount(double amount) {
        if (amount <= 0) {
            return "金额必须大于0";
        }
        if (amount > MAX_TRANSFER_AMOUNT) {
            return "单笔交易金额不能超过" + String.format("%,.0f", MAX_TRANSFER_AMOUNT);
        }
        return null;
    }

    /**
     * 验证权限标志
     */
    public static String validatePermissionsFlags(int flags) {
        if (flags < 0 || flags > 15) {
            return "权限标志必须在0-15之间";
        }
        return null;
    }
} 