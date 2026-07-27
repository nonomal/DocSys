package com.docsys.agent.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Async support filter for SSE streaming endpoints.
 *
 * Pass-through filter that enables async processing for the
 * <code>/api/agent/stream</code> endpoint. The actual async support
 * is declared via <code>&lt;async-supported&gt;true&lt;/async-supported&gt;</code>
 * on this filter and on the dispatcher servlet in web.xml. This
 * class exists because web.xml declares a filter-class for it.
 */
public class AsyncSupportFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No-op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No-op
    }
}
