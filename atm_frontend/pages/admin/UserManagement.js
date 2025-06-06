// 用户管理页面
const UserManagementPage = {
    template: `
        <div>
            <el-card>
                <div slot="header">
                    <span>用户管理</span>
                    <el-button 
                        style="float: right; padding: 3px 0" 
                        type="text" 
                        @click="openCreateUserDialog"
                    >
                        <i class="el-icon-plus"></i> 创建用户
                    </el-button>
                </div>
                
                <!-- 用户列表 -->
                <el-table
                    v-loading="loading"
                    :data="users"
                    border
                    style="width: 100%"
                >
                    <el-table-column
                        prop="id"
                        label="ID"
                        width="80">
                    </el-table-column>
                    <el-table-column
                        prop="username"
                        label="用户名"
                        width="180">
                    </el-table-column>
                    <el-table-column
                        label="权限"
                        width="280">
                        <template slot-scope="scope">
                            <el-tag v-if="hasPermission(scope.row.permissionsFlags, 1)" type="success" size="mini" style="margin-right: 5px;">存款</el-tag>
                            <el-tag v-if="hasPermission(scope.row.permissionsFlags, 2)" type="warning" size="mini" style="margin-right: 5px;">取款</el-tag>
                            <el-tag v-if="hasPermission(scope.row.permissionsFlags, 4)" type="info" size="mini" style="margin-right: 5px;">转出</el-tag>
                            <el-tag v-if="hasPermission(scope.row.permissionsFlags, 8)" type="primary" size="mini">转入</el-tag>
                            <span v-if="scope.row.permissionsFlags === 0">无权限</span>
                        </template>
                    </el-table-column>
                    <el-table-column
                        prop="enabled"
                        label="状态"
                        width="120">
                        <template slot-scope="scope">
                            <el-tag :type="scope.row.enabled ? 'success' : 'danger'">
                                {{ scope.row.enabled ? '启用' : '禁用' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column
                        label="操作">
                        <template slot-scope="scope">
                            <el-button
                                size="mini"
                                @click="handleEditUser(scope.row)">编辑</el-button>
                            <el-button
                                size="mini"
                                :type="scope.row.enabled ? 'danger' : 'success'"
                                @click="handleToggleUserStatus(scope.row)">
                                {{ scope.row.enabled ? '禁用' : '启用' }}
                            </el-button>
                            <el-button
                                size="mini"
                                type="danger"
                                @click="handleDeleteUser(scope.row)">删除</el-button>
                            <el-button
                                size="mini"
                                type="info"
                                @click="handleViewLogs(scope.row)">日志</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-card>
            
            <!-- 创建用户对话框 -->
            <el-dialog title="创建新用户" :visible.sync="createUserDialogVisible" width="500px">
                <el-form :model="userForm" :rules="userFormRules" ref="userForm" label-width="100px">
                    <el-form-item label="用户名" prop="username">
                        <el-input v-model="userForm.username"></el-input>
                    </el-form-item>
                    <el-form-item label="密码" prop="password">
                        <el-input type="password" v-model="userForm.password" show-password></el-input>
                    </el-form-item>
                    <el-form-item label="权限设置">
                        <el-checkbox-group v-model="selectedPermissions">
                            <el-checkbox label="1">存款</el-checkbox>
                            <el-checkbox label="2">取款</el-checkbox>
                            <el-checkbox label="4">转出</el-checkbox>
                            <el-checkbox label="8">转入</el-checkbox>
                        </el-checkbox-group>
                    </el-form-item>
                </el-form>
                <div slot="footer">
                    <el-button @click="createUserDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="submitUserForm" :loading="userFormLoading">创建</el-button>
                </div>
            </el-dialog>
            
            <!-- 编辑用户对话框 -->
            <el-dialog title="编辑用户" :visible.sync="editUserDialogVisible" width="500px">
                <el-form :model="userForm" :rules="userFormRules" ref="editUserForm" label-width="100px">
                    <el-form-item label="用户ID">
                        <el-input v-model="userForm.userId" disabled></el-input>
                    </el-form-item>
                    <el-form-item label="用户名" prop="username">
                        <el-input v-model="userForm.username"></el-input>
                    </el-form-item>
                    <el-form-item label="密码" prop="password">
                        <el-input type="password" v-model="userForm.password" show-password placeholder="不修改请留空"></el-input>
                    </el-form-item>
                    <el-form-item label="权限设置">
                        <el-checkbox-group v-model="selectedPermissions">
                            <el-checkbox label="1">存款</el-checkbox>
                            <el-checkbox label="2">取款</el-checkbox>
                            <el-checkbox label="4">转出</el-checkbox>
                            <el-checkbox label="8">转入</el-checkbox>
                        </el-checkbox-group>
                    </el-form-item>
                </el-form>
                <div slot="footer">
                    <el-button @click="editUserDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="submitEditUserForm" :loading="userFormLoading">保存</el-button>
                </div>
            </el-dialog>
        </div>
    `,
    data() {
        return {
            users: [],
            loading: false,
            createUserDialogVisible: false,
            editUserDialogVisible: false,
            selectedPermissions: [],
            userForm: {
                userId: '',
                username: '',
                password: '',
                permissionsFlags: 0
            },
            userFormRules: {
                username: [
                    { required: true, message: '请输入用户名', trigger: 'blur' }
                ],
                password: [
                    { required: false, message: '请输入密码', trigger: 'blur' },
                    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
                ]
            },
            userFormLoading: false
        };
    },
    created() {
        this.fetchUsers();
    },
    methods: {
        hasPermission(permissionsFlags, flag) {
            return (permissionsFlags & flag) !== 0;
        },
        calculatePermissionsFlags() {
            return this.selectedPermissions.reduce((sum, current) => sum + parseInt(current), 0);
        },
        fetchUsers() {
            this.loading = true;
            api.admin.listUsers()
                .then(response => {
                    if (response.data && response.data.message) {
                        this.users = response.data.message;
                    }
                })
                .catch(error => {
                    this.$message.error('获取用户列表失败');
                })
                .finally(() => {
                    this.loading = false;
                });
        },
        openCreateUserDialog() {
            this.userForm = {
                userId: '',
                username: '',
                password: '',
                permissionsFlags: 0
            };
            this.selectedPermissions = [];
            this.createUserDialogVisible = true;
            this.$nextTick(() => {
                this.$refs.userForm && this.$refs.userForm.clearValidate();
            });
        },
        submitUserForm() {
            this.$refs.userForm.validate(valid => {
                if (valid) {
                    this.userFormLoading = true;
                    const permissionsFlags = this.calculatePermissionsFlags();
                    api.admin.createUser(
                        this.userForm.username,
                        this.userForm.password,
                        permissionsFlags
                    )
                        .then(response => {
                            this.$message.success('用户创建成功');
                            this.createUserDialogVisible = false;
                            this.fetchUsers();
                        })
                        .catch(error => {
                            this.$message.error(error.response?.data?.message || '用户创建失败');
                        })
                        .finally(() => {
                            this.userFormLoading = false;
                        });
                }
            });
        },
        handleEditUser(row) {
            this.userForm = {
                userId: row.id,
                username: row.username,
                password: '',
                permissionsFlags: row.permissionsFlags
            };
            
            // 设置选中的权限
            this.selectedPermissions = [];
            if (this.hasPermission(row.permissionsFlags, 1)) this.selectedPermissions.push("1");
            if (this.hasPermission(row.permissionsFlags, 2)) this.selectedPermissions.push("2");
            if (this.hasPermission(row.permissionsFlags, 4)) this.selectedPermissions.push("4");
            if (this.hasPermission(row.permissionsFlags, 8)) this.selectedPermissions.push("8");
            
            this.editUserDialogVisible = true;
            this.$nextTick(() => {
                this.$refs.editUserForm && this.$refs.editUserForm.clearValidate();
            });
        },
        submitEditUserForm() {
            this.$refs.editUserForm.validate(valid => {
                if (valid) {
                    this.userFormLoading = true;
                    const permissionsFlags = this.calculatePermissionsFlags();
                    api.admin.updateUser(
                        this.userForm.userId,
                        this.userForm.username,
                        this.userForm.password,
                        permissionsFlags
                    )
                        .then(response => {
                            this.$message.success('用户信息更新成功');
                            this.editUserDialogVisible = false;
                            this.fetchUsers();
                        })
                        .catch(error => {
                            this.$message.error(error.response?.data?.message || '更新用户信息失败');
                        })
                        .finally(() => {
                            this.userFormLoading = false;
                        });
                }
            });
        },
        handleToggleUserStatus(row) {
            this.$confirm(`确定要${row.enabled ? '禁用' : '启用'}该用户吗?`, '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                api.admin.disableUser(row.id, !row.enabled)
                    .then(response => {
                        this.$message.success(`用户${row.enabled ? '禁用' : '启用'}成功`);
                        this.fetchUsers();
                    })
                    .catch(error => {
                        this.$message.error(error.response?.data?.message || `用户${row.enabled ? '禁用' : '启用'}失败`);
                    });
            }).catch(() => {});
        },
        handleDeleteUser(row) {
            this.$confirm('此操作将永久删除该用户, 是否继续?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                api.admin.deleteUser(row.id)
                    .then(response => {
                        this.$message.success('用户删除成功');
                        this.fetchUsers();
                    })
                    .catch(error => {
                        this.$message.error(error.response?.data?.message || '用户删除失败');
                    });
            }).catch(() => {});
        },
        handleViewLogs(row) {
            this.$router.push(`/admin/logs?userId=${row.id}`);
        }
    }
};
