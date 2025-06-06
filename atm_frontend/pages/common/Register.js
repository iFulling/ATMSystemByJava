// 注册页面组件
const RegisterPage = {
    template: `
        <div class="login-container">
            <div class="login-form">
                <h2 class="login-title">用户注册</h2>
                <el-form :model="registerForm" :rules="rules" ref="registerForm" label-width="0px">
                    <el-form-item prop="username">
                        <el-input v-model="registerForm.username" prefix-icon="el-icon-user" placeholder="用户名"></el-input>
                    </el-form-item>
                    <el-form-item prop="password">
                        <el-input type="password" v-model="registerForm.password" prefix-icon="el-icon-lock" placeholder="密码"></el-input>
                    </el-form-item>
                    <el-form-item prop="confirmPassword">
                        <el-input type="password" v-model="registerForm.confirmPassword" prefix-icon="el-icon-lock" placeholder="确认密码"></el-input>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" class="login-btn" @click="handleRegister" :loading="loading">注册</el-button>
                    </el-form-item>
                </el-form>
                <div style="text-align: center; margin-top: 20px;">
                    <el-button type="text" @click="$router.push('/login')">已有账户？登录</el-button>
                </div>
            </div>
        </div>
    `,
    data() {
        const validateConfirmPassword = (rule, value, callback) => {
            if (value !== this.registerForm.password) {
                callback(new Error('两次输入的密码不一致'));
            } else {
                callback();
            }
        };

        return {
            registerForm: {
                username: '',
                password: '',
                confirmPassword: ''
            },
            rules: {
                username: [
                    { required: true, message: '请输入用户名', trigger: 'blur' }
                ],
                password: [
                    { required: true, message: '请输入密码', trigger: 'blur' },
                    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
                ],
                confirmPassword: [
                    { required: true, message: '请确认密码', trigger: 'blur' },
                    { validator: validateConfirmPassword, trigger: 'blur' }
                ]
            },
            loading: false
        };
    },
    methods: {
        handleRegister() {
            this.$refs.registerForm.validate(valid => {
                if (valid) {
                    this.loading = true;
                    api.user.register(this.registerForm.username, this.registerForm.password)
                        .then(response => {
                            this.loading = false;
                            this.$message({
                                type: 'success',
                                message: '注册成功，请登录'
                            });
                            this.$router.push('/login');
                        })
                        .catch(error => {
                            this.loading = false;
                            this.$message({
                                type: 'error',
                                message: error.response?.data?.message || '注册失败，请稍后再试'
                            });
                        });
                }
            });
        }
    }
};
