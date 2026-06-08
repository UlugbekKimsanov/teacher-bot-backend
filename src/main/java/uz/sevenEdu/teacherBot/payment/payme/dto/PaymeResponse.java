package uz.sevenEdu.teacherBot.payment.payme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymeResponse {
    private String id;          // JSON-RPC request id ni echo qilamiz
    private Object result;
    private PaymeError error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymeError {
        private int code;
        private String message;
        private Object data;
    }

    // ── Factory metodlar ────────────────────────────────────────────

    public static PaymeResponse ok(String requestId, Object result) {
        return PaymeResponse.builder().id(requestId).result(result).build();
    }

    public static PaymeResponse error(String requestId, int code, String message) {
        return PaymeResponse.builder()
                .id(requestId)
                .error(PaymeError.builder().code(code).message(message).build())
                .build();
    }

    public static PaymeResponse error(String requestId, int code, String message, Object data) {
        return PaymeResponse.builder()
                .id(requestId)
                .error(PaymeError.builder().code(code).message(message).data(data).build())
                .build();
    }
}
