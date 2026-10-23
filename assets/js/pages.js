/* 仓序 WMS — page-specific rendering and interactions. */
(function () {
  const D = window.WMS_DATA;
  const WMS = window.WMS;
  if (!D || !WMS) return;
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
  const esc = WMS.escape;
  const page = document.body.dataset.page;

  const tagFor = (status) => {
    const map = {
      "待收货": "tag-info", "质检中": "tag-warning", "待上架": "tag-accent", "已完成": "tag-success",
      "待分配": "tag-neutral", "拣货中": "tag-accent", "待复核": "tag-warning", "已发货": "tag-success",
      "正常": "tag-success", "低库存": "tag-danger", "待检": "tag-violet",
      "在线": "tag-success", "作业中": "tag-accent", "充电中": "tag-warning", "离线": "tag-danger",
      "已完成·已上架": "tag-success"
    };
    return map[status] || "tag-neutral";
  };
  const dotTag = (status, cls) => `<span class="tag ${cls || tagFor(status)}"><span class="dot"></span>${esc(status)}</span>`;

  /* =========================================================
     Dashboard
     ========================================================= */
  if (page === "dashboard") {
    const tb = $("#js-activities");
    if (tb) {
      tb.innerHTML = D.ACTIVITIES.map((r) => `
        <tr>
          <td class="num">${r.time}</td>
          <td>${dotTag(r.type, r.type === "入库" ? "tag-info" : r.type === "出库" ? "tag-accent" : "tag-neutral")}</td>
          <td><span class="code">${esc(r.doc)}</span></td>
          <td><span class="truncate" style="display:block;max-width:200px">${esc(r.sku)}</span></td>
          <td>${esc(r.operator)}</td>
          <td class="num">${WMS.fmt(r.qty)}</td>
          <td>${dotTag(r.status)}</td>
        </tr>`).join("");
    }
    const alerts = $("#js-alerts");
    if (alerts) {
      alerts.innerHTML = D.ALERTS.map((a) => `
        <div class="list-item">
          <span class="li-icon ${a.level === "danger" ? "danger" : a.level === "warn" ? "warn" : "info"}">${WMS.icon(a.icon, "icon-sm")}</span>
          <span class="li-body"><strong>${esc(a.title)}</strong><small>${esc(a.meta)}</small></span>
          <button class="btn btn-sm btn-ghost" type="button" data-toast="已打开：${esc(a.action)} · ${esc(a.title)}">${esc(a.action)}</button>
        </div>`).join("");
    }
    const mini = $("#js-algo-mini");
    if (mini) {
      mini.innerHTML = D.ALGO.candidates.slice(0, 3).map((c, i) => `
        <div class="rank-row ${i === 0 ? "top" : ""}">
          <span class="rk">${i + 1}</span>
          <span class="rloc">${esc(c.code)}</span>
          <span class="rbar"><i style="width:${Math.round(c.score * 100)}%"></i></span>
          <span class="rscore">${c.score.toFixed(3)}</span>
        </div>`).join("");
    }
    const dockLive = $("#js-docks");
    if (dockLive) {
      dockLive.innerHTML = D.DOCKS.map((d) => `
        <div class="dock ${d.state}">
          <span class="dock-no">${d.no}</span>
          <span class="dock-body">
            <span class="dock-name"><span class="dot"></span>${esc(d.name)}</span>
            <span class="dock-meta">${esc(d.meta)}</span>
          </span>
          <span class="tag ${d.state === "busy" ? "tag-accent" : d.state === "idle" ? "tag-success" : "tag-warning"}"><span class="dot"></span>${esc(d.label)}</span>
        </div>`).join("");
    }
  }

  /* =========================================================
     Inbound
     ========================================================= */
  if (page === "inbound") {
    let filter = "全部";
    const bodyEl = $("#js-inbound-body");
    const search = $("#js-inbound-search");
    const count = $("#js-inbound-count");
    const tabs = $$("[data-inbound-tab]");
    let selected = null;

    const counts = () => {
      const out = { "全部": D.INBOUND.length };
      ["待收货", "质检中", "待上架", "已完成"].forEach((s) => (out[s] = D.INBOUND.filter((x) => x.status === s).length));
      return out;
    };
    const updateTabs = () => {
      const c = counts();
      tabs.forEach((t) => {
        const n = t.querySelector(".count");
        if (n) n.textContent = c[t.dataset.inboundTab] ?? 0;
      });
    };
    const render = () => {
      const q = (search ? search.value : "").trim().toLowerCase();
      const rows = D.INBOUND.filter((r) => {
        const okState = filter === "全部" || r.status === filter;
        const okQ = !q || [r.id, r.supplier, r.product, r.sku].join(" ").toLowerCase().includes(q);
        return okState && okQ;
      });
      bodyEl.innerHTML = rows.length ? rows.map((r) => `
        <tr data-state="${esc(r.status)}">
          <td><span class="code">${esc(r.id)}</span></td>
          <td><div class="cell-main">${esc(r.supplier)}</div><div class="cell-sub">${esc(r.dock)}</div></td>
          <td><div class="cell-main">${esc(r.product)}</div><div class="cell-sub code">${esc(r.sku)}</div></td>
          <td class="num">${WMS.fmt(r.qty)}</td>
          <td class="tight">${esc(r.eta)}</td>
          <td>${dotTag(r.status)}</td>
          <td class="actions">
            ${r.status === "待上架"
              ? `<button class="btn btn-sm btn-primary" type="button" data-slot="${esc(r.id)}">推荐库位</button>`
              : `<button class="btn btn-sm btn-ghost" type="button" data-toast="已打开入库单 ${esc(r.id)}">查看</button>`}
          </td>
        </tr>`).join("") : `<tr class="empty-row"><td colspan="7">
            <div class="empty-state">
              <img src="assets/img/undraw/not-found.svg" alt="">
              <p>没有匹配的入库单</p>
              <button class="btn btn-sm" type="button" data-clear-inbound>清除筛选</button>
            </div></td></tr>`;
      $$("#js-inbound-count, #js-inbound-count-2").forEach((el) => (el.textContent = rows.length));
      updateTabs();
    };
    tabs.forEach((t) => t.addEventListener("click", () => {
      filter = t.dataset.inboundTab;
      tabs.forEach((x) => x.classList.toggle("is-active", x === t));
      render();
    }));
    if (search) search.addEventListener("input", render);
    document.addEventListener("click", (e) => {
      if (e.target.closest("[data-clear-inbound]")) {
        filter = "全部"; if (search) search.value = "";
        tabs.forEach((x) => x.classList.toggle("is-active", x.dataset.inboundTab === "全部"));
        render();
      }
      const slotBtn = e.target.closest("[data-slot]");
      if (slotBtn) {
        selected = D.INBOUND.find((x) => x.id === slotBtn.dataset.slot);
        fillSlotDrawer(selected);
        WMS.openDrawer("drawer-slot");
      }
    });
    const fillSlotDrawer = (row) => {
      if (!row) return;
      $("#slot-drawer-sub").textContent = `${row.id} · ${row.supplier}`;
      $("#slot-sku").textContent = `${row.sku} ${row.product}`;
      $("#slot-qty").textContent = `${WMS.fmt(row.qty)} 件`;
      $("#slot-dock").textContent = row.dock;
      $("#js-slot-rank").innerHTML = D.ALGO.candidates.map((c, i) => `
        <div class="rank-row ${i === 0 ? "top" : ""}">
          <span class="rk">${i + 1}</span>
          <span class="rloc">${esc(c.code)}</span>
          <span class="rbar"><i style="width:${Math.round((c.score - 0.6) / 0.4 * 100)}%"></i></span>
          <span class="rscore">${c.score.toFixed(3)}</span>
        </div>`).join("");
      $("#js-slot-metrics").innerHTML = D.ALGO.candidates.slice(0, 3).map((c) => `
        <div class="stat-line">
          <span class="sl mono">${esc(c.code)}</span>
          <span class="sv">周转 ${c.turnover} · 相关 ${c.corr} · 距离 ${c.dist} · 适配 ${c.fit}</span>
        </div>`).join("");
    };
    const confirm = $("#js-confirm-slot");
    if (confirm) confirm.addEventListener("click", () => {
      if (selected) {
        selected.status = "已完成";
        render();
        WMS.toast(`${selected.id} 已按推荐库位 ${D.ALGO.candidates[0].code} 上架`);
      }
      WMS.closeDrawer("drawer-slot");
    });
    const form = $("#form-inbound");
    if (form) form.addEventListener("submit", (e) => {
      e.preventDefault();
      const data = new FormData(form);
      D.INBOUND.unshift({
        id: `ASN-20261020-${String(D.INBOUND.length + 1).padStart(3, "0")}`,
        supplier: data.get("supplier") || "深圳华创电子",
        product: data.get("product") || "新品",
        sku: data.get("sku") || "WH-NEW",
        qty: Number(data.get("qty")) || 0,
        eta: data.get("eta") || "10-20 16:00",
        dock: data.get("dock") || "1 号月台",
        status: "待收货",
        operator: "陈屿"
      });
      filter = "全部";
      tabs.forEach((x) => x.classList.toggle("is-active", x.dataset.inboundTab === "全部"));
      render();
      form.reset();
      WMS.closeModal("modal-inbound");
      WMS.toast("入库单已创建，等待月台收货");
    });
    render();
  }

  /* =========================================================
     Outbound
     ========================================================= */
  if (page === "outbound") {
    let filter = "全部";
    const bodyEl = $("#js-outbound-body");
    const search = $("#js-outbound-search");
    const count = $("#js-outbound-count");
    const tabs = $$("[data-outbound-tab]");
    const render = () => {
      const q = (search ? search.value : "").trim().toLowerCase();
      const rows = D.OUTBOUND.filter((r) => {
        const okState = filter === "全部" || r.status === filter;
        const okQ = !q || [r.id, r.customer, r.wave].join(" ").toLowerCase().includes(q);
        return okState && okQ;
      });
      bodyEl.innerHTML = rows.length ? rows.map((r) => `
        <tr>
          <td><span class="code">${esc(r.id)}</span></td>
          <td><div class="cell-main">${esc(r.customer)}</div><div class="cell-sub">${r.items} 个商品</div></td>
          <td class="num">${WMS.fmt(r.qty)}</td>
          <td>${r.priority === "加急" ? `<span class="tag tag-accent"><span class="dot"></span>加急</span>` : `<span class="tag tag-neutral">普通</span>`}</td>
          <td class="code">${esc(r.wave)}</td>
          <td>${dotTag(r.status)}</td>
          <td class="tight">${esc(r.eta)}</td>
          <td class="actions"><button class="btn btn-sm btn-ghost" type="button" data-toast="已打开出库单 ${esc(r.id)}">查看</button></td>
        </tr>`).join("") : `<tr class="empty-row"><td colspan="8"><div class="empty-state"><img src="assets/img/undraw/not-found.svg" alt=""><p>没有匹配的出库单</p></div></td></tr>`;
      $$("#js-outbound-count, #js-outbound-count-2").forEach((el) => (el.textContent = rows.length));
      const c = { "全部": D.OUTBOUND.length };
      ["待分配", "拣货中", "待复核", "已发货"].forEach((s) => (c[s] = D.OUTBOUND.filter((x) => x.status === s).length));
      tabs.forEach((t) => {
        const n = t.querySelector(".count");
        if (n) n.textContent = c[t.dataset.outboundTab] ?? 0;
      });
    };
    tabs.forEach((t) => t.addEventListener("click", () => {
      filter = t.dataset.outboundTab;
      tabs.forEach((x) => x.classList.toggle("is-active", x === t));
      render();
    }));
    if (search) search.addEventListener("input", render);
    const waveBtn = $("#js-wave");
    if (waveBtn) waveBtn.addEventListener("click", () => {
      const pending = D.OUTBOUND.filter((x) => x.status === "待分配");
      if (!pending.length) { WMS.toast("没有待分配订单", "warn"); return; }
      pending.forEach((x) => { x.wave = "W-08"; x.status = "拣货中"; });
      render();
      WMS.toast(`波次 W-08 已生成，${pending.length} 个订单进入拣货`);
    });
    const stops = [
      { code: "A-01-03", name: "降噪蓝牙耳机", qty: 36 },
      { code: "A-02-04", name: "无线静音鼠标", qty: 24 },
      { code: "B-01-02", name: "护眼台灯 Pro", qty: 12 },
      { code: "B-01-06", name: "智能音箱 Mini", qty: 18 },
      { code: "C-02-03", name: "城市通勤双肩包", qty: 20 }
    ];
    const stopList = $("#js-route-stops");
    if (stopList) {
      stopList.innerHTML = stops.map((s, i) => `
        <div class="stop-item" data-stop="${i}">
          <span class="sn">${i + 1}</span>
          <span class="sl"><strong>${s.code}</strong><small>${s.name}</small></span>
          <span class="sq">× ${s.qty}</span>
        </div>`).join("");
    }
    const pickBtn = $("#js-pick");
    let picking = false;
    if (pickBtn) pickBtn.addEventListener("click", () => {
      if (picking) return;
      picking = true;
      pickBtn.disabled = true;
      pickBtn.innerHTML = `${WMS.icon("route")}拣货进行中`;
      const items = $$(".stop-item");
      items.forEach((el, i) => {
        setTimeout(() => {
          el.classList.add("done");
          el.style.opacity = "0.55";
          el.querySelector(".sn").innerHTML = WMS.icon("check", "icon-sm");
          const pct = Math.round(((i + 1) / items.length) * 100);
          const bar = $("#js-pick-bar");
          if (bar) bar.style.width = pct + "%";
          const lbl = $("#js-pick-pct");
          if (lbl) lbl.textContent = pct + "%";
          if (i === items.length - 1) {
            WMS.toast("波次 W-07 拣货完成，已交接复核台");
            pickBtn.innerHTML = `${WMS.icon("circle-check")}拣货完成`;
            setTimeout(() => { pickBtn.disabled = false; pickBtn.innerHTML = `${WMS.icon("play")}重新演示`; picking = false; }, 1600);
          }
        }, 650 * (i + 1));
      });
    });
    render();
  }

  /* =========================================================
     Inventory
     ========================================================= */
  if (page === "inventory") {
    let pageNo = 1;
    const per = 8;
    const bodyEl = $("#js-inventory-body");
    const search = $("#js-inventory-search");
    const cat = $("#js-inventory-cat");
    const status = $("#js-inventory-status");
    const count = $("#js-inventory-count");
    const pg = $("#js-pagination");
    let current = null;

    const filtered = () => {
      const q = (search ? search.value : "").trim().toLowerCase();
      const c = cat ? cat.value : "";
      const s = status ? status.value : "";
      return D.INVENTORY.filter((x) => {
        const okQ = !q || [x.sku, x.name, x.loc].join(" ").toLowerCase().includes(q);
        const okC = !c || x.cat === c;
        const okS = !s || x.status === s;
        return okQ && okC && okS;
      });
    };
    const render = () => {
      const rows = filtered();
      const pages = Math.max(1, Math.ceil(rows.length / per));
      if (pageNo > pages) pageNo = pages;
      const slice = rows.slice((pageNo - 1) * per, pageNo * per);
      bodyEl.innerHTML = slice.length ? slice.map((r) => `
        <tr class="row-link" data-sku="${esc(r.sku)}">
          <td><div class="cell-media"><img class="thumb" src="${esc(r.img)}" alt="" loading="lazy"><div class="min-w-0"><div class="cell-main">${esc(r.name)}</div><div class="cell-sub code">${esc(r.sku)}</div></div></div></td>
          <td>${esc(r.cat)}</td>
          <td class="code">${esc(r.loc)}</td>
          <td class="num">${WMS.fmt(r.stock)}</td>
          <td class="num">${WMS.fmt(r.available)}</td>
          <td class="num">${WMS.fmt(r.locked)}</td>
          <td class="num">${WMS.fmt(r.safety)}</td>
          <td>${dotTag(r.status)}</td>
          <td class="tight muted" style="font-size:10.5px">${esc(r.updated)}</td>
        </tr>`).join("") : `<tr class="empty-row"><td colspan="9"><div class="empty-state">
            <img src="assets/img/undraw/not-found.svg" alt="">
            <p>没有匹配的商品</p>
            <small>试试更换关键词或清除筛选条件</small>
            <button class="btn btn-sm" type="button" data-clear-inventory>清除筛选</button>
          </div></td></tr>`;
      $$("#js-inventory-count, #js-inventory-count-2").forEach((el) => (el.textContent = rows.length));
      if (pg) {
        pg.innerHTML = `<span class="pg">显示 ${rows.length ? (pageNo - 1) * per + 1 : 0}–${Math.min(pageNo * per, rows.length)} / 共 ${rows.length} 条</span>
          <button class="pg-btn" type="button" data-pg="prev" ${pageNo === 1 ? "disabled" : ""}>上一页</button>
          ${Array.from({ length: pages }, (_, i) => `<button class="pg-btn ${i + 1 === pageNo ? "is-active" : ""}" type="button" data-pg="${i + 1}">${i + 1}</button>`).join("")}
          <button class="pg-btn" type="button" data-pg="next" ${pageNo === pages ? "disabled" : ""}>下一页</button>`;
      }
    };
    [search, cat, status].forEach((el) => el && el.addEventListener(el.tagName === "INPUT" ? "input" : "change", () => { pageNo = 1; render(); }));
    if (pg) pg.addEventListener("click", (e) => {
      const b = e.target.closest("[data-pg]");
      if (!b || b.disabled) return;
      const val = b.dataset.pg;
      const pages = Math.max(1, Math.ceil(filtered().length / per));
      if (val === "prev") pageNo = Math.max(1, pageNo - 1);
      else if (val === "next") pageNo = Math.min(pages, pageNo + 1);
      else pageNo = Number(val);
      render();
    });
    document.addEventListener("click", (e) => {
      if (e.target.closest("[data-clear-inventory]")) {
        if (search) search.value = "";
        if (cat) cat.value = "";
        if (status) status.value = "";
        pageNo = 1; render();
      }
      const row = e.target.closest("[data-sku]");
      if (row) {
        current = D.INVENTORY.find((x) => x.sku === row.dataset.sku);
        fillDrawer(current);
        WMS.openDrawer("drawer-inventory");
      }
    });
    const fillDrawer = (r) => {
      if (!r) return;
      $("#inv-sub").textContent = `${r.sku} · ${r.cat}`;
      $("#inv-name").textContent = r.name;
      $("#inv-img").src = r.img;
      $("#inv-img").alt = r.name;
      $("#inv-status").innerHTML = dotTag(r.status);
      $("#inv-stock").textContent = WMS.fmt(r.stock);
      $("#inv-avail").textContent = WMS.fmt(r.available);
      $("#inv-lock").textContent = WMS.fmt(r.locked);
      $("#inv-safety").textContent = WMS.fmt(r.safety);
      $("#inv-loc").textContent = r.loc;
      $("#inv-cat").textContent = r.cat;
      $("#inv-updated").textContent = r.updated;
      $("#inv-barcode").textContent = "||| |||| || ||||| |||| |||";
      $("#inv-locs").innerHTML = [
        { loc: r.loc, qty: Math.round(r.stock * 0.72), type: "拣货位" },
        { loc: r.loc.replace(/-(\d+)$/, "-0" + ((Number(r.loc.slice(-1)) % 8) + 1)), qty: Math.round(r.stock * 0.2), type: "存储位" },
        { loc: "Q-00-01", qty: Math.round(r.stock * 0.08), type: "质检暂存" }
      ].map((x) => `<div class="stat-line"><span class="sl code">${esc(x.loc)} · ${x.type}</span><span class="sv">${WMS.fmt(x.qty)} 件</span></div>`).join("");
    };
    render();
  }

  /* =========================================================
     Locations
     ========================================================= */
  if (page === "locations") {
    let selected = "A-01-03";
    const map = $("#js-floor-map");
    const detail = $("#js-loc-detail");
    const statusLabel = { occ: "已占用", free: "空闲", lock: "锁定", check: "待盘点" };
    const statusTag = { occ: "tag-neutral", free: "tag-success", lock: "tag-warning", check: "tag-violet" };
    const draw = () => {
      map.innerHTML = D.ZONES.map((z) => {
        const locs = D.LOCATIONS.filter((l) => l.zone === z.code);
        const used = locs.filter((l) => l.status === "occ").length;
        let rows = "";
        for (let r = 1; r <= z.rows; r++) {
          const cells = locs.filter((l) => l.row === r);
          rows += `<div class="racks">${cells.map((l) => `
            <button class="loc ${l.status} ${l.code === selected ? "is-selected" : ""}" type="button"
              data-loc="${l.code}" title="${l.code} · ${statusLabel[l.status]}" aria-label="${l.code} ${statusLabel[l.status]} ${l.sku || ""}"></button>`).join("")}</div>`;
          if (r < z.rows) rows += `<div class="aisle-gap"></div>`;
        }
        return `<div class="zone-block">
          <div class="zone-label"><strong>${z.code}</strong><span>${z.name} · ${z.temp}</span><span class="zone-stats">${used}/${locs.length} 已占用</span></div>
          ${rows}</div>`;
      }).join("");
    };
    const fill = (code) => {
      const l = D.LOCATIONS.find((x) => x.code === code) || D.LOCATIONS[0];
      selected = l.code;
      detail.innerHTML = `
        <div class="card-pad">
          <div class="flex between"><span class="tag ${statusTag[l.status]}"><span class="dot"></span>${statusLabel[l.status]}</span><span class="page-meta">${l.zoneName}</span></div>
          <div class="detail-code mt12">${esc(l.code)}</div>
          <p class="card-sub" style="margin-top:2px">${l.temp} · ${l.zone === "D" ? "托盘位" : "料箱位"}</p>
          <div class="detail-grid">
            <div><dt>容量</dt><dd>${l.capacity} 件</dd></div>
            <div><dt>已使用</dt><dd>${l.used} 件</dd></div>
            <div><dt>当前商品</dt><dd class="code" style="font-size:11px">${esc(l.sku || "— 空闲 —")}</dd></div>
            <div><dt>最近盘点</dt><dd>${esc(l.lastCheck)}</dd></div>
            <div><dt>温区</dt><dd>${l.temp}</dd></div>
            <div><dt>巷道</dt><dd class="mono">${l.zone}-${String(l.row).padStart(2, "0")}</dd></div>
          </div>
          <div class="stack mt16">
            <button class="btn btn-primary" type="button" data-toast="已生成 ${esc(l.code)} 的盘点任务">生成盘点任务</button>
            <button class="btn" type="button" data-toggle-loc="${esc(l.code)}">${l.status === "lock" ? "解除锁定" : "锁定库位"}</button>
          </div>
          <div class="hint-box mt12">${WMS.icon("info")}<span>库位推荐由 AHP-TOPSIS 计算，综合周转率、订单相关性、拣货距离与体积适配四项指标。</span></div>
        </div>`;
    };
    draw();
    fill(selected);
    document.addEventListener("click", (e) => {
      const b = e.target.closest("[data-loc]");
      if (b) {
        fill(b.dataset.loc);
        draw();
        return;
      }
      const t = e.target.closest("[data-toggle-loc]");
      if (t) {
        const l = D.LOCATIONS.find((x) => x.code === t.dataset.toggleLoc);
        if (l) {
          l.status = l.status === "lock" ? (l.sku ? "occ" : "free") : "lock";
          draw(); fill(l.code);
          WMS.toast(l.status === "lock" ? `${l.code} 已锁定` : `${l.code} 已解除锁定`);
        }
      }
    });
  }

  /* =========================================================
     Devices
     ========================================================= */
  if (page === "devices") {
    const tb = $("#js-device-body");
    if (tb) {
      tb.innerHTML = D.DEVICES.map((d) => `
        <tr>
          <td><div class="cell-media"><span class="li-icon" style="width:26px;height:26px;border-radius:5px">${WMS.icon(d.icon, "icon-sm")}</span><div><div class="cell-main">${esc(d.id)}</div><div class="cell-sub">${esc(d.type)}</div></div></div></td>
          <td>${esc(d.loc)}</td>
          <td>${dotTag(d.status, d.status === "在线" ? "tag-success" : d.status === "作业中" ? "tag-accent" : d.status === "充电中" ? "tag-warning" : "tag-danger")}</td>
          <td class="num">${esc(d.battery)}</td>
          <td class="num" style="font-size:10px">${esc(d.signal)}</td>
          <td class="tight muted" style="font-size:10.5px">${esc(d.beat)}</td>
          <td class="actions"><button class="btn btn-sm btn-ghost" type="button" data-toast="已打开设备 ${esc(d.id)}">详情</button></td>
        </tr>`).join("");
    }
    const agv = $("#js-agv-list");
    if (agv) {
      agv.innerHTML = D.AGVS.map((a) => `
        <div class="list-item">
          <span class="li-icon ${a.state === "搬运中" ? "info" : a.state === "离线" ? "danger" : "ok"}">${WMS.icon("bot", "icon-sm")}</span>
          <span class="li-body"><strong>${esc(a.id)} · ${esc(a.task)}</strong>
            <small>电量 ${a.battery}% · ${esc(a.state)}</small>
            <span class="spark" style="margin-top:6px"><i style="width:${a.battery}%;background:${a.battery < 25 ? "var(--danger)" : a.battery < 50 ? "var(--gold)" : "var(--green)"}"></i></span>
          </span>
          <span class="li-end">${a.battery}%</span>
        </div>`).join("");
    }
    let temp = 22.6, hum = 48;
    let live = null;
    const startLive = () => {
      if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
      live = setInterval(() => {
        temp = Math.round((temp + (Math.random() - 0.5) * 0.3) * 10) / 10;
        hum = Math.round(hum + (Math.random() - 0.5) * 1.4);
        temp = Math.min(25.6, Math.max(21.2, temp));
        hum = Math.min(55, Math.max(42, hum));
        const t = $("#js-temp-val"), h = $("#js-hum-val");
        if (t) t.textContent = temp.toFixed(1);
        if (h) h.textContent = hum;
      }, 6000);
    };
    startLive();
    const refresh = $("#js-refresh-devices");
    if (refresh) refresh.addEventListener("click", () => {
      refresh.innerHTML = `${WMS.icon("refresh-cw")}刷新中`;
      setTimeout(() => {
        refresh.innerHTML = `${WMS.icon("refresh-cw")}刷新`;
        WMS.toast("设备状态已刷新 · 24 台在线");
      }, 600);
    });
  }

  /* =========================================================
     Analytics
     ========================================================= */
  if (page === "analytics") {
    $$("[data-range]").forEach((b) => b.addEventListener("click", () => {
      $$("[data-range]").forEach((x) => {
        x.classList.toggle("btn-primary", x === b);
        x.classList.toggle("btn-sm", true);
      });
      WMS.toast(`已切换到${b.dataset.range}（演示数据）`);
    }));
    const exp = $("#js-export-report");
    if (exp) exp.addEventListener("click", () => WMS.toast("报表已生成，正在下载（演示）"));
  }

  /* =========================================================
     Stocktake
     ========================================================= */
  if (page === "stocktake") {
    let filter = "全部";
    let selectedTask = D.STOCKTAKES.find((t) => t.status === "进行中") || D.STOCKTAKES[0];
    const bodyEl = $("#js-stocktake-body");
    const search = $("#js-stocktake-search");
    const tabs = $$("[data-stocktake-tab]");
    const statusCls = (s) => s === "进行中" ? "tag-accent" : s === "待开始" ? "tag-info" : "tag-success";
    const renderProgress = () => {
      const t = selectedTask;
      if (!t) return;
      $("#js-progress-sub").textContent = `${t.id} · ${t.scope}`;
      $("#js-progress-val").textContent = t.progress;
      $("#js-progress-count").textContent = `${t.counted} / ${t.total} 库位`;
      $("#js-progress-bar").style.width = t.progress + "%";
      $("#js-counted").textContent = t.counted;
      $("#js-progress-diff").textContent = t.diff;
    };
    const render = () => {
      const q = (search ? search.value : "").trim().toLowerCase();
      const rows = D.STOCKTAKES.filter((t) => {
        const okState = filter === "全部" || t.status === filter;
        const okQ = !q || [t.id, t.scope, t.type, t.owner].join(" ").toLowerCase().includes(q);
        return okState && okQ;
      });
      bodyEl.innerHTML = rows.length ? rows.map((t) => `
        <tr>
          <td><span class="code">${esc(t.id)}</span></td>
          <td><div class="cell-main">${esc(t.scope)}</div><div class="cell-sub">${esc(t.type)}</div></td>
          <td>${esc(t.type)}</td>
          <td>${esc(t.owner)}</td>
          <td><div class="flex" style="gap:6px"><span class="mini-bar" style="max-width:64px"><i style="width:${t.progress}%"></i></span><span class="mono" style="font-size:10px">${t.progress}%</span></div></td>
          <td><span class="tag ${statusCls(t.status)}"><span class="dot"></span>${esc(t.status)}</span></td>
          <td class="tight" style="font-size:10.5px;color:var(--ink-3)">${esc(t.start)}</td>
          <td class="actions">
            ${t.status === "进行中"
              ? `<button class="btn btn-sm btn-primary" type="button" data-stocktake-pick="${esc(t.id)}">录入盘点</button>`
              : t.status === "待开始"
                ? `<button class="btn btn-sm" type="button" data-stocktake-start="${esc(t.id)}">开始</button>`
                : `<button class="btn btn-sm btn-ghost" type="button" data-toast="已打开 ${esc(t.id)} 盘点报告">查看</button>`}
          </td>
        </tr>`).join("") : `<tr class="empty-row"><td colspan="8"><div class="empty-state"><img src="assets/img/undraw/not-found.svg" alt=""><p>没有匹配的盘点任务</p></div></td></tr>`;
      $$("#js-stocktake-count, #js-stocktake-count-2").forEach((el) => (el.textContent = rows.length));
      const c = { "全部": D.STOCKTAKES.length };
      ["进行中", "待开始", "已完成"].forEach((s) => (c[s] = D.STOCKTAKES.filter((x) => x.status === s).length));
      tabs.forEach((tab) => {
        const n = tab.querySelector(".count");
        if (n) n.textContent = c[tab.dataset.stocktakeTab] ?? 0;
      });
      renderProgress();
    };
    const diffs = $("#js-diff-body");
    if (diffs) {
      diffs.innerHTML = D.STOCKTAKE_DIFFS.map((d) => `
        <tr>
          <td><div class="cell-main">${esc(d.sku.split(" ").slice(1).join(" "))}</div><div class="cell-sub code">${esc(d.sku.split(" ")[0])}</div></td>
          <td class="code">${esc(d.loc)}</td>
          <td class="num">${WMS.fmt(d.book)}</td>
          <td class="num">${WMS.fmt(d.actual)}</td>
          <td class="num" style="color:${d.diff < 0 ? "var(--danger)" : "var(--green)"}">${d.diff > 0 ? "+" : ""}${d.diff}</td>
          <td>${esc(d.reason)}</td>
          <td><span class="tag ${d.action === "已复核" ? "tag-success" : "tag-warning"}"><span class="dot"></span>${esc(d.action)}</span></td>
          <td class="actions"><button class="btn btn-sm btn-ghost" type="button" data-toast="已打开 ${esc(d.loc)} 差异处理">处理</button></td>
        </tr>`).join("");
    }
    tabs.forEach((t) => t.addEventListener("click", () => {
      filter = t.dataset.stocktakeTab;
      tabs.forEach((x) => x.classList.toggle("is-active", x === t));
      render();
    }));
    if (search) search.addEventListener("input", render);
    document.addEventListener("click", (e) => {
      const pick = e.target.closest("[data-stocktake-pick]");
      if (pick) {
        selectedTask = D.STOCKTAKES.find((t) => t.id === pick.dataset.stocktakePick);
        renderProgress();
        WMS.toast(`已切换到 ${selectedTask.id}，可开始扫码盘点`);
        const input = $("#js-scan-input");
        if (input) input.focus();
        return;
      }
      const start = e.target.closest("[data-stocktake-start]");
      if (start) {
        const t = D.STOCKTAKES.find((x) => x.id === start.dataset.stocktakeStart);
        if (t) {
          t.status = "进行中";
          selectedTask = t;
          render();
          WMS.toast(`${t.id} 已开始，等待扫码`);
        }
      }
    });
    const scan = $("#js-scan-btn");
    if (scan) scan.addEventListener("click", () => {
      if (!selectedTask || selectedTask.status !== "进行中") { WMS.toast("请先选择一个进行中的盘点任务", "warn"); return; }
      const input = $("#js-scan-input");
      const code = (input && input.value.trim()) || `A-02-${String(selectedTask.counted + 1).padStart(2, "0")}`;
      selectedTask.counted = Math.min(selectedTask.total, selectedTask.counted + 1);
      selectedTask.progress = Math.round(selectedTask.counted / selectedTask.total * 100);
      if (selectedTask.progress === 100) selectedTask.status = "已完成";
      if (input) input.value = "";
      render();
      WMS.toast(`已扫描 ${code} · 计数 +1`);
    });
    const form = $("#form-stocktake");
    if (form) form.addEventListener("submit", (e) => {
      e.preventDefault();
      const data = new FormData(form);
      const scope = data.get("scope") || "A 区 · 电子配件";
      const total = scope.includes("全仓") ? 1280 : scope.startsWith("D") ? 60 : scope.startsWith("E") ? 40 : 120;
      D.STOCKTAKES.unshift({
        id: `CC-20261020-${String(D.STOCKTAKES.length + 1).padStart(3, "0")}`,
        scope, type: data.get("type") || "循环盘点", owner: data.get("owner") || "周可",
        progress: 0, status: "待开始", start: data.get("date") || "10-21 09:00",
        total, counted: 0, diff: 0
      });
      filter = "全部";
      tabs.forEach((x) => x.classList.toggle("is-active", x.dataset.stocktakeTab === "全部"));
      render();
      form.reset();
      WMS.closeModal("modal-stocktake");
      WMS.toast("盘点任务已创建，可在 PDA 领取");
    });
    render();
  }

  /* =========================================================
     Returns
     ========================================================= */
  if (page === "returns") {
    let filter = "全部";
    let selected = null;
    const bodyEl = $("#js-returns-body");
    const search = $("#js-returns-search");
    const tabs = $$("[data-return-tab]");
    const statusCls = (s) => s === "待质检" ? "tag-warning" : s === "待上架" ? "tag-accent" : s === "已入库" ? "tag-success" : "tag-danger";
    const render = () => {
      const q = (search ? search.value : "").trim().toLowerCase();
      const rows = D.RETURNS.filter((r) => {
        const okState = filter === "全部" || r.status === filter;
        const okQ = !q || [r.id, r.customer, r.product, r.sku].join(" ").toLowerCase().includes(q);
        return okState && okQ;
      });
      bodyEl.innerHTML = rows.length ? rows.map((r) => `
        <tr>
          <td><span class="code">${esc(r.id)}</span></td>
          <td><div class="cell-main">${esc(r.customer)}</div><div class="cell-sub">退货入库 · E 区暂存</div></td>
          <td><div class="cell-main">${esc(r.product)}</div><div class="cell-sub code">${esc(r.sku)}</div></td>
          <td class="num">${WMS.fmt(r.qty)}</td>
          <td>${esc(r.reason)}</td>
          <td><span class="tag ${statusCls(r.status)}"><span class="dot"></span>${esc(r.status)}</span></td>
          <td class="tight muted" style="font-size:10.5px">${esc(r.time)}</td>
          <td class="actions"><button class="btn btn-sm ${r.status === "待质检" ? "btn-primary" : "btn-ghost"}" type="button" data-return="${esc(r.id)}">${r.status === "待质检" ? "质检" : "查看"}</button></td>
        </tr>`).join("") : `<tr class="empty-row"><td colspan="8"><div class="empty-state"><img src="assets/img/undraw/not-found.svg" alt=""><p>没有匹配的退货单</p></div></td></tr>`;
      $$("#js-returns-count, #js-returns-count-2").forEach((el) => (el.textContent = rows.length));
      const c = { "全部": D.RETURNS.length };
      ["待质检", "待上架", "已入库", "已报废"].forEach((s) => (c[s] = D.RETURNS.filter((x) => x.status === s).length));
      tabs.forEach((tab) => {
        const n = tab.querySelector(".count");
        if (n) n.textContent = c[tab.dataset.returnTab] ?? 0;
      });
    };
    tabs.forEach((t) => t.addEventListener("click", () => {
      filter = t.dataset.returnTab;
      tabs.forEach((x) => x.classList.toggle("is-active", x === t));
      render();
    }));
    if (search) search.addEventListener("input", render);
    document.addEventListener("click", (e) => {
      const btn = e.target.closest("[data-return]");
      if (!btn) return;
      selected = D.RETURNS.find((r) => r.id === btn.dataset.return);
      if (!selected) return;
      const p = D.PRODUCTS.find((x) => x.sku === selected.sku);
      $("#return-sub").textContent = `${selected.id} · ${selected.customer}`;
      $("#return-product").textContent = selected.product;
      $("#return-sku").textContent = `${selected.sku} · × ${selected.qty}`;
      $("#return-reason").innerHTML = `<span class="dot"></span>${esc(selected.reason)}`;
      $("#return-status").className = "tag " + statusCls(selected.status);
      $("#return-status").innerHTML = `<span class="dot"></span>${esc(selected.status)}`;
      $("#return-img").src = p ? p.img : "assets/img/products/hub.jpg";
      $("#return-img").alt = selected.product;
      $("#return-note").value = "";
      $$("#drawer-return input[type=radio]").forEach((r) => (r.checked = r.value === "resell"));
      $$("#drawer-return input[type=checkbox]").forEach((c) => (c.checked = true));
      WMS.openDrawer("drawer-return");
    });
    const save = $("#js-save-return");
    if (save) save.addEventListener("click", () => {
      if (!selected) return;
      const d = document.querySelector('#drawer-return input[name="disposition"]:checked');
      const val = d ? d.value : "resell";
      selected.status = val === "scrap" ? "已报废" : val === "repair" ? "已入库" : "待上架";
      render();
      WMS.closeDrawer("drawer-return");
      WMS.toast(val === "scrap" ? "已判定报废，等待主管复核" : val === "repair" ? "已转维修区，退货单已更新" : "已判定可再上架，等待上架推荐");
    });
    render();
  }

  /* =========================================================
     Audit
     ========================================================= */
  if (page === "audit") {
    const search = $("#js-audit-search");
    const moduleSel = $("#js-audit-module");
    const resultSel = $("#js-audit-result");
    const bodyEl = $("#js-audit-body");
    let selected = null;
    const resultCls = (r) => r === "成功" ? "tag-success" : r === "警告" ? "tag-warning" : "tag-danger";
    const render = () => {
      const q = (search ? search.value : "").trim().toLowerCase();
      const mod = moduleSel ? moduleSel.value : "";
      const res = resultSel ? resultSel.value : "";
      const rows = D.AUDIT_LOGS.filter((l) => {
        const okQ = !q || [l.user, l.role, l.module, l.action, l.target, l.ip].join(" ").toLowerCase().includes(q);
        return okQ && (!mod || l.module === mod) && (!res || l.result === res);
      });
      bodyEl.innerHTML = rows.length ? rows.map((l, i) => `
        <tr>
          <td class="mono" style="font-size:10.5px">${esc(l.time)}</td>
          <td><div class="cell-main">${esc(l.user)}</div><div class="cell-sub">${esc(l.role)}</div></td>
          <td><span class="tag tag-neutral">${esc(l.module)}</span></td>
          <td class="tight">${esc(l.action)}</td>
          <td class="code" style="font-size:10px">${esc(l.target)}</td>
          <td class="code" style="font-size:10px">${esc(l.ip)}</td>
          <td><span class="tag ${resultCls(l.result)}"><span class="dot"></span>${esc(l.result)}</span></td>
          <td class="actions"><button class="btn btn-sm btn-ghost" type="button" data-audit-index="${D.AUDIT_LOGS.indexOf(l)}">详情</button></td>
        </tr>`).join("") : `<tr class="empty-row"><td colspan="8"><div class="empty-state"><img src="assets/img/undraw/not-found.svg" alt=""><p>没有匹配的日志</p></div></td></tr>`;
      $$("#js-audit-count, #js-audit-count-2").forEach((el) => (el.textContent = rows.length));
    };
    [search, moduleSel, resultSel].forEach((el) => el && el.addEventListener(el.tagName === "INPUT" ? "input" : "change", render));
    const sensitive = $("#js-sensitive-list");
    if (sensitive) {
      sensitive.innerHTML = D.SENSITIVE_ACTIONS.map((a) => `
        <div class="list-item">
          <span class="li-icon ${a.icon === "circle-x" ? "danger" : "warn"}">${WMS.icon(a.icon, "icon-sm")}</span>
          <span class="li-body"><strong>${esc(a.title)}</strong><small>${esc(a.meta)}</small></span>
          <span class="li-end">${esc(a.time)}</span>
        </div>`).join("");
    }
    document.addEventListener("click", (e) => {
      const b = e.target.closest("[data-audit-index]");
      if (!b) return;
      selected = D.AUDIT_LOGS[Number(b.dataset.auditIndex)];
      if (!selected) return;
      $("#audit-sub").textContent = `AUD-20261020-${selected.time.replace(/:/g, "")}`;
      $("#audit-detail").innerHTML = `
        <div class="stat-line"><span class="sl">时间</span><span class="sv">2026-10-20 ${esc(selected.time)}</span></div>
        <div class="stat-line"><span class="sl">操作人</span><span class="sv">${esc(selected.user)} · ${esc(selected.role)}</span></div>
        <div class="stat-line"><span class="sl">模块 / 操作</span><span class="sv">${esc(selected.module)} · ${esc(selected.action)}</span></div>
        <div class="stat-line"><span class="sl">对象</span><span class="sv code">${esc(selected.target)}</span></div>
        <div class="stat-line"><span class="sl">IP 地址</span><span class="sv">${esc(selected.ip)}</span></div>
        <div class="stat-line"><span class="sl">结果</span><span class="tag ${resultCls(selected.result)}"><span class="dot"></span>${esc(selected.result)}</span></div>`;
      $("#audit-before").textContent = selected.action.includes("修改") ? "旧权限：只读" : "—";
      $("#audit-after").textContent = selected.action.includes("修改") ? "新权限：库存调整" : "—";
      WMS.openDrawer("drawer-audit");
    });
    render();
  }

  /* =========================================================
     Master data
     ========================================================= */
  if (page === "masterdata") {
    let active = "products";
    const search = $("#js-master-search");
    const status = $("#js-master-status");
    const count = $("#js-master-count");
    const pb = $("#js-products-body");
    const sb = $("#js-suppliers-body");
    const cb = $("#js-customers-body");
    const statusTag = (s) => s === "在售" || s === "合作中" ? "tag-success" : s === "评估中" ? "tag-warning" : "tag-danger";
    const empty = (cols, msg) => `<tr class="empty-row"><td colspan="${cols}"><div class="empty-state"><img src="assets/img/undraw/not-found.svg" alt=""><p>${msg}</p></div></td></tr>`;
    const render = () => {
      const q = (search ? search.value : "").trim().toLowerCase();
      const st = status ? status.value : "";
      const match = (o) => (!q || Object.values(o).join(" ").toLowerCase().includes(q)) && (!st || o.status === st);
      const products = D.PRODUCTS.filter(match);
      const suppliers = D.SUPPLIERS.filter(match);
      const customers = D.CUSTOMERS.filter(match);
      pb.innerHTML = products.length ? products.map((r) => `
        <tr>
          <td><div class="cell-media"><img class="thumb" src="${esc(r.img)}" alt="" loading="lazy"><div><div class="cell-main">${esc(r.name)}</div><div class="cell-sub code">${esc(r.sku)}</div></div></div></td>
          <td>${esc(r.cat)}</td>
          <td>${esc(r.spec)}</td>
          <td>${esc(r.unit)}</td>
          <td><span class="tag ${r.temp === "恒温" ? "tag-info" : "tag-neutral"}">${esc(r.temp)}</span></td>
          <td><span class="tag ${statusTag(r.status)}"><span class="dot"></span>${esc(r.status)}</span></td>
          <td class="tight muted" style="font-size:10.5px">${esc(r.updated)}</td>
        </tr>`).join("") : empty(7, "没有匹配的商品");
      sb.innerHTML = suppliers.length ? suppliers.map((r) => `
        <tr>
          <td class="code">${esc(r.code)}</td>
          <td><div class="cell-main">${esc(r.name)}</div><div class="cell-sub">${esc(r.cat)}</div></td>
          <td>${esc(r.contact)}</td>
          <td class="code">${esc(r.phone)}</td>
          <td>${esc(r.cat)}</td>
          <td><span class="tag ${statusTag(r.status)}"><span class="dot"></span>${esc(r.status)}</span></td>
          <td class="actions"><button class="btn btn-sm btn-ghost" type="button" data-toast="已打开供应商 ${esc(r.code)}">查看</button></td>
        </tr>`).join("") : empty(7, "没有匹配的供应商");
      cb.innerHTML = customers.length ? customers.map((r) => `
        <tr>
          <td class="code">${esc(r.code)}</td>
          <td><div class="cell-main">${esc(r.name)}</div><div class="cell-sub">${esc(r.channel)}</div></td>
          <td>${esc(r.channel)}</td>
          <td>${esc(r.contact)}</td>
          <td class="code">${esc(r.phone)}</td>
          <td><span class="tag ${statusTag(r.status)}"><span class="dot"></span>${esc(r.status)}</span></td>
          <td class="actions"><button class="btn btn-sm btn-ghost" type="button" data-toast="已打开客户 ${esc(r.code)}">查看</button></td>
        </tr>`).join("") : empty(7, "没有匹配的客户");
      if (count) count.textContent = active === "products" ? products.length : active === "suppliers" ? suppliers.length : customers.length;
      const first = $('[data-tab="products"] .count');
      if (first) first.textContent = D.PRODUCTS.length;
    };
    $$("[data-tab]").forEach((t) => t.addEventListener("click", () => {
      active = t.dataset.tab;
      render();
    }));
    [search, status].forEach((el) => el && el.addEventListener(el.tagName === "INPUT" ? "input" : "change", render));
    const form = $("#form-product");
    if (form) form.addEventListener("submit", (e) => {
      e.preventDefault();
      const data = new FormData(form);
      D.PRODUCTS.unshift({
        sku: data.get("sku") || "WH-NEW",
        name: data.get("name") || "新商品",
        cat: data.get("cat") || "电子配件",
        spec: data.get("spec") || "—",
        unit: data.get("unit") || "件",
        temp: data.get("temp") || "常温",
        status: "在售",
        img: "assets/img/products/hub.jpg",
        updated: "刚刚"
      });
      active = "products";
      $$("[data-tab]").forEach((t) => {
        t.classList.toggle("is-active", t.dataset.tab === "products");
        t.setAttribute("aria-selected", String(t.dataset.tab === "products"));
      });
      $$("[data-panel]").forEach((p) => (p.hidden = p.dataset.panel !== "products"));
      render();
      form.reset();
      WMS.closeModal("modal-product");
      WMS.toast("商品已创建，可在入库单中引用");
    });
    render();
  }

  /* =========================================================
     Settings
     ========================================================= */
  if (page === "settings") {
    $$("[data-save]").forEach((b) => b.addEventListener("click", () => WMS.toast("设置已保存")));
    $$("[data-invite]").forEach((b) => b.addEventListener("click", () => WMS.toast("邀请链接已生成，有效期 24 小时")));
    $$(".switch input").forEach((i) => i.addEventListener("change", () => {
      WMS.toast(`${i.dataset.label || "规则"}已${i.checked ? "开启" : "关闭"}`);
    }));
  }
})();
