/* 仓序 WMS — mock data for the static demo.
   All figures are simulated for presentation purposes. */
(function () {
  const pad = (n) => String(n).padStart(2, "0");

  const NAV = [
    { group: "总览", items: [
      { id: "dashboard", label: "工作台", href: "index.html", icon: "layout-dashboard" }
    ]},
    { group: "作业", items: [
      { id: "inbound", label: "入库作业", href: "inbound.html", icon: "package-plus", badge: "12" },
      { id: "outbound", label: "出库作业", href: "outbound.html", icon: "package-minus", badge: "8" },
      { id: "stocktake", label: "盘点管理", href: "stocktake.html", icon: "clipboard-check" },
      { id: "returns", label: "退货管理", href: "returns.html", icon: "rotate-ccw", badge: "18" }
    ]},
    { group: "库存", items: [
      { id: "inventory", label: "库存查询", href: "inventory.html", icon: "boxes" },
      { id: "locations", label: "库位管理", href: "locations.html", icon: "map" }
    ]},
    { group: "资料", items: [
      { id: "masterdata", label: "基础资料", href: "masterdata.html", icon: "database" }
    ]},
    { group: "物联", items: [
      { id: "devices", label: "设备监控", href: "devices.html", icon: "cpu", badge: "2" }
    ]},
    { group: "分析", items: [
      { id: "analytics", label: "数据分析", href: "analytics.html", icon: "chart-no-axes-combined" }
    ]}
  ];

  const PAGE_META = {
    dashboard: { title: "工作台", crumb: "工作台", desc: "仓库运行全景：月台、库存、作业与异常。" },
    inbound: { title: "入库作业", crumb: "入库作业", desc: "从预约到上架，一条链路完成收货与质检。" },
    outbound: { title: "出库作业", crumb: "出库作业", desc: "订单分配、波次拣货与复核发运。" },
    inventory: { title: "库存查询", crumb: "库存查询", desc: "按商品、库区与状态查看实时库存。" },
    locations: { title: "库位管理", crumb: "库位管理", desc: "库区平面图、库位占用与温区属性。" },
    devices: { title: "设备监控", crumb: "设备监控", desc: "MQTT 实时接入的传感器、AGV 与读写器。" },
    analytics: { title: "数据分析", crumb: "数据分析", desc: "出入库趋势、库存结构与算法效果对比。" },
    masterdata: { title: "基础资料", crumb: "基础资料", desc: "商品、供应商与客户主数据。" },
    stocktake: { title: "盘点管理", crumb: "盘点管理", desc: "盘点任务、账实差异与调整。" },
    returns: { title: "退货管理", crumb: "退货管理", desc: "退货质检、处置与再上架。" },
    audit: { title: "操作日志", crumb: "操作日志", desc: "关键操作审计与登录安全。" },
    settings: { title: "系统设置", crumb: "系统设置", desc: "仓库资料、用户角色与预警规则。" }
  };

  const DOCKS = [
    { no: "01", name: "1 号月台", state: "busy", label: "卸货中", meta: "粤B·D3821 · 已用 18 分钟" },
    { no: "02", name: "2 号月台", state: "idle", label: "空闲", meta: "下一预约 13:40" },
    { no: "03", name: "3 号月台", state: "wait", label: "待质检", meta: "沪A·K9210 · 排队 6 分钟" },
    { no: "04", name: "4 号月台", state: "busy", label: "装车中", meta: "苏E·F2708 · 已用 12 分钟" }
  ];

  const KPIS = [
    { label: "在库 SKU", icon: "boxes", value: 8426, unit: "种", delta: "+2.4%", dir: "up", foot: "共 42,680 件", progress: 0.86 },
    { label: "今日入库", icon: "package-plus", value: 1286, unit: "件", delta: "+12.6%", dir: "up", foot: "目标 1,500 件", progress: 0.857, hot: true },
    { label: "今日出库", icon: "package-minus", value: 1742, unit: "件", delta: "+8.1%", dir: "up", foot: "目标 1,600 件", progress: 1 },
    { label: "库存准确率", icon: "circle-check", value: 99.2, unit: "%", delta: "-0.3%", dir: "down", foot: "盘点差异 12 件", progress: 0.992 },
    { label: "平均周转", icon: "refresh-cw", value: 18.4, unit: "天", delta: "-1.2 天", dir: "good", foot: "目标 ≤ 20 天", progress: 0.92 }
  ];

  const ACTIVITIES = [
    { time: "10:42", type: "入库", doc: "ASN-20261020-004", sku: "WH-1001 降噪蓝牙耳机", operator: "林舟", qty: 240, status: "已上架" },
    { time: "10:36", type: "出库", doc: "SO-20261020-018", sku: "WH-2002 智能音箱 Mini", operator: "周可", qty: 36, status: "拣货中" },
    { time: "10:28", type: "移库", doc: "MV-20261020-007", sku: "WH-3002 保温杯 500ml", operator: "系统", qty: 120, status: "已完成" },
    { time: "10:15", type: "出库", doc: "SO-20261020-016", sku: "WH-1004 无线静音鼠标", operator: "陈屿", qty: 80, status: "待复核" },
    { time: "10:03", type: "入库", doc: "ASN-20261020-003", sku: "WH-1006 65W 氮化镓充电器", operator: "林舟", qty: 180, status: "质检中" },
    { time: "09:51", type: "盘点", doc: "CC-20261020-A02", sku: "A 区 · 02 巷道", operator: "周可", qty: 64, status: "已完成" }
  ];

  const ALERTS = [
    { level: "danger", icon: "triangle-alert", title: "A-03-02 温度 28.4°C，超过阈值 26°C", meta: "温湿度传感器 TH-A01 · 2 分钟前", action: "查看" },
    { level: "warn", icon: "package-search", title: "WH-1003 充电宝低于安全库存", meta: "可用 60 件 / 安全库存 120 件", action: "补货" },
    { level: "info", icon: "bot", title: "AGV-07 电量 18%，已自动回充", meta: "充电位 C-00-02 · 8 分钟前", action: "详情" },
    { level: "warn", icon: "clipboard-check", title: "出库单 SO-20261020-018 复核差异 2 件", meta: "复核员 周可 · 需人工确认", action: "处理" }
  ];

  const INBOUND = [
    { id: "ASN-20261020-001", supplier: "深圳华创电子", product: "降噪蓝牙耳机", sku: "WH-1001", qty: 240, eta: "10-20 09:30", dock: "1 号月台", status: "待收货", operator: "林舟" },
    { id: "ASN-20261020-002", supplier: "东莞智造科技", product: "机械键盘 K87", sku: "WH-1002", qty: 160, eta: "10-20 10:10", dock: "1 号月台", status: "待收货", operator: "林舟" },
    { id: "ASN-20261020-003", supplier: "深圳华创电子", product: "65W 氮化镓充电器", sku: "WH-1006", qty: 180, eta: "10-20 10:35", dock: "3 号月台", status: "质检中", operator: "周可" },
    { id: "ASN-20261020-004", supplier: "苏州恒达物流", product: "护眼台灯 Pro", sku: "WH-2001", qty: 120, eta: "10-20 11:00", dock: "2 号月台", status: "待上架", operator: "陈屿" },
    { id: "ASN-20261020-005", supplier: "杭州云集供应链", product: "城市通勤双肩包", sku: "WH-3001", qty: 300, eta: "10-20 11:20", dock: "4 号月台", status: "待收货", operator: "林舟" },
    { id: "ASN-20261020-006", supplier: "宁波海川日用品", product: "不锈钢保温杯 500ml", sku: "WH-3002", qty: 480, eta: "10-20 11:45", dock: "1 号月台", status: "待上架", operator: "周可" },
    { id: "ASN-20261020-007", supplier: "东莞智造科技", product: "无线静音鼠标", sku: "WH-1004", qty: 260, eta: "10-20 12:10", dock: "2 号月台", status: "质检中", operator: "陈屿" },
    { id: "ASN-20261020-008", supplier: "深圳华创电子", product: "智能运动手表", sku: "WH-1005", qty: 90, eta: "10-20 13:00", dock: "3 号月台", status: "待收货", operator: "林舟" },
    { id: "ASN-20261020-009", supplier: "杭州云集供应链", product: "智能音箱 Mini", sku: "WH-2002", qty: 140, eta: "10-20 13:30", dock: "4 号月台", status: "待上架", operator: "周可" },
    { id: "ASN-20261020-010", supplier: "宁波海川日用品", product: "USB-C 编织数据线", sku: "WH-1007", qty: 600, eta: "10-20 14:00", dock: "1 号月台", status: "待收货", operator: "陈屿" },
    { id: "ASN-20261020-011", supplier: "苏州恒达物流", product: "七合一扩展坞", sku: "WH-2003", qty: 150, eta: "10-20 14:20", dock: "2 号月台", status: "质检中", operator: "林舟" },
    { id: "ASN-20261020-012", supplier: "深圳华创电子", product: "智能运动手表", sku: "WH-1005", qty: 60, eta: "10-19 16:20", dock: "1 号月台", status: "已完成", operator: "周可" },
    { id: "ASN-20261020-013", supplier: "苏州恒达物流", product: "护眼台灯 Pro", sku: "WH-2001", qty: 80, eta: "10-19 15:40", dock: "3 号月台", status: "已完成", operator: "陈屿" },
    { id: "ASN-20261020-014", supplier: "杭州云集供应链", product: "城市通勤双肩包", sku: "WH-3001", qty: 200, eta: "10-19 14:10", dock: "2 号月台", status: "已完成", operator: "林舟" }
  ];

  const OUTBOUND = [
    { id: "SO-20261020-018", customer: "天猫旗舰店", items: 12, qty: 86, priority: "加急", wave: "W-07", status: "拣货中", eta: "11:30" },
    { id: "SO-20261020-017", customer: "京东自营", items: 8, qty: 64, priority: "普通", wave: "W-07", status: "拣货中", eta: "12:00" },
    { id: "SO-20261020-016", customer: "抖音商城", items: 5, qty: 80, priority: "加急", wave: "W-06", status: "待复核", eta: "10:50" },
    { id: "SO-20261020-015", customer: "线下门店 · 苏州中心", items: 21, qty: 210, priority: "普通", wave: "W-06", status: "待复核", eta: "11:10" },
    { id: "SO-20261020-014", customer: "天猫旗舰店", items: 3, qty: 24, priority: "普通", wave: "W-05", status: "已发货", eta: "09:40" },
    { id: "SO-20261020-013", customer: "拼多多官方店", items: 16, qty: 128, priority: "普通", wave: "W-05", status: "已发货", eta: "09:20" },
    { id: "SO-20261020-012", customer: "京东自营", items: 6, qty: 42, priority: "加急", wave: "W-04", status: "已发货", eta: "08:55" },
    { id: "SO-20261020-011", customer: "抖音商城", items: 9, qty: 96, priority: "普通", wave: "—", status: "待分配", eta: "13:00" },
    { id: "SO-20261020-010", customer: "线下门店 · 园区店", items: 4, qty: 38, priority: "普通", wave: "—", status: "待分配", eta: "13:30" },
    { id: "SO-20261020-009", customer: "天猫旗舰店", items: 7, qty: 56, priority: "加急", wave: "—", status: "待分配", eta: "13:40" }
  ];

  const INVENTORY = [
    { sku: "WH-1001", name: "降噪蓝牙耳机", cat: "电子配件", loc: "A-01-03", stock: 1280, available: 1090, locked: 190, safety: 300, status: "正常", updated: "10-20 10:42", img: "assets/img/products/headphone.jpg" },
    { sku: "WH-1002", name: "机械键盘 K87", cat: "电子配件", loc: "A-01-05", stock: 640, available: 610, locked: 30, safety: 200, status: "正常", updated: "10-20 10:38", img: "assets/img/products/keyboard.jpg" },
    { sku: "WH-1003", name: "20000mAh 充电宝", cat: "电子配件", loc: "A-02-01", stock: 86, available: 60, locked: 26, safety: 120, status: "低库存", updated: "10-20 10:31", img: "assets/img/products/powerbank.jpg" },
    { sku: "WH-1004", name: "无线静音鼠标", cat: "电子配件", loc: "A-02-04", stock: 940, available: 920, locked: 20, safety: 200, status: "正常", updated: "10-20 10:26", img: "assets/img/products/mouse.jpg" },
    { sku: "WH-1005", name: "智能运动手表", cat: "电子配件", loc: "A-03-02", stock: 220, available: 150, locked: 70, safety: 80, status: "待检", updated: "10-20 10:18", img: "assets/img/products/watch.jpg" },
    { sku: "WH-1006", name: "65W 氮化镓充电器", cat: "电子配件", loc: "A-03-05", stock: 58, available: 40, locked: 18, safety: 100, status: "低库存", updated: "10-20 10:12", img: "assets/img/products/charger.jpg" },
    { sku: "WH-1007", name: "USB-C 编织数据线", cat: "电子配件", loc: "A-04-01", stock: 2400, available: 2260, locked: 140, safety: 500, status: "正常", updated: "10-20 10:04", img: "assets/img/products/cable.jpg" },
    { sku: "WH-2001", name: "护眼台灯 Pro", cat: "智能家居", loc: "B-01-02", stock: 420, available: 380, locked: 40, safety: 150, status: "正常", updated: "10-20 09:58", img: "assets/img/products/lamp.jpg" },
    { sku: "WH-2002", name: "智能音箱 Mini", cat: "智能家居", loc: "B-01-06", stock: 310, available: 290, locked: 20, safety: 100, status: "正常", updated: "10-20 09:51", img: "assets/img/products/speaker.jpg" },
    { sku: "WH-3001", name: "城市通勤双肩包", cat: "户外装备", loc: "C-02-03", stock: 780, available: 720, locked: 60, safety: 250, status: "正常", updated: "10-20 09:44", img: "assets/img/products/backpack.jpg" },
    { sku: "WH-3002", name: "不锈钢保温杯 500ml", cat: "生活用品", loc: "C-03-01", stock: 1560, available: 1500, locked: 60, safety: 400, status: "正常", updated: "10-20 09:36", img: "assets/img/products/bottle.jpg" },
    { sku: "WH-2003", name: "七合一扩展坞", cat: "电子配件", loc: "B-02-02", stock: 340, available: 300, locked: 40, safety: 100, status: "正常", updated: "10-20 09:20", img: "assets/img/products/hub.jpg" }
  ];

  const ZONES = [
    { code: "A", name: "电子配件", rows: 3, cols: 10, temp: "常温" },
    { code: "B", name: "智能家居", rows: 3, cols: 10, temp: "常温" },
    { code: "C", name: "户外与生活", rows: 3, cols: 10, temp: "常温" },
    { code: "D", name: "大件商品", rows: 3, cols: 10, temp: "恒温" },
    { code: "E", name: "退货暂存", rows: 2, cols: 10, temp: "常温" }
  ];

  const LOCATIONS = [];
  const locSkus = INVENTORY.map((x) => x.sku + " " + x.name);
  ZONES.forEach((z, zi) => {
    for (let r = 1; r <= z.rows; r++) {
      for (let c = 1; c <= z.cols; c++) {
        const code = `${z.code}-${pad(r)}-${pad(c)}`;
        const h = (zi * 37 + r * 11 + c * 7) % 100;
        let status = "occ";
        if (h >= 78 && h < 90) status = "free";
        else if (h >= 90 && h < 96) status = "lock";
        else if (h >= 96) status = "check";
        const sku = status === "occ" ? locSkus[(zi * 7 + r * 3 + c) % locSkus.length] : "";
        LOCATIONS.push({
          code, zone: z.code, zoneName: z.name, row: r, col: c, temp: z.temp,
          status, sku,
          capacity: z.code === "D" ? 60 : 120,
          used: status === "occ" ? 40 + ((h * 13) % 75) : 0,
          lastCheck: `10-${pad(14 + (h % 6))} ${pad(8 + (h % 9))}:${pad((h * 7) % 60)}`
        });
      }
    }
  });

  // Align the map's occupied cells with the inventory master locations.
  INVENTORY.forEach((item) => {
    const l = LOCATIONS.find((x) => x.code === item.loc);
    if (l) {
      l.status = "occ";
      l.sku = `${item.sku} ${item.name}`;
      l.used = Math.min(l.capacity, Math.max(40, Math.round(item.stock * 0.06)));
    }
  });

  const DEVICES = [
    { id: "GW-EDGE-01", type: "边缘网关", icon: "server", loc: "机房 · 1F", status: "在线", battery: "—", signal: "-42 dBm", beat: "3 秒前" },
    { id: "TH-A01", type: "温湿度传感器", icon: "thermometer", loc: "A 区 · 03 巷道", status: "在线", battery: "86%", signal: "-51 dBm", beat: "5 秒前" },
    { id: "TH-B01", type: "温湿度传感器", icon: "thermometer", loc: "B 区 · 01 巷道", status: "在线", battery: "74%", signal: "-58 dBm", beat: "8 秒前" },
    { id: "TH-D01", type: "温湿度传感器", icon: "thermometer", loc: "D 区 · 02 巷道", status: "在线", battery: "91%", signal: "-49 dBm", beat: "4 秒前" },
    { id: "SM-A01", type: "烟雾传感器", icon: "wind", loc: "A 区 · 顶部", status: "在线", battery: "98%", signal: "-60 dBm", beat: "12 秒前" },
    { id: "DR-MAIN", type: "门禁控制器", icon: "door-open", loc: "主入口", status: "在线", battery: "—", signal: "有线", beat: "2 秒前" },
    { id: "RFID-A01", type: "RFID 读写器", icon: "radio", loc: "A 区 · 收货口", status: "在线", battery: "—", signal: "有线", beat: "1 秒前" },
    { id: "AGV-01", type: "搬运机器人", icon: "bot", loc: "A 区 · 主通道", status: "作业中", battery: "72%", signal: "WiFi -47 dBm", beat: "2 秒前" },
    { id: "AGV-04", type: "搬运机器人", icon: "bot", loc: "B 区 · 主通道", status: "作业中", battery: "64%", signal: "WiFi -53 dBm", beat: "3 秒前" },
    { id: "AGV-07", type: "搬运机器人", icon: "bot", loc: "充电位 C-00-02", status: "充电中", battery: "18%", signal: "WiFi -61 dBm", beat: "6 秒前" }
  ];

  const AGVS = [
    { id: "AGV-01", task: "A-02-04 → 出库缓存区", battery: 72, state: "搬运中" },
    { id: "AGV-02", task: "待命", battery: 88, state: "空闲" },
    { id: "AGV-03", task: "1 号月台 → A-01-03", battery: 56, state: "搬运中" },
    { id: "AGV-04", task: "B-01-06 → 复核台", battery: 64, state: "搬运中" },
    { id: "AGV-05", task: "待命", battery: 93, state: "空闲" },
    { id: "AGV-06", task: "故障待修", battery: 41, state: "离线" },
    { id: "AGV-07", task: "充电位 C-00-02", battery: 18, state: "充电中" },
    { id: "AGV-08", task: "待命", battery: 81, state: "空闲" }
  ];

  const CHART = {
    trend7: {
      dates: ["10/14", "10/15", "10/16", "10/17", "10/18", "10/19", "10/20"],
      inbound: [980, 1120, 1040, 1286, 1190, 860, 1286],
      outbound: [1240, 1380, 1290, 1510, 1640, 980, 1742]
    },
    zoneOccupancy: [
      { zone: "A 区 电子配件", value: 92 },
      { zone: "B 区 智能家居", value: 78 },
      { zone: "C 区 户外生活", value: 64 },
      { zone: "D 区 大件商品", value: 86 },
      { zone: "E 区 退货暂存", value: 41 }
    ],
    categoryShare: [
      { name: "电子配件", value: 38 },
      { name: "智能家居", value: 24 },
      { name: "户外装备", value: 16 },
      { name: "生活用品", value: 13 },
      { name: "其他", value: 9 }
    ],
    algo: [
      { method: "随机上架", distance: 83.6, time: 42.8, utilization: 68.4 },
      { method: "ABC 分类", distance: 76.3, time: 38.2, utilization: 74.1 },
      { method: "AHP-TOPSIS", distance: 68.1, time: 32.6, utilization: 83.4 }
    ],
    temp: [22.1, 21.8, 21.6, 21.4, 21.5, 21.9, 22.4, 22.8, 23.2, 23.6, 24.1, 24.6, 24.9, 25.1, 24.8, 24.2, 23.7, 23.1, 22.8, 22.5, 22.3, 22.2, 22.0, 22.6],
    humidity: [49, 48, 47, 46, 45, 46, 47, 48, 50, 51, 52, 53, 53, 52, 51, 50, 49, 48, 47, 47, 46, 46, 47, 48]
  };

  const PRODUCTS = [
    { sku: "WH-1001", name: "降噪蓝牙耳机", cat: "电子配件", spec: "入耳式 / 蓝牙 5.3", unit: "件", temp: "常温", status: "在售", img: "assets/img/products/headphone.jpg", updated: "10-20 10:42" },
    { sku: "WH-1002", name: "机械键盘 K87", cat: "电子配件", spec: "87 键 / 青轴", unit: "件", temp: "常温", status: "在售", img: "assets/img/products/keyboard.jpg", updated: "10-20 10:38" },
    { sku: "WH-1003", name: "20000mAh 充电宝", cat: "电子配件", spec: "20000mAh / 22.5W", unit: "件", temp: "常温", status: "在售", img: "assets/img/products/powerbank.jpg", updated: "10-20 10:31" },
    { sku: "WH-1004", name: "无线静音鼠标", cat: "电子配件", spec: "2.4G / 静音微动", unit: "件", temp: "常温", status: "在售", img: "assets/img/products/mouse.jpg", updated: "10-20 10:26" },
    { sku: "WH-1005", name: "智能运动手表", cat: "电子配件", spec: "46mm / GPS", unit: "件", temp: "常温", status: "在售", img: "assets/img/products/watch.jpg", updated: "10-20 10:18" },
    { sku: "WH-1006", name: "65W 氮化镓充电器", cat: "电子配件", spec: "65W / 双 C 口", unit: "件", temp: "常温", status: "在售", img: "assets/img/products/charger.jpg", updated: "10-20 10:12" },
    { sku: "WH-1007", name: "USB-C 编织数据线", cat: "电子配件", spec: "1.5m / 100W", unit: "件", temp: "常温", status: "在售", img: "assets/img/products/cable.jpg", updated: "10-20 10:04" },
    { sku: "WH-2001", name: "护眼台灯 Pro", cat: "智能家居", spec: "国 AA / 无频闪", unit: "台", temp: "常温", status: "在售", img: "assets/img/products/lamp.jpg", updated: "10-20 09:58" },
    { sku: "WH-2002", name: "智能音箱 Mini", cat: "智能家居", spec: "5W / WiFi", unit: "台", temp: "常温", status: "在售", img: "assets/img/products/speaker.jpg", updated: "10-20 09:51" },
    { sku: "WH-3001", name: "城市通勤双肩包", cat: "户外装备", spec: "25L / 防泼水", unit: "件", temp: "常温", status: "在售", img: "assets/img/products/backpack.jpg", updated: "10-20 09:44" },
    { sku: "WH-3002", name: "不锈钢保温杯 500ml", cat: "生活用品", spec: "316L / 500ml", unit: "个", temp: "常温", status: "在售", img: "assets/img/products/bottle.jpg", updated: "10-20 09:36" },
    { sku: "WH-2003", name: "七合一扩展坞", cat: "电子配件", spec: "7 合 1 / HDMI", unit: "件", temp: "常温", status: "在售", img: "assets/img/products/hub.jpg", updated: "10-20 09:20" }
  ];

  const SUPPLIERS = [
    { code: "SUP-001", name: "深圳华创电子", contact: "李工", phone: "138****6210", cat: "电子配件", status: "合作中" },
    { code: "SUP-002", name: "东莞智造科技", contact: "陈工", phone: "139****3382", cat: "智能家居", status: "合作中" },
    { code: "SUP-003", name: "苏州恒达物流", contact: "周经理", phone: "137****8821", cat: "日用百货", status: "合作中" },
    { code: "SUP-004", name: "杭州云集供应链", contact: "王经理", phone: "135****1076", cat: "户外装备", status: "合作中" },
    { code: "SUP-005", name: "宁波海川日用品", contact: "徐经理", phone: "136****4420", cat: "生活用品", status: "合作中" },
    { code: "SUP-006", name: "上海蓝芯半导体", contact: "赵工", phone: "133****7712", cat: "电子元件", status: "评估中" },
    { code: "SUP-007", name: "无锡捷运包装", contact: "钱经理", phone: "132****9088", cat: "包装耗材", status: "合作中" },
    { code: "SUP-008", name: "南京云仓科技", contact: "孙工", phone: "131****5566", cat: "仓储服务", status: "暂停" }
  ];

  const CUSTOMERS = [
    { code: "CUS-001", name: "天猫旗舰店", channel: "电商", contact: "王经理", phone: "138****1024", status: "合作中" },
    { code: "CUS-002", name: "京东自营", channel: "电商", contact: "李经理", phone: "139****2088", status: "合作中" },
    { code: "CUS-003", name: "抖音商城", channel: "电商", contact: "周经理", phone: "137****3301", status: "合作中" },
    { code: "CUS-004", name: "拼多多官方店", channel: "电商", contact: "陈经理", phone: "136****4420", status: "合作中" },
    { code: "CUS-005", name: "线下门店 · 苏州中心", channel: "线下", contact: "许店长", phone: "135****7712", status: "合作中" },
    { code: "CUS-006", name: "线下门店 · 园区店", channel: "线下", contact: "赵店长", phone: "134****9088", status: "合作中" },
    { code: "CUS-007", name: "企业采购 · 恒远科技", channel: "B2B", contact: "钱经理", phone: "133****5566", status: "合作中" },
    { code: "CUS-008", name: "企业采购 · 启明制造", channel: "B2B", contact: "孙经理", phone: "132****1199", status: "暂停" }
  ];

  const STOCKTAKES = [
    { id: "CC-20261020-A02", scope: "A 区 · 02 巷道", type: "循环盘点", owner: "周可", progress: 68, status: "进行中", start: "10-20 09:20", total: 120, counted: 82, diff: 2 },
    { id: "CC-20261020-B01", scope: "B 区 · 01 巷道", type: "循环盘点", owner: "陈屿", progress: 42, status: "进行中", start: "10-20 10:10", total: 120, counted: 50, diff: 0 },
    { id: "CC-20261020-D01", scope: "D 区 · 大件区", type: "抽盘", owner: "林舟", progress: 0, status: "待开始", start: "10-20 14:00", total: 60, counted: 0, diff: 0 },
    { id: "CC-20261019-A01", scope: "A 区 · 01 巷道", type: "循环盘点", owner: "周可", progress: 100, status: "已完成", start: "10-19 09:00", total: 120, counted: 120, diff: 0 },
    { id: "CC-20261019-C03", scope: "C 区 · 03 巷道", type: "循环盘点", owner: "陈屿", progress: 100, status: "已完成", start: "10-19 13:30", total: 120, counted: 120, diff: 4 },
    { id: "CC-20261018-ALL", scope: "全仓 · A–E 区", type: "全盘", owner: "陈屿", progress: 100, status: "已完成", start: "10-18 08:30", total: 1280, counted: 1280, diff: 12 },
    { id: "CC-20261017-E01", scope: "E 区 · 退货暂存", type: "抽盘", owner: "林舟", progress: 100, status: "已完成", start: "10-17 15:00", total: 40, counted: 40, diff: 1 },
    { id: "CC-20261021-B02", scope: "B 区 · 02 巷道", type: "循环盘点", owner: "周可", progress: 0, status: "待开始", start: "10-21 09:00", total: 120, counted: 0, diff: 0 }
  ];

  const STOCKTAKE_DIFFS = [
    { sku: "WH-1005 智能运动手表", loc: "A-03-02", book: 220, actual: 218, diff: -2, reason: "拣货未及时扣减", action: "调整库存" },
    { sku: "WH-3002 保温杯 500ml", loc: "C-03-01", book: 1560, actual: 1558, diff: -2, reason: "包装破损报损", action: "调整库存" },
    { sku: "WH-1007 USB-C 数据线", loc: "A-04-01", book: 2400, actual: 2401, diff: 1, reason: "上架重复扫描", action: "已复核" },
    { sku: "WH-2002 智能音箱 Mini", loc: "B-01-06", book: 310, actual: 308, diff: -2, reason: "待检区未隔离", action: "调整库存" },
    { sku: "WH-1003 充电宝", loc: "A-02-01", book: 86, actual: 89, diff: 3, reason: "退货未入账", action: "已复核" }
  ];

  const RETURNS = [
    { id: "RT-20261020-006", customer: "天猫旗舰店", product: "降噪蓝牙耳机", sku: "WH-1001", qty: 6, reason: "七天无理由", status: "待质检", time: "10-20 10:12" },
    { id: "RT-20261020-005", customer: "京东自营", product: "机械键盘 K87", sku: "WH-1002", qty: 3, reason: "质量问题", status: "待质检", time: "10-20 09:48" },
    { id: "RT-20261020-004", customer: "抖音商城", product: "智能音箱 Mini", sku: "WH-2002", qty: 2, reason: "包装破损", status: "待上架", time: "10-20 09:20" },
    { id: "RT-20261020-003", customer: "线下门店 · 苏州中心", product: "护眼台灯 Pro", sku: "WH-2001", qty: 1, reason: "发错货", status: "待上架", time: "10-20 08:55" },
    { id: "RT-20261020-002", customer: "拼多多官方店", product: "USB-C 编织数据线", sku: "WH-1007", qty: 12, reason: "七天无理由", status: "已入库", time: "10-19 16:40" },
    { id: "RT-20261020-001", customer: "天猫旗舰店", product: "城市通勤双肩包", sku: "WH-3001", qty: 4, reason: "质量问题", status: "已入库", time: "10-19 15:30" },
    { id: "RT-20261019-008", customer: "京东自营", product: "20000mAh 充电宝", sku: "WH-1003", qty: 2, reason: "包装破损", status: "已报废", time: "10-19 14:10" },
    { id: "RT-20261019-007", customer: "抖音商城", product: "无线静音鼠标", sku: "WH-1004", qty: 5, reason: "七天无理由", status: "已入库", time: "10-19 11:25" }
  ];

  const RETURN_REASONS = [
    { label: "七天无理由", value: 42 },
    { label: "质量问题", value: 26 },
    { label: "发错货", value: 14 },
    { label: "包装破损", value: 11 },
    { label: "其他", value: 7 }
  ];

  const AUDIT_LOGS = [
    { time: "10:42:18", user: "林舟", role: "收货员", module: "入库", action: "上架确认", target: "ASN-20261020-004 / A-01-10", ip: "10.12.3.41", result: "成功" },
    { time: "10:36:54", user: "周可", role: "质检员", module: "出库", action: "复核差异处理", target: "SO-20261020-018", ip: "10.12.3.57", result: "警告" },
    { time: "10:31:07", user: "陈屿", role: "仓储主管", module: "库位", action: "锁定库位", target: "A-03-02", ip: "10.12.2.10", result: "成功" },
    { time: "10:24:42", user: "陈屿", role: "仓储主管", module: "系统", action: "导出库存报表", target: "库存明细 · 42,680 件", ip: "10.12.2.10", result: "成功" },
    { time: "10:18:33", user: "林舟", role: "收货员", module: "入库", action: "创建入库单", target: "ASN-20261020-011", ip: "10.12.3.41", result: "成功" },
    { time: "10:12:09", user: "周可", role: "质检员", module: "库存", action: "库存调整", target: "WH-1005 / -2 件", ip: "10.12.3.57", result: "成功" },
    { time: "10:04:51", user: "许知远", role: "设备运维", module: "设备", action: "设备配置变更", target: "AGV-07 回充阈值", ip: "10.12.4.22", result: "成功" },
    { time: "09:58:12", user: "未知", role: "—", module: "登录", action: "登录失败", target: "账号 admin", ip: "10.12.9.88", result: "失败" },
    { time: "09:51:26", user: "陈屿", role: "仓储主管", module: "系统", action: "修改角色权限", target: "质检员 · 库存调整", ip: "10.12.2.10", result: "成功" },
    { time: "09:44:03", user: "林舟", role: "收货员", module: "入库", action: "质检退回", target: "ASN-20261020-008 / WH-1005", ip: "10.12.3.41", result: "警告" },
    { time: "09:30:45", user: "陈屿", role: "仓储主管", module: "出库", action: "生成波次", target: "W-07 · 5 个订单", ip: "10.12.2.10", result: "成功" },
    { time: "09:12:18", user: "周可", role: "质检员", module: "库存", action: "盘点任务创建", target: "CC-20261020-A02", ip: "10.12.3.57", result: "成功" }
  ];

  const SENSITIVE_ACTIONS = [
    { icon: "shield-check", title: "修改角色权限", meta: "质检员 · 库存调整 · 陈屿", time: "09:51" },
    { icon: "download", title: "导出库存报表", meta: "42,680 件 · 陈屿", time: "10:24" },
    { icon: "lock", title: "锁定库位 A-03-02", meta: "温区异常 · 陈屿", time: "10:31" },
    { icon: "circle-x", title: "连续登录失败", meta: "账号 admin · IP 10.12.9.88", time: "09:58" }
  ];

  const ALGO = {
    weights: [
      { label: "周转率", value: 0.34 },
      { label: "订单相关性", value: 0.27 },
      { label: "拣货距离", value: 0.22 },
      { label: "体积适配", value: 0.17 }
    ],
    cr: 0.042,
    candidates: [
      { code: "A-01-10", score: 0.892, turnover: 92, corr: 86, dist: 78, fit: 96 },
      { code: "A-02-08", score: 0.861, turnover: 90, corr: 81, dist: 79, fit: 91 },
      { code: "A-02-09", score: 0.834, turnover: 84, corr: 77, dist: 74, fit: 88 },
      { code: "B-01-05", score: 0.798, turnover: 80, corr: 72, dist: 71, fit: 84 },
      { code: "B-02-03", score: 0.742, turnover: 72, corr: 66, dist: 68, fit: 82 }
    ]
  };

  const NOTIFICATIONS = [
    { level: "danger", icon: "triangle-alert", title: "A-03-02 温度超过阈值", meta: "TH-A01 · 2 分钟前" },
    { level: "warn", icon: "package-search", title: "2 个 SKU 低于安全库存", meta: "库存预警 · 18 分钟前" },
    { level: "info", icon: "bot", title: "AGV-07 已回充", meta: "设备监控 · 8 分钟前" }
  ];

  window.WMS_DATA = { NAV, PAGE_META, DOCKS, KPIS, ACTIVITIES, ALERTS, INBOUND, OUTBOUND, INVENTORY, ZONES, LOCATIONS, DEVICES, AGVS, CHART, ALGO, NOTIFICATIONS, PRODUCTS, SUPPLIERS, CUSTOMERS, STOCKTAKES, STOCKTAKE_DIFFS, RETURNS, RETURN_REASONS, AUDIT_LOGS, SENSITIVE_ACTIONS };
})();
