<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";

const items = ref([]);
const loading = ref(false);
const keyword = ref("");
const filterAudit = ref("0");

const pagination = reactive({
  page: 1,
  limit: 10,
  total: 0,
});

const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref(null);

const auditVisible = ref(false);
const auditSubmitting = ref(false);
const auditTarget = ref(null);
const auditForm = reactive({
  auditStatus: 1,
  remark: "",
});

const authHeader = () => ({
  "Content-Type": "application/json",
  Authorization: localStorage.getItem("token") || "",
});

const auditText = (status) => {
  if (status === 0) return "待审核";
  if (status === 1) return "已通过";
  if (status === 2) return "已驳回";
  return "-";
};

const auditTagType = (status) => {
  if (status === 1) return "success";
  if (status === 2) return "danger";
  return "warning";
};

const statusText = (status) => {
  if (status === 0) return "待认领";
  if (status === 1) return "认领审核中";
  if (status === 2) return "已交接";
  if (status === 3) return "已下架";
  return "-";
};

const custodyText = (custody) => (custody === 1 ? "自行保管" : "交服务台");

const loadItems = async () => {
  loading.value = true;
  try {
    const params = new URLSearchParams({
      page: pagination.page,
      limit: pagination.limit,
    });
    if (filterAudit.value !== "") {
      params.append("auditStatus", filterAudit.value);
    }
    if (keyword.value.trim()) {
      params.append("keyword", keyword.value.trim());
    }
    const res = await fetch(`/admin/found?${params.toString()}`, {
      headers: authHeader(),
    });
    if (res.status === 401) {
      ElMessage.error("登录已过期，请重新登录");
      return;
    }
    if (res.status === 403) {
      ElMessage.error("无权限访问招领审核");
      return;
    }
    const data = await res.json();
    if (data.code === 0) {
      items.value = data.data || [];
      pagination.total = data.count || 0;
      if (items.value.length === 0 && pagination.page > 1) {
        pagination.page = Math.max(
          1,
          Math.ceil(pagination.total / pagination.limit)
        );
        loading.value = false;
        await loadItems();
        return;
      }
    } else {
      ElMessage.error(data.msg || "加载招领列表失败");
    }
  } catch (err) {
    console.error("加载招领列表异常:", err);
    ElMessage.error("网络异常，请确认后端服务已启动");
  } finally {
    loading.value = false;
  }
};

const handlePageChange = (page) => {
  pagination.page = page;
  loadItems();
};

const handleSizeChange = (limit) => {
  pagination.limit = limit;
  pagination.page = 1;
  loadItems();
};

const handleFilterChange = () => {
  pagination.page = 1;
  loadItems();
};

const resetFilter = () => {
  keyword.value = "";
  filterAudit.value = "0";
  pagination.page = 1;
  loadItems();
};

const openDetail = async (row) => {
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = row;
  try {
    const res = await fetch(`/admin/found/${row.foundId}`, {
      headers: authHeader(),
    });
    const data = await res.json();
    if (data.code === 0) {
      detail.value = data.data1;
    }
  } catch (err) {
    console.error("加载详情异常:", err);
  } finally {
    detailLoading.value = false;
  }
};

const openAudit = (row) => {
  auditTarget.value = row;
  auditForm.auditStatus = 1;
  auditForm.remark = "";
  auditVisible.value = true;
};

