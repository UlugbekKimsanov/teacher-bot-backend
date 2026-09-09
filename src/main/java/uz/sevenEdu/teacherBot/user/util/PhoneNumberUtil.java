package uz.sevenEdu.teacherBot.user.util;

import uz.sevenEdu.teacherBot.common.exception.BadRequestException;

/** Canonicalizes Uzbek phone numbers for storage and lookup. */
public final class PhoneNumberUtil {

    private static final String COUNTRY_CODE = "998";
    private static final int NATIONAL_NUMBER_LENGTH = 9;

    private PhoneNumberUtil() {
    }

    /** Accepts common formatted variants and returns {@code +998XXXXXXXXX}. */
    public static String normalizeUzbekPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Telefon raqam kiritilishi shart");
        }

        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == NATIONAL_NUMBER_LENGTH) {
            digits = COUNTRY_CODE + digits;
        }

        if (digits.length() != COUNTRY_CODE.length() + NATIONAL_NUMBER_LENGTH
                || !digits.startsWith(COUNTRY_CODE)) {
            throw new BadRequestException("Telefon raqam formati noto'g'ri");
        }

        return "+" + digits;
    }

    /** Digits-only representation used by legacy-format-compatible queries. */
    public static String digits(String canonicalPhone) {
        return canonicalPhone.substring(1);
    }

    /** Keeps nullable phone fields nullable while canonicalizing real values. */
    public static String normalizeOptionalUzbekPhone(String phone) {
        return phone == null || phone.isBlank() ? null : normalizeUzbekPhone(phone);
    }
}
