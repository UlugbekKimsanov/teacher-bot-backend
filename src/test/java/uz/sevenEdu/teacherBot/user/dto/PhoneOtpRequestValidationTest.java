package uz.sevenEdu.teacherBot.user.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneOtpRequestValidationTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsFormattedUzbekPhone() {
        PhoneOtpRequest request = new PhoneOtpRequest();
        request.setPhone("+998 (90) 123 45 67");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsBlankAndMalformedPhones() {
        PhoneOtpRequest request = new PhoneOtpRequest();
        request.setPhone(" ");
        assertThat(validator.validate(request)).isNotEmpty();

        request.setPhone("+997901234567");
        assertThat(validator.validate(request)).isNotEmpty();
    }
}
