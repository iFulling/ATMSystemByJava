package handler;

import com.google.gson.JsonObject;
import model.*;
import util.PasswordUtil;
import util.ValidationUtil;
import middleware.RequestContext;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class UserRequestHandler extends BaseRequestHandler {
    public UserRequestHandler() {
        super();
    }

    @Override
    public ResponseResult handleRequest(RequestContext context) {
        try {
            tokenManager.cleanupExpiredTokens();
            String path = extractPath(context);
            String method = extractMethod(context);
            String body = extractBody(context);

            // 验证请求方法
            if (path.equals("/api/balance") || path.equals("/api/profile")) {
                if (!method.equals("GET")) {
                    logger.warning(String.format("[%s] 请求方法错误: %s %s - 只允许 GET 请求",
                        LocalDateTime.now().format(formatter), method, path));
                    return new ResponseResult(NOT_FOUND, "页面未找到");
                }
            } else {
                if (!method.equals("POST")) {
                    logger.warning(String.format("[%s] 请求方法错误: %s %s - 只允许 POST 请求", 
                        LocalDateTime.now().format(formatter), method, path));
                    return new ResponseResult(NOT_FOUND, "页面未找到");
                }
            }

            ResponseResult response;
            switch (path) {
                case "/api/register":
                    response = handleRegister(body);
                    break;
                case "/api/login":
                    response = handleLogin(body, context);
                    break;
                case "/api/logout":
                    response = handleLogout(context);
                    break;
                case "/api/deposit":
                    response = handleDeposit(body, context);
                    break;
                case "/api/withdraw":
                    response = handleWithdraw(body, context);
                    break;
                case "/api/transfer":
                    response = handleTransfer(body, context);
                    break;
                case "/api/balance":
                    response = handleBalance(context);
                    break;
                case "/api/profile":
                    response = handleProfile(context);
                    break;
                case "/api/change-password":
                    response = handleChangePassword(body, context);
                    break;
                default:
                    response = new ResponseResult(NOT_FOUND, "未知的请求路径");
            }

            return response;
        } catch (Exception e) {
            return new ResponseResult(INTERNAL_ERROR, e.getMessage());
        }
    }

    private ResponseResult handleRegister(String body) throws SQLException {
        JsonObject params = gson.fromJson(body, JsonObject.class);
        String requiredError = ValidationUtil.validateRequired(params, "username", "password");
        if (requiredError != null) {
            return new ResponseResult(BAD_REQUEST, requiredError);
        }
        String username = params.get("username").getAsString();
        String password = params.get("password").getAsString();

        String usernameError = ValidationUtil.validateUsername(username);
        if (usernameError != null) {
            return new ResponseResult(BAD_REQUEST, usernameError);
        }

        String passwordError = ValidationUtil.validatePassword(password);
        if (passwordError != null) {
            return new ResponseResult(BAD_REQUEST, passwordError);
        }
        if (userDAO.findByUsername(username) != null) {
            return new ResponseResult(BAD_REQUEST, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.generateSecurePassword(password));
        user.setEnabled(true);
        user.setBalance(0.0);
        user.setPermissionsFlags(15);

        userDAO.create(user);
        logOperation(user.getId(), "用户注册");

        return new ResponseResult(OK, "注册成功");
    }

    private ResponseResult handleLogin(String body, RequestContext context) throws SQLException {
        JsonObject params = gson.fromJson(body, JsonObject.class);
        
        String requiredError = ValidationUtil.validateRequired(params, "username", "password");
        if (requiredError != null) {
            return new ResponseResult(BAD_REQUEST, requiredError);
        }

        String username = params.get("username").getAsString();
        String password = params.get("password").getAsString();

        User user = userDAO.findByUsername(username);
        if (user == null || PasswordUtil.verifySecurePassword(password, user.getPassword())) {
            logger.warning(String.format("[%s] 登录失败: %s - 用户名或密码错误", 
                LocalDateTime.now().format(formatter), username));
            return new ResponseResult(UNAUTHORIZED, "用户名或密码错误");
        }

        if (!user.isEnabled()) {
            logger.warning(String.format("[%s] 登录失败: %s - 账户已被禁用", 
                LocalDateTime.now().format(formatter), username));
            return new ResponseResult(FORBIDDEN, "账户已被禁用");
        }
        String deviceInfo = getHeaderValue(context.getHeaders(), "User-Agent");
        String token = tokenManager.addToken(user.getId(), deviceInfo);
        logOperation(user.getId(), "用户登录 - 设备: " + deviceInfo);

        JsonObject userJson = new JsonObject();
        userJson.addProperty("id", user.getId());
        userJson.addProperty("username", user.getUsername());
        userJson.addProperty("balance", user.getBalance());
        userJson.addProperty("token", token);

        return new ResponseResult(OK, userJson.toString());
    }

    private ResponseResult handleLogout(RequestContext context) throws SQLException {
        User user = context.getCurrentUser();

        String token = getHeaderValue(context.getHeaders(), "Authorization");
        token = token.substring(7);
        tokenManager.removeToken(token);
        logOperation(user.getId(), "用户登出");

        logger.info(String.format("[%s] 登出成功: 用户ID %d", 
            LocalDateTime.now().format(formatter), user.getId()));
        return new ResponseResult(OK, "登出成功");
    }

    private ResponseResult handleDeposit(String body, RequestContext context) throws SQLException {
        User user = context.getCurrentUser();

        JsonObject params = gson.fromJson(body, JsonObject.class);
        
        String requiredError = ValidationUtil.validateRequired(params, "amount");
        if (requiredError != null) {
            return new ResponseResult(BAD_REQUEST, requiredError);
        }

        double amount = params.get("amount").getAsDouble();

        String amountError = ValidationUtil.validateAmount(amount);
        if (amountError != null) {
            return new ResponseResult(BAD_REQUEST, amountError);
        }

        if (!user.isEnabled()) {
            return new ResponseResult(FORBIDDEN, "账户已被禁用");
        }

        if (!user.hasDepositPermission()) {
            return new ResponseResult(FORBIDDEN, "没有存款权限");
        }

        double newBalance = user.addAndGetBalance(amount);
        userDAO.updateBalance(user.getId(), newBalance);

        logOperation(user.getId(), "存款: " + amount);

        return new ResponseResult(OK, "存款成功，当前余额: " + newBalance);
    }

    private ResponseResult handleWithdraw(String body, RequestContext context) throws SQLException {
        User user = context.getCurrentUser();
        JsonObject params = gson.fromJson(body, JsonObject.class);
        
        String requiredError = ValidationUtil.validateRequired(params, "amount");
        if (requiredError != null) {
            return new ResponseResult(BAD_REQUEST, requiredError);
        }

        double amount = params.get("amount").getAsDouble();

        String amountError = ValidationUtil.validateAmount(amount);
        if (amountError != null) {
            return new ResponseResult(BAD_REQUEST, amountError);
        }

        if (!user.isEnabled()) {
            return new ResponseResult(FORBIDDEN, "账户已被禁用");
        }

        if (!user.hasWithdrawPermission()) {
            return new ResponseResult(FORBIDDEN, "没有取款权限");
        }

        double currentBalance = user.getBalance();
        if (currentBalance < amount) {
            return new ResponseResult(BAD_REQUEST, "余额不足");
        }

        if (!user.compareAndSetBalance(currentBalance, currentBalance - amount)) {
            return new ResponseResult(BAD_REQUEST, "取款失败，请重试");
        }

        double newBalance = currentBalance - amount;
        userDAO.updateBalance(user.getId(), newBalance);

        logOperation(user.getId(), "取款: " + amount);

        return new ResponseResult(OK, "取款成功，当前余额: " + newBalance);
    }

    private ResponseResult handleTransfer(String body, RequestContext context) throws SQLException {
        User fromUser = context.getCurrentUser();
        JsonObject params = gson.fromJson(body, JsonObject.class);
        String requiredError = ValidationUtil.validateRequired(params, "toUserName", "amount");
        if (requiredError != null) {
            return new ResponseResult(BAD_REQUEST, requiredError);
        }

        String toUserName = params.get("toUserName").getAsString();
        double amount = params.get("amount").getAsDouble();
        String remark = params.has("remark") ? params.get("remark").getAsString() : "";

        String amountError = ValidationUtil.validateAmount(amount);
        if (amountError != null) {
            return new ResponseResult(BAD_REQUEST, amountError);
        }

        String remarkError = ValidationUtil.validateString(params, "remark", 0, 100);
        if (remarkError != null) {
            return new ResponseResult(BAD_REQUEST, remarkError);
        }

        User toUser = userDAO.findByUsername(toUserName);
        if (toUser == null) {
            return new ResponseResult(NOT_FOUND, "用户不存在");
        }

        if (!fromUser.isEnabled() || !toUser.isEnabled()) {
            return new ResponseResult(FORBIDDEN, "账户已被禁用");
        }

        if (!fromUser.hasTransferOutPermission()) {
            return new ResponseResult(FORBIDDEN, "没有转账权限");
        }

        if (!toUser.hasTransferInPermission()) {
            return new ResponseResult(FORBIDDEN, "对方没有收款权限");
        }

        // 创建转账记录
        TransferRecord record = new TransferRecord();
        record.setFromUserId(fromUser.getId());
        record.setToUserId(toUser.getId());
        record.setAmount(BigDecimal.valueOf(amount));
        record.setTimestamp(LocalDateTime.now());
        record.setRemark(remark);
        record.setStatus("SUCCESS");
        record.setPermissionsSnapshot(fromUser.getPermissionsFlags());
        
        // 执行转账
        boolean success = userDAO.transfer(fromUser.getId(), toUser.getId(), amount, record);

        if (!success) {
            return new ResponseResult(BAD_REQUEST, "转账失败");
        }

        return new ResponseResult(OK, "转账成功");
    }

    private ResponseResult handleBalance(RequestContext context) {
        User user = context.getCurrentUser();
        return new ResponseResult(OK, String.valueOf(user.getBalance()));
    }

    private ResponseResult handleProfile(RequestContext context) {
        User user = context.getCurrentUser();

        JsonObject profileJson = new JsonObject();
        profileJson.addProperty("id", user.getId());
        profileJson.addProperty("username", user.getUsername());
        profileJson.addProperty("balance", user.getBalance());
        profileJson.addProperty("enabled", user.isEnabled());
        profileJson.addProperty("permissionsFlags", user.getPermissionsFlags());

        // 获取用户的权限描述
        JsonObject permissions = new JsonObject();
        permissions.addProperty("deposit", user.hasDepositPermission());
        permissions.addProperty("withdraw", user.hasWithdrawPermission());
        permissions.addProperty("transferOut", user.hasTransferOutPermission());
        permissions.addProperty("transferIn", user.hasTransferInPermission());
        profileJson.add("permissions", permissions);

        return new ResponseResult(OK, profileJson.toString());
    }

    private ResponseResult handleChangePassword(String body, RequestContext context) throws SQLException {
        User user = context.getCurrentUser();

        JsonObject params = gson.fromJson(body, JsonObject.class);
        
        String requiredError = ValidationUtil.validateRequired(params, "oldPassword", "newPassword");
        if (requiredError != null) {
            return new ResponseResult(BAD_REQUEST, requiredError);
        }

        String oldPassword = params.get("oldPassword").getAsString();
        String newPassword = params.get("newPassword").getAsString();

        String passwordError = ValidationUtil.validatePassword(newPassword);
        if (passwordError != null) {
            return new ResponseResult(BAD_REQUEST, passwordError);
        }

        if (PasswordUtil.verifySecurePassword(oldPassword, user.getPassword())) {
            return new ResponseResult(UNAUTHORIZED, "原密码错误");
        }

        user.setPassword(PasswordUtil.generateSecurePassword(newPassword));
        userDAO.update(user);

        logOperation(user.getId(), "修改密码");

        return new ResponseResult(OK, "密码修改成功");
    }
} 