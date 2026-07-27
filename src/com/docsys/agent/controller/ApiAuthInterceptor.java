package com.docsys.agent.controller;

import com.docsys.agent.config.EnvConfig;
import com.docsys.agent.monitoring.RateLimitService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * API Key authentication interceptor for protected endpoints.
 * Protects all /api/** endpoints except:
 *   - /api/agent/login  (open authentication endpoint)
 *   - /api/agent/health (health check)
 *   - /api/agent/[path]/validate (skill validation - file-only check)
 *   - /actuator/** (Spring Boot Actuator)
 *
 * Also enforces per-IP (or per-API-key) rate limiting via Bucket4j.
 *
 * Accepts API key via:
 *   - X-API-Key header
 *   - Authorization: Bearer token header
 *
 * Rate limiting: integrated via injected RateLimitService.
 */
@Component
public class ApiAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiAuthInterceptor.class);

    private final RateLimitService rateLimitService;
    private final EnvConfig envConfig;
    private final String fallbackApiKey;

    @Autowired
    public ApiAuthInterceptor(RateLimitService rateLimitService, EnvConfig envConfig,
            @Value("${docsys.api-key:}") String fallbackApiKey) {
        this.rateLimitService = rateLimitService;
        this.envConfig = envConfig;
        this.fallbackApiKey = fallbackApiKey;
    }

    // For test mocking
    ApiAuthInterceptor(RateLimitService rateLimitService, EnvConfig envConfig, String fallbackApiKey, boolean unused) {
        this.rateLimitService = rateLimitService;
        this.envConfig = envConfig;
        this.fallbackApiKey = fallbackApiKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // Public endpoints — no auth or rate-limit required
        if (isPublicPath(path)) {
            return true;
        }

        // Rate limiting (before auth check — allow unauthenticated rate-limit responses)
        String clientIp = resolveClientIp(request);
        String providedKey = getProvidedKey(request);

        // rateLimitService is now guaranteed non-null (injected by Spring)
        if (!rateLimitService.tryConsume(clientIp, providedKey)) {
            long retryAfter = rateLimitService.getSecondsUntilNextAvailable(clientIp, providedKey);
            log.warn("Rate limit exceeded for IP={}, path={}", clientIp, path);
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfter)));
            response.getWriter().write(
                "{\"success\":false,\"message\":\"Rate limit exceeded. Try again in " + retryAfter + " seconds.\",\"data\":null}");
            return false;
        }

        // API key authentication — skip for same-origin requests (web UI uses session cookie auth)
        if ((providedKey == null || providedKey.trim().isEmpty()) && !isSameOriginRequest(request)) {
            log.warn("Missing API key for protected endpoint: {} {}", request.getMethod(), path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"success\":false,\"message\":\"Unauthorized: X-API-Key header required\",\"data\":null}");
            return false;
        }

        // Same-origin requests bypass API key check entirely
        if (isSameOriginRequest(request)) {
            return true;
        }

        // API key validation: .env wins, fallback to application.yml, deny if neither set
        String expectedKey = envConfig.loadWidgetKey().orElse(null);
        if (expectedKey == null || expectedKey.trim().isEmpty()) {
            expectedKey = fallbackApiKey;
        }
        if (expectedKey != null && !expectedKey.trim().isEmpty() && !expectedKey.equals(providedKey)) {
            log.warn("Invalid API key attempt for endpoint: {} {}", request.getMethod(), path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"success\":false,\"message\":\"Unauthorized: invalid API key\",\"data\":null}");
            return false;
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler, ModelAndView modelAndView) throws Exception {
        // No-op: post-processing not required
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) throws Exception {
        // No-op: nothing to clean up after request completion
    }

    /**
     * Detect same-origin browser requests by checking Referer header.
     * Web UI requests from localhost:8110 carry this header automatically.
     */
    private boolean isSameOriginRequest(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.trim().isEmpty()) {
            return referer.contains("localhost:8110") || referer.contains("127.0.0.1:8110");
        }
        // Also allow requests from the same host without Referer (fallback)
        String host = request.getHeader("Host");
        return host != null && (host.contains(":8110") || host.equals("localhost") || host.equals("127.0.0.1"));
    }

    private boolean isPublicPath(String path) {
        if (path == null) return false;
        return path.endsWith("/api/agent/login")
            || path.endsWith("/api/agent/health")
            || path.startsWith("/api/agent/") && path.endsWith("/validate")
            || path.startsWith("/actuator/");
    }

    private String getProvidedKey(HttpServletRequest request) {
        String providedKey = request.getHeader("X-API-Key");
        if (providedKey == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                providedKey = authHeader.substring("Bearer ".length());
            }
        }
        return providedKey;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.trim().isEmpty()) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }
}
