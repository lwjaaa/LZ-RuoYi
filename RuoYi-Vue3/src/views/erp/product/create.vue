<template>
  <div class="app-container">
    <ProductCreationWizard
        ref="wizardRef"
        @submit="handleSubmit"
        @back="handleBack"
    />
  </div>
</template>

<script setup name="ProductCreate">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductCreationWizard from '@/components/erp/ProductCreationWizard.vue'
import useTagsViewStore from '@/store/modules/tagsView'
const { proxy } = getCurrentInstance();

const route = useRoute()
const router = useRouter()
const tagsViewStore = useTagsViewStore()
const wizardRef = ref(null)

onMounted(() => {
  const selectedTagIds = route.query.tagIds ? JSON.parse(route.query.tagIds) : []
  const productId = route.params.productId || route.query.productId || null
  const step = parseInt(route.query.step) || 0

  // 直接调用 open 方法初始化数据
  wizardRef.value?.open(selectedTagIds, productId, step)
})

function handleBack() {
  // 返回商品列表
  tagsViewStore.delView(route)
  router.push('/erp/product')
}

function handleSubmit({ action, hasChanged }) {
  if (action === 'close') {
    handleBack()
  } else  {
    // continue/nextprev
    if (hasChanged) {
      proxy.$modal.msgSuccess("保存成功");
    }
  }
}
</script>

<style scoped>
.app-container {
  padding: 0;
}
</style>
