package uz.sevenEdu.teacherBot.payment.paynet.enums;

public enum PaynetTransactionStatus {
    CREATED(0),
    SUCCESSFUL(1),
    CANCELLED(2);

    public final int code;

    PaynetTransactionStatus(int code) {
        this.code = code;
    }
}
