// 管理员仪表盘
const AdminDashboard = {
    template: `
        <div>
            <el-row :gutter="20">
                <el-col :span="8">
                    <el-card shadow="hover" class="dashboard-card">
                        <div style="text-align: center;">
                            <i class="el-icon-user" style="font-size: 48px; color: #409EFF;"></i>
                            <div style="font-size: 20px; margin: 15px 0 10px;">用户总数</div>
                            <div style="font-size: 30px; color: #409EFF;">{{ stats.userCount }}</div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card shadow="hover" class="dashboard-card">
                        <div style="text-align: center;">
                            <i class="el-icon-lock" style="font-size: 48px; color: #F56C6C;"></i>
                            <div style="font-size: 20px; margin: 15px 0 10px;">禁用用户</div>
                            <div style="font-size: 30px; color: #F56C6C;">{{ stats.disabledUsers }}</div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card shadow="hover" class="dashboard-card">
                        <div style="text-align: center;">
                            <i class="el-icon-money" style="font-size: 48px; color: #67C23A;"></i>
                            <div style="font-size: 20px; margin: 15px 0 10px;">交易金额</div>
                            <div style="font-size: 30px; color: #67C23A;">￥{{ formatAmount(stats.transactionAmount) }}</div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
            
            <el-card style="margin-top: 20px;">
                <div slot="header">
                    <span>快捷操作</span>
                </div>
                <el-row :gutter="20">
                    <el-col :span="8">
                        <el-card shadow="hover" style="text-align: center; cursor: pointer;" @click.native="$router.push('/admin/users')">
                            <i class="el-icon-user" style="font-size: 30px; color: #409EFF; margin-bottom: 10px;"></i>
                            <div>用户管理</div>
                        </el-card>
                    </el-col>
                    <el-col :span="8">
                        <el-card shadow="hover" style="text-align: center; cursor: pointer;" @click.native="openCreateUserDialog">
                            <i class="el-icon-plus" style="font-size: 30px; color: #67C23A; margin-bottom: 10px;"></i>
                            <div>创建用户</div>
                        </el-card>
                    </el-col>
                    <el-col :span="8">
                        <el-card shadow="hover" style="text-align: center; cursor: pointer;" @click.native="$router.push('/admin/logs')">
                            <i class="el-icon-document" style="font-size: 30px; color: #E6A23C; margin-bottom: 10px;"></i>
                            <div>日志管理</div>
                        </el-card>
                    </el-col>
                </el-row>
            </el-card>
            
            <!-- 创建用户对话框 -->
            <el-dialog title="创建新用户" :visible.sync="createUserDialogVisible" width="500px">
                <el-form :model="createUserForm" :rules="createUserRules" ref="createUserForm" label-width="100px">
                    <el-form-item label="用户名" prop="username">
                        <el-input v-model="createUserForm.username"></el-input>
                    </el-form-item>
                    <el-form-item label="密码" prop="password">
                        <el-input type="password" v-model="createUserForm.password" show-password></el-input>
                    </el-form-item>
                    <el-form-item label="权限标志" prop="permissionsFlags">
                        <el-input-number v-model="createUserForm.permissionsFlags" :min="0" :max="15"></el-input-number>
                    </el-form-item>
                </el-form>
                <div slot="footer">
                    <el-button @click="createUserDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="handleCreateUser" :loading="createUserLoading">创建</el-button>
                </div>
            </el-dialog>
        </div>
    `,
    data() {
        return {
            stats: {
                userCount: 0,
                disabledUsers: 0,
                transactionAmount: 0
            },
            createUserDialogVisible: false,
            createUserForm: {
                username: '',
                password: '',
                permissionsFlags: 1
            },
            createUserRules: {
                username: [
                    { required: true, message: '请输入用户名', trigger: 'blur' }
                ],
                password: [
                    { required: true, message: '请输入密码', trigger: 'blur' },
                    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
                ]
            },
            createUserLoading: false
        };
    },
    created() {
        this.fetchData();
    },
    methods: {
        fetchData() {
            // 获取用户统计信息
            api.admin.listUsers()
                .then(response => {
                    if (response.data && response.data.message) {
                        const users = response.data.message;
                        this.stats.userCount = users.length;
                        this.stats.disabledUsers = users.filter(user => !user.enabled).length;
                    }
                })
                .catch(error => {
                    this.$message.error('获取用户数据失败');
                });

            // 获取交易总金额
            api.admin.getTransactionTotalAmount()
                .then(response => {
                    if (response.data && response.data.message) {
                        this.stats.transactionAmount = parseFloat(response.data.message.totalAmount) || 0;
                    }
                })
                .catch(error => {
                    this.$message.error('获取交易金额失败');
                    console.error(error);
                });
        },
        formatAmount(amount) {
            return amount.toFixed(2);
        },
        openCreateUserDialog() {
            this.createUserDialogVisible = true;
        },
        handleCreateUser() {
            this.$refs.createUserForm.validate(valid => {
                if (valid) {
                    this.createUserLoading = true;
                    api.admin.createUser(
                        this.createUserForm.username,
                        this.createUserForm.password,
                        this.createUserForm.permissionsFlags
                    )
                        .then(response => {
                            this.$message.success('用户创建成功');
                            this.createUserDialogVisible = false;
                            this.fetchData();
                        })
                        .catch(error => {
                            this.$message.error(error.response?.data?.message || '用户创建失败');
                        })
                        .finally(() => {
                            this.createUserLoading = false;
                        });
                }
            });
        }
    }
};
