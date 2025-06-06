// API基础URL
const API_BASE_URL = 'http://127.0.0.1:8888';

// 请求拦截器 - 添加认证令牌
axios.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
}, error => {
    return Promise.reject(error);
});

// 响应拦截器 - 处理错误和认证失效
axios.interceptors.response.use(response => {
    return response;
}, error => {
    // 处理401认证错误或账户被禁用的情况
    if (error.response) {
        // 获取当前路径
        const currentPath = window.location.hash;
        // 登录相关页面列表
        const loginPages = ['/login', '/register'];
        // 检查当前页面是否为登录页
        const isLoginPage = loginPages.some(page => currentPath.includes(page));

        // 只有在非登录页面时才处理认证错误
        if (!isLoginPage) {
            if (error.response.status === 401) {
                // 认证失效，清除token并跳转到登录页
                localStorage.removeItem('token');
                localStorage.removeItem('userType');
                router.push('/login');
                ELEMENT.Message.error('会话已过期，请重新登录');
            } else if (error.response.data && error.response.data.message === "账户已被禁用") {
                // 账户被禁用，清除token并跳转到登录页，显示特定消息
                localStorage.removeItem('token');
                localStorage.removeItem('userType');
                router.push('/login');
                ELEMENT.Message.error('您的账户已被禁用，请联系管理员');
            }
        }
    }
    return Promise.reject(error);
});

// 从Content-Disposition头中提取文件名
function getFilenameFromHeader(contentDisposition) {
    if (!contentDisposition) return null;

    // 匹配双引号中的文件名
    const matches = contentDisposition.match(/filename="([^"]+)"/);
    if (matches && matches.length > 1) {
        return matches[1];
    }

    // 如果没有匹配到，返回默认文件名
    return `logs_export_${new Date().toISOString().slice(0, 10)}.csv`;
}

// API服务
const api = {
    // 用户相关接口
    user: {
        // 用户注册
        register(username, password) {
            return axios.post(`${API_BASE_URL}/api/register`, {
                username,
                password
            });
        },

        // 用户登录
        login(username, password) {
            return axios.post(`${API_BASE_URL}/api/login`, {
                username,
                password
            }).then(response => {
                if (response.data && response.data.message && response.data.message.token) {
                    localStorage.setItem('token', response.data.message.token);
                    localStorage.setItem('userType', 'user');
                }
                return response;
            });
        },

        // 用户退出
        logout() {
            return axios.post(`${API_BASE_URL}/api/logout`);
        },

        // 获取用户余额
        getBalance() {
            return axios.get(`${API_BASE_URL}/api/balance`);
        },

        // 存款
        deposit(amount) {
            return axios.post(`${API_BASE_URL}/api/deposit`, { amount });
        },

        // 取款
        withdraw(amount) {
            return axios.post(`${API_BASE_URL}/api/withdraw`, { amount });
        },

        // 转账
        transfer(amount, toUserName, remark) {
            return axios.post(`${API_BASE_URL}/api/transfer`, {
                amount,
                toUserName,
                remark
            });
        },

        // 修改密码
        changePassword(oldPassword, newPassword) {
            return axios.post(`${API_BASE_URL}/api/change-password`, {
                oldPassword,
                newPassword
            });
        },

        // 获取个人信息
        getProfile() {
            return axios.get(`${API_BASE_URL}/api/profile`);
        }
    },

    // 管理员相关接口
    admin: {
        // 管理员登录
        login(username, password) {
            return axios.post(`${API_BASE_URL}/api/admin/login`, {
                username,
                password
            }).then(response => {
                if (response.data && response.data.message && response.data.message.token) {
                    localStorage.setItem('token', response.data.message.token);
                    localStorage.setItem('userType', 'admin');
                }
                return response;
            });
        },

        // 管理员退出
        logout() {
            return axios.post(`${API_BASE_URL}/api/admin/logout`);
        },

        // 创建用户
        createUser(username, password, permissionsFlags) {
            return axios.post(`${API_BASE_URL}/api/admin/create-user`, {
                username,
                password,
                permissionsFlags
            });
        },

        // 修改用户信息
        updateUser(userId, username, password, permissionsFlags) {
            const data  = {
                userId,
                username,
                permissionsFlags
            }
            if (password) {
                data.password = password;
            }
            return axios.post(`${API_BASE_URL}/api/admin/update-user`, data);
        },

        // 删除用户
        deleteUser(userId) {
            return axios.post(`${API_BASE_URL}/api/admin/delete-user`, { userId });
        },

        // 获取所有用户列表
        listUsers() {
            return axios.get(`${API_BASE_URL}/api/admin/list-users`);
        },

        // 禁用用户
        disableUser(userId, enabled) {
            return axios.post(`${API_BASE_URL}/api/admin/disable-user`, {
                userId,
                enabled
            });
        },

        // 获取用户日志 - 分页
        getLogs(params) {
            return axios.get(`${API_BASE_URL}/api/admin/logs`, { params });
        },

        // 导出日志 - 使用axios下载
        exportLogs(userId) {
            const params = {};
            if (userId) {
                params.userId = userId;
            }

            return axios.get(`${API_BASE_URL}/api/admin/export-logs`, {
                params,
                responseType: 'blob'
            }).then(response => {
                // 从响应头中获取文件名
                const contentDisposition = response.headers['content-disposition'];
                const filename = getFilenameFromHeader(contentDisposition);

                // 创建Blob对象
                const blob = new Blob([response.data], { type: 'text/csv' });

                // 创建下载链接
                const link = document.createElement('a');
                link.href = window.URL.createObjectURL(blob);
                link.download = filename;
                link.click();

                // 释放URL对象
                window.URL.revokeObjectURL(link.href);
            });
        },

        // 获取交易总金额
        getTransactionTotalAmount() {
            return axios.get(`${API_BASE_URL}/api/admin/transaction-total-amount`);
        }
    },

    // 通用函数
    logout() {
        const userType = localStorage.getItem('userType');
        const logoutPromise = userType === 'admin'
            ? api.admin.logout()
            : api.user.logout();

        return logoutPromise
            .then(() => {
                localStorage.removeItem('token');
                localStorage.removeItem('userType');
                router.push('/login');
            })
            .catch(error => {
                console.error('退出登录失败', error);
                // 即使API调用失败，也清除本地存储并跳转到登录页
                localStorage.removeItem('token');
                localStorage.removeItem('userType');
                router.push('/login');
            });
    }
};
