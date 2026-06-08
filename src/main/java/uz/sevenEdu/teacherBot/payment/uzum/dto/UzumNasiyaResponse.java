package uz.sevenEdu.teacherBot.payment.uzum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UzumNasiyaResponse {
    private int status;
    private Object data;
    private String error;

    public static UzumNasiyaResponse ok(Object data) {
        return UzumNasiyaResponse.builder().status(0).data(data).build();
    }

    public static UzumNasiyaResponse error(int status, String message) {
        return UzumNasiyaResponse.builder().status(status).error(message).build();
    }
}
