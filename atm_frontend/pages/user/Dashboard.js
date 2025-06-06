// 用户仪表盘
const UserDashboard = {
    template: `
        <div>
            <el-row :gutter="20">
                <el-col :xs="24" :sm="24" :md="12" :lg="8">
                    <div class="card balance-card">
                        <h3>当前余额</h3>
                        <div class="balance-amount">￥{{ balance.toFixed(2) }}</div>
                        <el-button-group>
                            <el-button type="primary" icon="el-icon-top" @click="$router.push('/user/deposit')">存款</el-button>
                            <el-button type="primary" icon="el-icon-bottom" @click="$router.push('/user/withdraw')">取款</el-button>
                            <el-button type="primary" icon="el-icon-right" @click="$router.push('/user/transfer')">转账</el-button>
                        </el-button-group>
                    </div>
                </el-col>
                <el-col :xs="24" :sm="24" :md="12" :lg="16">
                    <div class="card">
                        <h3>快捷功能</h3>
                        <el-row :gutter="20" style="margin-top: 20px;">
                            <el-col :span="8">
                                <el-card shadow="hover" style="text-align: center; cursor: pointer;" @click.native="$router.push('/user/deposit')">
                                    <i class="el-icon-top" style="font-size: 30px; color: #67C23A; margin-bottom: 10px;"></i>
                                    <div>存款</div>
                                </el-card>
                            </el-col>
                            <el-col :span="8">
                                <el-card shadow="hover" style="text-align: center; cursor: pointer;" @click.native="$router.push('/user/withdraw')">
                                    <i class="el-icon-bottom" style="font-size: 30px; color: #F56C6C; margin-bottom: 10px;"></i>
                                    <div>取款</div>
                                </el-card>
                            </el-col>
                            <el-col :span="8">
                                <el-card shadow="hover" style="text-align: center; cursor: pointer;" @click.native="$router.push('/user/transfer')">
                                    <i class="el-icon-right" style="font-size: 30px; color: #409EFF; margin-bottom: 10px;"></i>
                                    <div>转账</div>
                                </el-card>
                            </el-col>
                        </el-row>
                    </div>
                </el-col>
            </el-row>
            <div class="card" style="margin-top: 20px;">
                <h3>快速信息</h3>
                <el-row :gutter="20" style="margin-top: 20px;">
                    <el-col :span="12">
                        <el-card shadow="never">
                            <div slot="header">
                                <span>账户信息</span>
                            </div>
                            <div v-if="userInfo">
                                <p><strong>用户名：</strong>{{ userInfo.username }}</p>
                                <p><strong>用户ID：</strong>{{ userInfo.id }}</p>
                                <p><strong>账户状态：</strong>
                                    <el-tag :type="userInfo.enabled ? 'success' : 'danger'">
                                        {{ userInfo.enabled ? '正常' : '已禁用' }}
                                    </el-tag>
                                </p>
                            </div>
                            <div v-else>
                                <el-skeleton :rows="3" animated></el-skeleton>
                            </div>
                        </el-card>
                    </el-col>
                    <el-col :span="12">
                        <el-card shadow="never">
                            <div slot="header">
                                <span>安全提示</span>
                            </div>
                            <ul style="padding-left: 20px;">
                                <li>请定期修改您的密码</li>
                                <li>请勿向他人透露您的账户信息</li>
                                <li>使用完毕后请安全退出</li>
                                <li>如发现异常交易，请立即联系客服</li>
                            </ul>
                        </el-card>
                    </el-col>
                </el-row>
            </div>
        </div>
    `,
    data() {
        return {
            balance: 0,
            userInfo: null,
            loading: false
        };
    },
    created() {
        this.fetchData();
    },
    methods: {
        fetchData() {
            this.loading = true;

            // 获取余额
            api.user.getBalance()
                .then(response => {
                    if (response.data && response.data.message) {
                        // 直接使用message字段作为余额值
                        this.balance = parseFloat(response.data.message) || 0;
                    }
                })
                .catch(error => {
                    this.$message.error('获取余额失败');
                    console.error(error);
                })
                .finally(() => {
                    this.loading = false;
                });

            // 获取用户信息
            api.user.getProfile()
                .then(response => {
                    if (response.data && response.data.message) {
                        this.userInfo = response.data.message;
                    }
                })
                .catch(error => {
                    console.error('获取用户信息失败', error);
                });
        }
    }
};
