/* Public tracking: the map (the road still to go, the truck's rough area, the destination) and the help options (one panel open at a time). */
(function () {
  'use strict';

  // ------------------------------------------------------------------ help options
  const help = document.querySelector('[data-help]');
  if (help) {
    const tabs = [...help.querySelectorAll('[data-help-tab]')];
    const panels = [...help.querySelectorAll('[data-help-panel]')];
    function open(name, focus) {
      tabs.forEach(t => {
        const on = t.dataset.helpTab === name;
        t.classList.toggle('on', on);
        t.setAttribute('aria-selected', String(on));
      });
      panels.forEach(p => { p.hidden = p.dataset.helpPanel !== name; });
      if (focus) {
        const panel = panels.find(p => p.dataset.helpPanel === name);
        panel?.querySelector('textarea, input:not([type=hidden]), a.btn')?.focus();
      }
    }
    tabs.forEach(t => t.addEventListener('click', () => {
      const already = t.classList.contains('on');
      if (already) {
        t.classList.remove('on');
        panels.forEach(p => { p.hidden = true; });
      } else {
        open(t.dataset.helpTab, true);
      }
    }));
    if (help.dataset.open) {
      open(help.dataset.open, false);
      help.scrollIntoView({ block: 'start' });
    }

    // Character count for the note.
    help.querySelectorAll('[data-count]').forEach(area => {
      const out = document.querySelector(area.dataset.count);
      const max = Number(area.getAttribute('maxlength'));
      const update = () => { out.textContent = area.value.length + ' / ' + max; };
      area.addEventListener('input', update);
      update();
    });
  }
})();
