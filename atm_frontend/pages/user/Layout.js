// 用户布局组件
const UserLayout = {
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
                    <el-menu-item index="/user/dashboard">
                        <i class="el-icon-s-home"></i>
                        <span slot="title">主页</span>
                    </el-menu-item>
                    <el-menu-item index="/user/deposit">
                        <i class="el-icon-top"></i>
                        <span slot="title">存款</span>
                    </el-menu-item>
                    <el-menu-item index="/user/withdraw">
                        <i class="el-icon-bottom"></i>
                        <span slot="title">取款</span>
                    </el-menu-item>
                    <el-menu-item index="/user/transfer">
                        <i class="el-icon-right"></i>
                        <span slot="title">转账</span>
                    </el-menu-item>
                    <el-menu-item index="/user/profile">
                        <i class="el-icon-user"></i>
                        <span slot="title">个人信息</span>
                    </el-menu-item>
                    <el-menu-item index="/user/change-password">
                        <i class="el-icon-lock"></i>
                        <span slot="title">修改密码</span>
                    </el-menu-item>
                </el-menu>
            </el-aside>
            <el-container>
                <el-header height="60px" class="page-header">
                    <h3 style="margin: 0;">ATM系统 - 用户端</h3>
                    <el-dropdown @command="handleCommand">
                        <span class="el-dropdown-link" style="cursor: pointer;">
                            <i class="el-icon-user" style="margin-right: 5px;"></i>
                            {{ username }}<i class="el-icon-arrow-down el-icon--right"></i>
                        </span>
                        <el-dropdown-menu slot="dropdown">
                            <el-dropdown-item command="profile">个人信息</el-dropdown-item>
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
    data() {
        return {
            username: '用户'
        };
    },
    created() {
        // 获取用户信息
        this.getUserInfo();
    },
    methods: {
        getUserInfo() {
            api.user.getProfile()
                .then(response => {
                    if (response.data && response.data.message) {
                        this.username = response.data.message.username || '用户';
                    }
                })
                .catch(error => {
                    console.error('获取用户信息失败', error);
                });
        },
        handleCommand(command) {
            if (command === 'profile') {
                this.$router.push('/user/profile');
            } else if (command === 'logout') {
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
