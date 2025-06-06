// 登录页面组件
const LoginPage = {
    template: `
        <div class="login-container">
            <div class="login-form">
                <h2 class="login-title">{{ isAdmin ? '管理员登录' : '用户登录' }}</h2>
                <el-form :model="loginForm" :rules="rules" ref="loginForm" label-width="0px">
                    <el-form-item prop="username">
                        <el-input v-model="loginForm.username" prefix-icon="el-icon-user" placeholder="用户名"></el-input>
                    </el-form-item>
                    <el-form-item prop="password">
                        <el-input type="password" v-model="loginForm.password" prefix-icon="el-icon-lock" placeholder="密码"></el-input>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" class="login-btn" @click="handleLogin" :loading="loading">登录</el-button>
                    </el-form-item>
                </el-form>
                <div style="text-align: center; margin-top: 20px;">
                    <el-button v-if="!isAdmin" type="text" @click="$router.push('/register')">注册新账户</el-button>
                    <el-button type="text" @click="toggleUserType">{{ isAdmin ? '普通用户登录' : '管理员登录' }}</el-button>
                </div>
            </div>
        </div>
    `,
    data() {
        return {
            loginForm: {
                username: '',
                password: ''
            },
            rules: {
                username: [
                    { required: true, message: '请输入用户名', trigger: 'blur' }
                ],
                password: [
                    { required: true, message: '请输入密码', trigger: 'blur' }
                ]
            },
            loading: false,
            isAdmin: false
        };
    },
    methods: {
        handleLogin() {
            this.$refs.loginForm.validate(valid => {
                if (valid) {
                    this.loading = true;
                    const apiMethod = this.isAdmin
                        ? api.admin.login
                        : api.user.login;

                    apiMethod(this.loginForm.username, this.loginForm.password)
                        .then(response => {
                            this.loading = false;
                            this.$message({
                                type: 'success',
                                message: '登录成功'
                            });

                            const redirectPath = this.isAdmin ? '/admin/dashboard' : '/user/dashboard';
                            this.$router.push(redirectPath);
                        })
                        .catch(error => {
                            this.loading = false;
                            console.log(error);
                            this.$message({
                                type: 'error',
                                message: error.response?.data?.message || '登录失败，请检查用户名和密码'
                            });
                        });
                }
            });
        },
        toggleUserType() {
            this.isAdmin = !this.isAdmin;
            // if (this.isAdmin) {
            //     this.$router.push('/login');
            // } else {
            //     this.$router.push('/admin/login');
            // }
        }
    }
};
