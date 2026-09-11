package uz.sevenEdu.teacherBot.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uz.sevenEdu.teacherBot.common.exception.ServiceUnavailableException;
import uz.sevenEdu.teacherBot.common.exception.UnauthorizedException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Verifies Google OpenID Connect ID tokens before an OAZIS session is issued. */
@Service
public class GoogleIdentityVerifier {

    private static final String INVALID_TOKEN_MESSAGE =
            "Google autentifikatsiyasi tasdiqlanmadi";

    private final GoogleIdTokenVerifier verifier;
    private final boolean configured;

    public GoogleIdentityVerifier(
            @Value("${app.google.oauth-client-ids:}") String configuredClientIds
    ) {
        List<String> clientIds = Arrays.stream(configuredClientIds.split("[,\\s]+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        this.configured = !clientIds.isEmpty();
        this.verifier = configured
                ? new GoogleIdTokenVerifier.Builder(
                        new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                        .setAudience(clientIds)
                        .build()
                : null;
    }

    public Mono<GoogleIdentity> verify(String rawIdToken) {
        if (!configured) {
            return Mono.error(new ServiceUnavailableException(
                    "Google orqali kirish serverda hali sozlanmagan"));
        }

        return Mono.fromCallable(() -> verifier.verify(rawIdToken))
                .subscribeOn(Schedulers.boundedElastic())
                .switchIfEmpty(Mono.error(new UnauthorizedException(INVALID_TOKEN_MESSAGE)))
                .flatMap(this::toIdentity)
                .onErrorMap(IOException.class, error -> new ServiceUnavailableException(
                        "Google autentifikatsiya xizmatiga ulanib bo'lmadi", error))
                .onErrorMap(GeneralSecurityException.class,
                        error -> new UnauthorizedException(INVALID_TOKEN_MESSAGE));
    }

    private Mono<GoogleIdentity> toIdentity(GoogleIdToken token) {
        GoogleIdToken.Payload payload = token.getPayload();
        String subject = clean(payload.getSubject());
        String email = clean(payload.getEmail());

        if (subject == null || email == null || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            return Mono.error(new UnauthorizedException(INVALID_TOKEN_MESSAGE));
        }

        return Mono.just(new GoogleIdentity(
                subject,
                email.toLowerCase(Locale.ROOT),
                valueOrEmpty(payload.get("given_name")),
                valueOrEmpty(payload.get("family_name"))
        ));
    }

    private static String clean(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String valueOrEmpty(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    public record GoogleIdentity(
            String subject,
            String email,
            String firstName,
            String lastName
    ) {
    }
}
