import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ElMessageBox } from 'element-plus'
import ProductCreationWizard from './ProductCreationWizard.vue'

// Mock 依赖和全局 API
vi.mock('element-plus', () => ({
  ElMessageBox: {
    confirm: vi.fn()
  }
}))

describe('ProductCreationWizard 返回逻辑交互规范测试', () => {
  let wrapper

  beforeEach(() => {
    vi.clearAllMocks()
    wrapper = mount(ProductCreationWizard, {
      global: {
        mocks: {
          $modal: {
            msgSuccess: vi.fn(),
            msgError: vi.fn()
          }
        },
        stubs: {
          // 根据实际引入进行 stubs
          ElPageHeader: true,
          ElButton: true,
          ElTooltip: true,
          ElSteps: true,
          ElStep: true,
          ElForm: true,
          ElFormItem: true,
          ElInput: true,
          ElTable: true,
          ElTableColumn: true
        }
      }
    })
  })

  it('1. 表单无变更时，直接返回，不弹出确认', async () => {
    // 模拟无变更状态
    wrapper.vm.activeStep = 0
    wrapper.vm.hasStep1DataChanged = vi.fn().mockReturnValue(false)
    
    await wrapper.vm.handleBack()
    
    expect(ElMessageBox.confirm).not.toHaveBeenCalled()
    expect(wrapper.emitted('back')).toBeTruthy()
  })

  it('2. 存在未保存变更，点击【保存并返回】且保存成功，执行返回', async () => {
    // 模拟有变更状态
    wrapper.vm.activeStep = 0
    wrapper.vm.hasStep1DataChanged = vi.fn().mockReturnValue(true)
    
    // 模拟确认弹窗返回 confirm（保存并返回）
    ElMessageBox.confirm.mockResolvedValue('confirm')
    
    // 模拟保存成功
    wrapper.vm.handleSave = vi.fn().mockResolvedValue(true)

    await wrapper.vm.handleBack()
    
    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      '您有未保存的商品信息，是否立即保存后再离开？',
      '提示',
      expect.objectContaining({
        confirmButtonText: '保存并返回',
        cancelButtonText: '不保存并返回',
        distinguishCancelAndClose: true,
        type: 'warning'
      })
    )
    expect(wrapper.vm.handleSave).toHaveBeenCalled()
    expect(wrapper.emitted('back')).toBeTruthy()
  })

  it('3. 存在未保存变更，点击【保存并返回】且保存失败，停留原页', async () => {
    wrapper.vm.activeStep = 0
    wrapper.vm.hasStep1DataChanged = vi.fn().mockReturnValue(true)
    
    ElMessageBox.confirm.mockResolvedValue('confirm')
    
    // 模拟保存失败
    wrapper.vm.handleSave = vi.fn().mockResolvedValue(false)

    await wrapper.vm.handleBack()
    
    expect(wrapper.vm.handleSave).toHaveBeenCalled()
    // 不触发 back 事件，保留在原页
    expect(wrapper.emitted('back')).toBeFalsy()
  })

  it('4. 存在未保存变更，点击【不保存并返回】，直接返回', async () => {
    wrapper.vm.activeStep = 0
    wrapper.vm.hasStep1DataChanged = vi.fn().mockReturnValue(true)
    
    // 模拟确认弹窗返回 cancel（不保存并返回）
    ElMessageBox.confirm.mockRejectedValue('cancel')

    await wrapper.vm.handleBack()
    
    expect(ElMessageBox.confirm).toHaveBeenCalled()
    // 不调用保存逻辑
    expect(wrapper.vm.handleSave).toBeUndefined() || expect(wrapper.vm.handleSave).not.toBeCalled()
    // 直接触发 back 事件
    expect(wrapper.emitted('back')).toBeTruthy()
  })

  it('5. 存在未保存变更，点击【取消】(关闭弹窗)，停留原页保持不变', async () => {
    wrapper.vm.activeStep = 0
    wrapper.vm.hasStep1DataChanged = vi.fn().mockReturnValue(true)
    
    // 模拟确认弹窗返回 close（取消）
    ElMessageBox.confirm.mockRejectedValue('close')

    await wrapper.vm.handleBack()
    
    expect(ElMessageBox.confirm).toHaveBeenCalled()
    // 不触发 back 事件
    expect(wrapper.emitted('back')).toBeFalsy()
  })
})
