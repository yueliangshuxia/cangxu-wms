/* 仓序 WMS — application shell and shared interactions. */
(function () {
  const D = window.WMS_DATA;
  const body = document.body;
  const page = body.dataset.page || "";
  const meta = (D.PAGE_META && D.PAGE_META[page]) || { title: "", crumb: "" };
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  const WMS = (window.WMS = window.WMS || {});
  WMS.page = page;
  WMS.icon = (name, cls = "") =>
    `<svg class="icon ${cls}" aria-hidden="true"><use href="#i-${name}"></use></svg>`;
  WMS.escape = (s) => String(s).replace(/[&<>"']/g, (m) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[m]));
  WMS.fmt = (n, decimals = 0) =>
    Number(n).toLocaleString("zh-CN", { minimumFractionDigits: decimals, maximumFractionDigits: decimals });

  /* ---------------- demo accounts (static demo only) ---------------- */
  const DEMO_USER_KEY = "wms_demo_user";
  const DEMO_ACCOUNTS = [
    { username: "chenyu", password: "wms2026", name: "陈屿", role: "仓储主管" },
    { username: "admin", password: "admin123", name: "系统管理员", role: "系统管理员" },
    { username: "linzhou", password: "wms2026", name: "林舟", role: "收货员" },
    { username: "zhouke", password: "wms2026", name: "周可", role: "质检员" }
  ];
  function currentDemoUser() {
    try {
      const saved = JSON.parse(localStorage.getItem(DEMO_USER_KEY) || "null");
      if (saved && saved.name) return saved;
    } catch {
      // Ignore malformed localStorage and use the default demo account.
    }
    return DEMO_ACCOUNTS[0];
  }

  // Static demo gate: every page except login/404 requires a demo session.
  if (page === "login") {
    localStorage.removeItem(DEMO_USER_KEY);
  } else if (page !== "notfound" && !localStorage.getItem(DEMO_USER_KEY)) {
    location.replace("login.html");
    return;
  }

  /* ---------------- theme ---------------- */
  const THEME_KEY = "wms-theme";
  function getTheme() {
    return document.documentElement.getAttribute("data-theme") || "light";
  }
  function applyTheme(theme, persist = true) {
    document.documentElement.setAttribute("data-theme", theme);
    if (persist) localStorage.setItem(THEME_KEY, theme);
    document.querySelectorAll("[data-theme-toggle]").forEach((btn) => {
      btn.innerHTML = theme === "dark" ? WMS.icon("sun") + `<span class="sr-only">切换到浅色</span>` : WMS.icon("moon") + `<span class="sr-only">切换到深色</span>`;
      btn.setAttribute("aria-label", theme === "dark" ? "切换到浅色模式" : "切换到深色模式");
      btn.title = theme === "dark" ? "切换到浅色模式" : "切换到深色模式";
    });
    document.dispatchEvent(new CustomEvent("themechange", { detail: { theme } }));
    const mc = document.querySelector('meta[name="theme-color"]');
    if (mc) mc.setAttribute("content", theme === "dark" ? "#101410" : "#E8EAE3");
  }
  WMS.toggleTheme = () => applyTheme(getTheme() === "dark" ? "light" : "dark");
  const initialTheme = new URLSearchParams(location.search).get("theme")
    || localStorage.getItem(THEME_KEY)
    || document.documentElement.getAttribute("data-theme")
    || "light";
  applyTheme(initialTheme, false);

  /* ---------------- shell ---------------- */
  function railHTML() {
    const user = currentDemoUser();
    const groups = D.NAV.map((g) => `
      <div class="nav-group">
        <p class="nav-label">${g.group}</p>
        ${g.items.map((it) => `
          <a class="nav-item ${it.id === page ? "is-active" : ""}" href="${it.href}" ${it.id === page ? 'aria-current="page"' : ""}>
            ${WMS.icon(it.icon)}
            <span>${it.label}</span>
            ${it.badge ? `<span class="nav-badge">${it.badge}</span>` : ""}
          </a>`).join("")}
      </div>`).join("");
    return `
      <div class="rail-top">
        <a class="brand" href="index.html" aria-label="仓序 WMS 首页">
          <span class="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 7h16M4 12h16M4 17h10"/><circle cx="18" cy="17" r="2" fill="currentColor" stroke="none"/></svg>
          </span>
          <span class="brand-text"><strong>仓序</strong><small>CANGXU WMS</small></span>
        </a>
        <button class="wh-switch" type="button" data-toast="仓库切换为「华东一号仓」">
          <span class="wh-dot"></span>
          <span class="wh-meta"><strong>华东一号仓</strong><small>苏州 · 华东运营中心</small></span>
          ${WMS.icon("chevron-down")}
        </button>
      </div>
      <nav class="rail-nav" aria-label="主导航">${groups}
        <div class="nav-group">
          <p class="nav-label">系统</p>
          <a class="nav-item ${page === "audit" ? "is-active" : ""}" href="audit.html" ${page === "audit" ? 'aria-current="page"' : ""}>
            ${WMS.icon("file-text")}<span>操作日志</span>
          </a>
          <a class="nav-item ${page === "settings" ? "is-active" : ""}" href="settings.html" ${page === "settings" ? 'aria-current="page"' : ""}>
            ${WMS.icon("settings-2")}<span>系统设置</span>
          </a>
        </div>
      </nav>
      <div class="rail-bottom">
        <div class="rail-user">
          <span class="avatar">${user.name.slice(0, 1)}</span>
          <span class="meta"><strong>${WMS.escape(user.name)}</strong><small>${WMS.escape(user.role)}</small></span>
          <a class="out" href="login.html" aria-label="退出登录" title="退出登录">${WMS.icon("log-out", "icon-sm")}</a>
        </div>
      </div>`;
  }

  function topbarHTML() {
    const user = currentDemoUser();
    const notifs = D.NOTIFICATIONS.map((n) => `
      <div class="notif-item">
        <span class="nicon ${n.level === "danger" ? "danger" : n.level === "info" ? "info" : ""}">${WMS.icon(n.icon, "icon-sm")}</span>
        <span class="ntext">${n.title}<small>${n.meta}</small></span>
      </div>`).join("");
    return `
      <button class="icon-btn rail-toggle" type="button" aria-label="打开导航" aria-expanded="false" aria-controls="rail">${WMS.icon("menu")}</button>
      <div class="crumbs"><span>华东一号仓</span>${WMS.icon("chevron-right")}<strong>${meta.crumb}</strong></div>
      <div class="topbar-right">
        <button class="search-trigger" type="button" aria-label="搜索">
          ${WMS.icon("search")}<span>搜索单据、商品、库位</span><kbd>Ctrl K</kbd>
        </button>
        <span class="sys-live"><i></i>系统在线</span>
        <button class="icon-btn" type="button" data-theme-toggle></button>
        <div class="pop-wrap">
          <button class="icon-btn" type="button" data-pop="notif" aria-label="通知" aria-expanded="false" style="position:relative">
            ${WMS.icon("bell")}<span class="dot-mark" style="position:absolute;top:7px;right:8px;width:6px;height:6px;border-radius:50%;background:var(--accent)"></span>
          </button>
          <div class="popover" data-pop-panel="notif">
            <div class="pop-head"><strong>通知</strong><button type="button" data-read-all>全部已读</button></div>
            ${notifs}
          </div>
        </div>
        <div class="pop-wrap">
          <button class="user-btn" type="button" data-pop="user" aria-label="账户菜单" aria-expanded="false">
            <span class="avatar">${user.name.slice(0, 1)}</span>
            <span class="user-meta"><strong>${WMS.escape(user.name)}</strong><small>${WMS.escape(user.role)}</small></span>
            ${WMS.icon("chevron-down", "icon-sm")}
          </button>
          <div class="popover user-pop" data-pop-panel="user">
            <a href="settings.html">${WMS.icon("user")}账户与权限</a>
            <a href="settings.html">${WMS.icon("bell")}通知设置</a>
            <div class="sep"></div>
            <button type="button" data-theme-toggle>${WMS.icon("moon")}切换主题</button>
            <a href="login.html">${WMS.icon("log-out")}退出登录</a>
          </div>
        </div>
      </div>`;
  }

  const railEl = document.getElementById("rail");
  const topbarEl = document.getElementById("topbar");
  if (railEl) railEl.innerHTML = railHTML();
  if (topbarEl) topbarEl.innerHTML = topbarHTML();
  applyTheme(initialTheme, false);

  /* ---------------- mobile rail ---------------- */
  const scrim = document.createElement("div");
  scrim.className = "scrim";
  scrim.setAttribute("data-close-rail", "");
  document.body.appendChild(scrim);
  function setRail(open) {
    if (!railEl) return;
    railEl.classList.toggle("is-open", open);
    scrim.classList.toggle("is-open", open);
    const btn = document.querySelector(".rail-toggle");
    if (btn) btn.setAttribute("aria-expanded", String(open));
    document.body.style.overflow = open ? "hidden" : "";
  }
  document.addEventListener("click", (e) => {
    if (e.target.closest(".rail-toggle")) setRail(!railEl.classList.contains("is-open"));
    else if (e.target.closest("[data-close-rail]")) setRail(false);
  });

  /* ---------------- clock / greeting ---------------- */
  function tick() {
    const now = new Date();
    const week = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"][now.getDay()];
    const text = `${now.getMonth() + 1}月${now.getDate()}日 ${week} ${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
    document.querySelectorAll("[data-clock]").forEach((el) => (el.textContent = text));
    const hour = now.getHours();
    const greet = hour < 6 ? "夜深了" : hour < 11 ? "早上好" : hour < 14 ? "中午好" : hour < 18 ? "下午好" : "晚上好";
    document.querySelectorAll("[data-greeting]").forEach((el) => (el.textContent = greet));
    document.querySelectorAll("[data-date]").forEach((el) => (el.textContent = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`));
  }
  tick();
  setInterval(tick, 30000);

  /* ---------------- popovers ---------------- */
  function closePops(except) {
    document.querySelectorAll(".popover.is-open").forEach((p) => {
      if (p === except) return;
      p.classList.remove("is-open");
      const b = p.parentElement.querySelector("[data-pop]");
      if (b) b.setAttribute("aria-expanded", "false");
    });
  }
  document.addEventListener("click", (e) => {
    const trigger = e.target.closest("[data-pop]");
    if (trigger) {
      const panel = trigger.parentElement.querySelector("[data-pop-panel]");
      const open = !panel.classList.contains("is-open");
      closePops(panel);
      panel.classList.toggle("is-open", open);
      trigger.setAttribute("aria-expanded", String(open));
      return;
    }
    if (e.target.closest("[data-read-all]")) {
      document.querySelectorAll(".dot-mark").forEach((d) => (d.style.display = "none"));
      WMS.toast("通知已全部标记为已读");
      return;
    }
    if (!e.target.closest(".popover")) closePops();
  });
  document.addEventListener("click", (e) => {
    const t = e.target.closest("[data-theme-toggle]");
    if (t) WMS.toggleTheme();
  });

  /* ---------------- toast ---------------- */
  let toastStack = document.querySelector(".toast-stack");
  if (!toastStack) {
    toastStack = document.createElement("div");
    toastStack.className = "toast-stack";
    toastStack.setAttribute("role", "status");
    toastStack.setAttribute("aria-live", "polite");
    document.body.appendChild(toastStack);
  }
  WMS.toast = (message, type = "ok") => {
    const el = document.createElement("div");
    el.className = "toast" + (type === "warn" ? " warn" : type === "err" ? " err" : "");
    el.innerHTML = WMS.icon(type === "err" ? "circle-x" : type === "warn" ? "triangle-alert" : "circle-check") + `<span>${WMS.escape(message)}</span>`;
    toastStack.appendChild(el);
    setTimeout(() => {
      el.style.transition = "opacity .2s ease, transform .2s ease";
      el.style.opacity = "0";
      el.style.transform = "translateY(6px)";
      setTimeout(() => el.remove(), 220);
    }, 3200);
  };

  /* ---------------- modal / drawer ---------------- */
  const overlay = document.createElement("div");
  overlay.className = "overlay";
  document.body.appendChild(overlay);
  let lastFocus = null;
  let openLayer = null;
  function showLayer(el) {
    if (!el) return;
    lastFocus = document.activeElement;
    openLayer = el;
    overlay.classList.add("is-open");
    el.classList.add("is-open");
    el.removeAttribute("hidden");
    document.body.style.overflow = "hidden";
    const f = el.querySelector("input,select,textarea,button,a[href]");
    setTimeout(() => f && f.focus(), 60);
  }
  function hideLayer(el) {
    if (!el) return;
    el.classList.remove("is-open");
    overlay.classList.remove("is-open");
    document.body.style.overflow = "";
    openLayer = null;
    if (lastFocus && lastFocus.focus) lastFocus.focus();
  }
  function resolveLayer(el) {
    if (typeof el !== "string") return el;
    return document.getElementById(el) || document.querySelector(el);
  }
  WMS.openModal = (id) => showLayer(resolveLayer(id));
  WMS.closeModal = (id) => hideLayer(resolveLayer(id));
  WMS.openDrawer = (id) => showLayer(resolveLayer(id));
  WMS.closeDrawer = (id) => hideLayer(resolveLayer(id));
  overlay.addEventListener("click", () => {
    if (cmdk && cmdk.classList.contains("is-open")) closeCmdk();
    else hideLayer(openLayer);
  });
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      if (cmdk && cmdk.classList.contains("is-open")) closeCmdk();
      else if (openLayer) hideLayer(openLayer);
      else setRail(false);
    }
  });
  document.addEventListener("click", (e) => {
    const close = e.target.closest("[data-close-layer]");
    if (close) hideLayer(openLayer);
    const openM = e.target.closest("[data-open-modal]");
    if (openM) { e.preventDefault(); WMS.openModal(openM.getAttribute("data-open-modal")); }
    const openD = e.target.closest("[data-open-drawer]");
    if (openD) { e.preventDefault(); WMS.openDrawer(openD.getAttribute("data-open-drawer")); }
    const toast = e.target.closest("[data-toast]");
    if (toast) WMS.toast(toast.getAttribute("data-toast"), toast.getAttribute("data-toast-type") || "ok");
  });

  /* ---------------- command palette ---------------- */
  let cmdk = null;
  function buildCmdk() {
    cmdk = document.createElement("div");
    cmdk.className = "cmdk";
    cmdk.setAttribute("role", "dialog");
    cmdk.setAttribute("aria-label", "快速搜索");
    cmdk.innerHTML = `
      <div class="cmdk-input">${WMS.icon("search")}<input type="text" placeholder="搜索页面、单据或操作…" aria-label="搜索"></div>
      <div class="cmdk-list"></div>`;
    document.body.appendChild(cmdk);
  }
  function cmdkItems() {
    const pages = D.NAV.flatMap((g) => g.items).concat([{ id: "settings", label: "系统设置", href: "settings.html", icon: "settings-2" }])
      .map((p) => ({ label: p.label, icon: p.icon, kind: "页面", run: () => (location.href = p.href) }));
    const actions = [
      { label: "新建入库单", icon: "plus", kind: "操作", run: () => { if (page === "inbound") WMS.openModal("modal-inbound"); else location.href = "inbound.html#new"; } },
      { label: "新建出库单", icon: "send", kind: "操作", run: () => { if (page === "outbound") WMS.toast("出库单草稿已创建"); else location.href = "outbound.html"; } },
      { label: "创建盘点任务", icon: "clipboard-list", kind: "操作", run: () => WMS.toast("盘点任务 CC-20261020-008 已创建") },
      { label: "切换深色 / 浅色模式", icon: "moon", kind: "设置", run: () => WMS.toggleTheme() }
    ];
    return pages.concat(actions);
  }
  function renderCmdk(q = "") {
    const list = cmdk.querySelector(".cmdk-list");
    const items = cmdkItems().filter((i) => (i.label + i.kind).toLowerCase().includes(q.toLowerCase()));
    list.innerHTML = items.length ? items.map((i, idx) => `
      <div class="cmdk-item ${idx === 0 ? "is-active" : ""}" data-cmdk-index="${idx}">
        ${WMS.icon(i.icon)}<span>${i.label}</span><span class="k">${i.kind}</span>
      </div>`).join("") : `<div class="cmdk-empty">没有匹配的结果</div>`;
    list._items = items;
  }
  let cmdkActive = 0;
  function openCmdk() {
    if (!cmdk) buildCmdk();
    cmdk.classList.add("is-open");
    overlay.classList.add("is-open");
    document.body.style.overflow = "hidden";
    const input = cmdk.querySelector("input");
    input.value = "";
    cmdkActive = 0;
    renderCmdk("");
    setTimeout(() => input.focus(), 30);
  }
  function closeCmdk() {
    if (!cmdk) return;
    cmdk.classList.remove("is-open");
    overlay.classList.remove("is-open");
    document.body.style.overflow = "";
  }
  document.addEventListener("click", (e) => {
    if (e.target.closest(".search-trigger")) openCmdk();
  });
  document.addEventListener("keydown", (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
      e.preventDefault();
      cmdk && cmdk.classList.contains("is-open") ? closeCmdk() : openCmdk();
    }
  });
  document.addEventListener("keydown", (e) => {
    if (!cmdk || !cmdk.classList.contains("is-open")) return;
    const list = cmdk.querySelector(".cmdk-list");
    const items = list._items || [];
    if (e.key === "Escape") { closeCmdk(); return; }
    if (e.key === "ArrowDown" || e.key === "ArrowUp") {
      e.preventDefault();
      cmdkActive = (cmdkActive + (e.key === "ArrowDown" ? 1 : -1) + items.length) % items.length;
      list.querySelectorAll(".cmdk-item").forEach((n, i) => n.classList.toggle("is-active", i === cmdkActive));
    }
    if (e.key === "Enter" && items[cmdkActive]) { items[cmdkActive].run(); closeCmdk(); }
  });
  document.addEventListener("input", (e) => {
    if (cmdk && e.target.closest(".cmdk")) { cmdkActive = 0; renderCmdk(e.target.value); }
  });
  document.addEventListener("click", (e) => {
    const item = e.target.closest(".cmdk-item");
    if (!item || !cmdk) return;
    const items = cmdk.querySelector(".cmdk-list")._items || [];
    const it = items[Number(item.dataset.cmdkIndex)];
    if (it) { it.run(); closeCmdk(); }
  });

  /* ---------------- tabs ---------------- */
  WMS.bindTabs = (root) => {
    const scope = root.closest(".card") || root.parentElement || document;
    root.querySelectorAll("[data-tab]").forEach((tab) => {
      tab.addEventListener("click", () => {
        const name = tab.dataset.tab;
        root.querySelectorAll("[data-tab]").forEach((t) => {
          t.classList.toggle("is-active", t === tab);
          t.setAttribute("aria-selected", String(t === tab));
        });
        scope.querySelectorAll("[data-panel]").forEach((p) => {
          p.hidden = p.dataset.panel !== name;
        });
      });
    });
  };
  document.querySelectorAll("[data-tabs]").forEach((r) => WMS.bindTabs(r));

  /* ---------------- count up ---------------- */
  function countUp() {
    document.querySelectorAll("[data-count]").forEach((el) => {
      const target = Number(el.dataset.count);
      const decimals = Number(el.dataset.decimals || 0);
      if (reduceMotion || !isFinite(target)) { el.textContent = WMS.fmt(target, decimals); return; }
      const start = performance.now();
      const dur = 850;
      let finished = false;
      function frame(t) {
        if (finished) return;
        const p = Math.min(1, (t - start) / dur);
        const eased = 1 - Math.pow(1 - p, 3);
        el.textContent = WMS.fmt(target * eased, decimals);
        if (p < 1) requestAnimationFrame(frame);
        else { finished = true; el.textContent = WMS.fmt(target, decimals); }
      }
      requestAnimationFrame(frame);
      setTimeout(() => {
        if (!finished) {
          finished = true;
          el.textContent = WMS.fmt(target, decimals);
        }
      }, 1000);
    });
  }
  countUp();

  /* ---------------- hash actions ---------------- */
  if (location.hash === "#new" && page === "inbound") {
    setTimeout(() => {
      const m = document.getElementById("modal-inbound");
      if (m) WMS.openModal(m);
    }, 120);
  }

  /* ---------------- login ---------------- */
  if (page === "login") {
    const form = document.getElementById("login-form");
    if (form) {
      form.addEventListener("submit", (e) => {
        e.preventDefault();
        const username = document.getElementById("login-user").value.trim();
        const password = document.getElementById("login-pass").value;
        const account = DEMO_ACCOUNTS.find(
          (item) => item.username === username && item.password === password
        );
        const btn = form.querySelector('button[type="submit"]');
        const old = btn.innerHTML;
        if (!account) {
          WMS.toast("账号或密码错误，请使用页面下方的演示账号");
          document.getElementById("login-pass").select();
          return;
        }
        btn.disabled = true;
        btn.innerHTML = `<span class="mono" style="font-size:11px">正在验证…</span>`;
        localStorage.setItem(DEMO_USER_KEY, JSON.stringify({
          username: account.username, name: account.name, role: account.role
        }));
        setTimeout(() => {
          btn.innerHTML = old;
          btn.disabled = false;
          location.href = "index.html";
        }, 650);
      });
    }
    const demo = document.querySelector("[data-fill-demo]");
    if (demo) demo.addEventListener("click", () => {
      document.getElementById("login-user").value = "chenyu";
      document.getElementById("login-pass").value = "wms2026";
      WMS.toast("演示账号已填入");
    });
  }

  /* ---------------- reveal on load ---------------- */
  if (!reduceMotion) {
    document.querySelectorAll("[data-reveal]").forEach((el, i) => {
      el.style.opacity = "0";
      el.style.transform = "translateY(8px)";
      el.style.transition = "opacity .45s ease, transform .45s ease";
      el.style.transitionDelay = `${Math.min(i * 45, 220)}ms`;
      requestAnimationFrame(() => requestAnimationFrame(() => {
        el.style.opacity = "1";
        el.style.transform = "none";
      }));
    });
  }
})();
