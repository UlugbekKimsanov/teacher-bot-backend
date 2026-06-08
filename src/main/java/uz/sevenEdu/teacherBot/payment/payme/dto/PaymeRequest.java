package uz.sevenEdu.teacherBot.payment.payme.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class PaymeRequest {
    private String id;       // JSON-RPC request id
    private String method;
    private Map<String, Object> params;

    // Params ichidan yordamchi getter-lar
    public String getPaymeTransId() {
        Object v = params != null ? params.get("id") : null;
        return v != null ? v.toString() : null;
    }

    public Long getAmount() {
        Object v = params != null ? params.get("amount") : null;
        if (v == null) return null;
        return ((Number) v).longValue();
    }

    public String getMerchantTransId() {
        if (params == null) return null;
        Object account = params.get("account");
        if (account instanceof Map<?, ?> map) {
            Object orderId = map.get("order_id");
            return orderId != null ? orderId.toString() : null;
        }
        return null;
    }

    public Long getCreateTime() {
        Object v = params != null ? params.get("time") : null;
        if (v == null) return null;
        return ((Number) v).longValue();
    }

    public Integer getCancelReason() {
        Object v = params != null ? params.get("reason") : null;
        if (v == null) return null;
        return ((Number) v).intValue();
    }

    public Long getFrom() {
        Object v = params != null ? params.get("from") : null;
        if (v == null) return null;
        return ((Number) v).longValue();
    }

    public Long getTo() {
        Object v = params != null ? params.get("to") : null;
        if (v == null) return null;
        return ((Number) v).longValue();
    }
}
