// 个人信息页面
const UserProfilePage = {
    template: `
        <div>
            <el-card class="box-card">
                <div slot="header">
                    <span>个人信息</span>
                </div>
                <div v-if="loading">
                    <el-skeleton :rows="5" animated></el-skeleton>
                </div>
                <div v-else>
                    <el-descriptions :column="1" border>
                        <el-descriptions-item label="用户ID">{{ userInfo.id }}</el-descriptions-item>
                        <el-descriptions-item label="用户名">{{ userInfo.username }}</el-descriptions-item>
                        <el-descriptions-item label="账户状态">
                            <el-tag :type="userInfo.enabled ? 'success' : 'danger'">
                                {{ userInfo.enabled ? '正常' : '已禁用' }}
                            </el-tag>
                        </el-descriptions-item>
                        <el-descriptions-item label="账户余额">￥{{ userInfo.balance ? userInfo.balance.toFixed(2) : '0.00' }}</el-descriptions-item>
                        <el-descriptions-item label="权限">
                            <el-tag v-if="userInfo.permissions && userInfo.permissions.deposit" type="success" style="margin-right: 5px;">存款</el-tag>
                            <el-tag v-if="userInfo.permissions && userInfo.permissions.withdraw" type="warning" style="margin-right: 5px;">取款</el-tag>
                            <el-tag v-if="userInfo.permissions && userInfo.permissions.transferOut" type="info" style="margin-right: 5px;">转出</el-tag>
                            <el-tag v-if="userInfo.permissions && userInfo.permissions.transferIn" type="primary" style="margin-right: 5px;">转入</el-tag>
                            <span v-if="!userInfo.permissions || (!userInfo.permissions.deposit && !userInfo.permissions.withdraw && !userInfo.permissions.transferOut && !userInfo.permissions.transferIn)">无权限</span>
                        </el-descriptions-item>
                    </el-descriptions>
                    
                    <div style="margin-top: 20px;">
                        <el-button type="primary" @click="$router.push('/user/change-password')">修改密码</el-button>
                    </div>
                </div>
            </el-card>
        </div>
    `,
    data() {
        return {
            userInfo: {},
            loading: true
        };
    },
    created() {
        this.fetchData();
    },
    methods: {
        fetchData() {
            this.loading = true;
            api.user.getProfile()
                .then(response => {
                    if (response.data && response.data.message) {
                        this.userInfo = response.data.message;
                    }
                })
                .catch(error => {
                    this.$message.error('获取用户信息失败');
                })
                .finally(() => {
                    this.loading = false;
                });
        }
    }
};
