package uz.sevenEdu.teacherBot.payment.alif.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AlifAccountVerifyRequest {
    private String account;   // merchantTransId (order_id)
    private String id;        // bePaid transaction ID
    private Long amount;
    private String currency;
    private Map<String, Object> info;
    private Map<String, String> method;  // {"type": "alif_mobi"}
}
