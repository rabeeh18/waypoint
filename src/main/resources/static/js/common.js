/* Shared behaviour for signed-in pages: API calls, live connection,
   toasts and the live connection. Exposes window.Fleet. */
(function () {
  'use strict';

  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
  const TZ = 'Asia/Kolkata';
  const timeFmt = new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit', hour12: false, timeZone: TZ });
  const dateFmt = new Intl.DateTimeFormat('en-GB', { weekday: 'short', day: 'numeric', month: 'short', timeZone: TZ });

  async function api(method, url, body) {
    const res = await fetch(url, {
      method,
      headers: Object.assign({ 'Accept': 'application/json' },
        body !== undefined ? { 'Content-Type': 'application/json' } : {},
        method !== 'GET' ? { [csrfHeader]: csrfToken } : {}),
      body: body !== undefined ? JSON.stringify(body) : undefined,
      credentials: 'same-origin'
    });
    if (res.status === 401 || res.redirected && res.url.includes('/login')) {
      location.href = '/login';
      throw new Error('Signed out');
    }
    const data = res.headers.get('content-type')?.includes('json') ? await res.json() : null;
    if (!res.ok) {
      throw new Error(data?.error || ('Request failed (' + res.status + ')'));
    }
    return data;
  }

  /** Accepts epoch millis or ISO strings (Jackson writes Instants as ISO). */
  function toMs(v) {
    if (v === null || v === undefined || v === '') return null;
    if (typeof v === 'number') return v;
    const n = Date.parse(v);
    return Number.isNaN(n) ? null : n;
  }
  const fmtTime = v => { const ms = toMs(v); return ms === null ? '—' : timeFmt.format(ms); };
  const fmtDelta = m => m === null || m === undefined ? '—' : (m === 0 ? 'on plan' : (m > 0 ? '+' : '−') + Math.abs(m) + ' min');
  const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const icon = (name, cls) => `<svg class="icon ${cls || ''}" aria-hidden="true"><use href="/img/icons.svg#i-${name}"></use></svg>`;
  /** A trip's light: On track, Watch or Needs action; the risk points are in the tooltip. */
  // ------------------------------------------------------------------ toasts

  const toastStack = document.querySelector('[data-toasts]');
  function toast(title, body, severity, link) {
    if (!toastStack) return;
    const el = document.createElement(link ? 'a' : 'div');
    el.className = 'toast';
    if (link) { el.href = link; el.style.display = 'block'; el.style.color = 'inherit'; el.style.textDecoration = 'none'; }
    el.dataset.severity = severity || '';
    el.innerHTML = `<strong>${esc(title)}</strong>${body ? `<div class="small muted">${esc(body)}</div>` : ''}`;
    toastStack.appendChild(el);
    setTimeout(() => el.remove(), 8000);
    while (toastStack.children.length > 4) toastStack.firstChild.remove();
  }

  // ------------------------------------------------------------------ live connection

  const handlers = {};
  let stomp = null;
  function subscribe(topic, fn) {
    (handlers[topic] = handlers[topic] || []).push(fn);
    if (stomp && stomp.connected) stomp.subscribe(topic, m => fn(JSON.parse(m.body)));
  }
  function connect() {
    if (!window.StompJs || !clockEl) return;
    const proto = location.protocol === 'https:' ? 'wss' : 'ws';
    stomp = new StompJs.Client({
      brokerURL: `${proto}://${location.host}/ws`,
      connectHeaders: { [csrfHeader]: csrfToken },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000
    });
    stomp.onConnect = () => {
      document.documentElement.dataset.live = 'on';
      Object.entries(handlers).forEach(([topic, fns]) => stomp.subscribe(topic, m => {
        const data = JSON.parse(m.body);
        fns.forEach(fn => fn(data));
      }));
    };
    stomp.onWebSocketClose = () => { document.documentElement.dataset.live = 'off'; };
    stomp.activate();
  }


  // Rows that link somewhere.
  document.addEventListener('click', e => {
    const row = e.target.closest('tr[data-href]');
    if (row && !e.target.closest('a, button, input, select, label')) location.href = row.dataset.href;
  });
  // Selects that apply immediately (no inline handlers: the CSP forbids them).
  document.addEventListener('change', e => {
    if (e.target.matches('[data-autosubmit]')) e.target.form.submit();
  });
  // Confirm before destructive form posts.
  document.addEventListener('submit', e => {
    const msg = e.target.dataset.confirm;
    if (msg && !confirm(msg)) e.preventDefault();
  });

  window.Fleet = { api, toMs, fmtTime, fmtDelta, esc, icon, toast, subscribe, csrfToken, csrfHeader };
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', connect);
  } else {
    connect();
  }
})();