const submitAudit = async () => {
  if (!auditTarget.value) return;
  if (auditForm.auditStatus === 2 && !auditForm.remark.trim()) {
    ElMessage.warning("请填写驳回原因");
    return;
  }
  auditSubmitting.value = true;
  try {
    const res = await fetch(
      `/admin/found/${auditTarget.value.foundId}/audit`,
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
      ElMessage.error("无权限审核招领信息");
      return;
    }
    const data = await res.json();
    if (data.code === 0) {
      ElMessage.success(data.msg || "审核完成");
      auditVisible.value = false;
      detailVisible.value = false;
      loadItems();
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

onMounted(loadItems);
</script>

<template>
  <div class="found-page">
    <section class="card">
      <div class="card-head">
        <div>
          <h3 class="card-title">招领信息审核</h3>
          <p class="card-sub">
            审核旅客与乘务员提交的招领信息，通过后才会在前台展示。
          </p>
        </div>
      </div>

      <div class="filter-bar">
        <el-select
          v-model="filterAudit"
          class="filter-item"
          @change="handleFilterChange"
        >
          <el-option label="全部" value="" />
          <el-option label="待审核" value="0" />
          <el-option label="已通过" value="1" />
          <el-option label="已驳回" value="2" />
        </el-select>

        <el-input
          v-model="keyword"
          class="filter-item keyword"
          placeholder="物品名称 / 车次 / 发布者"
          clearable
          @keyup.enter="handleFilterChange"
        />

        <el-button type="primary" @click="handleFilterChange">
          查询
        </el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="items" class="found-table">
        <el-table-column label="照片" width="82">
          <template #default="{ row }">
            <el-image
              v-if="row.itemImage"
              :src="row.itemImage"
              :preview-src-list="[row.itemImage]"
              preview-teleported
              fit="cover"
              class="item-img"
            />
            <span v-else class="muted">无</span>
          </template>
        </el-table-column>

        <el-table-column prop="title" label="物品名称" min-width="120" />
        <el-table-column prop="categoryName" label="分类" min-width="90" />
        <el-table-column prop="trainNo" label="车次" min-width="80" />
        <el-table-column prop="foundTime" label="拾获时间" min-width="140" />
        <el-table-column prop="foundLocation" label="拾获地点" min-width="130" />
        <el-table-column label="保管方式" min-width="100">
          <template #default="{ row }">
            {{ custodyText(row.custody) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="storageLocation"
          label="保管地点"
          min-width="140"
        />
        <el-table-column label="发布者" min-width="130">
          <template #default="{ row }">
            <span>{{ row.publisherName || row.userId }}</span>
            <el-tag
              v-if="row.publisherRole === 'attendant'"
              type="success"
              size="small"
              class="official-tag"
            >
              官方认证
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="auditTagType(row.auditStatus)" size="small">
              {{ auditText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="物品状态" min-width="100">
          <template #default="{ row }">
            {{ statusText(row.status) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" min-width="140" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.auditStatus === 0"
              link
              type="warning"
              @click="openAudit(row)"
            >
              审核
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-bar">
        <el-pagination
          :current-page="pagination.page"
          :page-size="pagination.limit"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>
  </div>

  <!-- 详情 -->
  <el-dialog v-model="detailVisible" title="招领信息详情" width="720px">
    <div v-loading="detailLoading">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="物品名称" :span="2">
          {{ detail.title }}
        </el-descriptions-item>
        <el-descriptions-item label="分类">
          {{ detail.categoryName || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="车次">
          {{ detail.trainNo || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="车厢号">
          {{ detail.carriageNo || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="座位号">
          {{ detail.seatNo || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="拾获时间">
          {{ detail.foundTime || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="拾获地点">
          {{ detail.foundLocation || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="保管方式">
          {{ custodyText(detail.custody) }}
        </el-descriptions-item>
        <el-descriptions-item label="保管地点">
          {{ detail.storageLocation || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="物品状态">
          {{ statusText(detail.status) }}
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">
          {{ auditText(detail.auditStatus) }}
        </el-descriptions-item>
        <el-descriptions-item label="特征描述" :span="2">
          {{ detail.description || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="发布者">
          {{ detail.publisherName || detail.userId }}
          <el-tag
            v-if="detail.publisherRole === 'attendant'"
            type="success"
            size="small"
            class="official-tag"
          >
            官方认证
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发布者手机号">
          {{ detail.publisherPhone || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ detail.createTime || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="驳回原因">
          {{ detail.auditRemark || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="detail && detail.itemImage" class="detail-image">
        <el-image
          :src="detail.itemImage"
          :preview-src-list="[detail.itemImage]"
          preview-teleported
          fit="contain"
        />
      </div>
    </div>

    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
      <el-button
        v-if="detail && detail.auditStatus === 0"
        type="primary"
        @click="openAudit(detail)"
      >
        审核
      </el-button>
    </template>
  </el-dialog>

  <!-- 审核 -->
  <el-dialog v-model="auditVisible" title="审核招领信息" width="520px">
    <div v-if="auditTarget" class="audit-target">
      <span>物品：{{ auditTarget.title }}</span>
      <span>车次：{{ auditTarget.trainNo }}</span>
      <span>发布者：{{ auditTarget.publisherName || auditTarget.userId }}</span>
    </div>
    <el-form label-width="88px">
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
          maxlength="255"
          show-word-limit
          placeholder="必填，会展示给发布者"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="auditVisible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="auditSubmitting"
        @click="submitAudit"
      >
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.found-page {
  max-width: 1400px;
}

.card {
  padding: 24px 28px;
  background: #fff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
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
  margin: 0 0 20px;
  font-size: 12px;
  color: #8a97a8;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-item {
  width: 190px;
}

.filter-item.keyword {
  width: 260px;
}

.item-img {
  width: 52px;
  height: 52px;
  border-radius: 6px;
}

.muted {
  color: #a8b3c2;
}

.official-tag {
  margin-left: 6px;
}

.pager-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.audit-target {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 18px;
  padding: 12px 16px;
  font-size: 13px;
  color: #475569;
  background: #f5f7fb;
  border-radius: 6px;
}

.detail-image {
  margin-top: 18px;
  text-align: center;
}

.detail-image .el-image {
  max-width: 320px;
  max-height: 240px;
}
</style>
