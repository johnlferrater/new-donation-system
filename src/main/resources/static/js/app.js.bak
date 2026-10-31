// ===== Donation System - shared client helpers =====

/** Read the CSRF token that Spring Security exposes via meta tags. */
function csrfToken() {
  const el = document.querySelector('meta[name="_csrf"]');
  return el ? el.getAttribute('content') : null;
}

function csrfHeaderName() {
  const el = document.querySelector('meta[name="_csrf_header"]');
  return el ? el.getAttribute('content') : 'X-XSRF-TOKEN';
}

/**
 * Escape untrusted values before placing them in an HTML string.
 * Prefer textContent for new DOM rendering code.
 */
function escapeHtml(value) {
  const entities = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  };
  return String(value ?? '').replace(/[&<>"']/g, character => entities[character]);
}

/**
 * fetch() wrapper that automatically attaches the CSRF token and JSON headers.
 * Throws an Error with the response body text when the request fails.
 */
async function apiFetch(url, options = {}) {
  const opts = Object.assign({}, options);
  opts.headers = Object.assign({}, options.headers || {});

  const token = csrfToken();
  if (token) {
    opts.headers[csrfHeaderName()] = token;
  }
  if (opts.body && !opts.headers['Content-Type']) {
    opts.headers['Content-Type'] = 'application/json';
  }

  const res = await fetch(url, opts);
  if (!res.ok) {
    let message = res.status + ' ' + res.statusText;
    try {
      const text = await res.text();
      if (text) message = text;
    } catch (e) { /* ignore */ }
    if (res.status === 401) {
      alert('Your session has expired. Please log in again.');
      window.location.href = '/login';
      return Promise.reject(new Error(message));
    }
    throw new Error(message);
  }
  const contentType = res.headers.get('content-type') || '';
  return contentType.includes('application/json') ? res.json() : res.text();
}
