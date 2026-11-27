package uz.sevenEdu.teacherBot.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.nio.file.Paths;
import java.time.Duration;

@Configuration
public class WebConfig implements WebFluxConfigurer {

    private final String absoluteBasePath;

    public WebConfig(@Value("${app.storage.base-path}") String basePath) {
        this.absoluteBasePath = Paths.get(basePath).toAbsolutePath().normalize().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:///" + absoluteBasePath.replace("\\", "/") + "/";
        // Barcha yuklangan fayllar noyob nom (uid) bilan yoziladi va joyida qayta yozilmaydi
        // (FileStorageService.store, landing, avatar) — shuning uchun uzoq muddatli immutable kesh xavfsiz.
        // Fayl almashsa yangi nom => yangi URL. Last-Modified (useLastModified) saqlanadi.
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .setUseLastModified(true);
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
