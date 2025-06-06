// 修改密码页面
const ChangePasswordPage = {
    template: `
        <div>
            <el-card class="box-card">
                <div slot="header">
                    <span>修改密码</span>
                </div>
                <el-form :model="form" :rules="rules" ref="form" label-width="120px" class="transaction-form">
                    <el-form-item label="当前密码" prop="oldPassword">
                        <el-input type="password" v-model="form.oldPassword" show-password></el-input>
                    </el-form-item>
                    <el-form-item label="新密码" prop="newPassword">
                        <el-input type="password" v-model="form.newPassword" show-password></el-input>
                    </el-form-item>
                    <el-form-item label="确认新密码" prop="confirmPassword">
                        <el-input type="password" v-model="form.confirmPassword" show-password></el-input>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" @click="handleChangePassword" :loading="loading">确认修改</el-button>
                        <el-button @click="resetForm">重置</el-button>
                    </el-form-item>
                </el-form>
            </el-card>
        </div>
    `,
    data() {
        const validateConfirmPassword = (rule, value, callback) => {
            if (value !== this.form.newPassword) {
                callback(new Error('两次输入的密码不一致'));
            } else {
                callback();
            }
        };

        return {
            form: {
                oldPassword: '',
                newPassword: '',
                confirmPassword: ''
            },
            rules: {
                oldPassword: [
                    { required: true, message: '请输入当前密码', trigger: 'blur' }
                ],
                newPassword: [
                    { required: true, message: '请输入新密码', trigger: 'blur' },
                    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
                ],
                confirmPassword: [
                    { required: true, message: '请确认新密码', trigger: 'blur' },
                    { validator: validateConfirmPassword, trigger: 'blur' }
                ]
            },
            loading: false
        };
    },
    methods: {
        handleChangePassword() {
            this.$refs.form.validate(valid => {
                if (valid) {
                    this.loading = true;
                    api.user.changePassword(this.form.oldPassword, this.form.newPassword)
                        .then(response => {
                            this.$message.success('密码修改成功，请重新登录');
                            // 清除登录状态，返回登录页
                            localStorage.removeItem('token');
                            localStorage.removeItem('userType');
                            this.$router.push('/login');
                        })
                        .catch(error => {
                            this.$message.error(error.response?.data?.message || '密码修改失败');
                        })
                        .finally(() => {
                            this.loading = false;
                        });
                }
            });
        },
        resetForm() {
            this.$refs.form.resetFields();
        }
    }
};
