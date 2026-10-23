# 仓序 WMS · Vue 3 前端

这是完整项目的 Vue 3 + Vite 前端。设计 token 与静态 Demo 共用，业务接口通过 `/api` 代理到 Spring Boot。

## 启动

```bash
npm install
npm run dev
```

打开 <http://localhost:5173>。请先启动 `../backend`（默认 8080 端口）。

演示账号：`chenyu` / `wms2026`。

## 构建

```bash
npm run build
```

## 结构

- `src/views/` — 12 个业务页面 + 登录
- `src/layouts/AppLayout.vue` — 侧栏、顶栏、命令面板、主题
- `src/api/` — Axios 实例与接口封装
- `src/stores/` — Pinia：auth / theme / toast
- `src/composables/` — 通用组合式函数（Esc 关闭弹层）
- `src/styles/app.css` — 设计系统与全部样式
- `public/images/` — Pexels / unDraw / Picsum 本地资源
- `public/fonts/` — Barlow Condensed / IBM Plex Mono

后端接口说明见根目录 `README.md`。
