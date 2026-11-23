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
import uz.sevenEdu.teacherBot.common.util.GuestUtil;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.security.JwtUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final JwtUtil jwtUtil;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    /** Bitta xabar matnining eng katta uzunligi (belgi). */
    static final int MAX_TEXT_LENGTH = 4000;

    // Room key = "courseId:studentId" -> list of sinks (student + teacher(s) all share same room)
    private final Map<String, List<Sinks.Many<ChatMessageDto>>> roomSinks = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        Map<String, String> params = parseQuery(query);

        String token = params.get("token");
        String courseIdStr = params.get("courseId");

        if (token == null || courseIdStr == null) {
            return session.close();
        }

        if (!jwtUtil.isTokenValid(token)) {
            return session.close();
        }

        final Long userId = jwtUtil.extractUserId(token);
        final Long courseId;
        final Long studentIdParam;
        try {
            courseId = Long.parseLong(courseIdStr);
            String studentIdStr = params.get("studentId");
            studentIdParam = studentIdStr != null ? Long.parseLong(studentIdStr) : null;
        } catch (NumberFormatException e) {
            return session.close();
        }

        // Mehmon — hamma mehmonlar uchun bitta umumiy akkaunt: shaxsiy chat ochilsa,
        // barcha mehmonlar bir-birining yozishmasini ko'rar edi.
        if (userId == null || GuestUtil.isGuest(userId)) {
            return session.close();
        }

        // Rol DB'dan olinadi — JWT ichidagi rol o'zgargan bo'lishi mumkin
        return chatService.roleOf(userId)
                .map(java.util.Optional::of)
                .defaultIfEmpty(java.util.Optional.empty())
                .flatMap(roleOpt -> {
                    UserRole role = roleOpt.orElse(null);
                    if (role == null || role == UserRole.ADMIN) return session.close();
                    if (role == UserRole.TEACHER) {
                        // Ustoz qaysi o'quvchining chatini ochayotganini ko'rsatishi shart
                        if (studentIdParam == null) return session.close();
                        return chatService.isTeacherOfCourse(courseId, userId)
                                .flatMap(isTeacher -> isTeacher
                                        ? setupSession(session, courseId, studentIdParam, userId, "teacher")
                                        : session.close());
                    }
                    // O'quvchi — studentId o'zining userId'si
                    return setupSession(session, courseId, userId, userId, "user");
                });
    }

    private Mono<Void> setupSession(WebSocketSession session, Long courseId, Long studentId, Long senderId, String senderRole) {
        String roomKey = courseId + ":" + studentId;

        // Create a sink for this connection
        Sinks.Many<ChatMessageDto> sink = Sinks.many().multicast().onBackpressureBuffer();
        roomSinks.computeIfAbsent(roomKey, k -> new CopyOnWriteArrayList<>()).add(sink);

        // Send history then stream live messages
        Flux<ChatMessageDto> history = chatService.getHistory(courseId, studentId);
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

        // Handle incoming messages
        Mono<Void> inbound = session.receive()
                .filter(msg -> msg.getType() == WebSocketMessage.Type.TEXT)
                .flatMap(msg -> handleIncoming(msg.getPayloadAsText(), courseId, studentId, senderId, senderRole, roomKey))
                .then();

        return Mono.zip(session.send(outbound), inbound)
                .doFinally(sig -> {
                    List<Sinks.Many<ChatMessageDto>> sinks = roomSinks.get(roomKey);
                    if (sinks != null) {
                        sinks.remove(sink);
                        if (sinks.isEmpty()) roomSinks.remove(roomKey);
                    }
                })
                .then();
    }

    private Mono<Void> handleIncoming(String payload, Long courseId, Long studentId, Long senderId, String senderRole, String roomKey) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String text = node.hasNonNull("text") ? node.get("text").asText().trim() : "";
            if (text.length() > MAX_TEXT_LENGTH) text = text.substring(0, MAX_TEXT_LENGTH);
            String mediaPath = node.hasNonNull("mediaPath") ? node.get("mediaPath").asText() : null;
            String mediaType = node.hasNonNull("mediaType") ? node.get("mediaType").asText() : null;

            // Matn ham, media ham bo'lmasa — e'tiborsiz
            if (text.isBlank() && mediaPath == null) return Mono.empty();

            return chatService.saveMessage(courseId, studentId, senderId, senderRole, text, mediaPath, mediaType)
                    .doOnNext(saved -> broadcastToRoom(roomKey, saved))
                    .then();
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return Mono.empty();
        }
    }

    private void broadcastToRoom(String roomKey, ChatMessageDto msg) {
        List<Sinks.Many<ChatMessageDto>> sinks = roomSinks.get(roomKey);
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
