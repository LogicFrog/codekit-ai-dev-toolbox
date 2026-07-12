<template>
  <div class="thread-pool-monitor">
    <el-card header="线程池监控">
      <el-table :data="poolStats" stripe>
        <el-table-column prop="name" label="线程池名称" />
        <el-table-column prop="coreSize" label="核心线程数" />
        <el-table-column prop="activeCount" label="活跃线程" />
        <el-table-column prop="queueSize" label="队列长度" />
        <el-table-column prop="completedTasks" label="已完成任务" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="row.activeCount > row.coreSize * 0.8 ? 'danger' : 'success'">
              {{ row.activeCount > row.coreSize * 0.8 ? '繁忙' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const poolStats = ref([])
let timer = null

const fetchStats = async () => {
  const res = await fetch('/api/monitor/thread-pools')
  poolStats.value = await res.json()
}

onMounted(() => {
  fetchStats()
  timer = setInterval(fetchStats, 5000)
})

onUnmounted(() => clearInterval(timer))
</script>
