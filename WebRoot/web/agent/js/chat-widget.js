/**
 * DocSys Agent — Chat Widget Logic
 *
 * Usage (iframe embed):
 *   <iframe src="/static/chat-widget.html" id="dsa-iframe"></iframe>
 *
 * Usage (direct include):
 *   <link rel="stylesheet" href="/static/css/chat-widget.css">
 *   <script src="/static/js/chat-widget.js"></script>
 *   <div id="dsa-widget-root"></div>
 *   <script>DSAWidget.init({ apiBase: '/agent', sessionId: '...' });</script>
 *
 * SSE stream events:
 *   data: {"type":"start"}
 *   data: {"type":"chunk","content":"你"}
 *   data: {"type":"done","fullContent":"..."}
 *   data: {"type":"error","message":"..."}
 */

(function (global) {
  'use strict';

  // ─── SVG Icons ────────────────────────────────────────────────────────────

  const ICONS = {
    robot: `<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path d="M12 2a2 2 0 0 1 2 2v1h3a2 2 0 0 1 2 2v3h1a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h1V7a2 2 0 0 1 2-2h3V4a2 2 0 0 1 2-2zm-3 9a1 1 0 1 0 0 2 1 1 0 0 0 0-2zm6 0a1 1 0 1 0 0 2 1 1 0 0 0 0-2zm-3 5a3 3 0 0 1 3 3v1H6v-1a3 3 0 0 1 3-3z"/>
    </svg>`,
    send: `<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
    </svg>`,
    close: `<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
    </svg>`,
    collapse: `<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
    </svg>`,
    expand: `<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path d="M12 2a2 2 0 0 1 2 2v1h3a2 2 0 0 1 2 2v3h1a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h1V7a2 2 0 0 1 2-2h3V4a2 2 0 0 1 2-2z"/>
    </svg>`,
  };

  // ─── Default Config ────────────────────────────────────────────────────────

  const DEFAULT_CONFIG = {
    apiBase: '/DocSystem/agent',
    apiKey: null,        // X-API-Key for cross-origin requests (embed in DocSys page)
    sessionId: null,
    welcomeMessage: '你好！我是 DocSys Agent。有什么可以帮助你的吗？',
    collapsed: true,
    onMessage: null,     // callback({ role, content })
    onError: null,       // callback(error)
    onStateChange: null, // callback(state)
  };

  // ─── Widget State ─────────────────────────────────────────────────────────

  let config = null;
  let state = {
    collapsed: true,
    connected: false,
    sending: false,
    sessionId: null,
  };
  let eventSource = null;
  let currentController = null;

  // ─── DOM References ──────────────────────────────────────────────────────

  let root = null;        // #dsa-widget
  let collapsedBtn = null;
  let messagesEl = null;
  let inputEl = null;
  let sendBtn = null;
  let statusDot = null;
  let typingEl = null;

  // ─── Utilities ─────────────────────────────────────────────────────────────

  function generateSessionId() {
    return 'w_' + Math.random().toString(36).substr(2, 9) + '_' + Date.now().toString(36);
  }

  function escHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function setState(updates) {
    Object.assign(state, updates);
    if (config.onStateChange) config.onStateChange(state);
    updateStatusDot();
  }

  function updateStatusDot() {
    if (!statusDot) return;
    statusDot.className = '';
    if (state.collapsed) return;
    if (state.sending) statusDot.className = 'connecting';
    else if (state.connected) statusDot.className = 'connected';
    else statusDot.className = 'error';
  }

  // ─── Message Rendering ────────────────────────────────────────────────────

  function appendMessage(role, content, extraClass) {
    const msg = document.createElement('div');
    msg.className = 'dsa-msg ' + (role === 'user' ? 'user' : 'agent') + (extraClass ? ' ' + extraClass : '');
    // Basic markdown-lite: code blocks
    const escaped = escHtml(content);
    const withCode = escaped
      .replace(/```([\s\S]*?)```/g, '<pre>$1</pre>')
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/\n/g, '<br>');
    msg.innerHTML = withCode;
    messagesEl.appendChild(msg);
    scrollBottom();
    return msg;
  }

  function appendSystem(content) {
    const msg = document.createElement('div');
    msg.className = 'dsa-msg system';
    msg.textContent = content;
    messagesEl.appendChild(msg);
    scrollBottom();
  }

  function showTyping() {
    if (typingEl) return;
    typingEl = document.createElement('div');
    typingEl.className = 'dsa-typing';
    typingEl.innerHTML = '<span></span><span></span><span></span>';
    messagesEl.appendChild(typingEl);
    scrollBottom();
  }

  function hideTyping() {
    if (typingEl) {
      typingEl.remove();
      typingEl = null;
    }
  }

  function scrollBottom() {
    messagesEl.scrollTop = messagesEl.scrollHeight;
  }

  // ─── SSE Communication ────────────────────────────────────────────────────

  function connectSSE(command) {
    disconnectSSE();

    const sid = state.sessionId || generateSessionId();
    state.sessionId = sid;
    setState({ connected: false, sending: true });

    const url = config.apiBase + '/stream?command=' + encodeURIComponent(command) + '&sessionId=' + encodeURIComponent(sid);

    // Use XMLHttpRequest for streaming — supports custom headers (X-API-Key) and CORS
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    // Send DocSystem JSESSIONID cookie for session auth (required for cross-origin embed)
    xhr.withCredentials = true;
    xhr.setRequestHeader('Accept', 'text/event-stream');
    xhr.setRequestHeader('Cache-Control', 'no-cache');
    if (config.apiKey) {
      xhr.setRequestHeader('X-API-Key', config.apiKey);
    }
    // Track last position in responseText for incremental parsing
    var lastPos = 0;

    xhr.onprogress = function () {
      var text = xhr.responseText;
      // Parse SSE lines: "data: {...}" or "data: raw text"
      var lines = text.substring(lastPos).split('\n');
      lastPos = text.length;
      for (var i = 0; i < lines.length - 1; i++) {
        var line = lines[i].trim();
        if (line.indexOf('data:') === 0) {
          var data = line.substring(5).trim();
          try {
            var payload = JSON.parse(data);
            handleStreamEvent(payload);
          } catch (err) {
            // plain text fallback
            handleStreamEvent({ type: 'chunk', content: data });
          }
        }
      }
    };

    xhr.onload = function () {
      hideTyping();
      setState({ connected: false, sending: false });
      if (xhr.status >= 200 && xhr.status < 300) {
        // Done — handled by 'done' event in onprogress
      } else {
        try {
          var err = JSON.parse(xhr.responseText);
          appendMessage('agent', '错误 (' + xhr.status + '): ' + (err.message || err.data || '未知错误'), 'error');
        } catch (_) {
          appendMessage('agent', '连接错误 (HTTP ' + xhr.status + ')，请检查 Agent 服务是否可用。', 'error');
        }
      }
    };

    xhr.onerror = function () {
      hideTyping();
      setState({ connected: false, sending: false });
      if (config.onError) config.onError(new Error('SSE connection error'));
      appendMessage('agent', '网络错误，请检查 Agent 服务是否可用。', 'error');
    };

    xhr.send(null);
    currentController = xhr;
  }

  function disconnectSSE() {
    if (eventSource) {
      eventSource.close();
      eventSource = null;
    }
    if (currentController) {
      currentController.abort();
      currentController = null;
    }
  }

  function handleStreamEvent(payload) {
    switch (payload.type) {
      case 'start':
        showTyping();
        break;
      case 'chunk':
        // Streaming chunk — update the last agent message incrementally
        if (!typingEl) showTyping();
        updateAgentMessage(payload.content);
        break;
      case 'done':
        hideTyping();
        finalizeAgentMessage(payload.fullContent || '');
        setState({ sending: false });
        break;
      case 'error':
        hideTyping();
        appendMessage('agent', '错误: ' + (payload.message || '未知错误'), 'error');
        setState({ sending: false });
        break;
      default:
        break;
    }
  }

  let agentMsgEl = null;
  let agentText = '';

  function updateAgentMessage(chunk) {
    if (!agentMsgEl) {
      agentMsgEl = document.createElement('div');
      agentMsgEl.className = 'dsa-msg agent';
      messagesEl.appendChild(agentMsgEl);
    }
    agentText += chunk;
    const escaped = escHtml(agentText)
      .replace(/```([\s\S]*?)```/g, '<pre>$1</pre>')
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/\n/g, '<br>');
    agentMsgEl.innerHTML = escaped;
    scrollBottom();
  }

  function finalizeAgentMessage(fullContent) {
    agentMsgEl = null;
    agentText = '';
    if (config.onMessage) config.onMessage({ role: 'agent', content: fullContent });
  }

  // ─── Send Message ──────────────────────────────────────────────────────────

  function sendMessage() {
    const text = inputEl.value.trim();
    if (!text || state.sending) return;

    appendMessage('user', text);
    if (config.onMessage) config.onMessage({ role: 'user', content: text });

    inputEl.value = '';
    inputEl.style.height = 'auto';

    connectSSE(text);
  }

  // ─── Keyboard Handling ─────────────────────────────────────────────────────

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  }

  function autoResize(el) {
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 90) + 'px';
  }

  // ─── Drag Support ─────────────────────────────────────────────────────────

  function makeDraggable(headerEl) {
    let dragging = false;
    let startX, startY, origX, origY;

    headerEl.addEventListener('mousedown', function (e) {
      if (e.target.closest('.dsa-header-btns')) return;
      dragging = true;
      startX = e.clientX;
      startY = e.clientY;
      var rect = root.getBoundingClientRect();
      origX = rect.left;
      origY = rect.top;
      document.body.style.userSelect = 'none';
    });

    document.addEventListener('mousemove', function (e) {
      if (!dragging) return;
      var dx = e.clientX - startX;
      var dy = e.clientY - startY;
      root.style.left = (origX + dx) + 'px';
      root.style.top = (origY + dy) + 'px';
      root.style.right = 'auto';
      root.style.bottom = 'auto';
    });

    document.addEventListener('mouseup', function () {
      if (dragging) {
        dragging = false;
        document.body.style.userSelect = '';
      }
    });
  }

  // ─── Expand / Collapse ─────────────────────────────────────────────────────

  function expand() {
    setState({ collapsed: false });
    if (collapsedBtn) collapsedBtn.style.display = 'none';
    if (root) {
      root.style.display = 'flex';
      root.classList.remove('dsa-closing');
      root.classList.add('dsa-opening');
    }
    // Auto-focus input
    setTimeout(function () { if (inputEl) inputEl.focus(); }, 100);
  }

  function collapse() {
    setState({ collapsed: true });
    disconnectSSE();
    hideTyping();
    if (root) {
      root.classList.remove('dsa-opening');
      root.classList.add('dsa-closing');
      setTimeout(function () {
        root.style.display = 'none';
        if (collapsedBtn) collapsedBtn.style.display = 'flex';
      }, 250);
    }
  }

  function toggle() {
    if (state.collapsed) expand();
    else collapse();
  }

  // ─── Build DOM ────────────────────────────────────────────────────────────

  function buildDOM() {
    root = document.createElement('div');
    root.id = 'dsa-widget';
    root.style.display = 'none';

    root.innerHTML = [
      '<div id="dsa-header">',
      '  <div id="dsa-header-title">',
            ICONS.robot,
      '    DocSys Agent',
      '    <div id="dsa-status-dot"></div>',
      '  </div>',
      '  <div class="dsa-header-btns">',
      '    <button class="dsa-hbtn" id="dsa-collapse-btn" title="收起">',
              ICONS.close,
      '    </button>',
      '  </div>',
      '</div>',
      '<div id="dsa-messages"></div>',
      '<div id="dsa-input-area">',
      '  <textarea id="dsa-input" rows="1" placeholder="输入消息，按 Enter 发送..."></textarea>',
      '  <button id="dsa-send-btn" title="发送">',
              ICONS.send,
      '  </button>',
      '</div>',
    ].join('');

    document.body.appendChild(root);

    // Collapsed button
    collapsedBtn = document.createElement('button');
    collapsedBtn.id = 'dsa-collapsed-btn';
    collapsedBtn.className = 'dsa-collapsed';
    collapsedBtn.title = '打开 DocSys Agent';
    collapsedBtn.innerHTML = ICONS.robot;
    document.body.appendChild(collapsedBtn);

    // Cache refs
    messagesEl = document.getElementById('dsa-messages');
    inputEl = document.getElementById('dsa-input');
    sendBtn = document.getElementById('dsa-send-btn');
    statusDot = document.getElementById('dsa-status-dot');

    // Events
    document.getElementById('dsa-collapse-btn').addEventListener('click', collapse);
    collapsedBtn.addEventListener('click', expand);
    sendBtn.addEventListener('click', sendMessage);
    inputEl.addEventListener('keydown', handleKeyDown);
    inputEl.addEventListener('input', function () { autoResize(inputEl); });
    makeDraggable(document.getElementById('dsa-header'));

    // Welcome
    if (config.welcomeMessage) {
      appendMessage('agent', config.welcomeMessage);
    }
  }

  // ─── Widget Key Auto-Discovery ────────────────────────────────────────────

  function fetchCurrentKey() {
    var base = config.apiBase.replace(/\/agent\/?$/, '') || '';
    var url = base + '/agent/current-key';

    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.onload = function () {
      if (xhr.status === 200) {
        try {
          var resp = JSON.parse(xhr.responseText);
          if (resp.data && resp.data.key) {
            config.apiKey = resp.data.key;
          }
        } catch (e) {}
      }
    };
    xhr.onerror = function () {}; // silently fail — key remains null, widget still works same-origin
    xhr.send();
  }

  // ─── Public API ───────────────────────────────────────────────────────────

  function init(opts) {
    if (root) return; // already initialized
    config = Object.assign({}, DEFAULT_CONFIG, opts || {});

    // Ensure styles loaded
    if (!document.querySelector('link[href*="chat-widget.css"]')) {
      var link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = (config.apiBase.replace('/agent', '') || '') + '/css/chat-widget.css';
      document.head.appendChild(link);
    }

    // Auto-fetch widget key from Agent if not explicitly provided
    if (!config.apiKey) {
      fetchCurrentKey();
    }

    buildDOM();

    if (!config.collapsed) {
      expand();
    } else {
      collapsedBtn.style.display = 'flex';
    }

    // Restore session from sessionStorage
    try {
      var saved = sessionStorage.getItem('dsa_sessionId');
      if (saved) state.sessionId = saved;
    } catch (e) {}

    return DSAWidget;
  }

  function send(text) {
    if (!inputEl) return;
    inputEl.value = text;
    sendMessage();
  }

  function destroy() {
    disconnectSSE();
    if (root) { root.remove(); root = null; }
    if (collapsedBtn) { collapsedBtn.remove(); collapsedBtn = null; }
    config = null;
  }

  // ─── Export ───────────────────────────────────────────────────────────────

  var DSAWidget = { init: init, send: send, destroy: destroy };
  global.DSAWidget = DSAWidget;

})(window);
