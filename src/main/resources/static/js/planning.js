/* Planning: live load against the chosen vehicle's capacity, and a preview of the stops. */
(function () {
  'use strict';
  const form = document.querySelector('[data-planner]');
  if (!form) return;
  const boxes = [...form.querySelectorAll('input[name="shipment"]')];
  const vehicle = form.querySelector('[data-vehicle]');
  const loadEl = form.querySelector('[data-load]');
  const bar = form.querySelector('[data-load-bar]');
  const hint = form.querySelector('[data-load-hint]');
  const submit = form.querySelector('[data-submit]');
  const countEl = form.querySelector('[data-stop-count]');


  function update() {
    const chosen = boxes.filter(b => b.checked);
    const load = chosen.reduce((s, b) => s + Number(b.dataset.weight), 0);
    const cap = Number(vehicle.selectedOptions[0]?.dataset.capacity || 0);
    const pct = cap ? Math.min(100, Math.round(100 * load / cap)) : 0;
    loadEl.textContent = `${load} kg of ${cap} kg`;
    bar.style.width = pct + '%';
    bar.style.background = load > cap ? 'var(--road)' : (pct > 85 ? 'var(--orange)' : 'var(--ink)');
    hint.textContent = chosen.length === 0 ? 'Select shipments to see the load.'
      : load > cap ? `Over capacity by ${load - cap} kg — choose a bigger vehicle or fewer shipments.`
      : `${chosen.length} shipment${chosen.length === 1 ? '' : 's'}, ${cap - load} kg spare.`;
    hint.style.color = load > cap ? 'var(--road)' : '';
    submit.disabled = chosen.length === 0 || load > cap || !vehicle.value;

    const seen = new Set();
    chosen.forEach(b => seen.add(`${Number(b.dataset.lat).toFixed(4)},${Number(b.dataset.lng).toFixed(4)}`));
    countEl.textContent = `${seen.size} stop${seen.size === 1 ? '' : 's'}`;
  }

  boxes.forEach(b => b.addEventListener('change', update));
  vehicle.addEventListener('change', update);
  form.querySelector('[data-select-all]').addEventListener('change', e => {
    boxes.forEach(b => { b.checked = e.target.checked; });
    update();
  });
  update();
})();
