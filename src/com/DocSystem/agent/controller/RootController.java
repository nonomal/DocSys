package com.DocSystem.agent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Root controller for the DocSysAgent webapp.
 *
 * Serves the chat UI welcome page when a user navigates to the agent
 * context root (e.g. http://host:port/agent/).
 *
 * URL flow:
 *   GET /agent/             → RootController.index() → 302 → /agent/index.html
 *   GET /agent/index        → RootController.indexPage() → 302 → /agent/index.html
 *   GET /agent/index.html   → Tomcat default servlet (static)
 *
 * Why redirect (not forward):
 *   DispatcherServlet is mapped to "/*" and intercepts everything.
 *   forward:/index.html causes infinite recursion (StackOverflowError).
 *   A redirect issues a fresh browser request that goes through the
 *   default servlet for static files.
 *
 * Why /index.html (not /static/index.html):
 *   /static/ is inside WEB-INF/classes/ which the default servlet refuses
 *   to serve directly (404). /index.html is at the webapp root and is
 *   served correctly.
 */
@Controller
@RequestMapping("/agent")
public class RootController {

    @GetMapping("/")
    public String index() {
        return "redirect:/agent/index.html";
    }
}
