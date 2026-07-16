package com.softexpert.batalhanaval_api.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration BUCKET_EXPIRY = Duration.ofMinutes(10);

    private final Map<String, BucketEntry> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, BucketEntry> apiBuckets = new ConcurrentHashMap<>();

    @Value("${rate-limit.auth.requests-per-minute:5}")
    private int authRequestsPerMinute;

    @Value("${rate-limit.api.requests-per-minute:60}")
    private int apiRequestsPerMinute;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);

        if (path.startsWith("/api/auth/")) {
            BucketEntry entry = authBuckets.computeIfAbsent(clientIp, k -> new BucketEntry(createBucket(authRequestsPerMinute)));
            entry.touch();
            if (!entry.bucket().tryConsume(1)) {
                writeRateLimitResponse(response);
                return;
            }
        }

        BucketEntry apiEntry = apiBuckets.computeIfAbsent(clientIp, k -> new BucketEntry(createBucket(apiRequestsPerMinute)));
        apiEntry.touch();
        if (!apiEntry.bucket().tryConsume(1)) {
            writeRateLimitResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Scheduled(fixedDelay = 300000)
    public void evictExpiredBuckets() {
        Instant cutoff = Instant.now().minus(BUCKET_EXPIRY);
        authBuckets.entrySet().removeIf(e -> e.getValue().lastAccess().isBefore(cutoff));
        apiBuckets.entrySet().removeIf(e -> e.getValue().lastAccess().isBefore(cutoff));
    }

    private Bucket createBucket(int capacity) {
        return Bucket.builder()
            .addLimit(Bandwidth.simple(capacity, Duration.ofMinutes(1)))
            .build();
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests. Please try again later.\"}");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String[] ips = xForwardedFor.split(",");
            return ips[ips.length - 1].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class BucketEntry {
        private final Bucket bucket;
        private volatile Instant lastAccess;

        BucketEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccess = Instant.now();
        }

        Bucket bucket() {
            return bucket;
        }

        Instant lastAccess() {
            return lastAccess;
        }

        void touch() {
            this.lastAccess = Instant.now();
        }
    }
}
