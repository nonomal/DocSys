/**
 * DocSys Agent — Embedding Script
 *
 * 在 DocSys 页面底部注入此脚本，即可在左侧显示浮动对话窗口：
 *
 *   <script src="http://localhost:8110/js/docsys-embed.js"></script>
 *   <script>
 *     DocSysAgentEmbed.init({
 *       agentUrl: 'http://localhost:8110',  // DocSys Agent 地址
 *       collapsed: true                     // 默认收起
 *     });
 *   </script>
 *
 * 自动处理：
 *   - 加载 CSS / JS 资源
 *   - 建立 iframe
 *   - 暴露 DocSysAgentEmbed API
 */

(function () {
  'use strict';

  var DEFAULT_CONFIG = {
    agentUrl: (window.DOCSYS_AGENT_URL || 'http://localhost:8110').replace(/\/$/, ''),
    apiKey: (window.DOCSYS_AGENT_API_KEY || null),   // X-API-Key for cross-origin auth
    collapsed: true,
    position: 'left',   // 'left' | 'right'
    bottom: 16,
    side: 16,
  };

  var config = null;
  var initialized = false;

  // ── Load external resources ──────────────────────────────────────────────

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
    document.head.appendChild(script);
  }

  // ── Embed ─────────────────────────────────────────────────────────────────

  function embed() {
    if (initialized || !config) return;
    initialized = true;

    var base = config.agentUrl;

    loadCss(base + '/css/chat-widget.css');
    loadScript(base + '/js/chat-widget.js', function () {
      if (window.DSAWidget) {
        DSAWidget.init({
          apiBase: config.agentUrl + '/agent',
          apiKey: config.apiKey,
          collapsed: config.collapsed,
          welcomeMessage: '你好！我是 DocSys Agent。有什么可以帮助你的吗？'
        });
      }
    });
  }

  // ── Public API ───────────────────────────────────────────────────────────

  window.DocSysAgentEmbed = {
    init: function (opts) {
      config = Object.assign({}, DEFAULT_CONFIG, opts || {});
      if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', embed);
      } else {
        embed();
      }
      return this;
    },
    send: function (text) {
      if (window.DSAWidget) DSAWidget.send(text);
    },
    destroy: function () {
      if (window.DSAWidget) DSAWidget.destroy();
      initialized = false;
    },
  };

})();
