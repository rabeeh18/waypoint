/* Waypoint map: a clean Leaflet map with round truck pins and diamond depots. Exposes window.WaypointMap. */
(function () {
  'use strict';
  const INDIA = [[8.0, 68.5], [35.5, 97.5]];
  const COLORS = { LOW: '#2e7a4f', MEDIUM: '#d99a1d', HIGH: '#c0392b', CRITICAL: '#c0392b' };

  function create(host, options) {
    const opts = Object.assign({ interactive: true, bounds: INDIA }, options || {});
    host.classList.add('map-host');
    const map = L.map(host, {
      zoomControl: opts.interactive, scrollWheelZoom: opts.interactive, dragging: opts.interactive,
      doubleClickZoom: opts.interactive, boxZoom: false, keyboard: opts.interactive, zoomSnap: 0.25
    });
    map.attributionControl.setPrefix(false);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18, attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(map);
    map.createPane('routes').style.zIndex = 420;
    map.fitBounds(opts.bounds, { padding: [20, 20] });
    new ResizeObserver(() => map.invalidateSize({ pan: false })).observe(host);
    return map;
  }

  function decode(str) {
    let i = 0, lat = 0, lng = 0;
    const out = [];
    while (i < str.length) {
      let b, shift = 0, r = 0;
      do { b = str.charCodeAt(i++) - 63; r |= (b & 0x1f) << shift; shift += 5; } while (b >= 0x20);
      lat += (r & 1) ? ~(r >> 1) : (r >> 1);
      shift = 0; r = 0;
      do { b = str.charCodeAt(i++) - 63; r |= (b & 0x1f) << shift; shift += 5; } while (b >= 0x20);
      lng += (r & 1) ? ~(r >> 1) : (r >> 1);
      out.push([lat / 1e5, lng / 1e5]);
    }
    return out;
  }

  const truckIcon = (band, selected) => L.divIcon({
    className: 'hm-truck' + (selected ? ' is-selected' : ''),
    html: `<span style="background:${COLORS[band] || COLORS.LOW}"></span>`, iconSize: [18, 18], iconAnchor: [9, 9]
  });
  const depotIcon = () => L.divIcon({ className: 'hm-depot', html: '<span></span>', iconSize: [12, 12], iconAnchor: [6, 6] });
  const stopIcon = (label, status, next) => L.divIcon({
    className: 'hm-stop st-' + String(status).toLowerCase() + (next ? ' is-next' : ''),
    html: `<span>${label}</span>`, iconSize: [22, 22], iconAnchor: [11, 11]
  });

  window.WaypointMap = { create, decode, truckIcon, depotIcon, stopIcon, COLORS };
})();
