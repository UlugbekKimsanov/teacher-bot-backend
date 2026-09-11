package uz.sevenEdu.teacherBot.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Redis asosida sodda fixed-window rate-limiter (mavjud Redis bilan).
 * Faqat auth-sezgir yo'llarni cheklaydi — OTP/SMS spam (pul drenaji),
 * brute-force va DoS oldini oladi. Oddiy ilova trafigiga tegmaydi.
 * IP bo'yicha (nginx/Cloudflare ortida X-Forwarded-For).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitWebFilter implements WebFilter {

    private final ReactiveStringRedisTemplate redis;

    public RateLimitWebFilter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    private record Limit(String bucket, int max, int windowSec) {}

    private Limit limitFor(String path) {
        if (!path.contains("/auth/")) return null;
        // OTP/SMS yuborish — eng qattiq (pul ketadi)
        if (path.contains("otp") || path.contains("send")) {
            return new Limit("otp", 5, 60);     // 5 / daqiqa / IP
        }
        // login / verify — brute-force
        if (path.contains("login") || path.contains("verify") || path.contains("google")) {
            return new Limit("login", 20, 60);  // 20 / daqiqa / IP
        }
        return null;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Limit limit = limitFor(exchange.getRequest().getPath().value());
        if (limit == null) return chain.filter(exchange);

        String ip = clientIp(exchange);
        String key = "rl:" + limit.bucket() + ":" + ip;

        return redis.opsForValue().increment(key)
                .flatMap(count -> {
                    Mono<Boolean> ensureTtl = (count != null && count == 1L)
                            ? redis.expire(key, Duration.ofSeconds(limit.windowSec()))
                            : Mono.just(true);
                    return ensureTtl.then(Mono.defer(() -> {
                        if (count != null && count > limit.max()) {
                            return tooManyRequests(exchange);
                        }
                        return chain.filter(exchange);
                    }));
                })
                // Redis muammosi bo'lsa — so'rovni bloklamaymiz (fail-open)
                .onErrorResume(e -> {
                    log.warn("Rate-limit Redis xatosi: {}", e.getMessage());
                    return chain.filter(exchange);
                });
    }

    private String clientIp(ServerWebExchange exchange) {
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"success\":false,\"message\":\"Juda ko'p urinish. Birozdan so'ng qayta urining.\"}"
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buf = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buf));
    }
}
