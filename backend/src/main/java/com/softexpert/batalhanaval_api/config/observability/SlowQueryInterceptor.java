package com.softexpert.batalhanaval_api.config.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Intercepts JPA repository method executions to detect and log slow database queries.
 * Queries exceeding the configured threshold are logged as warnings and counted as metrics.
 */
@Aspect
@Component
@ConditionalOnBean(MeterRegistry.class)
public class SlowQueryInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SlowQueryInterceptor.class);

    private final MeterRegistry meterRegistry;
    private final long thresholdMs;

    private Counter slowQueryCounter;
    private Timer queryTimer;

    public SlowQueryInterceptor(
            MeterRegistry meterRegistry,
            @Value("${observability.slow-query.threshold-ms:200}") long thresholdMs) {
        this.meterRegistry = meterRegistry;
        this.thresholdMs = thresholdMs;
    }

    @PostConstruct
    void initMetrics() {
        this.slowQueryCounter = Counter.builder("db.query.slow")
                .description("Number of queries exceeding the slow query threshold")
                .tag("threshold_ms", String.valueOf(thresholdMs))
                .register(meterRegistry);

        this.queryTimer = Timer.builder("db.query.duration")
                .description("Duration of JPA repository queries")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    @Around("execution(* com.softexpert.batalhanaval_api.repository..*.*(..))")
    public Object interceptQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            String methodName = joinPoint.getSignature().toShortString();

            queryTimer.record(durationMs, TimeUnit.MILLISECONDS);

            if (durationMs >= thresholdMs) {
                slowQueryCounter.increment();
                log.warn("[SLOW QUERY] {}ms | method={} | args={}",
                        durationMs, methodName, summarizeArgs(joinPoint.getArgs()));
            }
        }
    }

    private String summarizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            if (args[i] == null) {
                sb.append("null");
            } else {
                String val = args[i].toString();
                sb.append(val.length() > 100 ? val.substring(0, 100) + "..." : val);
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
