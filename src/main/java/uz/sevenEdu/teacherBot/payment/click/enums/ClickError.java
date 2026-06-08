package uz.sevenEdu.teacherBot.payment.click.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClickError {
    SUCCESS(0, "Success"),
    SIGN_CHECK_FAILED(-1, "SIGN CHECK FAILED!"),
    INCORRECT_AMOUNT(-2, "Incorrect parameter amount"),
    ACTION_NOT_FOUND(-3, "Action not found"),
    ALREADY_PAID(-4, "Already paid"),
    USER_NOT_FOUND(-5, "User does not exist"),
    TRANSACTION_NOT_FOUND(-6, "Transaction does not exist"),
    FAILED_TO_UPDATE(-7, "Failed to update user"),
    ERROR_IN_REQUEST(-8, "Error in request from click"),
    TRANSACTION_CANCELLED(-9, "Transaction cancelled");

    private final int code;
    private final String message;
}
