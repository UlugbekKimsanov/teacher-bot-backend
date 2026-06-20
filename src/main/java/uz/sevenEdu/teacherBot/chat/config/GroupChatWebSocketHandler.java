package uz.sevenEdu.teacherBot.chat.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import uz.sevenEdu.teacherBot.chat.dto.ChatMessageDto;
import uz.sevenEdu.teacherBot.chat.service.ChatService;
import uz.sevenEdu.teacherBot.user.security.JwtUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Kurs guruh chati — bitta kursga a'zo barcha o'quvchi va o'qituvchilar
 * yagona xonada yozishadi. Xona kaliti = courseId.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupChatWebSocketHandler implements WebSocketHandler {

    private final JwtUtil jwtUtil;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    // Room key = courseId -> list of sinks
    private final Map<Long, List<Sinks.Many<ChatMessageDto>>> roomSinks = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        Map<String, String> params = parseQuery(query);

        String token = params.get("token");
        String courseIdStr = params.get("courseId");

        if (token == null || courseIdStr == null || !jwtUtil.isTokenValid(token)) {
            return session.close();
        }

        Long userId = jwtUtil.extractUserId(token);
        String userRole = jwtUtil.extractRole(token);
        Long courseId = Long.parseLong(courseIdStr);
        String senderRole = "TEACHER".equalsIgnoreCase(userRole) ? "teacher" : "user";

        // Faqat kurs a'zolari (o'quvchi yoki o'qituvchi) kira oladi
        return chatService.isMemberOfCourse(courseId, userId)
                .flatMap(isMember -> {
                    if (!isMember) return session.close();
                    return chatService.userFullName(userId)
                            .flatMap(name -> setupSession(session, courseId, userId, senderRole, name));
                });
    }

    private Mono<Void> setupSession(WebSocketSession session, Long courseId, Long senderId,
                                    String senderRole, String senderName) {
        Sinks.Many<ChatMessageDto> sink = Sinks.many().multicast().onBackpressureBuffer();
        roomSinks.computeIfAbsent(courseId, k -> new CopyOnWriteArrayList<>()).add(sink);

        Flux<ChatMessageDto> history = chatService.getGroupHistory(courseId);
        Flux<ChatMessageDto> live = sink.asFlux();

        Flux<WebSocketMessage> outbound = Flux.concat(
                history.collectList().flatMapMany(list -> {
                    ChatMessageDto historyMsg = ChatMessageDto.builder()
                            .type("history")
                            .text(toJson(list))
                            .build();
                    return Flux.just(session.textMessage(toJson(historyMsg)));
                }),
                live.map(msg -> session.textMessage(toJson(msg)))
        );

        Mono<Void> inbound = session.receive()
                .filter(msg -> msg.getType() == WebSocketMessage.Type.TEXT)
                .flatMap(msg -> handleIncoming(msg.getPayloadAsText(), courseId, senderId, senderRole, senderName))
                .then();

        return Mono.zip(session.send(outbound), inbound)
                .doFinally(sig -> {
                    List<Sinks.Many<ChatMessageDto>> sinks = roomSinks.get(courseId);
                    if (sinks != null) {
                        sinks.remove(sink);
                        if (sinks.isEmpty()) roomSinks.remove(courseId);
                    }
                })
                .then();
    }

    private Mono<Void> handleIncoming(String payload, Long courseId, Long senderId,
                                      String senderRole, String senderName) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String text = node.has("text") ? node.get("text").asText() : "";
            String mediaPath = node.hasNonNull("mediaPath") ? node.get("mediaPath").asText() : null;
            String mediaType = node.hasNonNull("mediaType") ? node.get("mediaType").asText() : null;

            if (text.isBlank() && mediaPath == null) return Mono.empty();

            return chatService.saveGroupMessage(courseId, senderId, senderRole, senderName, text, mediaPath, mediaType)
                    .doOnNext(saved -> broadcastToRoom(courseId, saved))
                    .then();
        } catch (Exception e) {
            log.error("Error processing group chat message", e);
            return Mono.empty();
        }
    }

    private void broadcastToRoom(Long courseId, ChatMessageDto msg) {
        List<Sinks.Many<ChatMessageDto>> sinks = roomSinks.get(courseId);
        if (sinks != null) {
            for (Sinks.Many<ChatMessageDto> sink : sinks) {
                sink.tryEmitNext(msg);
            }
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new ConcurrentHashMap<>();
        if (query == null) return map;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }
}
