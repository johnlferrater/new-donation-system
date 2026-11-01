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
function profilePictureUrl() {
  return '/api/users/me/profile-picture?t=' + Date.now();
}

function applyRoleNavigation(user) {
  if (user && user.role === 'ADMIN') return;
  const adminOnlyLinks = document.querySelectorAll(
    '.navbar-nav a[href="/users"], .navbar-nav a[href="/donation-records"]');
  for (let i = 0; i < adminOnlyLinks.length; i++) {
    const item = adminOnlyLinks[i].closest('.nav-item') || adminOnlyLinks[i];
    item.style.display = 'none';
  }
}

function loadNavAvatar() {
  return fetch('/api/users/me')
    .then(function (response) {
      if (!response.ok) return null;
      return response.json();
    })
    .then(function (user) {
      if (!user) return null;

      applyRoleNavigation(user);

      const name = document.getElementById('navAccountName');
      if (name) name.textContent = user.username;
      const header = document.getElementById('navAccountHeader');
      if (header) header.textContent = user.username;

      if (user.profilePicture) {
        const images = document.getElementsByClassName('nav-avatar');
        for (let i = 0; i < images.length; i++) {
          images[i].src = profilePictureUrl();
          images[i].style.display = 'inline-block';
          const placeholder = images[i].parentElement.querySelector('.nav-avatar-placeholder');
          if (placeholder) placeholder.style.display = 'none';
        }
      }
      return user;
    })
    .catch(function () {
      return null;
    });
}

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
