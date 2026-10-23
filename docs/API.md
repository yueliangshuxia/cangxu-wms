# 仓序 WMS 接口文档

> Base URL: `http://localhost:8080/api/v1`  
> 除登录与健康检查外，所有接口都需要 `Authorization: Bearer <token>`。  
> 统一响应：`{ "code": 200, "message": "ok", "data": ..., "traceId": "xxxxxxxx" }`

## 1. 认证与用户

| 方法 | 路径 | 说明 | 角色 |
|---|---|---|---|
| POST | `/auth/login` | 登录，返回 JWT 与用户信息 | 公开 |
| GET | `/auth/me` | 当前用户 | 登录 |
| POST | `/auth/logout` | 退出并写审计 | 登录 |
| GET | `/users?page=&size=&keyword=&role=&status=` | 用户分页 | ADMIN/MANAGER |
| GET | `/users/roles` | 角色字典 | ADMIN/MANAGER |
| POST | `/users` | 新增用户 | ADMIN/MANAGER |
| PUT | `/users/{id}` | 编辑姓名/角色/状态，可选重置密码 | ADMIN/MANAGER |
| POST | `/users/{id}/reset-password` | 重置密码 | ADMIN/MANAGER |
| DELETE | `/users/{id}` | 删除用户（不能删除自己或最后一个管理员） | ADMIN/MANAGER |

登录示例：

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"chenyu","password":"wms2026"}'
```

## 2. 工作台与分析

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/dashboard/overview` | 商品/供应商/客户/用户数、库存、7 日趋势、库区占用、最近审计 |
| GET | `/analytics/overview?days=30` | 出入库趋势、品类占比、库位热力、KPI、算法实验结果 |
| GET | `/alerts` | 低库存、设备离线、温湿度、待质检退货、进行中盘点 |

`analytics/overview` 的实验数据来自 `experiments/run_experiment.py` 生成的
`backend/src/main/resources/experiments/algorithm-comparison.json`。

## 3. 主数据与库存

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST | `/products` | 商品列表/新增 |
| PUT | `/products/{id}` | 编辑商品 |
| GET/POST | `/suppliers` | 供应商 |
| GET/POST | `/customers` | 客户 |
| GET | `/inventory?page=&size=&keyword=&category=&status=` | 库存明细分页 |
| GET | `/inventory/summary` | 总库存、可用、锁定、低库存 SKU 数 |
| GET | `/locations` | 全部库位 |
| GET | `/locations/summary` | 占用/空闲/锁定/待盘点 |
| PATCH | `/locations/{code}/status` | 修改库位状态 |

## 4. 入库（AHP-TOPSIS）

| 方法 | 路径 | 说明 | 角色 |
|---|---|---|---|
| GET | `/inbound/orders?page=&size=&status=&keyword=` | 入库单分页 | 登录 |
| GET | `/inbound/summary` | 状态汇总 | 登录 |
| POST | `/inbound/orders` | 创建入库单 | ADMIN/MANAGER/RECEIVER |
| POST | `/inbound/orders/{id}/receive` | 收货 | ADMIN/MANAGER/RECEIVER |
| POST | `/inbound/orders/{id}/inspect` | 质检 | ADMIN/MANAGER/INSPECTOR |
| GET | `/inbound/orders/{id}/slot-recommendation` | AHP-TOPSIS 上架推荐 | 登录 |
| POST | `/inbound/orders/{id}/putaway` | 确认上架，body `{"locationCode":"A-01-10"}` | ADMIN/MANAGER/RECEIVER |

推荐接口返回 AHP 权重、一致性比例 CR、候选库位的四项指标与贴近度。
周转率来自 `outbound_item` 历史出库数量，相关性来自 Apriori 订单关联规则，
距离按库区/巷道/列计算，体积适配按库位容量与入库数量计算。

## 5. 出库

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/outbound/orders?page=&size=&status=&keyword=` | 出库单分页 |
| GET | `/outbound/summary` | 状态汇总 |
| GET | `/outbound/route?waveNo=W-07` | 按波次返回真实拣货路线（库位、商品、数量） |
| POST | `/outbound/orders` | 创建出库单 |
| POST | `/outbound/waves` | 生成波次 |
| POST | `/outbound/orders/{id}/pick` | 拣货完成 |
| POST | `/outbound/orders/{id}/ship` | 发运 |

## 6. 盘点、退货、设备、设置、审计

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/stocktakes?page=&size=&status=&keyword=` | 盘点任务 |
| GET | `/stocktakes/summary` | 状态汇总 |
| GET | `/stocktakes/diffs` | 差异明细 |
| POST | `/stocktakes` | 创建盘点 |
| POST | `/stocktakes/{id}/start` | 开始盘点 |
| POST | `/stocktakes/{id}/scan` | 扫描库位，body `{"code":"A-01-03"}` |
| GET | `/returns?page=&size=&status=&keyword=` | 退货单 |
| GET | `/returns/summary` | 退货统计、原因分布、再上架率 |
| POST | `/returns` | 新建退货单 |
| POST | `/returns/{id}/inspect` | 质检处置，body `{"disposition":"resell"}` |
| GET | `/devices` | 设备列表 |
| GET | `/devices/summary` | 设备汇总与最新温湿度 |
| GET | `/devices/{code}/telemetry?metric=temp&hours=24` | 遥测曲线 |
| PUT | `/devices/{code}` | 设备配置（位置/状态/电量/信号） |
| GET/PUT | `/settings` | 系统设置键值对 |
| GET | `/audit-logs?page=&size=&module=&result=&keyword=` | 审计日志 |
| GET | `/audit-logs/stats` | 登录、失败、敏感操作统计 |

## 7. CSV 导出

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/exports/inventory` | 库存明细 |
| GET | `/exports/locations` | 库位 |
| GET | `/exports/inbound` | 入库单 + 明细 |
| GET | `/exports/outbound` | 出库单 |
| GET | `/exports/stocktakes` | 盘点任务 |
| GET | `/exports/returns` | 退货单 |
| GET | `/exports/audit-logs` | 审计日志（ADMIN/MANAGER） |
| GET | `/exports/masterdata` | 商品/供应商/客户 |
| GET | `/exports/analytics?days=30` | 分析报告 |

导出文件为 UTF-8 BOM CSV，可直接用 Excel 打开中文。

## 8. 错误码

| code | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 参数或业务校验失败 |
| 401 | 未登录/令牌过期 |
| 403 | 角色无权限 |
| 404 | 资源不存在 |
| 409 | 状态冲突（如库位锁定） |
| 500 | 未处理异常 |
