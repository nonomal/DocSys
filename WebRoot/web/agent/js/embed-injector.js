/**
 * DocSys Agent — Embed Injector (V3)
 *
 * Adaptive widget injection: works with or without reverse proxy,
 * with or without explicit configuration. Auto-discovers DocSystem login
 * state and Agent URL/API key from the Agent's /widget-config endpoint.
 *
 * ── Deployment modes ────────────────────────────────────────────────────────
 *
 * Mode 1 — Apache reverse proxy (production, recommended):
 *   <link rel="stylesheet" href="/css/embed-injector.css">
 *   <script src="/js/embed-injector.js"></script>
 *   <script>DocSysAgentEmbedV2.init({});</script>
 *
 * Mode 2 — Direct connection (development):
 *   <script>DocSysAgentEmbedV2.init({ agentUrl: 'http://localhost:8110' });</script>
 *
 * Mode 3 — Auto (no config needed):
 *   Auto-discovers from own script src, or falls back to /agent.
 *
 * ── User configuration priority ─────────────────────────────────────────────
 *   1. window.DOCSYS_AGENT_URL (override)
 *   2. .env DOCSYS_AGENT_URL   (via /widget-config endpoint)
 *   3. Auto-discovery from /js/embed-injector.js script src
 *   4. Fallback: http://localhost:8110
 *
 * ── Widget config endpoint response ─────────────────────────────────────────
 *   GET /widget-config → { agentUrl, apiKey, version, docsysUrl }
 */
