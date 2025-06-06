// 存款页面
const DepositPage = {
    template: `
        <div>
            <el-card class="box-card">
                <div slot="header">
                    <span>存款</span>
                </div>
                <el-form :model="form" :rules="rules" ref="form" label-width="100px" class="transaction-form">
                    <el-form-item label="当前余额">
                        <span>￥{{ balance.toFixed(2) }}</span>
                    </el-form-item>
                    <el-form-item label="存款金额" prop="amount">
                        <el-input v-model.number="form.amount" type="number" min="0.01" step="0.01">
                            <template slot="prepend">￥</template>
                        </el-input>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" @click="handleDeposit" :loading="loading">确认存款</el-button>
                        <el-button @click="resetForm">重置</el-button>
                    </el-form-item>
                </el-form>
            </el-card>
        </div>
    `,
    data() {
        return {
            balance: 0,
            form: {
                amount: ''
            },
            rules: {
                amount: [
                    { required: true, message: '请输入存款金额', trigger: 'blur' },
                    { type: 'number', message: '金额必须为数字', trigger: 'blur' },
                    { validator: (rule, value, callback) => {
                        if (value <= 0) {
                            callback(new Error('金额必须大于0'));
                        } else {
                            callback();
                        }
                    }, trigger: 'blur' }
                ]
            },
            loading: false
        };
    },
    created() {
        this.getBalance();
    },
    methods: {
        getBalance() {
            api.user.getBalance()
                .then(response => {
                    if (response.data && response.data.message) {
                        // 直接使用message字段作为余额值
                        this.balance = parseFloat(response.data.message) || 0;
                    }
                })
                .catch(error => {
                    this.$message.error('获取余额失败');
                });
        },
        handleDeposit() {
            this.$refs.form.validate(valid => {
                if (valid) {
                    this.loading = true;
                    api.user.deposit(this.form.amount)
                        .then(response => {
                            this.$message.success('存款成功');
                            this.getBalance();
                            this.resetForm();
                        })
                        .catch(error => {
                            this.$message.error(error.response?.data?.message || '存款失败');
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
