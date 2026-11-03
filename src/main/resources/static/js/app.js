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
    const adminOnlyLinks = document.querySelectorAll(
        '.navbar-nav a[href="/users"], .navbar-nav a[href="/donation-records"]'
    );
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
        let message = res.status + ' ' + res.statusText;
        try {
            const text = await res.text();
            if (text) message = text;
        } catch (e) {
            /* ignore */
        }
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
