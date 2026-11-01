package project2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import project2.exception.ApiError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Token-bucket rate limiter for the public rate-limit test endpoint.
 *
 * <p>Each client IP gets its own bucket holding {@value #REQUESTS_PER_WINDOW}
 * tokens, refilled greedily at the same rate over one minute. Requests to any
 * other path are not affected. The filter runs before the Spring Security
 * filter chain so the limit is applied consistently regardless of whether the
 * request is authenticated.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String TEST_PATH = "/api/rate-limit-test";
    private static final int REQUESTS_PER_WINDOW = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private static final String LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String REMAINING_HEADER = "X-RateLimit-Remaining";

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !HttpMethod.GET.matches(request.getMethod()) || !TEST_PATH.equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, key -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader(LIMIT_HEADER, String.valueOf(REQUESTS_PER_WINDOW));
        response.setHeader(REMAINING_HEADER, String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1,
                TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill() + TimeUnit.SECONDS.toNanos(1) - 1));
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiError error = new ApiError(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Rate limit exceeded. Please try again later.",
                request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), error);
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(REQUESTS_PER_WINDOW)
                        .refillGreedy(REQUESTS_PER_WINDOW, WINDOW)
                        .build())
                .build();
    }

    /**
     * Uses the direct connection address. Forwarded headers are deliberately
     * ignored because no trusted proxy is configured for this application.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "unknown" : remoteAddr;
    }
}
