// 创建路由实例
const router = new VueRouter({
    routes: [
        // 重定向到登录页
        { path: '/', redirect: '/login' },

        // 用户登录
        {
            path: '/login',
            component: LoginPage,
            meta: { requiresAuth: false }
        },

        // 用户注册
        {
            path: '/register',
            component: RegisterPage,
            meta: { requiresAuth: false }
        },

        // 用户路由 - 使用动态加载的组件
        {
            path: '/user',
            component: UserLayout,
            meta: { requiresAuth: true, userType: 'user' },
            children: [
                { path: 'dashboard', component: UserDashboard },
                { path: 'deposit', component: DepositPage },
                { path: 'withdraw', component: WithdrawPage },
                { path: 'transfer', component: TransferPage },
                { path: 'profile', component: UserProfilePage },
                { path: 'change-password', component: ChangePasswordPage },
                { path: '', redirect: 'dashboard' }
            ]
        },

        // 管理员登录
        {
            path: '/admin/login',
            component: LoginPage,
            meta: { requiresAuth: false }
        },

        // 管理员路由
        {
            path: '/admin',
            component: AdminLayout,
            meta: { requiresAuth: true, userType: 'admin' },
            children: [
                { path: 'dashboard', component: AdminDashboard },
                { path: 'users', component: UserManagementPage },
                { path: 'logs', component: LogsManagementPage },
                { path: '', redirect: 'dashboard' }
            ]
        },

        // 404页面
        {
            path: '*',
            component: NotFound
        }
    ]
});

// 处理NavigationDuplicated错误
const originalPush = VueRouter.prototype.push;
VueRouter.prototype.push = function push(location) {
    return originalPush.call(this, location).catch(err => {
        // 忽略NavigationDuplicated错误，但将其他错误重新抛出
        if (err.name !== 'NavigationDuplicated') {
            throw err;
        }
    });
};

// 路由守卫 - 全局前置守卫
router.beforeEach((to, from, next) => {
    const isAuthenticated = !!localStorage.getItem('token');
    const userType = localStorage.getItem('userType');

    // 需要认证的路由
    if (to.matched.some(record => record.meta.requiresAuth)) {
        if (!isAuthenticated) {
            next({ path: '/login' });
        } else {
            // 检查用户类型是否匹配
            const requiredUserType = to.matched.find(record => record.meta.userType)?.meta.userType;

            if (requiredUserType && requiredUserType !== userType) {
                // 用户类型不匹配，重定向到对应的登录页
                if (userType === 'admin') {
                    next({ path: '/admin/dashboard' });
                } else {
                    next({ path: '/user/dashboard' });
                }
            } else {
                next();
            }
        }
    } else {
        // 对于不需要认证的路由
        if (isAuthenticated) {
            // 如果用户已登录，且访问登录或注册页面，重定向到其对应的仪表盘
            if (to.path === '/login' || to.path === '/register' || to.path === '/admin/login') {
                if (userType === 'admin') {
                    next({ path: '/admin/dashboard' });
                } else {
                    next({ path: '/user/dashboard' });
                }
            } else {
                next();
            }
        } else {
            next();
        }
    }
});

// 全局后置钩子
router.afterEach((to, from) => {
    // 可以在这里设置页面标题等
    document.title = to.meta.title || 'ATM系统';
});
