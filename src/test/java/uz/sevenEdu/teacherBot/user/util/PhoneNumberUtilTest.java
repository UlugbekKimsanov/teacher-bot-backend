package uz.sevenEdu.teacherBot.user.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "+998 (90) 123-45-67",
            "998901234567",
            "90 123 45 67",
            "+998901234567"
    })
    void normalizesSupportedUzbekPhoneFormats(String input) {
        String result = PhoneNumberUtil.normalizeUzbekPhone(input);

        assertThat(result).isEqualTo("+998901234567");
        assertThat(PhoneNumberUtil.digits(result)).isEqualTo("998901234567");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12345678", "1234567890", "+997901234567", "+9989012345678"})
    void rejectsInvalidPhoneFormats(String input) {
        assertThatThrownBy(() -> PhoneNumberUtil.normalizeUzbekPhone(input))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsNullPhone() {
        assertThatThrownBy(() -> PhoneNumberUtil.normalizeUzbekPhone(null))
                .isInstanceOf(BadRequestException.class);
    }
}
