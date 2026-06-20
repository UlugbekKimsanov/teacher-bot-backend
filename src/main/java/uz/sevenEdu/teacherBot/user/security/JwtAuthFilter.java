package uz.sevenEdu.teacherBot.user.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    // Kunlik guard: bir foydalanuvchi uchun kuniga faqat bir marta DB yangilash
    private final Map<Long, LocalDate> activeTouched = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            return chain.filter(exchange);
        }
        try {
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractClaims(token).get("role", String.class);

            // O'quvchi faolligini belgilash (kuniga bir marta, fire-and-forget)
            if ("STUDENT".equalsIgnoreCase(role)) {
                LocalDate today = LocalDate.now();
                if (!today.equals(activeTouched.get(userId))) {
                    activeTouched.put(userId, today);
                    userRepository.touchLastActive(userId)
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                }
            }

            var auth = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
        } catch (Exception ignored) {
            return chain.filter(exchange);
        }
    }
}
