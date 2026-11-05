<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";

const admins = ref([]);
const stations = ref([]);
const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const stationDialogVisible = ref(false);
const stationSubmitting = ref(false);
const formRef = ref(null);
const stationFormRef = ref(null);
const noStationMatch = ref(false);
const filterStationId = ref(null);
const filterStatus = ref("");

const currentName = localStorage.getItem("username") || "";
const isSuperAdmin = localStorage.getItem("role") === "super_admin";

const form = reactive({
  name: "",
  pwd: "",
  staffNo: "",
  stationId: null,
  realName: "",
  phone: "",
  idCard: "",
  remark: "",
});

const stationForm = reactive({
  stationName: "",
  city: "",
});

const rules = {
  name: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    {
      pattern: /^[A-Za-z0-9_]{3,20}$/,
      message: "用户名为 3-20 位字母、数字或下划线",
      trigger: "blur",
    },
  ],
  pwd: [
    { required: true, message: "请输入密码", trigger: "blur" },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).{6,20}$/,
      message: "密码为 6-20 位，且至少包含字母和数字",
      trigger: "blur",
    },
  ],
  staffNo: [
    { required: true, message: "请输入员工工号", trigger: "blur" },
    {
      pattern: /^[A-Za-z0-9]{6,12}$/,
      message: "员工工号为 6-12 位字母或数字",
      trigger: "blur",
    },
  ],
  stationId: [{ required: true, message: "请选择所属车站", trigger: "change" }],
  realName: [
    { required: true, message: "请输入真实姓名", trigger: "blur" },
    { min: 2, max: 20, message: "真实姓名需为 2-20 个字符", trigger: "blur" },
  ],
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的 11 位手机号",
      trigger: "blur",
    },
  ],
  idCard: [
    {
      pattern: /^(\d{15}|\d{17}[\dXx])$/,
      message: "身份证号格式不正确",
      trigger: "blur",
    },
  ],
  remark: [{ max: 200, message: "备注最长 200 字", trigger: "blur" }],
};

const stationRules = {
  stationName: [
    { required: true, message: "请输入车站名称", trigger: "blur" },
    { min: 2, max: 20, message: "车站名称需为 2-20 个字符", trigger: "blur" },
  ],
};

const authHeader = () => ({
  "Content-Type": "application/json",
  Authorization: localStorage.getItem("token") || "",
});

const roleText = (role) => {
  if (role === "super_admin") return "超级管理员";
  if (role === "admin") return "车站管理员";
  return "未知角色";
};

const filteredAdmins = computed(() =>
  admins.value.filter((row) => {
    if (
      filterStationId.value !== null &&
      row.stationId !== filterStationId.value
    ) {
      return false;
    }
    if (
      filterStatus.value !== "" &&
      String(row.status) !== String(filterStatus.value)
    ) {
      return false;
    }
    return true;
  })
);

const loadAdmins = async () => {
  loading.value = true;
  try {
    const res = await fetch("/admin/admins", { headers: authHeader() });
    if (res.status === 403) {
      ElMessage.error("无权限：仅超级管理员可查看管理员列表");
      return;
    }
    const data = await res.json();
    if (data.code === 0) {
      admins.value = data.data || [];
    } else {
      ElMessage.error(data.msg || "加载管理员列表失败");
    }
  } catch (err) {
    console.error("加载管理员列表异常:", err);
    ElMessage.error("网络异常，请确认后端服务已启动");
  } finally {
    loading.value = false;
  }
};

const loadStations = async (keyword = "") => {
  try {
    const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : "";
    const res = await fetch(`/admin/stations${query}`, {
      headers: authHeader(),
    });
    const data = await res.json();
    if (data.code === 0) {
      stations.value = data.data || [];
    }
  } catch (err) {
    console.error("加载车站失败:", err);
  }
};

const remoteStation = async (keyword) => {
  await loadStations(keyword);
  noStationMatch.value = keyword.trim() !== "" && stations.value.length === 0;
};

