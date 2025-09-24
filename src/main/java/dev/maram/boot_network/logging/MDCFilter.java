package dev.maram.boot_network.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

public class MDCFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            // Retrieve the currently authenticated user's principal (cast to String)
            String userEmail = (String) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();

            // Add the userId to MDC (Mapped Diagnostic Context) for logging purposes
            MDC.put("userEmail", userEmail);

            // Continue the filter chain (process the request)
            chain.doFilter(request, response);
        } finally {
            // Clear MDC to avoid data leakage between threads
            MDC.clear();
        }
    }
}
