package uz.sevenEdu.teacherBot.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;

@Configuration
public class SwaggerRedirectConfig {

    @Bean
    @Order(-100)
    public WebFilter swaggerRedirectFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            if ("/swagger-ui/index.html".equals(path) || "/swagger-ui.html".equals(path)) {
                exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                exchange.getResponse().getHeaders().setLocation(URI.create("/webjars/swagger-ui/index.html"));
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        };
    }
}