const openDialog = () => {
  form.name = "";
  form.pwd = "";
  form.staffNo = "";
  form.stationId = null;
  form.realName = "";
  form.phone = "";
  form.idCard = "";
  form.remark = "";
  noStationMatch.value = false;
  dialogVisible.value = true;
  formRef.value?.clearValidate();
  loadStations("");
};

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const payload = {
      name: form.name,
      pwd: form.pwd,
      staffNo: form.staffNo,
      stationId: form.stationId,
      realName: form.realName,
      phone: form.phone,
      idCard: form.idCard,
      remark: form.remark,
    };
    const res = await fetch("/admin/admins", {
      method: "POST",
      headers: authHeader(),
      body: JSON.stringify(payload),
    });
    if (res.status === 403) {
      ElMessage.error("无权限：仅超级管理员可新增管理员");
      return;
    }
    const data = await res.json();
    if (data.code === 0) {
      ElMessage.success("新增管理员成功");
      dialogVisible.value = false;
      loadAdmins();
    } else {
      ElMessage.error(data.msg || "新增管理员失败");
    }
  } catch (err) {
    console.error("新增管理员异常:", err);
    ElMessage.error("网络异常，请确认后端服务已启动");
  } finally {
    submitting.value = false;
  }
};

const changeStatus = async (row) => {
  const target = row.status === 1 ? 0 : 1;
  const action = target === 0 ? "停用" : "启用";
  try {
    await ElMessageBox.confirm(
      `确认${action}管理员「${row.name}」吗？`,
      "提示",
      { type: "warning" }
    );
  } catch {
    return;
  }
  try {
    const res = await fetch(
      `/admin/admins/${encodeURIComponent(row.name)}/status`,
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
      loadAdmins();
    } else {
      ElMessage.error(data.msg || `${action}失败`);
    }
  } catch (err) {
    console.error("更新管理员状态异常:", err);
    ElMessage.error("网络异常，请确认后端服务已启动");
  }
};

const openStationDialog = () => {
  stationForm.stationName = "";
  stationForm.city = "";
  stationDialogVisible.value = true;
  stationFormRef.value?.clearValidate();
};

const submitStation = async () => {
  const valid = await stationFormRef.value.validate().catch(() => false);
  if (!valid) return;
  stationSubmitting.value = true;
  try {
    const res = await fetch("/admin/stations", {
      method: "POST",
      headers: authHeader(),
      body: JSON.stringify({
        stationName: stationForm.stationName,
        city: stationForm.city,
      }),
    });
    if (res.status === 403) {
      ElMessage.error("无权限：仅超级管理员可新增车站");
      return;
    }
    const data = await res.json();
    if (data.code === 0) {
      ElMessage.success("新增车站成功");
      stationDialogVisible.value = false;
      await loadStations("");
      const newStation = (data.data1 && data.data1.id) || null;
      const found = stations.value.find(
        (s) => s.id === newStation || s.stationName === stationForm.stationName
      );
      if (found) {
        form.stationId = found.id;
      }
      noStationMatch.value = false;
    } else {
      ElMessage.error(data.msg || "新增车站失败");
    }
  } catch (err) {
    console.error("新增车站异常:", err);
    ElMessage.error("网络异常，请确认后端服务已启动");
  } finally {
    stationSubmitting.value = false;
  }
};

onMounted(() => {
  loadAdmins();
  loadStations("");
});
</script>

