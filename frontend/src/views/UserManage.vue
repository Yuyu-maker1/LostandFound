<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";

const activeTab = ref("user");
const users = ref([]);
const loading = ref(false);
const filterStatus = ref("");
const filterAudit = ref("");
// 模糊查询关键字：账号 / 姓名 / 手机号 / 员工工号
const filterKeyword = ref("");

// 分页参数：与后端约定 page（第几页）/ limit（每页条数），total 由后端 count 回填
const pagination = reactive({
  page: 1,
  limit: 5,
  total: 0,
});

const auditDialogVisible = ref(false);
const auditSubmitting = ref(false);
const auditTarget = ref(null);
const auditForm = reactive({
  auditStatus: 1,
  remark: "",
});

const isAttendant = computed(() => activeTab.value === "attendant");

const authHeader = () => ({
  "Content-Type": "application/json",
  Authorization: localStorage.getItem("token") || "",
});

const auditText = (status) => {
  if (status === 0) return "待审核";
  if (status === 1) return "已通过";
  if (status === 2) return "已驳回";
  return "";
};

const auditTagType = (status) => {
  if (status === 1) return "success";
  if (status === 2) return "danger";
  return "warning";
};

const loadUsers = async () => {
  loading.value = true;
  try {
    const params = new URLSearchParams({
      role: activeTab.value,
      page: pagination.page,
      limit: pagination.limit,
    });
    if (filterStatus.value !== "") {
      params.append("status", filterStatus.value);
    }
    if (isAttendant.value && filterAudit.value !== "") {
      params.append("auditStatus", filterAudit.value);
    }
    if (filterKeyword.value.trim()) {
      params.append("keyword", filterKeyword.value.trim());
    }
    const res = await fetch(`/admin/users?${params.toString()}`, {
      headers: authHeader(),
    });
    if (res.status === 403) {
      ElMessage.error("无权限：仅超级管理员可查看用户列表");
      return;
    }
    const data = await res.json();
    if (data.code === 0) {
      users.value = data.data || [];
      pagination.total = data.count || 0;
      // 当前页为空但总数不为 0（筛选或审核后数据变少），回退到最后一页重新查询
      if (users.value.length === 0 && pagination.page > 1) {
        pagination.page = Math.max(
          1,
          Math.ceil(pagination.total / pagination.limit)
        );
        loading.value = false;
        await loadUsers();
        return;
      }
    } else {
      ElMessage.error(data.msg || "加载用户列表失败");
    }
  } catch (err) {
    console.error("加载用户列表异常:", err);
    ElMessage.error("网络异常，请确认后端服务已启动");
  } finally {
    loading.value = false;
  }
};

// 点击页码 / 上一页下一页
const handlePageChange = (page) => {
  pagination.page = page;
  loadUsers();
};

// 改变每页条数后回到第一页
const handleSizeChange = (limit) => {
  pagination.limit = limit;
  pagination.page = 1;
  loadUsers();
};

// 筛选条件变化：重置到第一页，由后端按条件分页查询
const handleFilterChange = () => {
  pagination.page = 1;
  loadUsers();
};

// 切换旅客/乘务员标签：清空筛选、回到第一页
const handleTabChange = () => {
  filterStatus.value = "";
  filterAudit.value = "";
  filterKeyword.value = "";
  users.value = [];
  pagination.page = 1;
  pagination.total = 0;
  loadUsers();
};

const changeStatus = async (row) => {
  const target = row.status === 1 ? 0 : 1;
  const action = target === 0 ? "停用" : "启用";
  try {
    await ElMessageBox.confirm(`确认${action}账号「${row.name}」吗？`, "提示", {
      type: "warning",
    });
  } catch {
    return;
  }
  try {
    const res = await fetch(
      `/admin/users/${encodeURIComponent(row.name)}/status`,
      {
        method: "POST",
        headers: authHeader(),
        body: JSON.stringify({ status: target }),
      }
    );
    if (res.status === 403) {
      ElMessage.error("无权限：仅超级管理员可操作");
      return;
    }
    const data = await res.json();
    if (data.code === 0) {
      ElMessage.success(data.msg || `${action}成功`);
      loadUsers();
    } else {
      ElMessage.error(data.msg || `${action}失败`);
    }
  } catch (err) {
    console.error("更新用户状态异常:", err);
    ElMessage.error("网络异常，请确认后端服务已启动");
  }
};

const openAudit = (row) => {
  auditTarget.value = row;
  auditForm.auditStatus = 1;
  auditForm.remark = "";
  auditDialogVisible.value = true;
};