(function () {
  'use strict';

  var CONFIG = {
    agentUrl: null,   // resolved dynamically below
    apiKey: null,
    collapsed: true,
    position: 'right',
    bottom: 16,
    side: 16,
    timeout: 5000,
  };

  // ── Step 1: Try explicit window.DOCSYS_AGENT_URL override ─────────────────
  if (window.DOCSYS_AGENT_URL) {
    CONFIG.agentUrl = window.DOCSYS_AGENT_URL.replace(/\/$/, '');
  }

  // ── Step 2: Discover from own script src ───────────────────────────────────
  (function resolveFromScriptSrc() {
    if (CONFIG.agentUrl) return;
    var scripts = document.querySelectorAll('script[src]');
    for (var i = 0; i < scripts.length; i++) {
      var src = scripts[i].src;
      var idx = src.indexOf('/js/embed-injector.js');
      if (idx !== -1) {
        CONFIG.agentUrl = src.substring(0, idx);
        return;
      }
    }
  })();

  // ── Step 3: Fallback ────────────────────────────────────────────────────────
  if (!CONFIG.agentUrl) {
    // Try the conventional proxy path; if that fails the config fetch will fix it
    CONFIG.agentUrl = 'http://localhost:8110';
  }

  // Will be updated after /widget-config response
  var _finalAgentUrl = null;

  var _cachedApiKey = null;
  var _cachedAgentUrl = null;
  var _user = null;

  // ─── Resource Loaders ───────────────────────────────────────────────────

  function loadCss(href) {
    if (document.querySelector('link[href="' + href + '"]')) return;
    var link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = href;
    document.head.appendChild(link);
  }

  function loadScript(src, onload) {
    if (document.querySelector('script[src="' + src + '"]')) { onload && onload(); return; }
    var script = document.createElement('script');
    script.src = src;
    script.onload = onload || function () {};
    script.onerror = function () {};
    document.head.appendChild(script);
  }

  // ─── Session Bridge ────────────────────────────────────────────────────

  /**
   * Check DocSystem login state by calling getLoginUser.do.
   * Uses withCredentials so the browser auto-sends the JSESSIONID cookie.
   * Returns { id, name, email } or null if not logged in.
   */
  function getDocSysLoginUser() {
    return new Promise(function (resolve) {
      var xhr = new XMLHttpRequest();
      xhr.open('POST', '/DocSystem/User/getLoginUser.do', true);
      xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
      xhr.withCredentials = true;
      xhr.timeout = CONFIG.timeout;

      xhr.onload = function () {
        if (xhr.status === 200) {
          try {
            var ret = JSON.parse(xhr.responseText);
            if (ret.status === 'ok' && ret.data && ret.data.id) {
              resolve({ id: ret.data.id, name: ret.data.name, email: ret.data.email || '' });
              return;
            }
          } catch (e) {}
        }
        resolve(null);
      };

      xhr.onerror = function () { resolve(null); };
      xhr.ontimeout = function () { resolve(null); };

      xhr.send('');
    });
  }

  // ─── Agent API Key Auto-Discovery ─────────────────────────────────────

  /**
   * Fetch the full widget config from the Agent's /widget-config endpoint.
   * Returns { agentUrl, apiKey, version, docsysUrl }.
   * Caches result in memory per page load.
   * Adaptive: if the endpoint is unreachable, uses the already-resolved CONFIG.agentUrl.
   */
  function fetchWidgetConfig() {
    return new Promise(function (resolve) {
      var xhr = new XMLHttpRequest();
      // Use the resolved agentUrl as base, add /agent if it's a plain host:port
      var base = CONFIG.agentUrl;
      var apiBase = (base.indexOf('/agent') !== -1) ? base : base + '/agent';
      xhr.open('GET', apiBase + '/widget-config', true);
      xhr.setRequestHeader('Accept', 'application/json');
      xhr.timeout = CONFIG.timeout;

      xhr.onload = function () {
        if (xhr.status === 200) {
          try {
            var resp = JSON.parse(xhr.responseText);
            var data = resp.data || {};
            _cachedAgentUrl = data.agentUrl || base;
            _cachedApiKey = data.apiKey || null;
            resolve({
              agentUrl: _cachedAgentUrl,
              apiKey: _cachedApiKey,
              version: data.version || '1.0.0',
              docsysUrl: data.docsysUrl || null
            });
            return;
          } catch (e) {}
        }
        // Fallback: use pre-resolved values
        resolve({ agentUrl: base, apiKey: _cachedApiKey, version: '1.0.0', docsysUrl: null });
      };

      xhr.onerror = function () {
        resolve({ agentUrl: base, apiKey: _cachedApiKey, version: '1.0.0', docsysUrl: null });
      };
      xhr.ontimeout = function () {
        resolve({ agentUrl: base, apiKey: _cachedApiKey, version: '1.0.0', docsysUrl: null });
      };

      xhr.send();
    });
  }

  // ─── Error Toast (Agent Unreachable) ───────────────────────────────────

  function showErrorToast(message) {
    var existing = document.getElementById('dsa-embed-error');
    if (existing) return;

    var toast = document.createElement('div');
    toast.id = 'dsa-embed-error';
    toast.style.cssText = [
      'position:fixed', 'top:80px', 'right:20px', 'z-index:2147483647',
      'background:#fdecea', 'color:#c62828', 'border:1px solid #ef9a9a',
      'border-radius:8px', 'padding:10px 16px', 'font-family:Verdana,sans-serif',
      'font-size:13px', 'max-width:280px', 'box-shadow:0 2px 8px rgba(0,0,0,0.15)',
      'animation:dsa-slide-in 0.3s ease forwards'
    ].join(';');
    toast.textContent = 'DocSys Agent: ' + message;
    document.body.appendChild(toast);

    setTimeout(function () {
      if (document.getElementById('dsa-embed-error')) {
        document.getElementById('dsa-embed-error').remove();
      }
    }, 8000);
  }

  // ─── Widget Initialization ─────────────────────────────────────────────

  function initWidget(user, apiKey, resolvedAgentUrl) {
    _user = user;

    // resolvedAgentUrl may be a path (/agent) or a full URL (http://localhost:8110)
    var apiBase = (resolvedAgentUrl.indexOf('/agent') !== -1)
        ? resolvedAgentUrl
        : resolvedAgentUrl + '/agent';
    var cssBase = (resolvedAgentUrl.indexOf('/agent') !== -1)
        ? resolvedAgentUrl.replace(/\/agent\/?$/, '')
        : resolvedAgentUrl;
    var sessionId = user ? 'docsys_' + user.id : null;

    // Load theme CSS first (before widget so theme applies)
    loadCss(cssBase + '/css/embed-injector.css');
    loadCss(cssBase + '/css/chat-widget.css');

    loadScript(cssBase + '/js/chat-widget.js', function () {
      if (window.DSAWidget) {
        DSAWidget.init({
          apiBase: apiBase,
          apiKey: apiKey || '',
          collapsed: CONFIG.collapsed,
          welcomeMessage: user
            ? '你好 ' + user.name + '！我是 DocSys Agent。有什么可以帮助你的吗？'
            : '你好！我是 DocSys Agent。有什么可以帮助你的吗？',
          sessionId: sessionId
        });

        // Report docsys user to Agent backend (fire-and-forget)
        if (user) {
          var infoXhr = new XMLHttpRequest();
          infoXhr.open('POST', apiBase + '/docsys-user', true);
          infoXhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
          if (apiKey) infoXhr.setRequestHeader('X-API-Key', apiKey);
          infoXhr.send('userId=' + encodeURIComponent(user.id) +
            '&userName=' + encodeURIComponent(user.name || '') +
            '&userEmail=' + encodeURIComponent(user.email || ''));
        }
      } else {
        showErrorToast('Widget 脚本加载失败，请检查 Agent 服务。');
      }
    });
  }

  function initWidgetWithErrorFallback(user, apiKey, resolvedAgentUrl) {
    try {
      initWidget(user, apiKey, resolvedAgentUrl);
    } catch (err) {
      showErrorToast('Widget 初始化失败，请检查 Agent 服务。');
    }
  }

  // ─── Public API ─────────────────────────────────────────────────────────

  var _initialized = false;

  window.DocSysAgentEmbedV2 = {
    init: function (opts) {
      if (_initialized) return this;
      _initialized = true;

      Object.keys(opts || {}).forEach(function (k) {
        if (k in CONFIG) CONFIG[k] = opts[k];
      });

      // Parallel: fetch DocSystem login state + widget config
      Promise.all([getDocSysLoginUser(), fetchWidgetConfig()])
        .then(function (results) {
          var user = results[0];
          var cfg = results[1];
          initWidgetWithErrorFallback(user, cfg.apiKey, cfg.agentUrl);
        })
        .catch(function () {
          initWidgetWithErrorFallback(null, null);
        });

      return this;
    },

    isUserLoggedIn: function () {
      return !!_user;
    },

    getCurrentUser: function () {
      return _user ? Object.assign({}, _user) : null;
    }
  };

  // Auto-init when DOM is ready (default behavior)
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      window.DocSysAgentEmbedV2.init({});
    });
  } else {
    window.DocSysAgentEmbedV2.init({});
  }

})();