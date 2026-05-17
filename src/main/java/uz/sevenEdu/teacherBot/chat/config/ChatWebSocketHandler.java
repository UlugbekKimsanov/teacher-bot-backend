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

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final JwtUtil jwtUtil;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

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

        Long userId = jwtUtil.extractUserId(token);
        String userRole = jwtUtil.extractRole(token);
        Long courseId = Long.parseLong(courseIdStr);

        // Determine studentId based on role
        if ("TEACHER".equalsIgnoreCase(userRole)) {
            // Teacher must specify which student's chat to view
            String studentIdStr = params.get("studentId");
            if (studentIdStr == null) {
                return session.close();
            }
            Long studentId = Long.parseLong(studentIdStr);

            return chatService.isTeacherOfCourse(courseId, userId)
                    .flatMap(isTeacher -> {
                        if (!isTeacher) return session.close();
                        return setupSession(session, courseId, studentId, userId, "teacher");
                    });
        } else {
            // Student — studentId is their own userId
            return setupSession(session, courseId, userId, userId, "user");
        }
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
            String text = node.has("text") ? node.get("text").asText() : "";

            if (text.isBlank()) return Mono.empty();

            return chatService.saveMessage(courseId, studentId, senderId, senderRole, text)
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
