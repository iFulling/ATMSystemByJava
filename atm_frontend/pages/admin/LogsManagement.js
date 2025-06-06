// 日志管理页面
const LogsManagementPage = {
    template: `
        <div>
            <el-card>
                <div slot="header">
                    <span>日志管理</span>
                    <el-button 
                        style="float: right; padding: 3px 0" 
                        type="text" 
                        @click="handleExportLogs"
                    >
                        <i class="el-icon-download"></i> 导出日志
                    </el-button>
                </div>
                
                <el-form :inline="true" class="demo-form-inline" style="margin-bottom: 20px;">
                    <el-form-item label="用户ID">
                        <el-input v-model="searchForm.userId" placeholder="输入用户ID查询"></el-input>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" @click="handleSearch">查询</el-button>
                        <el-button @click="resetSearch">重置</el-button>
                    </el-form-item>
                </el-form>
                
                <!-- 日志列表 -->
                <el-table
                    v-loading="loading"
                    :data="logs"
                    border
                    style="width: 100%"
                >
                    <el-table-column
                        prop="id"
                        label="日志ID"
                        width="80">
                    </el-table-column>
                    <el-table-column
                        prop="userId"
                        label="用户ID"
                        width="80">
                    </el-table-column>
                    <el-table-column
                        prop="operation"
                        label="操作类型">
                    </el-table-column>
                    <el-table-column
                        label="时间"
                        width="180">
                        <template slot-scope="scope">
                            {{ formatTimestamp(scope.row.timestamp) }}
                        </template>
                    </el-table-column>
                </el-table>
                
                <!-- 分页 -->
                <div style="margin-top: 20px; text-align: right;">
                    <el-pagination
                        @current-change="handleCurrentChange"
                        @size-change="handleSizeChange"
                        :current-page="currentPage"
                        :page-sizes="[10, 20, 50, 100]"
                        :page-size="pageSize"
                        layout="total, sizes, prev, pager, next, jumper"
                        :total="totalLogs">
                    </el-pagination>
                </div>
            </el-card>
        </div>
    `,
    data() {
        return {
            logs: [],
            loading: false,
            searchForm: {
                userId: ''
            },
            currentPage: 1,
            pageSize: 10,
            totalLogs: 0,
            totalPages: 0
        };
    },
    created() {
        // 从URL参数中获取用户ID
        const userId = this.$route.query.userId;
        if (userId) {
            this.searchForm.userId = userId;
        }
        this.fetchLogs();
    },
    methods: {
        fetchLogs() {
            this.loading = true;
            
            // 构建查询参数
            const params = {
                page: this.currentPage,
                pageSize: this.pageSize
            };
            
            // 如果有用户ID，添加到查询参数
            if (this.searchForm.userId) {
                params.userId = this.searchForm.userId;
            }
            
            // 使用API方法获取日志
            api.admin.getLogs(params)
                .then(response => {
                    if (response.data && response.data.message) {
                        const data = response.data.message;
                        this.logs = data.logs || [];
                        this.totalLogs = data.totalCount || 0;
                        this.totalPages = data.totalPages || 0;
                        this.currentPage = data.currentPage || 1;
                        this.pageSize = data.pageSize || 10;
                    }
                })
                .catch(error => {
                    this.$message.error('获取日志失败');
                    console.error(error);
                })
                .finally(() => {
                    this.loading = false;
                });
        },
        handleSearch() {
            this.currentPage = 1;
            this.fetchLogs();
        },
        resetSearch() {
            this.searchForm.userId = '';
            this.currentPage = 1;
            this.fetchLogs();
        },
        handleCurrentChange(page) {
            this.currentPage = page;
            this.fetchLogs();
        },
        handleSizeChange(size) {
            this.pageSize = size;
            this.currentPage = 1;
            this.fetchLogs();
        },
        handleExportLogs() {
            this.loading = true;
            
            // 使用API方法导出日志
            api.admin.exportLogs(this.searchForm.userId || null)
                .then(() => {
                    this.$message.success('日志导出成功');
                })
                .catch(error => {
                    this.$message.error('日志导出失败');
                    console.error(error);
                })
                .finally(() => {
                    this.loading = false;
                });
        },
        formatTimestamp(timestamp) {
            if (!timestamp || !timestamp.date || !timestamp.time) {
                return '无效时间';
            }
            
            const { date, time } = timestamp;
            return `${date.year}-${this.padZero(date.month)}-${this.padZero(date.day)} ${this.padZero(time.hour)}:${this.padZero(time.minute)}:${this.padZero(time.second)}`;
        },
        padZero(num) {
            return num.toString().padStart(2, '0');
        }
    }
};
