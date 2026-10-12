package uz.sevenEdu.teacherBot.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebFluxConfigurer {

    private final String absoluteBasePath;

    public WebConfig(@Value("${app.storage.base-path}") String basePath) {
        this.absoluteBasePath = Paths.get(basePath).toAbsolutePath().normalize().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:///" + absoluteBasePath.replace("\\", "/") + "/";
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/files/**")
                .allowedOrigins(
                        "https://oazisedu.uz",
                        "https://admin.oazisedu.uz",
                        "http://localhost:3000",
                        "http://localhost:3100",
                        "http://localhost:5173")
                .allowedOriginPatterns("https://*.trycloudflare.com")
                .allowedMethods("GET")
                .allowedHeaders("*");
    }
}
