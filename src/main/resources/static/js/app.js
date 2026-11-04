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
        "'": '&#39;',
    };
    return String(value ?? '').replace(/[&<>"']/g, (character) => entities[character]);
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
    const adminOnlyLinks = document.querySelectorAll('.navbar-nav a[href="/users"]');
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
                    const placeholder =
                        images[i].parentElement.querySelector('.nav-avatar-placeholder');
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
        const status = res.status;
        let raw = '';
        try {
            raw = await res.text();
        } catch (e) {
            /* ignore */
        }
        if (status === 401) {
            alert('Your session has expired. Please log in again.');
            window.location.href = '/login';
        }
        throw apiErrorFromResponse(status, raw);
    }
    const contentType = res.headers.get('content-type') || '';
    return contentType.includes('application/json') ? res.json() : res.text();
}

/**
 * Turn a failed API response into an Error with .status, .title (short human
 * summary), .details (array, from ApiError details[]), and .message (full text).
 * Handles plain-text/empty bodies and network failures (no status).
 */
function apiErrorFromResponse(status, raw) {
    const err = new Error();
    err.status = status;
    err.details = [];
    err.title = defaultErrorTitle(status);
    let message = '';
    if (raw) {
        try {
            const parsed = JSON.parse(raw);
            if (parsed && typeof parsed === 'object' && ('message' in parsed || 'error' in parsed || 'details' in parsed)) {
                message = parsed.message || parsed.error || '';
                if (Array.isArray(parsed.details)) {
                    err.details = parsed.details.map(String);
                }
            } else {
                message = String(raw);
            }
        } catch (e) {
            message = String(raw);
        }
    }
    err.message = (err.title ? err.title + ': ' : '') + (message || 'Something went wrong.');
    return err;
}

function defaultErrorTitle(status) {
    switch (status) {
        case 400:
            return 'Please fix the highlighted fields';
        case 401:
            return 'Your session has expired. Please log in again.';
        case 403:
            return 'You do not have permission to do that.';
        case 404:
            return 'That item could not be found.';
        case 409:
            return 'That change conflicts with existing data.';
        case 429:
            return 'Too many requests. Please wait a moment and try again.';
        case 0:
            return 'Network error. Please check your connection and try again.';
        default:
            return 'Something went wrong.';
    }
}

/** Show a parsed API error inside a modal form's error box (or alert as fallback). */
function showApiError(form, err, prefix) {
    const title = prefix || err.title || 'Something went wrong.';
    const lines = [title];
    if (err.details && err.details.length) {
        lines.push(err.details.join('\n'));
    }
    if (err.message && !err.message.startsWith(title)) {
        lines.push(err.message);
    }
    setModalError(form, lines.join('\n'));
}

// --- Confirm modal ---------------------------------------------------------
// Markup contract (layout.html): #confirmModal with #confirmTitle,
// #confirmMessage, #confirmActionBtn. Promise resolves true on confirm.
let confirmResolve = null;

function confirmAction(options) {
    const modalEl = document.getElementById('confirmModal');
    if (!modalEl) return Promise.resolve(window.confirm(options && options.message || options || 'Are you sure?'));
    const titleEl = document.getElementById('confirmTitle');
    const msgEl = document.getElementById('confirmMessage');
    const actionBtn = document.getElementById('confirmActionBtn');
    if (titleEl) titleEl.textContent = (options && options.title) || 'Confirm';
    if (msgEl) msgEl.textContent = (options && options.message) || 'Are you sure?';
    if (actionBtn) {
        actionBtn.textContent = (options && (options.confirmLabel || options.confirmText)) || 'Confirm';
        actionBtn.className = 'btn ' + ((options && options.danger) ? 'btn-danger' : 'btn-primary');
    }
    return new Promise(function (resolve) {
        confirmResolve = resolve;
        openModal(modalEl);
    });
}

function wireConfirmModal() {
    const modalEl = document.getElementById('confirmModal');
    if (!modalEl) return;
    const actionBtn = document.getElementById('confirmActionBtn');
    if (actionBtn) {
        actionBtn.addEventListener('click', function () {
            closeModal(modalEl);
            if (confirmResolve) {
                confirmResolve(true);
                confirmResolve = null;
            }
        });
    }
    modalEl.addEventListener('hidden.bs.modal', function () {
        if (confirmResolve) {
            confirmResolve(false);
            confirmResolve = null;
        }
    });
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', wireConfirmModal);
} else {
    wireConfirmModal();
}

function getBootstrapModal(element) {
    if (!element || !window.bootstrap || !window.bootstrap.Modal) return null;
    return window.bootstrap.Modal.getInstance(element) || new window.bootstrap.Modal(element);
}

