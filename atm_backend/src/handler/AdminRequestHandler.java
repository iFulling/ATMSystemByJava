package handler;

import com.google.gson.JsonObject;
import middleware.RequestContext;
import dao.*;
import model.*;
import util.PasswordUtil;
import util.ValidationUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.math.BigDecimal;

public class AdminRequestHandler extends BaseRequestHandler {
    public AdminRequestHandler() {
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
            if (path.contains("list-users") || path.contains("transaction-total-amount") || path.contains("logs")) {
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
                case "/api/admin/login":
                    response = handleAdminLogin(body);
                    break;
                case "/api/admin/create-user":
                    response = handleCreateUser(body);
                    break;
                case "/api/admin/update-user":
                    response = handleUpdateUser(body);
                    break;
                case "/api/admin/delete-user":
                    response = handleDeleteUser(body);
                    break;
                case "/api/admin/list-users":
                    response = handleListUsers(context);
                    break;
                case "/api/admin/logout":
                    response = handleAdminLogout(context);
                    break;
                case "/api/admin/disable-user":
                    response = handleDisableUser(body);
                    break;
                case "/api/admin/logs":
                    response = handleGetLogs(context);
                    break;
                case "/api/admin/export-logs":
                    response = handleExportLogs(context);
                    break;
                case "/api/admin/transaction-total-amount":
                    response = handleGetTransactionTotalAmount(context);
                    break;
                default:
                    response = new ResponseResult(NOT_FOUND, "未知的请求路径");
            }

            return response;
        } catch (Exception e) {
            return new ResponseResult(INTERNAL_ERROR, e.getMessage());
        }
    }

    private ResponseResult handleAdminLogin(String body) throws SQLException {
        JsonObject params = gson.fromJson(body, JsonObject.class);
        
        String requiredError = ValidationUtil.validateRequired(params, "username", "password");
        if (requiredError != null) {
            return new ResponseResult(BAD_REQUEST, requiredError);
        }

        String username = params.get("username").getAsString();
        String password = params.get("password").getAsString();

        Admin admin = adminDAO.findByUsername(username);
        if (admin == null || PasswordUtil.verifySecurePassword(password, admin.getPassword())) {
            logger.warning(String.format("[%s] 管理员登录失败: %s - 用户名或密码错误", 
                LocalDateTime.now().format(formatter), username));
            return new ResponseResult(UNAUTHORIZED, "用户名或密码错误");
        }

        if (!admin.isEnabled()) {
            logger.warning(String.format("[%s] 管理员登录失败: %s - 账户已被禁用", 
                LocalDateTime.now().format(formatter), username));
            return new ResponseResult(FORBIDDEN, "账户已被禁用");
        }

        String token = tokenManager.addAdminToken(admin.getId());
        // logOperation(admin.getId(), "管理员登录");

        JsonObject adminJson = new JsonObject();
        adminJson.addProperty("id", admin.getId());
        adminJson.addProperty("username", admin.getUsername());
        adminJson.addProperty("token", token);

        return new ResponseResult(OK, adminJson.toString());
    }

    private ResponseResult handleCreateUser(String body) throws SQLException {
        JsonObject params = gson.fromJson(body, JsonObject.class);
        
        String requiredError = ValidationUtil.validateRequired(params, "username", "password", "permissionsFlags");
        if (requiredError != null) {
            return new ResponseResult(BAD_REQUEST, requiredError);
        }

        String username = params.get("username").getAsString();
        String password = params.get("password").getAsString();
        int permissionsFlags = params.get("permissionsFlags").getAsInt();

        String usernameError = ValidationUtil.validateUsername(username);
        if (usernameError != null) {
            return new ResponseResult(BAD_REQUEST, usernameError);
        }

        String passwordError = ValidationUtil.validatePassword(password);
        if (passwordError != null) {
            return new ResponseResult(BAD_REQUEST, passwordError);
        }

        String permissionsError = ValidationUtil.validatePermissionsFlags(permissionsFlags);
        if (permissionsError != null) {
            return new ResponseResult(BAD_REQUEST, permissionsError);
        }

        if (userDAO.findByUsername(username) != null) {
            return new ResponseResult(BAD_REQUEST, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.generateSecurePassword(password));
        user.setEnabled(true);
        user.setBalance(0.0);
        user.setPermissionsFlags(permissionsFlags);

        userDAO.create(user);
        // logOperation(user.getId(), "管理员创建用户: " + username);

        return new ResponseResult(OK, "用户创建成功");
    }

    private ResponseResult handleUpdateUser(String body) throws SQLException {
        JsonObject params = gson.fromJson(body, JsonObject.class);
        
        String requiredError = ValidationUtil.validateRequired(params, "userId", "username", "permissionsFlags");
        if (requiredError != null) {
            return new ResponseResult(BAD_REQUEST, requiredError);
        }

        Long userId = params.get("userId").getAsLong();
        String username = params.get("username").getAsString();
        int permissionsFlags = params.get("permissionsFlags").getAsInt();

        String usernameError = ValidationUtil.validateUsername(username);
        if (usernameError != null) {
            return new ResponseResult(BAD_REQUEST, usernameError);
        }

        String permissionsError = ValidationUtil.validatePermissionsFlags(permissionsFlags);
        if (permissionsError != null) {
            return new ResponseResult(BAD_REQUEST, permissionsError);
        }
        String password = null;
        if (params.get("password") != null) {
            password = params.get("password").getAsString();
            String passwordError = ValidationUtil.validatePassword(password);
            if (passwordError != null) {
                return new ResponseResult(BAD_REQUEST, passwordError);
            }
        }

        User user = userDAO.findById(userId);
        if (user == null) {
            return new ResponseResult(NOT_FOUND, "用户不存在");
        }

        if (!user.getUsername().equals(username) && userDAO.findByUsername(username) != null) {
            return new ResponseResult(BAD_REQUEST, "用户名已存在");
        }

        user.setUsername(username);
        if (password != null) {
            user.setPassword(PasswordUtil.generateSecurePassword(password));
        }
        user.setPermissionsFlags(permissionsFlags);
        userDAO.update(user);

        // logOperation(userId, "管理员更新用户信息");

        return new ResponseResult(OK, "用户信息更新成功");
    }

    private ResponseResult handleDeleteUser(String body) throws SQLException {

        JsonObject params = gson.fromJson(body, JsonObject.class);
        
        String requiredError = ValidationUtil.validateRequired(params, "userId");
        if (requiredError != null) {
            return new ResponseResult(BAD_REQUEST, requiredError);
        }

        Long userId = params.get("userId").getAsLong();

        User user = userDAO.findById(userId);
        if (user == null) {
            return new ResponseResult(NOT_FOUND, "用户不存在");
        }

        // 检查是否存在关联的转账记录
        TransferRecordDAO transferRecordDAO = new TransferRecordDAO();
        List<TransferRecord> fromRecords = transferRecordDAO.findByFromUserId(userId);
        if (!fromRecords.isEmpty()) {
            return new ResponseResult(FORBIDDEN, "该用户存在转账记录，无法删除");
        }
        List<TransferRecord> toRecords = transferRecordDAO.findByToUserId(userId);
        if (!toRecords.isEmpty()) {
            return new ResponseResult(FORBIDDEN, "该用户存在转账记录，无法删除");
        }

        try {
            userDAO.delete(userId);
            // logOperation(userId, "管理员删除用户");
            return new ResponseResult(OK, "用户删除成功");
        } catch (SQLException e) {
            if (e.getMessage().contains("foreign key constraint")) {
                return new ResponseResult(FORBIDDEN, "该用户存在关联数据，无法删除");
            }
            throw e;
        }
    }

    private ResponseResult handleListUsers(RequestContext context) throws SQLException {
        List<User> users = userDAO.findAll();
        context.setMessage("获取所有用户");
        return new ResponseResult(OK, gson.toJson(users));
    }

    private ResponseResult handleAdminLogout(RequestContext context) throws SQLException {
        Admin admin = context.getCurrentAdmin();
        String token = context.getHeaders().get("Authorization").substring(7);
        tokenManager.removeToken(token);
        // logOperation(admin.getId(), "管理员登出");

        logger.info(String.format("[%s] 管理员登出成功: 管理员ID %d", 
            LocalDateTime.now().format(formatter), admin.getId()));
        return new ResponseResult(OK, "登出成功");
    }

    private ResponseResult handleDisableUser(String body) throws SQLException {
        JsonObject params = gson.fromJson(body, JsonObject.class);
        
        String requiredError = ValidationUtil.validateRequired(params, "userId", "enabled");
        if (requiredError != null) {
            return new ResponseResult(BAD_REQUEST, requiredError);
        }

        Long userId = params.get("userId").getAsLong();
        boolean enabled = params.get("enabled").getAsBoolean();

        User user = userDAO.findById(userId);
        if (user == null) {
            return new ResponseResult(NOT_FOUND, "用户不存在");
        }

        user.setEnabled(enabled);
        userDAO.update(user);

        // logOperation(userId, "管理员" + (enabled ? "启用" : "禁用") + "用户");

        return new ResponseResult(OK, "用户状态更新成功");
    }

    private ResponseResult handleGetLogs(RequestContext context) throws SQLException {
        String query = context.getExchange().getRequestURI().getQuery();
        Long userId = null;
        int page = 1;
        int pageSize = 10; // 默认每页10条记录
        
        // 解析查询参数
        if (query != null && !query.isEmpty()) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    
                    if ("userId".equals(key)) {
                        try {
                            userId = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            return new ResponseResult(BAD_REQUEST, "userId参数必须是数字");
                        }
                    } else if ("page".equals(key)) {
                        try {
                            page = Integer.parseInt(value);
                            if (page < 1) {
                                page = 1;
                            }
                        } catch (NumberFormatException e) {
                            return new ResponseResult(BAD_REQUEST, "page参数必须是正整数");
                        }
                    } else if ("pageSize".equals(key)) {
                        try {
                            pageSize = Integer.parseInt(value);
                            if (pageSize < 1) {
                                pageSize = 10;
                            } else if (pageSize > 100) {
                                pageSize = 100; // 限制最大每页数量
                            }
                        } catch (NumberFormatException e) {
                            return new ResponseResult(BAD_REQUEST, "pageSize参数必须是正整数");
                        }
                    }
                }
            }
        }

        List<OperationLog> logs;
        int totalCount;
        int totalPages;
        
        if (userId != null) {
            logs = operationLogDAO.findByUserIdPaginated(userId, page, pageSize);
            totalCount = operationLogDAO.getTotalCountByUserId(userId);
        } else {
            logs = operationLogDAO.findAllPaginated(page, pageSize);
            totalCount = operationLogDAO.getTotalCount();
        }
        
        totalPages = (int) Math.ceil((double) totalCount / pageSize);
        
        JsonObject result = new JsonObject();
        result.addProperty("totalCount", totalCount);
        result.addProperty("totalPages", totalPages);
        result.addProperty("currentPage", page);
        result.addProperty("pageSize", pageSize);
        result.add("logs", gson.toJsonTree(logs));
        
        context.setMessage("获取日志记录" + (userId == null ? "" : " - 用户ID：" + userId) + 
                           " - 第" + page + "页，共" + totalPages + "页");
        
        return new ResponseResult(OK, result.toString());
    }

    private ResponseResult handleExportLogs(RequestContext context) throws SQLException {
        String query = context.getExchange().getRequestURI().getQuery();
        Long userId = null;
        
        // 解析查询参数
        if (query != null && !query.isEmpty()) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    
                    if ("userId".equals(key)) {
                        try {
                            userId = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            return new ResponseResult(BAD_REQUEST, "userId参数必须是数字");
                        }
                    }
                }
            }
        }

        List<OperationLog> logs;
        if (userId != null) {
            logs = operationLogDAO.findByUserId(userId);
        } else {
            logs = operationLogDAO.findAll();
        }

        // 构建CSV内容
        StringBuilder csv = new StringBuilder();
        // 添加CSV头
        csv.append("用户ID,操作内容,操作时间\n");
        
        // 添加数据行
        for (OperationLog log : logs) {
            csv.append(String.format("%d,%s,%s\n",
                log.getUserId(),
                log.getOperation().replace(",", "，"), // 替换英文逗号，避免CSV格式问题
                log.getTimestamp().format(formatter)
            ));
        }

        context.setMessage("导出日志" + (userId == null ? "" : " - 用户ID：" + userId));

        return new ResponseResult(OK, csv.toString());
    }

    private ResponseResult handleGetTransactionTotalAmount(RequestContext context) throws SQLException {
        String query = context.getExchange().getRequestURI().getQuery();
        TransferRecordDAO transferRecordDAO = new TransferRecordDAO();
        BigDecimal totalAmount;
        
        // 解析查询参数
        if (query != null && !query.isEmpty()) {
            LocalDateTime startDate = null;
            LocalDateTime endDate = null;
            
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    
                    if ("startDate".equals(key)) {
                        try {
                            startDate = LocalDateTime.parse(value + "T00:00:00");
                        } catch (Exception e) {
                            return new ResponseResult(BAD_REQUEST, "开始日期格式错误，请使用yyyy-MM-dd格式");
                        }
                    } else if ("endDate".equals(key)) {
                        try {
                            endDate = LocalDateTime.parse(value + "T23:59:59");
                        } catch (Exception e) {
                            return new ResponseResult(BAD_REQUEST, "结束日期格式错误，请使用yyyy-MM-dd格式");
                        }
                    }
                }
            }
            
            if (startDate != null && endDate != null) {
                if (startDate.isAfter(endDate)) {
                    return new ResponseResult(BAD_REQUEST, "开始日期不能晚于结束日期");
                }
                totalAmount = transferRecordDAO.getTotalAmountByDateRange(startDate, endDate);
                context.setMessage("获取日期范围内交易记录总金额 - " + startDate.format(DateTimeFormatter.ISO_DATE) + " 至 " + endDate.format(DateTimeFormatter.ISO_DATE));
            } else if (startDate != null) {
                endDate = LocalDateTime.now();
                totalAmount = transferRecordDAO.getTotalAmountByDateRange(startDate, endDate);
                context.setMessage("获取日期范围内交易记录总金额 - " + startDate.format(DateTimeFormatter.ISO_DATE) + " 至今");
            } else if (endDate != null) {
                startDate = LocalDateTime.of(1970, 1, 1, 0, 0);
                totalAmount = transferRecordDAO.getTotalAmountByDateRange(startDate, endDate);
                context.setMessage("获取日期范围内交易记录总金额 - 截至 " + endDate.format(DateTimeFormatter.ISO_DATE));
            } else {
                totalAmount = transferRecordDAO.getTotalAmount();
                context.setMessage("获取所有交易记录总金额");
            }
        } else {
            totalAmount = transferRecordDAO.getTotalAmount();
            context.setMessage("获取所有交易记录总金额");
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("totalAmount", totalAmount);
        
        return new ResponseResult(OK, result.toString());
    }
} 