const submitAudit = async () => {
  if (!auditTarget.value) return;
  auditSubmitting.value = true;
  try {
    const res = await fetch(
      `/admin/users/${encodeURIComponent(auditTarget.value.name)}/audit`,
      {
        method: "POST",
        headers: authHeader(),
        body: JSON.stringify({
          auditStatus: auditForm.auditStatus,
          remark: auditForm.remark,
        }),
      }
    );
    if (res.status === 403) {
      ElMessage.error("无权限：仅超级管理员可审核");
      return;
    }
    const data = await res.json();
    if (data.code === 0) {
      ElMessage.success(data.msg || "审核完成");
      auditDialogVisible.value = false;
      loadUsers();
    } else {
      ElMessage.error(data.msg || "审核失败");
    }
  } catch (err) {
    console.error("审核异常:", err);
    ElMessage.error("网络异常，请确认后端服务已启动");
  } finally {
    auditSubmitting.value = false;
  }
};

onMounted(loadUsers);
</script>

<template>
  <div class="user-page">
    <section class="card">
      <div class="card-head">
        <div>
          <h3 class="card-title">用户管理</h3>
          <p class="card-sub">
            仅超级管理员可见；旅客注册即可用，乘务员需审核通过后才能登录。
          </p>
        </div>
      </div>

      <el-tabs
        v-model="activeTab"
        class="role-tabs"
        @tab-change="handleTabChange"
      >
        <el-tab-pane label="旅客" name="user" />
        <el-tab-pane label="乘务员" name="attendant" />
      </el-tabs>

      <div class="filter-bar">
        <el-select
          v-model="filterStatus"
          placeholder="按状态筛选"
          clearable
          class="filter-item"
          @change="handleFilterChange"
        >
          <el-option label="启用" value="1" />
          <el-option label="停用" value="0" />
        </el-select>
        <el-select
          v-if="isAttendant"
          v-model="filterAudit"
          placeholder="按审核状态筛选"
          clearable
          class="filter-item"
          @change="handleFilterChange"
        >
          <el-option label="待审核" value="0" />
          <el-option label="已通过" value="1" />
          <el-option label="已驳回" value="2" />
        </el-select>
        <el-input
          v-model="filterKeyword"
          :placeholder="
            isAttendant ? '账号 / 姓名 / 手机号 / 工号' : '账号 / 姓名 / 手机号'
          "
          clearable
          class="filter-item keyword-item"
          @keyup.enter="handleFilterChange"
          @clear="handleFilterChange"
        />
        <el-button type="primary" @click="handleFilterChange">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="users" class="user-table">
        <el-table-column prop="name" label="登录账号" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <template v-if="isAttendant">
          <el-table-column prop="staffNo" label="员工工号" min-width="110" />
          <el-table-column
            prop="stationName"
            label="所属车站"
            min-width="110"
          />
          <el-table-column label="审核状态" min-width="100">
            <template #default="{ row }">
              <el-tag :type="auditTagType(row.auditStatus)" size="small">
                {{ auditText(row.auditStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="auditTime" label="审核时间" min-width="160" />
          <el-table-column
            prop="auditRemark"
            label="驳回原因"
            min-width="150"
          />
        </template>
        <el-table-column label="角色" min-width="90">
          <template #default>
            <el-tag type="info" size="small">
              {{ isAttendant ? "乘务员" : "旅客" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              size="small"
            >
              {{ row.status === 1 ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="isAttendant && row.auditStatus !== 1"
              link
              type="primary"
              @click="openAudit(row)"
            >
              审核
            </el-button>
            <el-button
              v-else
              link
              :type="row.status === 1 ? 'danger' : 'primary'"
              @click="changeStatus(row)"
            >
              {{ row.status === 1 ? "停用" : "启用" }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-bar">
        <el-pagination
          :current-page="pagination.page"
          :page-size="pagination.limit"
          :page-sizes="[5, 10, 20, 50]"
          :total="pagination.total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>
  </div>

  <el-dialog v-model="auditDialogVisible" title="乘务员审核" width="480px">
    <div class="audit-target">
      <span>账号：{{ auditTarget?.name }}</span>
      <span>姓名：{{ auditTarget?.realName || "-" }}</span>
      <span>工号：{{ auditTarget?.staffNo || "-" }}</span>
      <span>车站：{{ auditTarget?.stationName || "-" }}</span>
    </div>
    <el-form label-width="80px">
      <el-form-item label="审核结果">
        <el-radio-group v-model="auditForm.auditStatus">
          <el-radio :value="1">通过</el-radio>
          <el-radio :value="2">驳回</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="auditForm.auditStatus === 2" label="驳回原因">
        <el-input
          v-model="auditForm.remark"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="选填，会展示给该乘务员"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="auditDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="auditSubmitting" @click="submitAudit">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.user-page {
  max-width: 1180px;
}

.card {
  padding: 24px 28px;
  background: #fff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.card-title {
  margin: 0 0 8px;
  padding-left: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #0e2f5c;
  border-left: 3px solid #14539e;
  line-height: 1.2;
}

.card-sub {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  color: #8a97a8;
}

.role-tabs {
  margin-bottom: 4px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-item {
  width: 180px;
}

.keyword-item {
  width: 240px;
}

.user-table {
  width: 100%;
}

.pager-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.audit-target {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  padding: 10px 14px;
  margin-bottom: 18px;
  background: #f5f8fc;
  border-radius: 6px;
  font-size: 13px;
  color: #4a5568;
}
</style>