function openModal(modalElement) {
    const modal = getBootstrapModal(modalElement);
    if (modal) modal.show();
}

function closeModal(modalElement) {
    const modal = getBootstrapModal(modalElement);
    if (modal) modal.hide();
}

function resetForm(form) {
    if (!form) return;
    form.reset();
    form.querySelectorAll('.is-invalid').forEach(function (el) {
        el.classList.remove('is-invalid');
    });
    form.querySelectorAll('.invalid-feedback').forEach(function (el) {
        el.textContent = '';
    });
    const errorBox = form.querySelector('.modal-error');
    if (errorBox) {
        errorBox.textContent = '';
        errorBox.style.display = 'none';
    }
}

function setModalError(form, message) {
    if (!form) return;
    const errorBox = form.querySelector('.modal-error');
    if (errorBox) {
        errorBox.textContent = message || 'Something went wrong. Please try again.';
        errorBox.style.display = 'block';
    } else if (message) {
        alert(message);
    }
}

function clearModalError(form) {
    if (!form) return;
    const errorBox = form.querySelector('.modal-error');
    if (errorBox) {
        errorBox.textContent = '';
        errorBox.style.display = 'none';
    }
}

function showPageFeedback(id, message, type) {
    const el = typeof id === 'string' ? document.getElementById(id) : id;
    if (!el) return;
    el.textContent = message;
    el.className = 'alert alert-' + (type || 'success');
    el.classList.remove('d-none');
    el.scrollIntoView({ block: 'nearest' });
}

function setButtonLoading(button, loading, loadingText) {
    if (!button) return;
    if (loading) {
        if (!button.dataset.originalHtml) button.dataset.originalHtml = button.innerHTML;
        button.disabled = true;
        button.innerHTML =
            '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>' +
            (loadingText || 'Saving...');
    } else {
        button.disabled = false;
        if (button.dataset.originalHtml) {
            button.innerHTML = button.dataset.originalHtml;
            delete button.dataset.originalHtml;
        }
    }
}

function clearFormValidation(form) {
    if (!form) return;
    form.querySelectorAll('.is-invalid').forEach(function (el) {
        el.classList.remove('is-invalid');
    });
    form.querySelectorAll('.invalid-feedback').forEach(function (el) {
        el.textContent = '';
    });
}

function markInvalid(field, message) {
    if (!field) return;
    field.classList.add('is-invalid');
    const parent = field.parentElement;
    let feedback = parent ? parent.querySelector('.invalid-feedback') : null;
    if (!feedback && parent) {
        feedback = document.createElement('div');
        feedback.className = 'invalid-feedback';
        field.insertAdjacentElement('afterend', feedback);
    }
    if (feedback) feedback.textContent = message || 'This field is required.';
}

function fillSelect(select, values, placeholder) {
    if (!select) return;
    const current = select.value;
    select.innerHTML = '';
    const placeholderOption = document.createElement('option');
    placeholderOption.value = '';
    placeholderOption.textContent = placeholder || 'Select an option';
    placeholderOption.disabled = true;
    placeholderOption.selected = true;
    placeholderOption.hidden = true;
    select.appendChild(placeholderOption);
    (values || []).forEach(function (value) {
        const option = document.createElement('option');
        option.value = value;
        option.textContent = value;
        select.appendChild(option);
    });
    if (current) select.value = current;
}

function ensureSelectOption(select, value) {
    if (!select || value === null || value === undefined || value === '') return;
    const exists = Array.prototype.some.call(select.options, function (option) {
        return option.value === String(value);
    });
    if (exists) return;
    const option = document.createElement('option');
    option.value = value;
    option.textContent = value;
    select.appendChild(option);
}

// --- Theme (dark/light) ----------------------------------------------------
// The <head> snippet sets data-bs-theme before paint; this wires the toggle.
function syncThemeToggleIcon() {
    const icon = document.getElementById('themeToggleIcon');
    if (!icon) return;
    const dark = document.documentElement.dataset.bsTheme === 'dark';
    icon.className = dark ? 'bi bi-sun' : 'bi bi-moon-stars';
}

function toggleTheme() {
    const root = document.documentElement;
    const next = root.dataset.bsTheme === 'dark' ? 'light' : 'dark';
    root.dataset.bsTheme = next;
    try {
        localStorage.setItem('theme', next);
    } catch (e) {
        /* storage unavailable; theme applies for this session only */
    }
    syncThemeToggleIcon();
}

(function wireThemeToggle() {
    function attach() {
        syncThemeToggleIcon();
        const toggle = document.getElementById('themeToggle');
        if (toggle) {
            toggle.addEventListener('click', toggleTheme);
        }
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', attach);
    } else {
        attach();
    }
})();
