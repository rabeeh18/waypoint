/* Trip detail map: planned route, travelled trail, stops and the truck while it is live. */
(function () {
  'use strict';
  const host = document.querySelector('[data-trip-map]');
  if (!host) return;
  const { api, fmtTime, esc } = window.Fleet;
  const map = ParceloMap.create(host);
  const layer = L.layerGroup().addTo(map);
  let truck = null;
  let first = true;
  let activePts = [];
  let trail = null;

  // The part of the planned route the truck has already covered, ending at its position.
  function covered(pos) {
    let best = 0, bestD = Infinity;
    activePts.forEach((p, i) => {
      const d = (p[0] - pos[0]) ** 2 + (p[1] - pos[1]) ** 2;
      if (d < bestD) { bestD = d; best = i; }
    });
    return activePts.slice(0, best + 1).concat([pos]);
  }

  async function draw() {
    const d = await api('GET', `/api/dashboard/trips/${host.dataset.tripId}`);
    layer.clearLayers();
    const bounds = [[d.warehouse.lat, d.warehouse.lng]];
    activePts = [];
    trail = null;
    d.routes.forEach(r => {
      const pts = ParceloMap.decode(r.geometry);
      L.polyline(pts, { pane: 'routes', color: r.active ? '#24231f' : '#9a978a', weight: r.active ? 4 : 2,
        opacity: r.active ? 0.85 : 0.6, dashArray: '8 8' }).addTo(layer);
      if (r.active) activePts = pts;
      pts.forEach(p => bounds.push(p));
    });
    const live = d.status === 'IN_PROGRESS' && d.summary && d.summary.lat && activePts.length > 1;
    const trailStyle = { pane: 'routes', color: '#24231f', weight: 4, opacity: 0.9 };
    if (live) trail = L.polyline(covered([d.summary.lat, d.summary.lng]), trailStyle).addTo(layer);
    else if (d.trail.length > 1) L.polyline(d.trail, trailStyle).addTo(layer);
    const next = (d.stops.find(s => s.status === 'PENDING' || s.status === 'ARRIVED') || {}).id;
    d.stops.forEach(s => {
      L.marker([s.lat, s.lng], { icon: ParceloMap.stopIcon(s.seq, s.status, s.id === next) })
        .bindTooltip(`${s.seq}. ${esc(s.name)} · ETA ${fmtTime(s.eta)}`).addTo(layer);
      bounds.push([s.lat, s.lng]);
    });
    L.marker([d.warehouse.lat, d.warehouse.lng], { icon: ParceloMap.depotIcon() }).bindTooltip(esc(d.warehouse.name)).addTo(layer);
    truck = null;
    if (d.status === 'IN_PROGRESS' && d.summary && d.summary.lat) {
      truck = L.marker([d.summary.lat, d.summary.lng], { icon: ParceloMap.truckIcon(d.summary.riskBand, true), zIndexOffset: 1000 }).addTo(layer);
      truck._vid = d.vehicle.id;
    }
    if (first) { first = false; map.fitBounds(bounds, { padding: [30, 30], maxZoom: 13 }); }
  }

  draw().catch(() => {});
  window.Fleet.subscribe('/topic/fleet/positions', frame => {
    if (!truck) return;
    const p = frame.vehicles.find(v => v.v === truck._vid);
    if (p) {
      truck.setLatLng([p.lat, p.lng]);
      if (trail) trail.setLatLngs(covered([p.lat, p.lng]));
    }
  });
})();
