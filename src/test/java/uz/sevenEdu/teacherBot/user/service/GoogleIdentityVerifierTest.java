package uz.sevenEdu.teacherBot.user.service;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import uz.sevenEdu.teacherBot.common.exception.ServiceUnavailableException;

class GoogleIdentityVerifierTest {

    @Test
    void refusesAuthenticationWhenAllowedAudienceIsNotConfigured() {
        GoogleIdentityVerifier verifier = new GoogleIdentityVerifier("  ");

        StepVerifier.create(verifier.verify("untrusted-token"))
                .expectError(ServiceUnavailableException.class)
                .verify();
    }
}
