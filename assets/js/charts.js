/* 仓序 WMS — ECharts renderers.
   Palette values come from CSS tokens and were validated for CVD separation. */
(function () {
  if (!window.echarts || !window.WMS_DATA) return;
  const D = window.WMS_DATA;
  const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const charts = [];
  const FONT = '"Barlow Condensed","PingFang SC","Microsoft YaHei",sans-serif';
  const MONO = '"IBM Plex Mono","Microsoft YaHei",monospace';

  function v() {
    const cs = getComputedStyle(document.documentElement);
    const g = (n) => cs.getPropertyValue(n).trim();
    return {
      surface: g("--surface"), surface2: g("--surface-2"), surface3: g("--surface-3"),
      ink: g("--ink"), ink2: g("--ink-2"), ink3: g("--ink-3"),
      line: g("--line"), lineStrong: g("--line-strong"), accent: g("--accent"),
      c1: g("--c1"), c2: g("--c2"), c3: g("--c3"), c4: g("--c4"), c5: g("--c5"),
      gold: g("--gold"), green: g("--green"), danger: g("--danger"),
      dark: document.documentElement.getAttribute("data-theme") === "dark"
    };
  }

  function axisCommon(c) {
    return {
      axisLine: { lineStyle: { color: c.line } },
      axisTick: { show: false },
      axisLabel: { color: c.ink3, fontSize: 10, fontFamily: FONT },
      splitLine: { lineStyle: { color: c.line, type: "dashed" } }
    };
  }

  function tooltip(c) {
    return {
      backgroundColor: c.surface,
      borderColor: c.line,
      borderWidth: 1,
      padding: [8, 10],
      textStyle: { color: c.ink, fontSize: 11, fontFamily: FONT },
      extraCssText: "border-radius:7px;box-shadow:0 10px 26px rgba(0,0,0,.14);"
    };
  }

  function legend(c, data) {
    return {
      show: true, top: 0, right: 0, itemWidth: 8, itemHeight: 8, icon: "circle",
      textStyle: { color: c.ink2, fontSize: 11, fontFamily: FONT },
      data
    };
  }

  function create(sel, factory) {
    const el = document.querySelector(sel);
    if (!el) return null;
    const chart = echarts.init(el, null, { renderer: "canvas" });
    const render = () => {
      const c = v();
      chart.setOption(factory(c), true);
    };
    render();
    const ro = new ResizeObserver(() => chart.resize());
    ro.observe(el);
    document.addEventListener("themechange", render);
    charts.push(chart);
    return chart;
  }

  /* ---------------- line: 7-day flow ---------------- */
  function lineFlow(sel, dates, a, b, unit, showEnd = true) {
    return create(sel, (c) => ({
      animationDuration: reduce ? 0 : 700,
      textStyle: { fontFamily: FONT },
      grid: { left: 42, right: 58, top: 34, bottom: 26 },
      legend: legend(c, ["入库", "出库"]),
      tooltip: Object.assign({ trigger: "axis", axisPointer: { lineStyle: { color: c.lineStrong } } }, tooltip(c)),
      xAxis: Object.assign({ type: "category", data: dates, boundaryGap: false }, axisCommon(c), { splitLine: { show: false } }),
      yAxis: Object.assign({ type: "value", name: unit || "件", nameTextStyle: { color: c.ink3, fontSize: 9, padding: [0, 0, 0, -24] } }, axisCommon(c)),
      series: [
        {
          name: "入库", type: "line", data: a, symbol: "circle", symbolSize: 5, smooth: false,
          lineStyle: { width: 2, color: c.c1 }, itemStyle: { color: c.c1, borderColor: c.surface, borderWidth: 2 },
          areaStyle: { color: { type: "linear", x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: c.c1 + "22" }, { offset: 1, color: c.c1 + "00" }] } },
          endLabel: { show: showEnd, formatter: "{a}", color: c.c1, fontSize: 10, fontFamily: FONT, distance: 6 }
        },
        {
          name: "出库", type: "line", data: b, symbol: "circle", symbolSize: 5, smooth: false,
          lineStyle: { width: 2, color: c.c2 }, itemStyle: { color: c.c2, borderColor: c.surface, borderWidth: 2 },
          areaStyle: { color: { type: "linear", x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: c.c2 + "1C" }, { offset: 1, color: c.c2 + "00" }] } },
          endLabel: { show: showEnd, formatter: "{a}", color: c.c2, fontSize: 10, fontFamily: FONT, distance: 6 }
        }
      ]
    }));
  }

  /* ---------------- horizontal bars: zone occupancy ---------------- */
  function zoneBars(sel) {
    return create(sel, (c) => ({
      animationDuration: reduce ? 0 : 650,
      textStyle: { fontFamily: FONT },
      grid: { left: 96, right: 44, top: 6, bottom: 8 },
      tooltip: Object.assign({ trigger: "item", formatter: (p) => `${p.name}<br/><b>${p.value}%</b> 已占用` }, tooltip(c)),
      xAxis: { type: "value", max: 100, show: false },
      yAxis: {
        type: "category", inverse: true, data: D.CHART.zoneOccupancy.map((x) => x.zone),
        axisLine: { show: false }, axisTick: { show: false },
        axisLabel: { color: c.ink2, fontSize: 10.5, fontFamily: FONT }
      },
      series: [{
        type: "bar", barWidth: 11, data: D.CHART.zoneOccupancy.map((x, i) => ({
          value: x.value,
          itemStyle: {
            borderRadius: [0, 4, 4, 0],
            color: echarts.color.modifyAlpha(c.c1, 0.28 + x.value / 145)
          }
        })),
        label: { show: true, position: "right", formatter: "{c}%", color: c.ink2, fontSize: 10.5, fontFamily: MONO }
      }]
    }));
  }

  /* ---------------- donut: category share ---------------- */
  function categoryDonut(sel) {
    const colors = (c) => [c.c1, c.c2, c.c3, c.c4, c.c5];
    return create(sel, (c) => ({
      animationDuration: reduce ? 0 : 700,
      textStyle: { fontFamily: FONT },
      color: colors(c),
      title: {
        text: "42,680", subtext: "件 · 总库存", left: "34%", top: "40%",
        textAlign: "center", textStyle: { color: c.ink, fontSize: 26, fontFamily: FONT, fontWeight: 600 },
        subtextStyle: { color: c.ink3, fontSize: 10, fontFamily: FONT }
      },
      tooltip: Object.assign({ trigger: "item", formatter: "{b}<br/><b>{c}%</b>" }, tooltip(c)),
      legend: {
        bottom: 0, left: "center", icon: "circle", itemWidth: 7, itemHeight: 7,
        textStyle: { color: c.ink2, fontSize: 10, fontFamily: FONT },
        data: D.CHART.categoryShare.map((x) => x.name)
      },
      series: [{
        type: "pie", radius: ["54%", "76%"], center: ["34%", "46%"],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: c.surface, borderWidth: 2, borderRadius: 3 },
        label: { formatter: "{d}%", color: c.ink2, fontSize: 10, fontFamily: MONO },
        labelLine: { length: 8, length2: 8, lineStyle: { color: c.lineStrong } },
        data: D.CHART.categoryShare
      }]
    }));
  }

  /* ---------------- heatmap: location utilisation ---------------- */
  function heatmap(sel) {
    const zones = ["A", "B", "C", "D", "E"];
    const aisles = ["01", "02", "03", "04", "05", "06"];
    const data = [];
    zones.forEach((z, zi) => {
      aisles.forEach((a, ai) => {
        const val = 38 + ((zi * 19 + ai * 13 + (zi + 1) * (ai + 2) * 7) % 58);
        data.push([ai, zi, val]);
      });
    });
    return create(sel, (c) => ({
      animationDuration: reduce ? 0 : 600,
      textStyle: { fontFamily: FONT },
      grid: { left: 42, right: 16, top: 8, bottom: 54 },
      tooltip: Object.assign({
        position: "top",
        formatter: (p) => `${zones[p.value[1]]} 区 · ${aisles[p.value[0]]} 巷道<br/>利用率 <b>${p.value[2]}%</b>`
      }, tooltip(c)),
      xAxis: {
        type: "category", data: aisles, splitArea: { show: false },
        axisLine: { lineStyle: { color: c.line } }, axisTick: { show: false },
        axisLabel: { color: c.ink3, fontSize: 9.5, fontFamily: MONO }
      },
      yAxis: {
        type: "category", data: zones, inverse: true, splitArea: { show: false },
        axisLine: { show: false }, axisTick: { show: false },
        axisLabel: { color: c.ink2, fontSize: 10, fontFamily: MONO }
      },
      visualMap: {
        min: 30, max: 100, calculable: false, orient: "horizontal", left: "center", bottom: 4,
        itemWidth: 11, itemHeight: 90, text: ["高", "低"],
        textStyle: { color: c.ink3, fontSize: 9.5, fontFamily: FONT },
        inRange: { color: c.dark ? ["#33261A", "#7A3A18", "#E85D1A", "#FFB07A"] : ["#FBE6DA", "#F3B48F", "#E4570F", "#8F3006"] }
      },
      series: [{
        type: "heatmap",
        data: data.map((d) => ({
          value: d,
          label: { color: d[2] >= 72 ? (c.dark ? "#2A1A10" : "#FFFFFF") : c.ink }
        })),
        itemStyle: { borderColor: c.surface, borderWidth: 2, borderRadius: 2 },
        label: { show: true, formatter: (p) => p.value[2], fontSize: 9, fontFamily: MONO }
      }]
    }));
  }

  /* ---------------- algorithm comparison ---------------- */
  function algoBars(sel) {
    return create(sel, (c) => ({
      animationDuration: reduce ? 0 : 700,
      textStyle: { fontFamily: FONT },
      grid: { left: 88, right: 52, top: 8, bottom: 24 },
      tooltip: Object.assign({
        trigger: "item",
        formatter: (p) => `${p.name}<br/>平均拣货距离 <b>${p.value} m</b>`
      }, tooltip(c)),
      xAxis: Object.assign({ type: "value", name: "m / 单", nameTextStyle: { color: c.ink3, fontSize: 9 } }, axisCommon(c)),
      yAxis: {
        type: "category", inverse: true, data: D.CHART.algo.map((x) => x.method),
        axisLine: { show: false }, axisTick: { show: false },
        axisLabel: {
          color: c.ink2, fontSize: 11, fontFamily: FONT,
          formatter: (val) => val
        }
      },
      series: [{
        type: "bar", barWidth: 18,
        data: D.CHART.algo.map((x, i) => ({
          value: x.distance,
          itemStyle: {
            borderRadius: [0, 4, 4, 0],
            color: i === 2 ? c.c1 : (c.dark ? "#4A5348" : "#C3C9BD")
          }
        })),
        label: {
          show: true, position: "right", formatter: "{c} m",
          color: c.ink2, fontSize: 10.5, fontFamily: MONO,
          fontWeight: 500
        }
      }]
    }));
  }

  /* ---------------- sensor line ---------------- */
  function sensorLine(sel, data, opts) {
    const labels = [];
    for (let i = 0; i < 24; i++) labels.push(`${String(i).padStart(2, "0")}:00`);
    return create(sel, (c) => ({
      animationDuration: reduce ? 0 : 600,
      textStyle: { fontFamily: FONT },
      grid: { left: 38, right: 16, top: 16, bottom: 24 },
      tooltip: Object.assign({ trigger: "axis", formatter: (p) => `${p[0].axisValue}<br/><b>${p[0].value}${opts.unit}</b>` }, tooltip(c)),
      xAxis: Object.assign({ type: "category", data: labels, boundaryGap: false }, axisCommon(c), {
        splitLine: { show: false },
        axisLabel: { color: c.ink3, fontSize: 9, fontFamily: MONO, interval: 5 }
      }),
      yAxis: Object.assign({ type: "value", min: opts.min, max: opts.max }, axisCommon(c), {
        axisLabel: { color: c.ink3, fontSize: 9, fontFamily: MONO }
      }),
      series: [{
        name: opts.name, type: "line", data, smooth: true, symbol: "none",
        lineStyle: { width: 2, color: opts.color(c) },
        areaStyle: {
          color: {
            type: "linear", x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [{ offset: 0, color: opts.color(c) + "2E" }, { offset: 1, color: opts.color(c) + "00" }]
          }
        },
        markLine: {
          silent: true, symbol: "none",
          lineStyle: { color: c.gold, type: "dashed", width: 1 },
          label: { formatter: `阈值 ${opts.threshold}${opts.unit}`, color: c.gold, fontSize: 9, fontFamily: MONO, position: "insideEndTop" },
          data: [{ yAxis: opts.threshold }]
        }
      }]
    }));
  }

  /* ---------------- init by page ---------------- */
  const page = document.body.dataset.page;
  if (page === "dashboard") {
    const t = D.CHART.trend7;
    lineFlow("#chart-trend", t.dates, t.inbound, t.outbound, "件");
    zoneBars("#chart-zones");
  }
  if (page === "analytics") {
    const dates = [];
    const inbound = [];
    const outbound = [];
    for (let i = 29; i >= 0; i--) {
      const d = new Date(2026, 9, 20);
      d.setDate(d.getDate() - i);
      dates.push(`${d.getMonth() + 1}/${d.getDate()}`);
      const k = 29 - i;
      inbound.push(Math.round(820 + 300 * Math.sin(k / 4.2) + k * 7 + (k % 5) * 32));
      outbound.push(Math.round(900 + 340 * Math.sin(k / 3.7 + 1.1) + k * 6 + (k % 4) * 36));
    }
    lineFlow("#chart-flow", dates, inbound, outbound, "件", false);
    categoryDonut("#chart-category");
    heatmap("#chart-heat");
    algoBars("#chart-algo");
  }
  if (page === "devices") {
    sensorLine("#chart-temp", D.CHART.temp, { name: "温度", unit: "°C", min: 18, max: 28, threshold: 26, color: (c) => c.c1 });
    sensorLine("#chart-humidity", D.CHART.humidity, { name: "湿度", unit: "%", min: 30, max: 70, threshold: 65, color: (c) => c.c2 });
  }

  window.addEventListener("resize", () => charts.forEach((c) => c.resize()));
})();
