export const navGroups = [
  {
    group: '总览',
    items: [{ label: '工作台', path: '/dashboard', icon: 'layout-dashboard' }]
  },
  {
    group: '作业',
    items: [
      { label: '入库作业', path: '/inbound', icon: 'package-plus', badge: '12' },
      { label: '出库作业', path: '/outbound', icon: 'package-minus', badge: '8' },
      { label: '盘点管理', path: '/stocktake', icon: 'clipboard-check' },
      { label: '退货管理', path: '/returns', icon: 'rotate-ccw', badge: '18' }
    ]
  },
  {
    group: '库存',
    items: [
      { label: '库存查询', path: '/inventory', icon: 'boxes' },
      { label: '库位管理', path: '/locations', icon: 'map' }
    ]
  },
  {
    group: '资料',
    items: [{ label: '基础资料', path: '/masterdata', icon: 'database' }]
  },
  {
    group: '物联',
    items: [{ label: '设备监控', path: '/devices', icon: 'cpu', badge: '2' }]
  },
  {
    group: '分析',
    items: [{ label: '数据分析', path: '/analytics', icon: 'chart-no-axes-combined' }]
  },
  {
    group: '系统',
    items: [
      { label: '操作日志', path: '/audit', icon: 'file-text' },
      { label: '系统设置', path: '/settings', icon: 'settings-2' }
    ]
  }
]

export const flatNav = navGroups.flatMap((group) => group.items)

export const pageMeta = {
  '/dashboard': { title: '工作台', desc: '仓库运行全景：月台、库存、作业与异常。' },
  '/inbound': { title: '入库作业', desc: '从预约到上架，收货、质检、库位推荐在同一条链路里完成。' },
  '/outbound': { title: '出库作业', desc: '订单分配、波次拣货、复核与发运。' },
  '/stocktake': { title: '盘点管理', desc: '盘点任务、扫码录入、差异复核与库存调整。' },
  '/returns': { title: '退货管理', desc: '退货登记、质检判定与处置。' },
  '/inventory': { title: '库存查询', desc: '按商品、库区与状态查看实时库存。' },
  '/locations': { title: '库位管理', desc: '库区平面图、库位占用与温区属性。' },
  '/masterdata': { title: '基础资料', desc: '商品、供应商与客户主数据。' },
  '/devices': { title: '设备监控', desc: 'MQTT 实时接入的传感器、AGV 与读写器。' },
  '/analytics': { title: '数据分析', desc: '出入库趋势、库存结构与算法效果对比。' },
  '/audit': { title: '操作日志', desc: '关键操作审计与登录安全。' },
  '/settings': { title: '系统设置', desc: '仓库资料、用户角色与预警规则。' }
}
