// 使用Element UI
Vue.use(ELEMENT);

// 全局配置
Vue.config.productionTip = false;

// 创建并挂载根实例
new Vue({
    el: '#app',
    router,
    template: `
        <div id="app">
            <router-view></router-view>
        </div>
    `,
    data: {
        // 全局数据
    },
    methods: {
        // 全局方法
    },
    created() {
        // 页面加载时检查用户登录状态
        const token = localStorage.getItem('token');
        const userType = localStorage.getItem('userType');
        
        if (token && userType) {
            // 已经登录，可以在这里执行自动跳转或其他初始化逻辑
        }
    }
}); 