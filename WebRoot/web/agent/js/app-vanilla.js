// DocSys AI Agent - Vanilla JavaScript Application

(function() {
    'use strict';

    // ==================== API Base ====================
    // Use absolute path so fetch works regardless of current URL depth
    const API_BASE = '/DocSystem/agent';
    
    // ==================== State ====================
    const state = {
        isLoggedIn: false,
        sessionId: null,
        username: 'admin',
        messages: [],
        sessions: [
            { id: '1', name: '新会话', createdAt: Date.now() }
        ],
        currentSession: '1',
        isLoading: false,
        settings: {
            theme: 'dark',
            fontSize: 14,
            llmModel: 'llama3',
            llmEndpoint: 'http://localhost:11434/v1',
            llmApiKey: ''
        },
        skills: [
            { name: 'docsys-repos', description: '仓库管理 - 创建、删除、列出仓库', icon: '📁', active: true },
            { name: 'docsys-docs', description: '文档操作 - 上传、下载、删除文档', icon: '📄', active: true },
            { name: 'docsys-search', description: '全文搜索 - 搜索文档内容', icon: '🔍', active: true },
            { name: 'docsys-chat', description: 'AI对话 - 与智能助手交流', icon: '💬', active: true },
            { name: 'docsys-admin', description: '系统管理 - 用户、权限管理', icon: '⚙️', active: false }
        ],
        // 待上传文件队列（当仓库不存在时）
        pendingUploads: [],
        // 网络搜索结果缓存
        lastWebSearchResult: null
    };
    
    // ==================== Confirmation Dialog ====================
    // Per D-13: SSE confirm flow — modal dialog for write operations
    function showConfirmDialog(message, operation) {
        return new Promise((resolve) => {
            // Create modal overlay
            const overlay = document.createElement('div');
            overlay.id = 'confirmOverlay';
            overlay.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.7);z-index:10000;display:flex;align-items:center;justify-content:center;';

            // Create modal
            const modal = document.createElement('div');
            modal.style.cssText = 'background:#1e1e1e;border:1px solid #333;border-radius:8px;padding:24px;max-width:500px;width:90%;font-family:system-ui,sans-serif;';

            const title = document.createElement('h3');
            title.textContent = '⚠️ 确认操作';
            title.style.cssText = 'margin:0 0 16px 0;color:#ff6b6b;font-size:18px;';

            const msg = document.createElement('p');
            msg.textContent = message;
            msg.style.cssText = 'margin:0 0 20px 0;color:#ccc;font-size:14px;line-height:1.5;white-space:pre-wrap;word-break:break-word;';

            const btnContainer = document.createElement('div');
            btnContainer.style.cssText = 'display:flex;gap:12px;justify-content:flex-end;';

            const cancelBtn = document.createElement('button');
            cancelBtn.textContent = '取消 (Cancel)';
            cancelBtn.style.cssText = 'padding:10px 20px;border:1px solid #444;border-radius:6px;background:#2a2a2a;color:#ccc;cursor:pointer;font-size:14px;';
            cancelBtn.onclick = () => {
                document.body.removeChild(overlay);
                resolve('reject');
            };

            const approveBtn = document.createElement('button');
            approveBtn.textContent = '确认 (Confirm)';
            approveBtn.style.cssText = 'padding:10px 20px;border:none;border-radius:6px;background:#dc3545;color:white;cursor:pointer;font-size:14px;font-weight:bold;';
            approveBtn.onclick = () => {
                document.body.removeChild(overlay);
                resolve('approve');
            };

            btnContainer.appendChild(cancelBtn);
            btnContainer.appendChild(approveBtn);
            modal.appendChild(title);
            modal.appendChild(msg);
            modal.appendChild(btnContainer);
            overlay.appendChild(modal);
            document.body.appendChild(overlay);

            // Focus approve button by default
            approveBtn.focus();
        });
    }

    // ==================== DOM Elements ====================
    const elements = {
        loginScreen: null,
        mainApp: null,
        loginForm: null,
        usernameInput: null,
        passwordInput: null,
        loginBtn: null,
        messagesArea: null,
        emptyState: null,
        messageInput: null,
        sendBtn: null,
        sessionList: null,
        skillsDialog: null,
        settingsDialog: null,
        skillsList: null,
        toastContainer: null
    };
    
    // ==================== Initialize ====================
    function init() {
        console.log('Initializing DocSys AI Agent...');
        
        // Cache DOM elements
        cacheElements();
        
        // Load settings from localStorage
        loadSettings();
        
        // Render initial UI
        renderSkills();
        renderSessions();
        
        // Bind events
        bindEvents();
        
        console.log('Initialization complete');
    }
    
    function cacheElements() {
        elements.loginScreen = document.getElementById('login-screen');
        elements.mainApp = document.getElementById('main-app');
        elements.loginForm = document.getElementById('login-form');
        elements.usernameInput = document.getElementById('username');
        elements.passwordInput = document.getElementById('password');
        elements.loginBtn = document.getElementById('login-btn');
        elements.messagesArea = document.getElementById('messages-area');
        elements.emptyState = document.getElementById('empty-state');
        elements.messageInput = document.getElementById('message-input');
        elements.sendBtn = document.getElementById('send-btn');
        elements.sessionList = document.getElementById('session-list');
        elements.skillsDialog = document.getElementById('skills-dialog');
        elements.settingsDialog = document.getElementById('settings-dialog');
        elements.skillsList = document.getElementById('skills-list');
        elements.toastContainer = document.getElementById('toast-container');

        // Guard: throw early if critical elements are missing so bugs are obvious
        const missing = Object.entries(elements)
            .filter(([, v]) => v === null)
            .map(([k]) => k);
        if (missing.length) {
            console.error('Missing DOM elements (check HTML ID attributes):', missing.join(', '));
        }
    }
    
    function loadSettings() {
        const saved = localStorage.getItem('docsys-settings');
        if (saved) {
            try {
                Object.assign(state.settings, JSON.parse(saved));
            } catch (e) {
                console.error('Failed to load settings:', e);
            }
        }
        
        // Check for existing session
        const savedSession = localStorage.getItem('docsys-session');
        if (savedSession) {
            try {
                const sessionData = JSON.parse(savedSession);
                state.sessionId = sessionData.sessionId;
                state.username = sessionData.username || 'admin';
                state.isLoggedIn = true;
                showMainApp();
            } catch (e) {
                console.error('Failed to restore session:', e);
            }
        }
    }
    
    // ==================== Events ====================
    function bindEvents() {
        // Login form
        elements.loginForm.addEventListener('submit', handleLogin);
        
        // Quick commands
        document.querySelectorAll('.quick-cmd').forEach(el => {
            el.addEventListener('click', () => {
                const cmd = el.dataset.cmd;
                elements.messageInput.value = cmd;
                sendMessage();
            });
        });
        
        // Message input
        elements.messageInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
        
        elements.messageInput.addEventListener('input', () => {
            elements.sendBtn.disabled = !elements.messageInput.value.trim();
        });
        
        // Send button
        elements.sendBtn.addEventListener('click', sendMessage);
        
        // New session button
        document.getElementById('new-session-btn').addEventListener('click', createNewSession);

        // Skills button
        document.getElementById('skills-btn').addEventListener('click', () => {
            elements.skillsDialog.style.display = 'flex';
        });

        // Settings button
        document.getElementById('settings-btn').addEventListener('click', () => {
            // Populate settings values
            document.getElementById('theme-select').value = state.settings.theme;
            document.getElementById('llm-model').value = state.settings.llmModel;
            document.getElementById('llm-endpoint').value = state.settings.llmEndpoint;
            document.getElementById('llm-apikey').value = state.settings.llmApiKey;

            elements.settingsDialog.style.display = 'flex';
        });

        document.getElementById('cancel-settings').addEventListener('click', () => {
            elements.settingsDialog.style.display = 'none';
        });

        document.getElementById('save-settings').addEventListener('click', saveSettingsHandler);

        // Close dialogs on background click
        elements.skillsDialog.addEventListener('click', (e) => {
            if (e.target === elements.skillsDialog) {
                elements.skillsDialog.style.display = 'none';
            }
        });

        elements.settingsDialog.addEventListener('click', (e) => {
            if (e.target === elements.settingsDialog) {
                elements.settingsDialog.style.display = 'none';
            }
        });
        
        // Drag and drop for file upload
        setupDragAndDrop();
    }
    
    // ==================== Login ====================
    async function handleLogin(e) {
        e.preventDefault();
        
        const username = elements.usernameInput.value.trim();
        const password = elements.passwordInput.value.trim();
        
        if (!username || !password) {
            showToast('请输入用户名和密码', 'error');
            return;
        }
        
        setLoginLoading(true);
        
        try {
            const response = await fetch(API_BASE + '/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, password })
            });
            
            const data = await response.json();
            
            if (data.success) {
                state.isLoggedIn = true;
                state.sessionId = data.data.sessionId;
                state.username = username;
                
                // Save session
                localStorage.setItem('docsys-session', JSON.stringify({
                    sessionId: state.sessionId,
                    username: state.username
                }));
                
                showToast('登录成功', 'success');
                showMainApp();
            } else {
                showToast(data.message || '登录失败', 'error');
            }
        } catch (error) {
            console.error('Login error:', error);
            showToast('登录失败: ' + error.message, 'error');
        } finally {
            setLoginLoading(false);
        }
    }
    
    function setLoginLoading(loading) {
        elements.loginBtn.disabled = loading;
        elements.loginBtn.textContent = loading ? '登录中...' : '登录';
    }
    
    // ==================== Chat ====================
    async function sendMessage() {
        const message = elements.messageInput.value.trim();
        if (!message || state.isLoading) return;

        // Add user message
        addMessage('user', message);
        elements.messageInput.value = '';
        elements.sendBtn.disabled = true;
        state.isLoading = true;

        // Show loading
        showLoading(true);

        // Per D-13: Use SSE streaming for real-time confirm flow support
        const sessionParam = state.sessionId ? '&sessionId=' + encodeURIComponent(state.sessionId) : '';
        const url = '/agent/stream?command=' + encodeURIComponent(message) + sessionParam;

        try {
            const response = await fetch(url);
            if (!response.ok) {
                addMessage('assistant', '请求失败: ' + response.status);
                state.isLoading = false;
                showLoading(false);
                return;
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';
            let fullContent = '';
            // Collect message div for streaming updates
            let assistantDiv = null;

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n');
                buffer = lines.pop();  // Keep incomplete last line in buffer

                for (const line of lines) {
                    if (!line.startsWith('data: ')) continue;
                    const raw = line.slice('data: '.length);
                    let data;
                    try { data = JSON.parse(raw); } catch { continue; }

                    // Per D-13: Handle SSE confirm event — pause stream, show dialog
                    if (data.type === 'confirm') {
                        const confirmToken = data.confirmToken;
                        const operation = data.operation;
                        const confirmMsg = data.message || '此操作需要确认。';

                        console.log('SSE confirm received: operation=' + operation + ', token=' + confirmToken);

                        // Hide loading indicator while waiting for user
                        showLoading(false);

                        try {
                            const action = await showConfirmDialog(confirmMsg, operation);
                            if (action === 'approve') {
                                await fetch(API_BASE + '/confirm', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({ confirmToken: confirmToken, action: 'approve' })
                                });
                                console.log('User approved operation: ' + operation);
                                // SSE stream continues automatically after approval
                                showLoading(true);
                            } else {
                                // User cancelled — send rejection and stop
                                await fetch(API_BASE + '/confirm', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({ confirmToken: confirmToken, action: 'reject' })
                                });
                                console.log('User rejected operation: ' + operation);
                                addMessage('assistant', '操作已取消。');
                                state.isLoading = false;
                                scrollToBottom();
                                reader.cancel();
                                return;
                            }
                        } catch (err) {
                            console.error('Confirm dialog error:', err);
                            await fetch(API_BASE + '/confirm', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({ confirmToken: confirmToken, action: 'reject' })
                            });
                            addMessage('assistant', '确认流程出错，操作已取消。');
                            state.isLoading = false;
                            scrollToBottom();
                            return;
                        }
                        continue;  // Continue processing SSE stream after approval
                    }

                    if (data.type === 'start') {
                        // Create assistant message div for streaming content
                        if (!assistantDiv) {
                            assistantDiv = document.createElement('div');
                            assistantDiv.className = 'message assistant';
                            assistantDiv.innerHTML = `
                                <div class="message-avatar">🤖</div>
                                <div class="message-content">
                                    <div class="message-header">
                                        <span class="message-sender">AI 助手</span>
                                        <span class="message-time">${new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}</span>
                                    </div>
                                    <div class="message-body"></div>
                                </div>`;
                            elements.messagesArea.appendChild(assistantDiv);
                        }
                    }

                    if (data.type === 'chunk' && data.content) {
                        fullContent += data.content;
                        if (assistantDiv) {
                            const bodyEl = assistantDiv.querySelector('.message-body');
                            if (bodyEl) bodyEl.textContent = formatContent(fullContent);
                            scrollToBottom();
                        }
                    }

                    if (data.type === 'done') {
                        const content = data.fullContent || fullContent;
                        if (assistantDiv) {
                            const bodyEl = assistantDiv.querySelector('.message-body');
                            if (bodyEl) bodyEl.innerHTML = formatContent(content);
                        } else {
                            addMessage('assistant', formatContent(content));
                        }
                    }

                    if (data.type === 'error') {
                        if (assistantDiv) assistantDiv.remove();
                        addMessage('assistant', '错误: ' + (data.message || '未知错误'));
                    }
                }
            }

            // If no chunk/done received, add fullContent as a normal message
            if (!fullContent && !assistantDiv) {
                addMessage('assistant', '未收到响应');
            }

        } catch (error) {
            console.error('Send message error:', error);
            if (!elements.messagesArea.querySelector('.message.assistant:last-child .message-body')?.textContent) {
                addMessage('assistant', '发送失败: ' + error.message);
            }
        } finally {
            state.isLoading = false;
            showLoading(false);
            scrollToBottom();
        }
    }
    
    function addMessage(role, content) {
        state.messages.push({
            role,
            content,
            time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
        });
        
        renderMessages();
    }
    
    function renderMessages() {
        // Clear messages area
        const messages = elements.messagesArea.querySelectorAll('.message');
        messages.forEach(el => el.remove());
        
        if (state.messages.length === 0) {
            elements.emptyState.style.display = 'flex';
            return;
        }
        
        elements.emptyState.style.display = 'none';
        
        state.messages.forEach(msg => {
            const msgEl = document.createElement('div');
            msgEl.className = `message ${msg.role}`;
            msgEl.innerHTML = `
                <div class="message-avatar">${msg.role === 'user' ? '👤' : '🤖'}</div>
                <div class="message-content">
                    <div class="message-header">
                        <span class="message-sender">${msg.role === 'user' ? '你' : 'AI 助手'}</span>
                        <span class="message-time">${msg.time}</span>
                    </div>
                    <div class="message-body">${formatContent(msg.content)}</div>
                </div>
            `;
            elements.messagesArea.appendChild(msgEl);
        });
        
        scrollToBottom();
    }
    
    function formatContent(content) {
        if (!content) return '';
        // Escape HTML first to prevent XSS and fix display issues
        const escaped = content
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
        // Then apply markdown-like formatting
        return escaped
            .replace(/`([^`]+)`/g, '<code>$1</code>')
            .replace(/\n/g, '<br>');
    }
    
    function showLoading(show) {
        let loadingEl = elements.messagesArea.querySelector('.loading');
        
        if (show) {
            if (!loadingEl) {
                loadingEl = document.createElement('div');
                loadingEl.className = 'message loading';
                loadingEl.innerHTML = `
                    <div class="message-avatar">🤖</div>
                    <div class="message-content">
                        <div class="loading-dots">
                            <span></span><span></span><span></span>
                        </div>
                    </div>
                `;
                elements.messagesArea.appendChild(loadingEl);
            }
            loadingEl.style.display = 'flex';
        } else if (loadingEl) {
            loadingEl.style.display = 'none';
        }
        
        scrollToBottom();
    }
    
    function scrollToBottom() {
        elements.messagesArea.scrollTop = elements.messagesArea.scrollHeight;
    }
    
    // ==================== Sessions ====================
    function createNewSession() {
        const newId = Date.now().toString();
        state.sessions.unshift({
            id: newId,
            name: '新会话',
            createdAt: Date.now()
        });
        state.currentSession = newId;
        state.messages = [];
        renderSessions();
        renderMessages();
    }
    
    function switchSession(sessionId) {
        state.currentSession = sessionId;
        state.messages = [];
        renderSessions();
        renderMessages();
    }
    
    function renderSessions() {
        elements.sessionList.innerHTML = state.sessions.map(session => `
            <div class="session-item ${session.id === state.currentSession ? 'active' : ''}" data-id="${session.id}">
                <span class="session-name">${escapeHtml(session.name)}</span>
                <button class="session-menu-btn" data-id="${session.id}">⋮</button>
            </div>
        `).join('');
        
        // Bind session click events
        elements.sessionList.querySelectorAll('.session-item').forEach(el => {
            el.addEventListener('click', (e) => {
                if (!e.target.classList.contains('session-menu-btn')) {
                    switchSession(el.dataset.id);
                }
            });
        });
        
        // Bind menu events
        elements.sessionList.querySelectorAll('.session-menu-btn').forEach(el => {
            el.addEventListener('click', (e) => {
                e.stopPropagation();
                showSessionMenu(el.dataset.id);
            });
        });
    }
    
    function showSessionMenu(sessionId) {
        const session = state.sessions.find(s => s.id === sessionId);
        if (!session) return;
        
        const action = prompt('输入操作: rename(重命名) / export(导出) / delete(删除)');
        if (!action) return;
        
        switch (action.toLowerCase()) {
            case 'rename':
                const newName = prompt('请输入新名称:', session.name);
                if (newName && newName.trim()) {
                    session.name = newName.trim();
                    renderSessions();
                }
                break;
            case 'export':
                exportSession(session);
                break;
            case 'delete':
                if (state.sessions.length > 1) {
                    state.sessions = state.sessions.filter(s => s.id !== sessionId);
                    if (state.currentSession === sessionId) {
                        state.currentSession = state.sessions[0].id;
                    }
                    renderSessions();
                }
                break;
        }
    }
    
    function exportSession(session) {
        const content = state.messages.map(m => 
            `**${m.role === 'user' ? '👤 你' : '🤖 AI'}** (${m.time}):\n\n${m.content}\n`
        ).join('\n---\n\n');
        
        const blob = new Blob([`# 会话导出\n\n${content}`], { type: 'text/markdown' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `session-${session.name}.md`;
        a.click();
        URL.revokeObjectURL(url);
        
        showToast('会话已导出', 'success');
    }
    
    // ==================== Skills ====================
    function renderSkills() {
        elements.skillsList.innerHTML = state.skills.map(skill => `
            <div class="skill-card">
                <div class="skill-icon">${skill.icon}</div>
                <div class="skill-info">
                    <h4>${escapeHtml(skill.name)}</h4>
                    <p>${escapeHtml(skill.description)}</p>
                </div>
                <label class="switch">
                    <input type="checkbox" ${skill.active ? 'checked' : ''} data-skill="${skill.name}">
                    <span class="slider"></span>
                </label>
            </div>
        `).join('');
        
        // Bind skill toggle events
        elements.skillsList.querySelectorAll('input[type="checkbox"]').forEach(el => {
            el.addEventListener('change', () => {
                const skill = state.skills.find(s => s.name === el.dataset.skill);
                if (skill) {
                    skill.active = el.checked;
                    showToast(`${skill.name} 已${skill.active ? '激活' : '停用'}`, 'success');
                }
            });
        });
    }
    
    // ==================== Settings ====================
    function saveSettingsHandler() {
        state.settings.theme = document.getElementById('theme-select').value;
        state.settings.llmModel = document.getElementById('llm-model').value;
        state.settings.llmEndpoint = document.getElementById('llm-endpoint').value;
        state.settings.llmApiKey = document.getElementById('llm-apikey').value;

        localStorage.setItem('docsys-settings', JSON.stringify(state.settings));

        elements.settingsDialog.style.display = 'none';
        showToast('设置已保存', 'success');
    }
    
    // ==================== Drag and Drop ====================
    function setupDragAndDrop() {
        const dropZone = elements.messageInput;
        
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            dropZone.addEventListener(eventName, preventDefaults, false);
        });
        
        function preventDefaults(e) {
            e.preventDefault();
            e.stopPropagation();
        }
        
        ['dragenter', 'dragover'].forEach(eventName => {
            dropZone.addEventListener(eventName, () => {
                dropZone.classList.add('drag-over');
            });
        });
        
        ['dragleave', 'drop'].forEach(eventName => {
            dropZone.addEventListener(eventName, () => {
                dropZone.classList.remove('drag-over');
            });
        });
        
        dropZone.addEventListener('drop', handleDrop);
    }
    
    function handleDrop(e) {
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            if (files.length === 1) {
                showToast(`准备上传文件: ${files[0].name}`, 'info');
                uploadFile(files[0]);
            } else {
                showToast(`准备上传 ${files.length} 个文件`, 'info');
                uploadMultipleFiles(files);
            }
        }
    }

    /**
     * 上传单个文件
     */
    async function uploadFile(file, reposId = 1, targetPath = '/') {
        if (!state.isLoggedIn) {
            showToast('请先登录', 'error');
            return;
        }

        showToast(`正在上传: ${file.name}...`, 'info');
        addMessage('system', `📤 上传中: ${file.name} (${formatFileSize(file.size)})`);

        try {
            const formData = new FormData();
            formData.append('file', file);
            formData.append('reposId', reposId);
            formData.append('path', targetPath);
            formData.append('name', file.name);

            const response = await fetch(API_BASE + '/upload', {
                method: 'POST',
                headers: state.sessionId ? { 'X-Session-Id': state.sessionId } : {},
                body: formData
            });

            const data = await response.json();

            if (data.success) {
                showToast('上传成功', 'success');
                addMessage('system', `✅ 上传成功: ${file.name}`);
            } else {
                const errorMsg = data.message || '未知错误';

                // 检查是否是仓库相关的错误
                if (errorMsg.includes('仓库') || errorMsg.includes('repo') || errorMsg.includes('不存在') || errorMsg.includes('not exist')) {
                    // 仓库不存在，记住待上传文件并提示创建仓库
                    if (!state.pendingUploads) state.pendingUploads = [];
                    state.pendingUploads.push({
                        file: file,
                        reposId: reposId,
                        targetPath: targetPath,
                        timestamp: Date.now()
                    });
                    savePendingUploads();

                    addMessage('system', `❌ 上传失败: ${file.name}\n\n📁 原因: 仓库不存在或已被删除\n\n💡 建议: 请先创建仓库，上传文件将被保存，稍后可以继续上传。\n   • 输入【创建仓库 仓库名称】来新建仓库\n   • 新建仓库后，输入【继续上传】完成上传`);
                    showToast('文件已暂存，仓库创建后可继续上传', 'warning');
                } else {
                    showToast('上传失败: ' + errorMsg, 'error');
                    addMessage('system', `❌ 上传失败: ${file.name} - ${errorMsg}`);
                }
            }
        } catch (error) {
            console.error('Upload error:', error);
            showToast('上传失败: ' + error.message, 'error');
            addMessage('system', `❌ 上传失败: ${file.name} - ${error.message}`);
        }
    }

    /**
     * 上传多个文件
     */
    async function uploadMultipleFiles(files, reposId = 1, targetPath = '/') {
        if (!state.isLoggedIn) {
            showToast('请先登录', 'error');
            return;
        }

        showToast(`正在上传 ${files.length} 个文件...`, 'info');
        addMessage('system', `📤 批量上传中: ${files.length} 个文件`);

        let successCount = 0;
        let failCount = 0;
        const failedFiles = [];

        for (const file of files) {
            try {
                const formData = new FormData();
                formData.append('files', file);  // 使用 files 参数名
                formData.append('reposId', reposId);
                formData.append('path', targetPath);
                formData.append('name', file.name);

                const response = await fetch(API_BASE + '/upload', {
                    method: 'POST',
                    headers: state.sessionId ? { 'X-Session-Id': state.sessionId } : {},
                    body: formData
                });

                const data = await response.json();
                if (data.success) {
                    successCount++;
                } else {
                    failCount++;
                    failedFiles.push(file.name);
                }
            } catch (error) {
                failCount++;
                failedFiles.push(file.name);
            }
        }

        if (failCount === 0) {
            showToast(`全部 ${successCount} 个文件上传成功`, 'success');
            addMessage('system', `✅ 批量上传成功: ${successCount} 个文件`);
        } else {
            showToast(`${successCount} 成功, ${failCount} 失败`, 'warning');
            addMessage('system', `⚠️ 批量上传完成: ${successCount} 成功, ${failCount} 失败`);
        }
    }

    /**
     * 上传文件夹（使用 webkitdirectory）
     */
    async function uploadFolder(files, reposId = 1, targetPath = '/') {
        if (!state.isLoggedIn) {
            showToast('请先登录', 'error');
            return;
        }

        const batchStartTime = Date.now();
        showToast(`正在上传文件夹 (${files.length} 个文件)...`, 'info');
        addMessage('system', `📁 上传文件夹: ${files.length} 个文件`);

        // 按 webkitRelativePath 排序，保持文件夹结构
        const sortedFiles = Array.from(files).sort((a, b) => {
            const pathA = a.webkitRelativePath || a.name;
            const pathB = b.webkitRelativePath || b.name;
            return pathA.localeCompare(pathB);
        });

        let successCount = 0;
        let failCount = 0;

        for (let i = 0; i < sortedFiles.length; i++) {
            const file = sortedFiles[i];
            const dirPath = file.webkitRelativePath || '';
            const isEnd = i === sortedFiles.length - 1 ? 1 : 0;

            try {
                const formData = new FormData();
                formData.append('file', file);
                formData.append('reposId', reposId);
                formData.append('path', targetPath);
                formData.append('name', file.name);
                formData.append('dirPath', dirPath);
                formData.append('batchStartTime', batchStartTime);
                formData.append('totalCount', sortedFiles.length);
                formData.append('isEnd', isEnd);

                const response = await fetch(API_BASE + '/upload', {
                    method: 'POST',
                    headers: state.sessionId ? { 'X-Session-Id': state.sessionId } : {},
                    body: formData
                });

                const data = await response.json();
                if (data.success) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (error) {
                failCount++;
            }

            // 更新进度
            if ((i + 1) % 10 === 0 || isEnd) {
                showToast(`上传进度: ${i + 1}/${sortedFiles.length}`, 'info');
            }
        }

        if (failCount === 0) {
            showToast(`文件夹上传成功: ${successCount} 个文件`, 'success');
            addMessage('system', `✅ 文件夹上传成功: ${successCount} 个文件`);
        } else {
            showToast(`文件夹上传完成: ${successCount} 成功, ${failCount} 失败`, 'warning');
            addMessage('system', `⚠️ 文件夹上传完成: ${successCount} 成功, ${failCount} 失败`);
        }
    }

    /**
     * 保存待上传文件到 localStorage
     */
    function savePendingUploads() {
        try {
            const data = state.pendingUploads.map(u => ({
                name: u.file.name,
                size: u.file.size,
                type: u.file.type,
                lastModified: u.file.lastModified,
                reposId: u.reposId,
                targetPath: u.targetPath,
                timestamp: u.timestamp
            }));
            localStorage.setItem('docsys-pending-uploads', JSON.stringify(data));
        } catch (e) {
            console.error('Failed to save pending uploads:', e);
        }
    }

    /**
     * 加载待上传文件
     */
    function loadPendingUploads() {
        // 注意：由于文件对象无法序列化，实际文件会丢失
        // 这里只是显示提示，让用户重新选择文件
        const saved = localStorage.getItem('docsys-pending-uploads');
        if (saved) {
            const data = JSON.parse(saved);
            if (data.length > 0) {
                addMessage('system', `📋 您有 ${data.length} 个待上传文件在上次操作中被暂存。\n\n请重新选择文件进行上传，或输入【新建仓库】创建仓库后再上传。`);
            }
        }
    }

    /**
     * 清除待上传文件
     */
    function clearPendingUploads() {
        state.pendingUploads = [];
        localStorage.removeItem('docsys-pending-uploads');
    }

    /**
     * 创建文件选择器（多文件选择）
     */
    function createFileInput() {
        const input = document.createElement('input');
        input.type = 'file';
        input.multiple = true;
        input.accept = '*/*';
        return input;
    }

    /**
     * 创建文件夹选择器（webkitdirectory）
     */
    function createFolderInput() {
        const input = document.createElement('input');
        input.type = 'file';
        input.webkitdirectory = true;
        return input;
    }

    function formatFileSize(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }
    
    // ==================== UI Helpers ====================
    function showMainApp() {
        elements.loginScreen.style.display = 'none';
        elements.mainApp.style.display = 'flex';
        elements.messageInput.disabled = false;
        elements.sendBtn.disabled = true;
    }
    
    function showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.textContent = message;
        elements.toastContainer.appendChild(toast);
        
        setTimeout(() => {
            toast.classList.add('show');
        }, 10);
        
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }
    
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    // ==================== Start ====================
    document.addEventListener('DOMContentLoaded', init);
})();
