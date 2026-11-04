// ===== Notification bell =====

(function () {
    const POLL_INTERVAL_MS = 30000;
    const bell = document.getElementById('notificationBell');
    const badge = document.getElementById('notificationBadge');
    const list = document.getElementById('notificationList');
    const markAllButton = document.getElementById('notificationMarkAll');

    if (!bell || !badge || !list || typeof apiFetch !== 'function') return;

    let pollTimer = null;

    function safeLink(value) {
        return typeof value === 'string' &&
            value.startsWith('/') &&
            !value.startsWith('//') &&
            !value.includes('://')
            ? value
            : '';
    }

    function formatRelativeTime(value) {
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return '';

        const seconds = Math.floor((Date.now() - date.getTime()) / 1000);
        if (seconds < 60) return 'Just now';
        if (seconds < 3600) return Math.floor(seconds / 60) + 'm ago';
        if (seconds < 86400) return Math.floor(seconds / 3600) + 'h ago';
        if (seconds < 604800) return Math.floor(seconds / 86400) + 'd ago';
        return date.toLocaleDateString();
    }

    function updateBadge(count) {
        const unread = Number(count) || 0;
        badge.textContent = unread > 9 ? '9+' : String(unread);
        badge.classList.toggle('d-none', unread === 0);
        bell.setAttribute(
            'aria-label',
            unread > 0 ? 'Notifications, ' + unread + ' unread' : 'Notifications'
        );
    }

    function renderEmpty() {
        list.replaceChildren();
        const empty = document.createElement('div');
        empty.className = 'notification-empty';
        empty.textContent = 'No notifications yet';
        list.appendChild(empty);
    }

    function renderNotifications(notifications) {
        list.replaceChildren();
        if (!notifications || notifications.length === 0) {
            renderEmpty();
            return;
        }

        notifications.forEach(function (notification) {
            const link = safeLink(notification.link);
            const item = document.createElement('a');
            item.className =
                'dropdown-item notification-item' +
                (notification.read ? '' : ' notification-item--unread');
            item.href = link || '#';
            item.dataset.notificationId = notification.id;
            item.dataset.link = link;

            const title = document.createElement('div');
            title.className = 'notification-item__title';

            if (!notification.read) {
                const dot = document.createElement('span');
                dot.className = 'notification-item__unread-dot';
                dot.setAttribute('aria-label', 'Unread');
                title.appendChild(dot);
            }

            const titleText = document.createElement('span');
            titleText.textContent = notification.title || 'Notification';
            title.appendChild(titleText);

            const message = document.createElement('div');
            message.className = 'notification-item__message';
            message.textContent = notification.message || '';

            const time = document.createElement('div');
            time.className = 'notification-item__time';
            time.textContent = formatRelativeTime(notification.createdAt);

            item.appendChild(title);
            item.appendChild(message);
            item.appendChild(time);
            list.appendChild(item);
        });
    }

    async function refreshUnreadCount() {
        try {
            const data = await apiFetch('/api/notifications/unread-count');
            updateBadge(data ? data.count : 0);
        } catch (error) {
            console.warn('Failed to load notification count:', error);
        }
    }

    async function refreshList() {
        try {
            const notifications = await apiFetch('/api/notifications');
            renderNotifications(notifications || []);
        } catch (error) {
            list.replaceChildren();
            const failed = document.createElement('div');
            failed.className = 'notification-empty text-danger';
            failed.textContent = 'Failed to load notifications.';
            list.appendChild(failed);
        }
    }

    async function markRead(notificationId) {
        await apiFetch('/api/notifications/' + encodeURIComponent(notificationId) + '/read', {
            method: 'POST',
        });
    }

    list.addEventListener('click', async function (event) {
        const item = event.target.closest('[data-notification-id]');
        if (!item) return;
        event.preventDefault();

        const notificationId = item.dataset.notificationId;
        const destination = safeLink(item.dataset.link);

        try {
            await markRead(notificationId);
        } catch (error) {
            console.warn('Failed to mark notification as read:', error);
        }

        if (destination) {
            window.location.assign(destination);
            return;
        }

        await Promise.all([refreshList(), refreshUnreadCount()]);
    });

    markAllButton.addEventListener('click', async function () {
        markAllButton.disabled = true;
        try {
            await apiFetch('/api/notifications/read-all', { method: 'POST' });
            await Promise.all([refreshList(), refreshUnreadCount()]);
        } catch (error) {
            console.warn('Failed to mark notifications as read:', error);
        } finally {
            markAllButton.disabled = false;
        }
    });

    bell.addEventListener('show.bs.dropdown', function () {
        refreshList();
    });

    function stopPolling() {
        if (pollTimer !== null) {
            window.clearInterval(pollTimer);
            pollTimer = null;
        }
    }

    function startPolling() {
        stopPolling();
        pollTimer = window.setInterval(function () {
            if (!document.hidden) refreshUnreadCount();
        }, POLL_INTERVAL_MS);
    }

    document.addEventListener('visibilitychange', function () {
        if (document.hidden) {
            stopPolling();
        } else {
            refreshUnreadCount();
            startPolling();
        }
    });

    refreshUnreadCount();
    startPolling();
})();
