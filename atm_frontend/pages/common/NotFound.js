// 404页面
const NotFound = {
    template: `
        <div style="display: flex; justify-content: center; align-items: center; height: 100%; flex-direction: column;">
            <h1 style="font-size: 72px; margin-bottom: 20px;">404</h1>
            <p style="font-size: 20px; margin-bottom: 30px;">页面不存在</p>
            <el-button type="primary" @click="goHome">返回首页</el-button>
        </div>
    `,
    methods: {
        goHome() {
            const userType = localStorage.getItem('userType');
            if (userType === 'admin') {
                this.$router.push('/admin/dashboard');
            } else if (userType === 'user') {
                this.$router.push('/user/dashboard');
            } else {
                this.$router.push('/login');
            }
        }
    }
};
