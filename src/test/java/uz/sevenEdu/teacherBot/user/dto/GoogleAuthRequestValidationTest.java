package uz.sevenEdu.teacherBot.user.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleAuthRequestValidationTest {

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @AfterEach
    void closeFactory() {
        factory.close();
    }

    @Test
    void requiresIdToken() {
        GoogleAuthRequest request = new GoogleAuthRequest();

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void acceptsReasonablySizedIdToken() {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("header.payload.signature");

        assertThat(validator.validate(request)).isEmpty();
    }
}
