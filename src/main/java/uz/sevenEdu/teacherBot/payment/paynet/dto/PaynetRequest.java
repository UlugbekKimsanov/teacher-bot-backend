package uz.sevenEdu.teacherBot.payment.paynet.dto;

import lombok.Data;

import java.util.Map;

@Data
public class PaynetRequest {
    private String jsonrpc = "2.0";
    private String id;
    private String method;
    private Map<String, Object> params;

    public String getTransactionId() {
        Object v = params != null ? params.get("transactionId") : null;
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
        if (account != null) return account.toString();
        // fields ichidan
        Object fields = params.get("fields");
        if (fields instanceof Map<?, ?> map) {
            Object orderId = map.get("order_id");
            return orderId != null ? orderId.toString() : null;
        }
        return null;
    }

    public String getDateFrom() {
        Object v = params != null ? params.get("dateFrom") : null;
        return v != null ? v.toString() : null;
    }

    public String getDateTo() {
        Object v = params != null ? params.get("dateTo") : null;
        return v != null ? v.toString() : null;
    }
}
