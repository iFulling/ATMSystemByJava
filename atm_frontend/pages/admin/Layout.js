// 管理员布局组件
const AdminLayout = {
    template: `
        <el-container class="page-container">
            <el-aside width="200px" class="page-sidebar">
                <el-menu 
                    :default-active="$route.path"
                    router
                    background-color="#304156"
                    text-color="#bfcbd9"
                    active-text-color="#409EFF"
                    style="height: 100%;"
                >
                    <el-menu-item index="/admin/dashboard">
                        <i class="el-icon-s-home"></i>
                        <span slot="title">主页</span>
                    </el-menu-item>
                    <el-menu-item index="/admin/users">
                        <i class="el-icon-user"></i>
                        <span slot="title">用户管理</span>
                    </el-menu-item>
                    <el-menu-item index="/admin/logs">
                        <i class="el-icon-document"></i>
                        <span slot="title">日志管理</span>
                    </el-menu-item>
                </el-menu>
            </el-aside>
            <el-container>
                <el-header height="60px" class="page-header">
                    <h3 style="margin: 0;">ATM系统 - 管理员端</h3>
                    <el-dropdown @command="handleCommand">
                        <span class="el-dropdown-link" style="cursor: pointer;">
                            <i class="el-icon-user" style="margin-right: 5px;"></i>
                            管理员<i class="el-icon-arrow-down el-icon--right"></i>
                        </span>
                        <el-dropdown-menu slot="dropdown">
                            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                        </el-dropdown-menu>
                    </el-dropdown>
                </el-header>
                <el-main class="page-content">
                    <router-view></router-view>
                </el-main>
            </el-container>
        </el-container>
    `,
    methods: {
        handleCommand(command) {
            if (command === 'logout') {
                // 显示加载提示
                const loading = this.$loading({
                    lock: true,
                    text: '正在退出...',
                    spinner: 'el-icon-loading',
                    background: 'rgba(0, 0, 0, 0.7)'
                });

                api.logout()
                    .then(() => {
                        loading.close();
                        this.$message.success('退出成功');
                    })
                    .catch(() => {
                        loading.close();
                        this.$message.error('退出失败，请重试');
                    });
            }
        }
    }
};
