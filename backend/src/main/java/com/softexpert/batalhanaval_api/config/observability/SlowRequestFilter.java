package com.softexpert.batalhanaval_api.config.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that detects and logs HTTP requests exceeding a configured duration threshold.
 * Slow requests are logged as warnings and counted as Prometheus metrics.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnBean(MeterRegistry.class)
public class SlowRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SlowRequestFilter.class);

    private final MeterRegistry meterRegistry;
    private final long thresholdMs;

    private Counter slowRequestCounter;

    public SlowRequestFilter(
            MeterRegistry meterRegistry,
            @Value("${observability.slow-request.threshold-ms:500}") long thresholdMs) {
        this.meterRegistry = meterRegistry;
        this.thresholdMs = thresholdMs;
    }

    @PostConstruct
    void initMetrics() {
        this.slowRequestCounter = Counter.builder("http.server.requests.slow")
                .description("Number of HTTP requests exceeding the slow request threshold")
                .tag("threshold_ms", String.valueOf(thresholdMs))
                .register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            if (durationMs >= thresholdMs) {
                slowRequestCounter.increment();
                log.warn("[SLOW REQUEST] {}ms | {} {} | status={} | remoteAddr={}",
                        durationMs,
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        request.getRemoteAddr());
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip actuator and static resource paths
        return path.startsWith("/actuator") || path.startsWith("/h2-console");
    }
}
