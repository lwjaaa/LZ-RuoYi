<template>
  <div class="shipping-fee-calculator">
    <el-card header="4px运费查询">
      <el-form :model="form" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="包装宽度(cm)">
              <el-input-number v-model="form.pkWidth" :min="1" :max="200" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="包装高度(cm)">
              <el-input-number v-model="form.pkHeight" :min="1" :max="200" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="包装长度(cm)">
              <el-input-number v-model="form.pkLength" :min="1" :max="200" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="重量(g)">
              <el-input-number v-model="form.pkWeight" :min="1" :max="50000" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="国家代码">
              <el-select v-model="form.countryCode" placeholder="请选择国家">
                <el-option label="美国" value="US" />
                <el-option label="中国" value="CN" />
                <el-option label="英国" value="GB" />
                <el-option label="德国" value="DE" />
                <el-option label="法国" value="FR" />
                <el-option label="日本" value="JP" />
                <el-option label="澳大利亚" value="AU" />
                <el-option label="加拿大" value="CA" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="邮政编码">
              <el-input v-model="form.postalCode" placeholder="请输入邮政编码" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" @click="handleCalculate" :loading="loading">
            查询运费
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 查询结果 -->
      <el-divider />
      <div v-if="result" class="result-section">
        <h3>查询结果</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="运费">
            <span class="price">¥{{ (result.shippingFee / 100).toFixed(2) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="预估时效">
            {{ result.estimatedDays || '7-15天' }}
          </el-descriptions-item>
          <el-descriptions-item label="物流服务">
            {{ result.serviceName || '4PX标准物流' }}
          </el-descriptions-item>
          <el-descriptions-item label="币种">
            {{ result.currency || 'CNY' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { calculateShipping } from '@/api/erp/shipping'

const loading = ref(false)
const result = ref(null)

const form = reactive({
  pkWidth: 20,
  pkHeight: 15,
  pkLength: 30,
  pkWeight: 500,
  countryCode: 'US',
  postalCode: '10001'
})

/**
 * 查询运费
 */
const handleCalculate = async () => {
  // 参数校验
  if (!form.pkWidth || !form.pkHeight || !form.pkLength || !form.pkWeight) {
    ElMessage.warning('请填写完整的包裹信息')
    return
  }

  loading.value = true
  try {
    const response = await calculateShipping(form)
    if (response.code === 200) {
      result.value = response.data
      ElMessage.success('运费查询成功')
    } else {
      ElMessage.error(response.msg || '运费查询失败')
    }
  } catch (error) {
    console.error('运费查询失败:', error)
    ElMessage.error('运费查询失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 重置表单
 */
const handleReset = () => {
  form.pkWidth = 20
  form.pkHeight = 15
  form.pkLength = 30
  form.pkWeight = 500
  form.countryCode = 'US'
  form.postalCode = '10001'
  result.value = null
}
</script>

<style scoped>
.shipping-fee-calculator {
  padding: 20px;
}

.result-section {
  margin-top: 20px;
}

.price {
  color: #f56c6c;
  font-size: 20px;
  font-weight: bold;
}
</style>