<template>
  <div class="admin-page">
    <section class="card">
      <div class="card-head">
        <div>
          <h3 class="card-title">管理员管理</h3>
          <p class="card-sub">
            仅超级管理员可查看与新增管理员；新增账号默认为普通管理员，账号只停用不删除。
          </p>
        </div>
        <el-button type="primary" class="add-btn" @click="openDialog">
          新增管理员
        </el-button>
      </div>

      <div class="filter-bar">
        <el-select
          v-model="filterStationId"
          placeholder="按车站筛选"
          clearable
          class="filter-item"
        >
          <el-option
            v-for="s in stations"
            :key="s.id"
            :label="s.stationName"
            :value="s.id"
          />
        </el-select>
        <el-select
          v-model="filterStatus"
          placeholder="按状态筛选"
          clearable
          class="filter-item"
        >
          <el-option label="启用" value="1" />
          <el-option label="停用" value="0" />
        </el-select>
      </div>

      <el-table v-loading="loading" :data="filteredAdmins" class="admin-table">
        <el-table-column prop="name" label="登录账号" min-width="130" />
        <el-table-column prop="staffNo" label="工号" min-width="110" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="stationName" label="所属车站" min-width="120" />
        <el-table-column label="角色" min-width="120">
          <template #default="{ row }">
            <el-tag
              :type="row.role === 'super_admin' ? 'warning' : 'info'"
              size="small"
            >
              {{ roleText(row.role) }}
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
        <el-table-column label="操作" min-width="100" fixed="right">
          <template #default="{ row }">
            <span v-if="row.name === currentName" class="self-tip"
              >当前账号</span
            >
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
    </section>
  </div>

  <el-dialog v-model="dialogVisible" title="新增管理员" width="560px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="登录名" prop="name">
        <el-input v-model="form.name" placeholder="3-20 位字母、数字或下划线" />
      </el-form-item>
      <el-form-item label="密码" prop="pwd">
        <el-input
          v-model="form.pwd"
          type="password"
          placeholder="6-20 位，含字母和数字"
          show-password
        />
      </el-form-item>
      <el-form-item label="员工工号" prop="staffNo">
        <el-input v-model="form.staffNo" placeholder="6-12 位字母或数字" />
      </el-form-item>
      <el-form-item label="所属车站" prop="stationId">
        <el-select
          v-model="form.stationId"
          filterable
          remote
          :remote-method="remoteStation"
          placeholder="输入车站名称搜索"
          class="station-select"
        >
          <el-option
            v-for="s in stations"
            :key="s.id"
            :label="s.city ? `${s.stationName}（${s.city}）` : s.stationName"
            :value="s.id"
          />
        </el-select>
        <el-button
          v-if="isSuperAdmin && noStationMatch"
          link
          type="primary"
          class="add-station-link"
          @click="openStationDialog"
        >
          新增该车站
        </el-button>
      </el-form-item>
      <el-form-item label="真实姓名" prop="realName">
        <el-input v-model="form.realName" placeholder="2-20 个字符" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="11 位手机号" />
      </el-form-item>
      <el-form-item label="身份证号" prop="idCard">
        <el-input v-model="form.idCard" placeholder="选填" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          maxlength="200"
          show-word-limit
          placeholder="选填，最长 200 字"
        />
      </el-form-item>
      <el-form-item label="角色">
        <el-tag type="info" size="small">普通管理员</el-tag>
        <span class="role-hint">角色由系统固定，不可选择</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit"
        >确定</el-button
      >
    </template>
  </el-dialog>

  <el-dialog v-model="stationDialogVisible" title="新增车站" width="400px">
    <el-form
      ref="stationFormRef"
      :model="stationForm"
      :rules="stationRules"
      label-width="80px"
    >
      <el-form-item label="车站名称" prop="stationName">
        <el-input v-model="stationForm.stationName" placeholder="如：北京南" />
      </el-form-item>
      <el-form-item label="所属城市" prop="city">
        <el-input v-model="stationForm.city" placeholder="选填，如：北京" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="stationDialogVisible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="stationSubmitting"
        @click="submitStation"
      >
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.admin-page {
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
  margin-bottom: 18px;
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

.add-btn {
  background: #14539e;
  border: none;
}

.add-btn:hover {
  background: #1a68c4;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-item {
  width: 180px;
}

.admin-table {
  width: 100%;
}

.self-tip {
  font-size: 12px;
  color: #8a97a8;
}

.station-select {
  width: 100%;
}

.add-station-link {
  padding: 0;
  margin-top: 4px;
}

.role-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #8a97a8;
}
</style>
