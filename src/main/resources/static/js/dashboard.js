/* Overview: live deliveries table, issues list and the headline numbers. Polls the snapshot. */
(function () {
  'use strict';
  const root = document.querySelector('[data-dashboard]');
  if (!root) return;
  const { api, fmtTime, esc } = window.Fleet;

  const mapHost = root.querySelector('[data-live-map]');
  const map = WaypointMap.create(mapHost);
  const pins = new Map();
  let fitted = false;
  const LABEL = { LOW: ['On time', 'tag-green'], MEDIUM: ['May be late', 'tag-ochre'], HIGH: ['Late', 'tag-red'], CRITICAL: ['Late', 'tag-red'] };

  function applyKpis(k) {
    root.querySelectorAll('[data-kpi]').forEach(el => {
      const v = k[el.dataset.kpi];
      el.textContent = v === null || v === undefined ? '—' : v;
    });
    const risk = root.querySelector('[data-tone-for="atRisk"]');
    if (risk) risk.dataset.tone = k.atRisk > 0 ? 'warn' : '';
  }

  function renderTrips(trips) {
    const rank = { CRITICAL: 3, HIGH: 2, MEDIUM: 1, LOW: 0 };
    const rows = [...trips].sort((a, b) => (rank[b.riskBand] - rank[a.riskBand]) || a.code.localeCompare(b.code));
    root.querySelector('[data-trip-count]').textContent = rows.length + ' trips';
    const body = root.querySelector('[data-trip-rows]');
    if (!rows.length) {
      body.innerHTML = '<tr><td colspan="6"><div class="empty"><strong>Nothing on the road</strong>Planned trips start at their departure time. <a href="/planning">Plan a trip</a></div></td></tr>';
      return;
    }
    body.innerHTML = rows.map(t => {
      const [text, cls] = t.lateMin > 0 ? LABEL.HIGH : LABEL.LOW;
      return `<tr class="click" data-href="/trips/${t.tripId}">
        <td><a href="/trips/${t.tripId}"><b>${esc(t.code)}</b></a></td>
        <td>${esc(t.route)}<span class="sub">${t.stopsDone}/${t.stopsTotal} stops</span></td>
        <td><span class="plate">${esc(t.registration)}</span><span class="sub">${esc(t.driver)}</span></td>
        <td><div class="bar"><i style="width:${t.progress}%"></i></div></td>
        <td>${t.nextEta ? esc(fmtTime(t.nextEta)) : '—'}<span class="sub">${esc(t.nextStop || '')}</span></td>
        <td><span class="tag ${cls}">${text}</span></td>
      </tr>`;
    }).join('');
  }

  function renderMap(snap) {
    if (!fitted) {
      snap.warehouses.forEach(w => L.marker([w.lat, w.lng], { icon: WaypointMap.depotIcon() }).bindTooltip(w.name).addTo(map));
    }
    const seen = new Set();
    const pts = [];
    snap.trips.forEach(t => {
      if (t.lat == null) return;
      seen.add(t.tripId);
      pts.push([t.lat, t.lng]);
      let m = pins.get(t.tripId);
      if (!m) {
        m = L.marker([t.lat, t.lng], { icon: WaypointMap.truckIcon(t.lateMin > 0 ? 'HIGH' : 'LOW') }).addTo(map);
        m.on('click', () => { location.href = '/trips/' + t.tripId; });
        pins.set(t.tripId, m);
      }
      m.setLatLng([t.lat, t.lng]).setIcon(WaypointMap.truckIcon(t.lateMin > 0 ? 'HIGH' : 'LOW'));
      m.unbindTooltip().bindTooltip(`${esc(t.code)} · ${esc(t.route)}`);
    });
    pins.forEach((m, id) => { if (!seen.has(id)) { map.removeLayer(m); pins.delete(id); } });
    if (!fitted) {
      fitted = true;
      const all = pts.concat(snap.warehouses.map(w => [w.lat, w.lng]));
      if (all.length) map.fitBounds(all, { padding: [30, 30], maxZoom: 9 });
    }
  }

  async function refresh() {
    try {
      const snap = await api('GET', '/api/dashboard/snapshot');
      applyKpis(snap.kpis);
      renderTrips(snap.trips);
      renderMap(snap);
    } catch (e) { /* keep the last numbers on screen */ }
  }
  root.addEventListener('click', e => {
    const row = e.target.closest('tr[data-href]');
    if (row && !e.target.closest('a')) location.href = row.dataset.href;
  });
  refresh();
  setInterval(refresh, 4000);
})();
