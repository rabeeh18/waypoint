/* Click the map to set the drop point; fills the latitude and longitude fields. */
(function () {
  'use strict';
  const host = document.querySelector('[data-pick-map]');
  if (!host) return;
  const lat = document.querySelector('[data-lat]');
  const lng = document.querySelector('[data-lng]');
  const label = document.querySelector('[data-pick-label]');
  const map = WaypointMap.create(host);
  let marker = null;

  function place(p) {
    if (!marker) {
      marker = L.marker(p, { icon: WaypointMap.stopIcon('•', 'PENDING', true), draggable: true }).addTo(map);
      marker.on('dragend', () => place(marker.getLatLng()));
    } else {
      marker.setLatLng(p);
    }
    const ll = L.latLng(p);
    lat.value = ll.lat.toFixed(5);
    lng.value = ll.lng.toFixed(5);
    if (label) label.textContent = `${ll.lat.toFixed(4)}, ${ll.lng.toFixed(4)}`;
  }

  map.on('click', e => place(e.latlng));
  [lat, lng].forEach(input => input.addEventListener('change', () => {
    if (lat.value && lng.value) {
      place([Number(lat.value), Number(lng.value)]);
      map.setView([Number(lat.value), Number(lng.value)], 13);
    }
  }));
})();
