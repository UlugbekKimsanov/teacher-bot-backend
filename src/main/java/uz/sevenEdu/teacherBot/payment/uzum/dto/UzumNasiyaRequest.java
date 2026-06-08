package uz.sevenEdu.teacherBot.payment.uzum.dto;

import lombok.Data;

import java.util.Map;

@Data
public class UzumNasiyaRequest {
    private int serviceId;
    private long timestamp;
    private String transId;
    private Long amount;
    private Map<String, Object> params;

    public String getMerchantTransId() {
        if (params == null) return null;
        Object v = params.get("order_id");
        return v != null ? v.toString() : null;
    }
}
