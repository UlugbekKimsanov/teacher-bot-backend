package uz.sevenEdu.teacherBot.payment.paynet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaynetResponse {
    private String jsonrpc = "2.0";
    private String id;
    private Object result;
    private PaynetError error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaynetError {
        private int code;
        private String message;
    }

    public static PaynetResponse ok(String requestId, Object result) {
        return PaynetResponse.builder()
                .jsonrpc("2.0")
                .id(requestId)
                .result(result)
                .build();
    }

    public static PaynetResponse error(String requestId, int code, String message) {
        return PaynetResponse.builder()
                .jsonrpc("2.0")
                .id(requestId)
                .error(PaynetError.builder().code(code).message(message).build())
                .build();
    }
}
