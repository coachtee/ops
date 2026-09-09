/*
 * OPS admin panel behaviour. Deliberately small and dependency-free — the
 * panel is server-rendered, so JS only handles what HTML can't: the mobile
 * nav drawer, dropdown menus, the dark-mode toggle, and making whole table
 * rows clickable.
 */
(function () {
	'use strict';

	/* ---- Mobile nav drawer ---- */
	function setNav(open) {
		document.body.classList.toggle('nav-open', open);
	}
	document.addEventListener('click', function (e) {
		if (e.target.closest('[data-nav-open]')) { setNav(true); }
		else if (e.target.closest('[data-nav-close]')) { setNav(false); }
	});

	/* ---- Dropdown menus ---- */
	document.addEventListener('click', function (e) {
		var toggle = e.target.closest('[data-menu-toggle]');
		var openMenus = document.querySelectorAll('[data-menu].open');

		for (var i = 0; i < openMenus.length; i++) {
			if (!toggle || openMenus[i] !== toggle.closest('[data-menu]')) {
				openMenus[i].classList.remove('open');
				var btn = openMenus[i].querySelector('[data-menu-toggle]');
				if (btn) { btn.setAttribute('aria-expanded', 'false'); }
			}
		}
		if (toggle) {
			var menu = toggle.closest('[data-menu]');
			var nowOpen = !menu.classList.contains('open');
			menu.classList.toggle('open', nowOpen);
			toggle.setAttribute('aria-expanded', String(nowOpen));
		}
	});

	document.addEventListener('keydown', function (e) {
		if (e.key !== 'Escape') { return; }
		setNav(false);
		var openMenus = document.querySelectorAll('[data-menu].open');
		for (var i = 0; i < openMenus.length; i++) { openMenus[i].classList.remove('open'); }
	});

	/* ---- Dark mode ---- */
	function currentTheme() {
		var stored;
		try { stored = localStorage.getItem('ops-theme'); } catch (e) { stored = null; }
		if (stored) { return stored; }
		return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
	}

	function paintThemeIcons() {
		var dark = currentTheme() === 'dark';
		var light = document.querySelectorAll('[data-theme-icon-light]');
		var darkEls = document.querySelectorAll('[data-theme-icon-dark]');
		for (var i = 0; i < light.length; i++) { light[i].classList.toggle('hide', dark); }
		for (var j = 0; j < darkEls.length; j++) { darkEls[j].classList.toggle('hide', !dark); }
	}

	document.addEventListener('click', function (e) {
		if (!e.target.closest('[data-theme-toggle]')) { return; }
		var next = currentTheme() === 'dark' ? 'light' : 'dark';
		document.documentElement.setAttribute('data-theme', next);
		try { localStorage.setItem('ops-theme', next); } catch (err) { /* private mode — session-only is fine */ }
		paintThemeIcons();
	});

	paintThemeIcons();

	/* ---- Clickable rows: the whole row is the hit target, but a real link
	   inside it (or a modifier-click for a new tab) still wins. ---- */
	document.addEventListener('click', function (e) {
		var row = e.target.closest('tr[data-href]');
		if (!row || e.target.closest('a, button, input, label')) { return; }
		if (e.metaKey || e.ctrlKey || e.shiftKey || e.button !== 0) { return; }
		window.location = row.getAttribute('data-href');
	});

	/* ---- Filter selects submit their form on change (no Apply button) ---- */
	document.addEventListener('change', function (e) {
		if (e.target.matches('[data-autosubmit]') && e.target.form) { e.target.form.submit(); }
	});
})();
